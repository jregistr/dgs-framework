package com.netflix.graphql.dgs.apolloapq

import graphql.ExecutionInput
import graphql.GraphqlErrorBuilder
import graphql.execution.preparsed.PreparsedDocumentEntry
import graphql.execution.preparsed.persisted.PersistedQueryCache
import graphql.execution.preparsed.persisted.PersistedQueryCacheMiss
import graphql.execution.preparsed.persisted.PersistedQueryNotFound
import graphql.execution.preparsed.persisted.PersistedQuerySupport
import org.springframework.cache.annotation.CacheConfig
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.CachePut
import org.springframework.cache.annotation.Cacheable
import java.math.BigInteger
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

/*
 * Copyright 2021 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
@CacheConfig(cacheNames = ["Persisted-Queries"])
open class DgsPersistedQueryCache : PersistedQueryCache {
    companion object {
        const val ChecksumDigestType = "SHA-256"
    }

    private val astDocumentsCache: ConcurrentHashMap<Any, PreparsedDocumentEntry> = ConcurrentHashMap()

    @CacheEvict(allEntries = true)
    open fun evictAllEntries() {
        astDocumentsCache.clear()
    }

    @Cacheable(key = "#queryHash.toString()")
    open fun getPersistedQueryString(queryHash: Any, query: String?): String? {
        return query
    }

    @CachePut(key = "#queryHash.toString()")
    open fun registerPersistedQuery(queryHash: Any, query: String): String {
        return query
    }

    override fun getPersistedQueryDocument(
        persistedQueryId: Any,
        executionInput: ExecutionInput,
        onCacheMiss: PersistedQueryCacheMiss
    ): PreparsedDocumentEntry {
        val reqQuery = when (val query = executionInput.query) {
            null, "", PersistedQuerySupport.PERSISTED_QUERY_MARKER -> null
            else -> query
        }

        /*
         Query has a query hash and wants to look up query in cache. E.G, request body json looks like below.
         {"variables": {id: "1"},"extensions":{"persistedQuery":{"version":1,"sha256Hash":"ecf4edb46db40b5132295c0291d62fb65d6759a9eedfa4d5d612dd5ec54a6b38"}}}
          */
        return if (reqQuery == null) {
            val maybeCachedEntry = astDocumentsCache[persistedQueryId]
            if (maybeCachedEntry != null) {
                maybeCachedEntry
            } else {
                val cachedQueryString = getPersistedQueryString(persistedQueryId, reqQuery)
                if (cachedQueryString == null) {
                    throw PersistedQueryNotFound(persistedQueryId)
                } else {
                    val validated = onCacheMiss.apply(cachedQueryString)
                    astDocumentsCache[persistedQueryId] = validated
                    validated
                }
            }
        } else {
            // The query is present, so this query wants to register a new Query.
            // E.G. {"query": "{__typename}", "variables": {id: "1"},"extensions":{"persistedQuery":{"version":1,"sha256Hash":"ecf4edb46db40b5132295c0291d62fb65d6759a9eedfa4d5d612dd5ec54a6b38"}}}
            if (!isValidChecksum(persistedQueryId, reqQuery)) {
                val error = GraphqlErrorBuilder.newError()
                    .message("provided sha does not match query")
                    .errorType(com.netflix.graphql.types.errors.ErrorType.BAD_REQUEST)
                    .build()
                return PreparsedDocumentEntry(error)
            }
            registerPersistedQuery(persistedQueryId, reqQuery)
            val validated = onCacheMiss.apply(reqQuery)
            astDocumentsCache[persistedQueryId] = validated
            validated
        }
    }

    private fun isValidChecksum(providedChecksum: Any, queryString: String): Boolean {
        val calculatedChecksum = String.format(
            "%064x",
            BigInteger(1, MessageDigest.getInstance(ChecksumDigestType).digest(queryString.toByteArray(Charsets.UTF_8)))
        )
        return calculatedChecksum == providedChecksum
    }
}
