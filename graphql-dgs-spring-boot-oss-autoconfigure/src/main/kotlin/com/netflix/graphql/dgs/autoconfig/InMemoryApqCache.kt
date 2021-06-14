package com.netflix.graphql.dgs.autoconfig

import graphql.ExecutionInput
import graphql.execution.preparsed.PreparsedDocumentEntry
import graphql.execution.preparsed.persisted.PersistedQueryCache
import graphql.execution.preparsed.persisted.PersistedQueryCacheMiss
import graphql.execution.preparsed.persisted.PersistedQueryNotFound
import graphql.execution.preparsed.persisted.PersistedQuerySupport
import java.util.concurrent.ConcurrentHashMap

class InMemoryApqCache : PersistedQueryCache {
    private val cache: ConcurrentHashMap<Any, PreparsedDocumentEntry> = ConcurrentHashMap()

    override fun getPersistedQueryDocument(
        persistedQueryId: Any,
        executionInput: ExecutionInput,
        onCacheMiss: PersistedQueryCacheMiss
    ): PreparsedDocumentEntry? {
        val reqQuery = when (val query = executionInput.query) {
            "", PersistedQuerySupport.PERSISTED_QUERY_MARKER -> null
            else -> query
        }

        /*
         Query has a query hash and wants to look up query in cache. E.G, request body json looks like below.
         {"variables": {id: "1"},"extensions":{"persistedQuery":{"version":1,"sha256Hash":"ecf4edb46db40b5132295c0291d62fb65d6759a9eedfa4d5d612dd5ec54a6b38"}}}
          */
        return if (reqQuery == null) {
            cache[persistedQueryId] ?: throw PersistedQueryNotFound(persistedQueryId)
        } else {
            // The query is present, so this query wants to register a new Query.
            // E.G. {"query": "{__typename}", "variables": {id: "1"},"extensions":{"persistedQuery":{"version":1,"sha256Hash":"ecf4edb46db40b5132295c0291d62fb65d6759a9eedfa4d5d612dd5ec54a6b38"}}}
            val validated = onCacheMiss.apply(reqQuery)
            cache[persistedQueryId] = validated
            validated
        }
    }
}
