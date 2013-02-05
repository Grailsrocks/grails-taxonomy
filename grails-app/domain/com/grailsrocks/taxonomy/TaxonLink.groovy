package com.grailsrocks.taxonomy

class TaxonLink {

    String className
    Long objectId

    static belongsTo = [taxon: Taxon]

    static constraints = {
        className(nullable:false, blank:false)
        objectId(nullable:false, blank:false)
    }
}
