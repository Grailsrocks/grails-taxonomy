package com.grailsrocks.taxonomy

class TaxonLink {

    Taxon taxon
    String className
    Long objectId
    
    static constraints = {
        className(nullable:false, blank:false)
        objectId(nullable:false, blank:false)
    }
}
