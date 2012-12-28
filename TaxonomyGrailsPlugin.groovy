import com.grailsrocks.taxonomy.Taxon
import com.grailsrocks.taxonomy.TaxonomyService

class TaxonomyGrailsPlugin {
    // the plugin version
    def version = "1.2"
    // the version or versions of Grails the plugin is designed for
    def grailsVersion = "1.1.1 > *"
    // the other plugins this plugin depends on
    def dependsOn = [domainClass:'1.1.1 > *']
    def observe = ['domainClass']
    def loadAfter = ['hibernate']
    
    // resources that are excluded from plugin packaging
    def pluginExcludes = [
            "grails-app/views/error.gsp",
            "grails-app/domain/com/grailsrocks/taxonomy/test/Book.groovy"
    ]

    def author = "Marc Palmer"
    def authorEmail = "marc@grailsrocks.com"
    def title = "Taxonomy Plugin"
    def description = '''\\
Add hierarichal tags (taxonomies) to any domain classes.
'''

    // URL to the plugin's documentation
    def documentation = "http://grails.org/Taxonomy+Plugin"

    def doWithWebDescriptor = { xml ->
        // TODO Implement additions to web.xml (optional), this event occurs before 
    }

    def doWithSpring = {
        // TODO Implement runtime spring config (optional)
    }

    def doWithDynamicMethods = { ctx ->
        // Removed initialization of service 
    }

    def applyDynamicMethods(application) {
        def taxoService = application.mainContext.taxonomyService

        application.domainClasses*.clazz.each { c ->
            //println "Checking for taxonomy convention on ${c}"
            if (c.metaClass.hasProperty(c, 'taxonomy') && c.taxonomy) {
                //println "Adding taxonomy methods to ${c}"
                // family can include "taxonomy" arg, string/Taxonomy instance 
                c.metaClass.'static'.findByTaxonomyFamily = { nodeOrPath, Map params = null ->
                    if (!params) {
                        params = [max:1] 
                    } else {
                        params.max = 1
                    }
                    o = taxoService.findObjectsByFamily(delegate, nodeOrPath, params)
                    return o.size() ? o.get(0) : null
                }
                // family can include "taxonomy" arg, string/Taxonomy instance 
                c.metaClass.'static'.findAllByTaxonomyFamily = { nodeOrPath, Map params = null ->
                    taxoService.findObjectsByFamily(delegate, nodeOrPath, params)
                }
                // family can include "taxonomy" arg, string/Taxonomy instance 
                c.metaClass.'static'.findByTaxonomyExact = { nodeOrPath, Map params = null ->
                    if (!params) {
                        params = [max:1] 
                    } else {
                        params.max = 1
                    }
                    def o = taxoService.findObjectsByTaxon(delegate, nodeOrPath, params)
                    return o.size() ? o.get(0) : null
                }
                // family can include "taxonomy" arg, string/Taxonomy instance 
                c.metaClass.'static'.findAllByTaxonomyExact = { nodeOrPath, Map params = null ->
                    taxoService.findObjectsByTaxon(delegate, nodeOrPath, params)
                }
                c.metaClass.addToTaxonomy = { nodeOrPath, taxonomy = null ->
                    def link = taxoService.findLink(delegate, nodeOrPath, taxonomy)
                    if (!link) {
                        if (!(nodeOrPath instanceof Taxon)) {
                            nodeOrPath = taxoService.createTaxonomyPath(nodeOrPath, taxonomy)
                        }
                        taxoService.saveNewLink(delegate, nodeOrPath)
                    }
                }
                c.metaClass.clearTaxonomies = { ->
                    taxoService.removeAllLinks(delegate)
                }
                c.metaClass.getTaxonomies = { ->
                    taxoService.findAllLinks(delegate)*.taxon
                }
                c.metaClass.hasTaxonomy = { nodeOrPath, taxonomy = null ->
                    taxoService.hasLink(delegate, nodeOrPath, taxonomy)
                }
                c.metaClass.removeTaxonomy = { nodeOrPath, taxonomy = null ->
                    taxoService.removeLink(delegate, nodeOrPath, taxonomy)
                }
            }
        }
    }

    def doWithApplicationContext = { applicationContext ->
        def taxoService = applicationContext.taxonomyService
        
        // Make sure global taxo is initialized
        taxoService.init()
        
        applyDynamicMethods(application)
    }

    def onChange = { event ->
        applyDynamicMethods(application)
    }

    def onConfigChange = { event ->
        // TODO Implement code that is executed when the project configuration changes.
        // The event is the same as for 'onChange'.
    }
}
