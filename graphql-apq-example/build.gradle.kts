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

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
//    implementation(platform(project(":graphql-dgs-platform-dependencies")))
    implementation(project(":graphql-dgs-spring-boot-starter"))
    implementation(project(":graphql-apollo-apq"))
//    implementation(project(":graphql-dgs-extended-scalars"))
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
//    implementation(project(":graphql-dgs-spring-boot-oss-autoconfigure"))

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
