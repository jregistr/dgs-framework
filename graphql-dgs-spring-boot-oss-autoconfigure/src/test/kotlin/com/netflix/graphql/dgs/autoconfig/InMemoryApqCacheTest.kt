package com.netflix.graphql.dgs.autoconfig

import graphql.ExecutionInput
import graphql.execution.preparsed.PreparsedDocumentEntry
import graphql.execution.preparsed.persisted.PersistedQueryCacheMiss
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
internal class InMemoryApqCacheTest {

    @MockK
    lateinit var mockedExInput: ExecutionInput

    @MockK
    lateinit var entry: PreparsedDocumentEntry

    val hash = "suchhashveryhashywow"
    val query = """
        query Foo {
          bar
        }
    """.trimIndent()

    val onMiss: PersistedQueryCacheMiss = PersistedQueryCacheMiss { entry }

    @Test
    fun lookUpRegisterQuery() {
        every { mockedExInput.query } returns query
        val cache = InMemoryApqCache()
        val resultEntry = cache.getPersistedQueryDocument(hash, mockedExInput, onMiss)
        assertThat(resultEntry).isNotNull
        assertThat(resultEntry).isEqualTo(entry)
    }

    @Test
    fun testLookUpAfterRegister() {
        every { mockedExInput.query } returns query
        val cache = InMemoryApqCache()
        cache.getPersistedQueryDocument(hash, mockedExInput, onMiss)
        // now remove the query and get
        every { mockedExInput.query } returns null
        val registeredVal = cache.getPersistedQueryDocument(hash, mockedExInput, onMiss)
        assertThat(registeredVal).isNotNull
        assertThat(registeredVal).isEqualTo(entry)
    }
}
