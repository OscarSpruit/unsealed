/*
 * Copyright (c) 2026 Oscar Spruit
 *
 * This file is open source and available under the Apache 2.0 license. See the LICENSE file for more info.
 */

import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinBasePlugin

plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.ksp) apply false
}

subprojects {
    group = project.property("GROUP") as String
    version = project.property("VERSION_NAME") as String

    plugins.withType<KotlinBasePlugin> {
        extensions.configure<KotlinProjectExtension> {
            jvmToolchain {
                languageVersion.set(libs.versions.jvm.target.map(JavaLanguageVersion::of))
            }
        }
    }
}
