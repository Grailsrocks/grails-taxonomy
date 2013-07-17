package com.grailsrocks.taxonomy

class Taxonomy {

    String name

    static mapping = {
        cache true
        datasources(['DEFAULT', 'readReplica'])
    }

    static constraints = {
        name(nullable:false, size:1..200)
    }
}
