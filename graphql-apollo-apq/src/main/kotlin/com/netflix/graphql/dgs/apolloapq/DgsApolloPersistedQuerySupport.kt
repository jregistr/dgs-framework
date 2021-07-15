package com.netflix.graphql.dgs.apolloapq

import com.netflix.graphql.dgs.internal.DefaultDgsQueryExecutor
import graphql.ExecutionInput
import graphql.execution.preparsed.PreparsedDocumentEntry
import graphql.execution.preparsed.persisted.ApolloPersistedQuerySupport
import java.util.function.Function

class DgsApolloPersistedQuerySupport(
    private val cache: DgsPersistedQueryDocCache,
    private val reloadIndicator: DefaultDgsQueryExecutor.ReloadSchemaIndicator
) : ApolloPersistedQuerySupport(cache) {
    override fun getDocument(
        executionInput: ExecutionInput?,
        parseAndValidateFunction: Function<ExecutionInput, PreparsedDocumentEntry>?
    ): PreparsedDocumentEntry {
        println("Called!")
        if (reloadIndicator.reloadSchema()) {
            cache.evictAll()
        }
        return super.getDocument(executionInput, parseAndValidateFunction)
    }
}
