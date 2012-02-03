package com.grailsrocks.taxonomy

class Taxon {

    String name
    Date dateCreated
    Date lastUpdated
    
    static mapping = {
        cache true
        name index:'taxon_name_idx'
    }
    
    static belongsTo = [parent:Taxon, scope:Taxonomy]
    
    static constraints = {
        parent(nullable:true)
    }
}
