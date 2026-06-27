/*
 * Copyright (c) 2026 Oscar Spruit
 *
 * This file is open source and available under the Apache 2.0 license. See the LICENSE file for more info.
 */

import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation
import org.jetbrains.kotlin.gradle.plugin.KotlinBasePlugin

plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.spotless)
}

spotless {
    format("misc") {
        target("*.gradle", "*.md", ".gitignore")
        leadingTabsToSpaces(4)
        trimTrailingWhitespace()
        endWithNewline()
    }
    kotlin {
        target("**/*.kt")
        ktlint(libs.versions.ktlint.get())
        trimTrailingWhitespace()
        endWithNewline()
    }
    kotlinGradle {
        target("**/*.kts", "*.kts")
        ktlint(libs.versions.ktlint.get())
        trimTrailingWhitespace()
        endWithNewline()
    }
}

subprojects {
    group = project.property("GROUP") as String
    version = project.property("VERSION_NAME") as String

    plugins.withType<KotlinBasePlugin> {
        extensions.configure<KotlinProjectExtension> {
            jvmToolchain {
                languageVersion.set(libs.versions.jvm.target.map(JavaLanguageVersion::of))
            }

            @OptIn(ExperimentalAbiValidation::class)
            abiValidation {
                filters {
                    exclude {
                        byNames.add("dev.oscarspruit.unsealed.sample.**")
                    }
                }
            }

            explicitApi()
        }
    }
}
