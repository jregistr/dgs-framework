package com.netflix.graphql.dgs.autoconfig

import graphql.Assert
import graphql.ExecutionInput
import graphql.execution.preparsed.PreparsedDocumentEntry
import graphql.execution.preparsed.persisted.PersistedQueryCache
import graphql.execution.preparsed.persisted.PersistedQueryNotFound
import graphql.execution.preparsed.persisted.PersistedQuerySupport
import java.util.*
import java.util.function.Function

/*
The InMemoryPersistedQueryCache of a Persisted Query cache is interesting.
It gets from th cache and then looks up on the knownQueries private field. If it's not there,
it throws a Persisted Query not found.

The behavior I was expecting is if the query is present in the request, a cache miss should result in a
query register.

Let's implement a Persisted Query Cache using Spring's concepts.
 */

class MyTestingApolloPersistedQuerySupport(private val cache: PersistedQueryCache) : PersistedQuerySupport(cache) {

    override fun getDocument(
        executionInput: ExecutionInput,
        parseAndValidateFunction: Function<ExecutionInput, PreparsedDocumentEntry>
    ): PreparsedDocumentEntry {
        println("!!! Get Document")
        println(executionInput)

        val queryIdOption = getPersistedQueryId(executionInput)
        Assert.assertNotNull(queryIdOption) { String.format("The class %s MUST return a non null optional query id", this.javaClass.name) }

        return try {
            if (queryIdOption.isPresent) {
                val persistedQueryId = queryIdOption.get()
                return cache.getPersistedQueryDocument(persistedQueryId, executionInput) { queryText: String? ->
                    println("!!!! = APQ Cache Miss")
                    // we have a miss and they gave us nothing - bah!
                    if (queryText == null || queryText.trim { it <= ' ' }.length == 0) {
                        throw PersistedQueryNotFound(persistedQueryId)
                    }
                    val newEI =
                        executionInput.transform { builder: ExecutionInput.Builder ->
                            builder.query(
                                queryText
                            )
                        }
                    parseAndValidateFunction.apply(newEI)
                }
            }
            // ok there is no query id - we assume the query is indeed ready to go as is - ie its not a persisted query
            parseAndValidateFunction.apply(executionInput)
        } catch (e: PersistedQueryNotFound) {
            mkMissingError(e)
        }
    }

    override fun getPersistedQueryId(executionInput: ExecutionInput): Optional<Any> {
        val extensions = executionInput.extensions
        val persistedQuery = extensions["persistedQuery"] as Map<String, Any>?
        if (persistedQuery != null) {
            val sha256Hash = persistedQuery["sha256Hash"]
            return Optional.ofNullable(sha256Hash)
        }
        return Optional.empty()
    }
}
