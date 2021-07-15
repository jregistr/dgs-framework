package com.netflix.graphql.dgs.apolloapq

import com.netflix.graphql.dgs.internal.DefaultDgsQueryExecutor
import graphql.execution.preparsed.persisted.PersistedQuerySupport
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.context.properties.bind.DefaultValue
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration

@ConstructorBinding
@ConfigurationProperties(prefix = "dgs.graphql.apq")
data class DgsApolloApqConfigProps(
    @DefaultValue("true") val enable: Boolean
)

@Configuration
@EnableConfigurationProperties(DgsApolloApqConfigProps::class)
open class DgsApolloApqConfigure {

    @Configuration
    @ConditionalOnProperty("dgs.graphql.apq.enable", havingValue = "true")
    @ComponentScan("com.netflix.graphql.dgs.autoconfig", "com.netflix.graphql.dgs.apolloapq")
    open class DgsApolloApqConfigureCache {
        @Bean
        open fun apqSupport(
            cache: DgsPersistedQueryDocCache,
            reloadSchemaIndicator: DefaultDgsQueryExecutor.ReloadSchemaIndicator
        ): PersistedQuerySupport {
            return DgsApolloPersistedQuerySupport(cache, reloadSchemaIndicator)
        }

        /*@Bean
        open fun apqRawStringCache(): DgsPersistedQueryRawStringCache {
            return DgsPersistedQueryRawStringCache()
        }

        @Bean
        open fun apqDocCache(rawStringCache: DgsPersistedQueryRawStringCache): DgsPersistedQueryDocCache {
            return DgsPersistedQueryDocCache(rawStringCache)
        }*/
    }
}


