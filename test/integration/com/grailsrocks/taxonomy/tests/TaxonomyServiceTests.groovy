package com.grailsrocks.taxonomy.tests

import grails.test.*
import com.grailsrocks.taxonomy.*
import com.grailsrocks.taxonomy.test.*

class TaxonomyServiceTests extends GrailsUnitTestCase {
    
    def svc
    
    protected void setUp() {
        super.setUp()

        svc = new TaxonomyService()
        svc.init()
    }

    protected void tearDown() {
        super.tearDown()
    }

    void testGlobalTaxonomyInit() { 
        def globalTaxonomy = Taxonomy.findByName(TaxonomyService.GLOBAL_TAXONOMY_NAME)       
        assertNotNull globalTaxonomy
        assertEquals svc.globalTaxonomy, globalTaxonomy
    }

    void testAddGlobalTaxonomy() {        
        def book = new Book(title:'Reality Check')
        assert book.save()
        
        book.addToTaxonomy(['Non-fiction', 'Business', 'Entrepreneurial'])
        
        // Check the taxon hierarchy was created
        assertEquals 3, Taxon.count()
        def nf = Taxon.findByName('Non-fiction')
        assertNotNull nf
        assertEquals svc.globalTaxonomy, nf.scope
        def bi = Taxon.findByName('Business')
        assertNotNull bi
        assertEquals svc.globalTaxonomy, bi.scope
        def en = Taxon.findByName('Entrepreneurial')
        assertNotNull en
        assertEquals svc.globalTaxonomy, en.scope
        
        // Check the parents of the Taxons are correct
        assertEquals nf, bi.parent
        assertEquals bi, en.parent
        
        // Check the taxon link was created between object and taxon
        def link = TaxonLink.findByObjectIdAndClassName(book.id, book.class.name)
        assertNotNull link
        assertEquals en, link.taxon
        
        // Check that adding the same taxonomy again does not create new Taxon(s) or links
        book.addToTaxonomy(['Non-fiction', 'Business', 'Entrepreneurial'])
        assertEquals 3, Taxon.count()
        def links = TaxonLink.findAllByObjectIdAndClassName(book.id, book.class.name)
        assertEquals 1, links.size()
        
        // Check that adding another taxonomy on the same path creates a new link
        book.addToTaxonomy(['Non-fiction', 'Business'])
        assertEquals 3, Taxon.count()
        links = TaxonLink.findAllByObjectIdAndClassName(book.id, book.class.name)
        assertEquals 2, links.size()
        
        // One end point should be entrepreneurial
        assertNotNull links.find { l -> l.taxon == en }
        // One end point should be business
        assertNotNull links.find { l -> l.taxon == bi }
    }

    void testParamsUsageWithCriteria() {
        def book1 = new Book(title:'Reality Check')
        assert book1.save()
        
        book1.addToTaxonomy(['Non-fiction', 'Business'])
        book1.addToTaxonomy(['Collection', 'Business'])
        book1.addToTaxonomy(['Collection', 'Blogging'])
        book1.addToTaxonomy(['Blogs', 'Business'])
        
        // Test the control case
        def taxons = svc.findTaxonsByParentAndCriteria('Collection', null) {
            ilike('name', 'b%')
        }
        assertEquals 2, taxons.size()

        // Test max
        taxons = svc.findTaxonsByParentAndCriteria('Collection', [max:1]) {
            ilike('name', 'b%')
        }
        assertEquals 1, taxons.size()

        // Test offset
        taxons = svc.findTaxonsByParentAndCriteria('Collection', [max:1, offset:0, sort:'name']) {
            ilike('name', 'b%')
        }
        assertEquals 1, taxons.size()
        assertEquals 'Blogging', taxons[0].name

        taxons = svc.findTaxonsByParentAndCriteria('Collection', [max:1, offset:1, sort:'name']) {
            ilike('name', 'b%')
        }
        assertEquals 1, taxons.size()
        assertEquals 'Business', taxons[0].name
    }

    void testFindTaxonsInFamily() {
        def book1 = new Book(title:'Reality Check')
        assert book1.save()
        
        /* Create this:
        
           Non-fiction
               |--- Business
           Collection
               |--- Business
               |--- Blogging
                       |--- Famous
           Blogs
               |--- Business

           ... and in "translations" taxonomy....
           
           Canada
               |--- French
               |--- English
        */
        book1.addToTaxonomy(['Non-fiction', 'Business'])
        book1.addToTaxonomy(['Collection', 'Business'])
        book1.addToTaxonomy(['Collection', 'Blogging'])
        book1.addToTaxonomy(['Collection', 'Blogging', 'Famous'])
        book1.addToTaxonomy(['Blogs', 'Business'])
        
        book1.addToTaxonomy(['Canada', 'French'], 'translations')
        book1.addToTaxonomy(['Canada', 'English'], 'translations')

        // Test full graph of specific taxonomies
        def taxons = svc.findTaxonsDescendedFrom(null)
        assertEquals 8, taxons.size()
        taxons = svc.findTaxonsDescendedFrom(null, [taxonomy:'translations'])
        assertEquals 3, taxons.size()

        def collectionTaxon = svc.resolveTaxon('Collection')
        taxons = svc.findTaxonsDescendedFrom('Collection')
        assertEquals 3, taxons.size()
        def t = taxons.find { it.name == 'Business' }
        assertNotNull t
        assertEquals collectionTaxon.id, t.parent.id
        
        println "Taxons found for 'collection' are: ${taxons*.dump()}"
        
        def blogt = taxons.find { it.name == 'Blogging' }
        assertNotNull blogt
        assertEquals collectionTaxon.id, blogt.parent.id

        def famt = taxons.find { it.name == 'Famous' }
        assertNotNull famt
        assertEquals blogt.id, famt.parent.id
    }

    void testFindObjectsInFamily() {
        def book1 = new Book(title:'Reality Check')
        assert book1.save()
        def book2 = new Book(title:'Out of Our Minds')
        assert book2.save()
        
        /* Create this:
        
           Non-fiction
               |--- Business
           Collection
               |--- Business
               |--- Blogging
                       |--- Famous
           Blogs
               |--- Business

           ... and in "translations" taxonomy....
           
           Canada
               |--- French
               |--- English
        */

        book1.addToTaxonomy(['Non-fiction', 'Business'])
        book1.addToTaxonomy(['Collection', 'Business'])
        book1.addToTaxonomy(['Collection', 'Blogging'])
        book1.addToTaxonomy(['Collection', 'Blogging', 'Famous'])
        book1.addToTaxonomy(['Blogs', 'Business'])
        
        book1.addToTaxonomy(['Canada', 'French'], 'translations')
        book1.addToTaxonomy(['Canada', 'English'], 'translations')

        book2.addToTaxonomy(['Collection', 'Blogging', 'Famous'])
        book2.addToTaxonomy(['Non-fiction', 'Business'])

        // This should find both books
        def collObjs = Book.findAllByTaxonomyFamily('Collection')
        assertEquals 2, collObjs.size()
        assertNotNull collObjs.contains(book1)
        assertNotNull collObjs.contains(book2)
        
        // This should find one book
        collObjs = Book.findAllByTaxonomyFamily(['Collection', 'Business'])
        assertEquals 1, collObjs.size()
        assertNotNull collObjs.contains(book1)
        
        // This should find both books
        collObjs = Book.findAllByTaxonomyFamily(['Collection', 'Blogging', 'Famous'])
        assertEquals 2, collObjs.size()
        assertNotNull collObjs.contains(book1)
        assertNotNull collObjs.contains(book2)
        
        // This should find both books
        collObjs = Book.findAllByTaxonomyFamily(['Non-fiction', 'Business'])
        assertEquals 2, collObjs.size()
        assertNotNull collObjs.contains(book1)
        assertNotNull collObjs.contains(book2)
    }
    
    void testFindTaxonsByParent() {
        def book1 = new Book(title:'Reality Check')
        assert book1.save()
        
        book1.addToTaxonomy(['Non-fiction', 'Business'])
        book1.addToTaxonomy(['Collection', 'Business'])
        book1.addToTaxonomy(['Collection', 'Blogging'])
        book1.addToTaxonomy(['Blogs', 'Business'])
        
        book1.addToTaxonomy(['Canada', 'French'], 'translations')
        book1.addToTaxonomy(['Canada', 'English'], 'translations')
        
        // Test the null parent case
        def taxons = svc.findTaxonsByParent(null)
        
        assertEquals 3, taxons.size() // Only 3 are in the default taxonomy
        assertNotNull taxons.find { t -> t.name == 'Non-fiction' }
        assertNotNull taxons.find { t -> t.name == 'Collection' }
        assertNotNull taxons.find { t -> t.name == 'Blogs' }

        // Test the non-null parent case
        taxons = svc.findTaxonsByParent('Collection')
        
        assertEquals 2, taxons.size() // Only 2 are in the default taxonomy
        assertNotNull taxons.find { t -> t.name == 'Business' }
        assertNotNull taxons.find { t -> t.name == 'Blogging' }

        // Test the null parent case with criteria
        taxons = svc.findTaxonsByParentAndCriteria(null, [:]) {
            ilike('name', '%ion')
        }   
        
        assertEquals 2, taxons.size()
        assertNotNull taxons.find { t -> t.name == 'Non-fiction' }
        assertNotNull taxons.find { t -> t.name == 'Collection' }

        svc.dumpTaxonomy()
        
        // Test the non-null parent case with criteria
        taxons = svc.findTaxonsByParentAndCriteria('Collection', [:]) {
            ilike('name', 'bus%')
        }   
        
        assertEquals 1, taxons.size()
        assertNotNull taxons.find { t -> t.name == 'Business' }

        // Test the non-null parent case
        taxons = svc.findTaxonsByParentAndCriteria('Collection', [:]) {
            ilike('name', 'b%')
        }   
        
        assertEquals 2, taxons.size()
        assertNotNull taxons.find { t -> t.name == 'Business' }
        assertNotNull taxons.find { t -> t.name == 'Blogging' }

        // Test the non-null parent case with alternative taxonomy
        taxons = svc.findTaxonsByParentAndCriteria('Canada', [taxonomy:'translations']) {
            ilike('name', '%ish')
        }   
        
        assertEquals 1, taxons.size()
        assertNotNull taxons.find { t -> t.name == 'English' }
    }

    void testFindByTaxonExact() {        
        def book1 = new Book(title:'Reality Check')
        assert book1.save()
        def book2 = new Book(title:'Tribes')
        assert book2.save()
        
        book1.addToTaxonomy(['Non-fiction', 'Web 2.0', 'Entrepreneurial'])
        book2.addToTaxonomy(['Non-fiction', 'Web 2.0', 'Entrepreneurial'])

        book1.addToTaxonomy(['Non-fiction', 'Business', 'Internet'])

        book1.addToTaxonomy(['Technology', 'Blogger'], 'author_category')
        
        def book
        // This must work
        book = Book.findByTaxonomyExact(['Non-fiction', 'Web 2.0', 'Entrepreneurial'])
        assertNotNull book

        // This must result in two
        def books = Book.findAllByTaxonomyExact(['Non-fiction', 'Web 2.0', 'Entrepreneurial'])
        assertEquals 2, books.size()
        assertTrue books.contains(book1)
        assertTrue books.contains(book2)
        
        // These must fail
        book = Book.findByTaxonomyExact(['Non-fiction', 'Web 2.0'])
        assertNull book
        book = Book.findByTaxonomyExact(['Non-fiction'])
        assertNull book
        book = Book.findByTaxonomyExact(['Nonsense'])
        assertNull book

        // Now test non-global taxonomy
        // This must be null
        book = Book.findByTaxonomyExact(['Non-fiction', 'Web 2.0', 'Entrepreneurial'], [taxonomy:'author_category'])
        assertNull book
        book = Book.findByTaxonomyExact(['Technology', 'Blogger']) // searches global taxon, not there
        assertNull book

        // This must NOT be null
        book = Book.findByTaxonomyExact(['Technology', 'Blogger'], [taxonomy:'author_category'])
        assertNotNull book

        // This must NOT be null
        book = Book.findByTaxonomyExact(['Technology', 'Blogger'], [taxonomy:'author_category'])
        assertNotNull book
    }



    void testGetAndClearTaxonomies() {        
        def book1 = new Book(title:'Reality Check')
        assert book1.save()
        def book2 = new Book(title:'Tribes')
        assert book2.save()
        
        book1.addToTaxonomy(['Non-fiction', 'Web 2.0', 'Entrepreneurial'])
        book2.addToTaxonomy(['Non-fiction', 'Web 2.0'])

        assertEquals 1, book1.getTaxonomies().size()
        assertEquals 1, book2.getTaxonomies().size()

        // remove all
        book1.clearTaxonomies() 
        assertEquals 0, book1.getTaxonomies().size()
        assertEquals 1, book2.getTaxonomies().size()

        // remove all
        book2.clearTaxonomies() 
        assertEquals 0, book2.getTaxonomies().size()
    }
    
    void testAddHasRemoveTaxons() {        
        def book1 = new Book(title:'Reality Check')
        assert book1.save()
        def book2 = new Book(title:'Tribes')
        assert book2.save()
        
        book1.addToTaxonomy(['Non-fiction', 'Web 2.0', 'Entrepreneurial'])
        book2.addToTaxonomy(['Non-fiction', 'Web 2.0', 'Entrepreneurial'])

        assertEquals svc.resolveTaxon(['Non-fiction', 'Web 2.0', 'Entrepreneurial']).ident(), 
            book1.getTaxonomies()[0].ident()
        assertTrue book1.hasTaxonomy(['Non-fiction', 'Web 2.0', 'Entrepreneurial'])
        assertFalse book1.hasTaxonomy(['Non-fiction', 'Web 2.0', 'BLABLABLA'])

        // remove all
        assertEquals 1, book1.getTaxonomies().size()
        book1.clearTaxonomies() 
        assertEquals 0, book1.getTaxonomies().size()
        assertFalse book1.hasTaxonomy(['Non-fiction', 'Web 2.0', 'Entrepreneurial'])

        // but its still on book2 right?
        assertEquals svc.resolveTaxon(['Non-fiction', 'Web 2.0', 'Entrepreneurial']).ident(), 
            book2.getTaxonomies()[0].ident()
    }
}
