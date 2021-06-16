package com.netflix.graphql.dgs.apqexample

import com.netflix.graphql.dgs.DgsQueryExecutor
import com.netflix.graphql.dgs.internal.DgsSchemaProvider
import com.netflix.graphql.dgs.mvc.DgsRestController
import com.netflix.graphql.dgs.mvc.DgsRestSchemaJsonController
import com.netflix.graphql.dgs.webmvc.autoconfigure.DgsWebMvcConfigurationProperties
import com.netflix.graphql.dgs.webmvc.autoconfigure.GraphiQLConfigurer
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.web.servlet.DispatcherServlet

@Configuration
@ConditionalOnWebApplication
@EnableConfigurationProperties(DgsWebMvcConfigurationProperties::class)
open class ApqExampleWebConf {

    @Bean
    open fun dgsRestController(dgsQueryExecutor: DgsQueryExecutor): DgsRestController {
        return DgsRestController(dgsQueryExecutor)
    }

    @Configuration
    @ConditionalOnClass(DispatcherServlet::class)
//    @ConditionalOnProperty(name = ["dgs.graphql.graphiql.enabled"], havingValue = "true", matchIfMissing = true)
    @Import(GraphiQLConfigurer::class)
    open class DgsGraphiQLConfiguration

    @Configuration
//    @ConditionalOnProperty(name = ["dgs.graphql.schema-json.enabled"], havingValue = "true", matchIfMissing = true)
    open class DgsWebMvcSchemaJsonConfiguration {
        @Bean
        open fun dgsRestSchemaJsonController(dgsSchemaProvider: DgsSchemaProvider): DgsRestSchemaJsonController {
            return DgsRestSchemaJsonController(dgsSchemaProvider)
        }
    }
}
