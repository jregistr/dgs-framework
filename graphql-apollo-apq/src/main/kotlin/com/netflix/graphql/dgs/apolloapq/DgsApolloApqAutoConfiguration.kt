package com.netflix.graphql.dgs.apolloapq

import com.netflix.graphql.dgs.internal.DefaultDgsQueryExecutor
import graphql.execution.preparsed.PreparsedDocumentProvider
import graphql.execution.preparsed.persisted.PersistedQuerySupport
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import java.util.*

@Configuration
@ComponentScan("com.netflix.graphql.dgs.autoconfig")
open class DgsApolloApqAutoConfiguration {

    @Bean
    open fun apqSupport(reloadSchemaIndicator: DefaultDgsQueryExecutor.ReloadSchemaIndicator): PersistedQuerySupport {
        val cache = DgsPersistedQueryCache()
        return DgsApolloPersistedQuerySupport(cache, reloadSchemaIndicator)
    }
}
