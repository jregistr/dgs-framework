package com.netflix.graphql.dgs.apolloapq

import graphql.ExecutionInput
import graphql.execution.preparsed.PreparsedDocumentEntry
import graphql.execution.preparsed.persisted.PersistedQueryCacheMiss
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.spyk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.cache.Cache
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.concurrent.ConcurrentMapCache
import org.springframework.cache.concurrent.ConcurrentMapCacheManager
import org.springframework.cache.support.SimpleValueWrapper
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.ConcurrentHashMap

@ExtendWith(MockKExtension::class, SpringExtension::class)
@ContextConfiguration
internal class DgsPersistedQueryCacheTest {
    @Configuration
    @EnableCaching
    open class TestConfig {
        @Bean
        open fun cacheManager(): CacheManager {
            return ConcurrentMapCacheManager()
        }
    }

    @MockK
    lateinit var mockedExInput: ExecutionInput

    @MockK
    lateinit var docEntry: PreparsedDocumentEntry

    lateinit var apqCache: DgsPersistedQueryCache

    @Autowired
    lateinit var manager: CacheManager

    @BeforeEach
    internal fun setUp() {
        apqCache = spyk(DgsPersistedQueryCache())
    }

    private val onCacheMiss = spyk(PersistedQueryCacheMiss { docEntry })

    private val hash = "ecf4edb46db40b5132295c0291d62fb65d6759a9eedfa4d5d612dd5ec54a6b38"
    private val query = "{__typename}"
    private val badHash = "0b57e91c758f9ed42619024e452801f3d9a768b0bd9e4ebbf8168875e1b2bee1"

    private fun getPrivateDocCache(): ConcurrentHashMap<Any, PreparsedDocumentEntry> {
        val privField = DgsPersistedQueryCache::class.java.getDeclaredField("astDocumentsCache")
        privField.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        return privField.get(apqCache) as ConcurrentHashMap<Any, PreparsedDocumentEntry>
    }

    @Test
    fun `test that it registers a new query into the cache`() {
        every { mockedExInput.query } returns query
        val resultEntry = apqCache.getPersistedQueryDocument(hash, mockedExInput, onCacheMiss)
        assertThat(resultEntry).isNotNull
        assertThat(resultEntry).isEqualTo(docEntry)

        val docCache = getPrivateDocCache()
        val cachedEntry = docCache[hash]
        assertThat(cachedEntry).isNotNull
        assertThat(cachedEntry).isEqualTo(docEntry)
    }

    @Test
    fun `test that it uses cached request after a register`() {
        every { mockedExInput.query } returns query
        apqCache.getPersistedQueryDocument(hash, mockedExInput, onCacheMiss)

        every { mockedExInput.query } returns null
        val resultEntry = apqCache.getPersistedQueryDocument(hash, mockedExInput, onCacheMiss)
        verify(exactly = 1){ apqCache.registerPersistedQuery(any(), any()) }
        assertThat(resultEntry).isNotNull
        verify(exactly = 0) { apqCache.getPersistedQueryString(any(), any()) }
    }

    @Test
    fun `test that it returns a GraphQL error if the provided hash does not match query's sha-256 checksum`() {
        every { mockedExInput.query } returns query
        val res = apqCache.getPersistedQueryDocument(badHash, mockedExInput, onCacheMiss)
        assertThat(res).isNotNull
        assertThat(res.hasErrors()).isTrue
        val error = res.errors[0]
        assertThat(error.message).contains("does not match query")
    }
}
