/*
 * Copyright (c) 2026 Oscar Spruit
 *
 * This file is open source and available under the Apache 2.0 license. See the LICENSE file for more info.
 */

plugins {
    kotlin("jvm")
    alias(libs.plugins.jetbrains.intellij)
}

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

intellijPlatform {
    buildSearchableOptions = false

    pluginConfiguration {
        ideaVersion {
            sinceBuild = "261"
        }
    }
}

dependencies {
    implementation(project(":compiler"))

    intellijPlatform {
        intellijIdeaUltimate("2026.1.3")
        bundledPlugin("com.intellij.java")
        bundledPlugin("org.jetbrains.kotlin")
    }
}
