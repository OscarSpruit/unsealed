/*
 * Copyright (c) 2026 Oscar Spruit
 *
 * This file is open source and available under the Apache 2.0 license. See the LICENSE file for more info.
 */

plugins {
    alias(libs.plugins.kotlin.jvm)
    `java-gradle-plugin`
}

gradlePlugin {
    plugins {
        create("unsealedPlugin") {
            id = "dev.oscarspruit.unsealed"
            implementationClass = "dev.oscarspruit.unsealed.gradle.plugin.UnsealedGradlePlugin"
        }
    }
}

dependencies {
    compileOnly(gradleApi())
    compileOnly(libs.kotlin.gradle.plugin.api)
}
