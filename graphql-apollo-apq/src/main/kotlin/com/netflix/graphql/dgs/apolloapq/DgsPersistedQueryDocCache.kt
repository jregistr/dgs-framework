package com.netflix.graphql.dgs.apolloapq

import graphql.ExecutionInput
import graphql.execution.preparsed.PreparsedDocumentEntry
import graphql.execution.preparsed.persisted.PersistedQueryCache
import graphql.execution.preparsed.persisted.PersistedQueryCacheMiss
import graphql.execution.preparsed.persisted.PersistedQueryNotFound
import graphql.execution.preparsed.persisted.PersistedQuerySupport
import org.springframework.cache.annotation.CacheConfig
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component

@Component
@CacheConfig(cacheNames = ["PersistedQueries"])
open class DgsPersistedQueryRawStringCache {
    @CacheEvict(allEntries = true)
    open fun evictAll() {
    }

    @Cacheable(key = "#persistedQueryId.toString()", unless = "#result == null")
    open fun getPersistedQueryString(persistedQueryId: Any, executionInput: ExecutionInput): String? {
        println("APQ")
        return when (val query = executionInput.query) {
            null, "", PersistedQuerySupport.PERSISTED_QUERY_MARKER -> null
            else -> query
        }
    }
}

@Component
@CacheConfig(cacheNames = ["DocumentCache"])
open class DgsPersistedQueryDocCache(
    private val rawQueryRawStringCache: DgsPersistedQueryRawStringCache
) : PersistedQueryCache {

    @CacheEvict(allEntries = true)
    open fun evictAll() {
        rawQueryRawStringCache.evictAll()
    }

    @Cacheable(key = "#persistedQueryId.toString()")
    override fun getPersistedQueryDocument(
        persistedQueryId: Any,
        executionInput: ExecutionInput,
        onCacheMiss: PersistedQueryCacheMiss
    ): PreparsedDocumentEntry {
        println("DOC STORE")
        val reqQuery = rawQueryRawStringCache.getPersistedQueryString(persistedQueryId, executionInput)
        if (reqQuery == null) {
            throw PersistedQueryNotFound(persistedQueryId)
        } else {
            return onCacheMiss.apply(reqQuery)
        }
    }
}
