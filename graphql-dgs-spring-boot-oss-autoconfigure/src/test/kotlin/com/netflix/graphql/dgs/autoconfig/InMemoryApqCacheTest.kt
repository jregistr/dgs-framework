package com.netflix.graphql.dgs.autoconfig

import graphql.execution.preparsed.PreparsedDocumentEntry
import graphql.schema.GraphQLSchema
import graphql.schema.idl.SchemaGenerator
import graphql.schema.idl.SchemaParser
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.concurrent.ConcurrentHashMap

class InMemoryApqCacheTest {

    lateinit var schema: GraphQLSchema

    @BeforeEach
    internal fun setUp() {
        val schemaString = """
            type Query {
               a: String!
            }
        """.trimIndent()
//        val parsed = SchemaParser().parse(schemaString)
    }

    @Test
    fun lookUpSuccess() {
        val map = ConcurrentHashMap<String, PreparsedDocumentEntry>()
        val hash = "thisisahash"

//        map[hash] =
    }
}
