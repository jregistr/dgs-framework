package com.netflix.graphql.dgs.apqexample

import com.netflix.graphql.dgs.DgsFederationResolver
import com.netflix.graphql.dgs.DgsQueryExecutor
import com.netflix.graphql.dgs.autoconfig.DgsConfigurationProperties
import com.netflix.graphql.dgs.context.DgsCustomContextBuilder
import com.netflix.graphql.dgs.context.DgsCustomContextBuilderWithRequest
import com.netflix.graphql.dgs.exceptions.DefaultDataFetcherExceptionHandler
import com.netflix.graphql.dgs.internal.DefaultDgsGraphQLContextBuilder
import com.netflix.graphql.dgs.internal.DefaultDgsQueryExecutor
import com.netflix.graphql.dgs.internal.DefaultDgsQueryExecutor.ReloadSchemaIndicator
import com.netflix.graphql.dgs.internal.DgsDataLoaderProvider
import com.netflix.graphql.dgs.internal.DgsSchemaProvider
import com.netflix.graphql.dgs.scalars.UploadScalar
import com.netflix.graphql.mocking.MockProvider
import graphql.execution.*
import graphql.execution.instrumentation.ChainedInstrumentation
import graphql.execution.instrumentation.Instrumentation
import graphql.execution.preparsed.PreparsedDocumentProvider
import graphql.execution.preparsed.persisted.ApolloPersistedQuerySupport
import graphql.execution.preparsed.persisted.InMemoryPersistedQueryCache
import graphql.execution.preparsed.persisted.PersistedQuerySupport
import graphql.schema.GraphQLCodeRegistry
import graphql.schema.GraphQLSchema
import graphql.schema.idl.TypeDefinitionRegistry
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import java.util.*

@Suppress("SpringJavaInjectionPointsAutowiringInspection")
@Configuration
@EnableConfigurationProperties(DgsConfigurationProperties::class)
@ImportAutoConfiguration(classes = [JacksonAutoConfiguration::class])
@ComponentScan("com.netflix.graphql.dgs.apolloapq")
open class ApqExampleConf(private val configProps: DgsConfigurationProperties) {
    @Bean
    open fun dgsQueryExecutor(
        applicationContext: ApplicationContext,
        schema: GraphQLSchema,
        schemaProvider: DgsSchemaProvider,
        dgsDataLoaderProvider: DgsDataLoaderProvider,
        dgsContextBuilder: DefaultDgsGraphQLContextBuilder,
        dataFetcherExceptionHandler: DataFetcherExceptionHandler,
        chainedInstrumentation: ChainedInstrumentation,
        environment: Environment,
        @Qualifier("query") providedQueryExecutionStrategy: Optional<ExecutionStrategy>,
        @Qualifier("mutation") providedMutationExecutionStrategy: Optional<ExecutionStrategy>,
        idProvider: Optional<ExecutionIdProvider>,
        reloadSchemaIndicator: ReloadSchemaIndicator,
        preparsedProvider: Optional<PreparsedDocumentProvider> = Optional.empty()
    ): DgsQueryExecutor {
        val queryExecutionStrategy = providedQueryExecutionStrategy.orElse(AsyncExecutionStrategy(dataFetcherExceptionHandler))
        val mutationExecutionStrategy = providedMutationExecutionStrategy.orElse(AsyncSerialExecutionStrategy(dataFetcherExceptionHandler))
        return DefaultDgsQueryExecutor(
            schema,
            schemaProvider,
            dgsDataLoaderProvider,
            dgsContextBuilder,
            chainedInstrumentation,
            queryExecutionStrategy,
            mutationExecutionStrategy,
            idProvider,
            reloadSchemaIndicator,
            preparsedProvider
        )
    }

    @Bean
    open fun dgsDataLoaderProvider(applicationContext: ApplicationContext): DgsDataLoaderProvider {
        return DgsDataLoaderProvider(applicationContext)
    }

    @Bean
    open fun dgsInstrumentation(instrumentation: Optional<List<Instrumentation>>): ChainedInstrumentation {
        val listOfInstrumentations = instrumentation.orElse(emptyList())
        return ChainedInstrumentation(listOfInstrumentations)
    }

    /**
     * Used by the [DefaultDgsQueryExecutor], it controls if, and when, such executor should reload the schema.
     * This implementation will return either the boolean value of the `dgs.reload` flag
     * or `true` if the `laptop` profile is an active Spring Boot profiles.
     * <p>
     * You can provide a bean of type [ReloadSchemaIndicator] if you want to control when the
     * [DefaultDgsQueryExecutor] should reload the schema.
     *
     * @implSpec the implementation of such bean should be thread-safe.
     */
    @Bean
    @ConditionalOnMissingBean
    open fun defaultReloadSchemaIndicator(environment: Environment): ReloadSchemaIndicator {
        val isLaptopProfile = environment.activeProfiles.contains("laptop")
        val hotReloadSetting = environment.getProperty("dgs.reload", Boolean::class.java, isLaptopProfile)

        return ReloadSchemaIndicator {
            hotReloadSetting
        }
    }

    @Bean
    @ConditionalOnMissingBean
    open fun dgsSchemaProvider(
        applicationContext: ApplicationContext,
        federationResolver: Optional<DgsFederationResolver>,
        dataFetcherExceptionHandler: DataFetcherExceptionHandler,
        existingTypeDefinitionFactory: Optional<TypeDefinitionRegistry>,
        existingCodeRegistry: Optional<GraphQLCodeRegistry>,
        mockProviders: Optional<Set<MockProvider>>
    ): DgsSchemaProvider {
        return DgsSchemaProvider(
            applicationContext,
            federationResolver,
            existingTypeDefinitionFactory,
            mockProviders,
            configProps.schemaLocations
        )
    }

    @Bean
    @ConditionalOnMissingBean
    open fun dataFetcherExceptionHandler(): DataFetcherExceptionHandler {
        return DefaultDataFetcherExceptionHandler()
    }

    @Bean
    @ConditionalOnMissingBean
    open fun schema(dgsSchemaProvider: DgsSchemaProvider): GraphQLSchema {
        return dgsSchemaProvider.schema()
    }

    @Bean
    @ConditionalOnMissingBean
    open fun graphQLContextBuilder(
        dgsCustomContextBuilder: Optional<DgsCustomContextBuilder<*>>,
        dgsCustomContextBuilderWithRequest: Optional<DgsCustomContextBuilderWithRequest<*>>
    ): DefaultDgsGraphQLContextBuilder {
        return DefaultDgsGraphQLContextBuilder(dgsCustomContextBuilder, dgsCustomContextBuilderWithRequest)
    }

    @Bean
    open fun uploadScalar(): UploadScalar {
        return UploadScalar()
    }
}
