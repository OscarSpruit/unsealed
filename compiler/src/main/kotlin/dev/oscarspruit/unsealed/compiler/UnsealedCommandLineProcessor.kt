/*
 * Copyright (c) 2026 Oscar Spruit
 *
 * This file is open source and available under the Apache 2.0 license. See the LICENSE file for more info.
 */

package dev.oscarspruit.unsealed.compiler

import dev.oscarspruit.unsealed.compiler.Constants.RESOURCE_OUTPUT_DIR
import dev.oscarspruit.unsealed.compiler.Constants.RESOURCE_OUTPUT_DIR_KEY
import org.jetbrains.kotlin.compiler.plugin.AbstractCliOption
import org.jetbrains.kotlin.compiler.plugin.CliOption
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration

@OptIn(ExperimentalCompilerApi::class)
public class UnsealedCommandLineProcessor : CommandLineProcessor {

    override val pluginId: String = Constants.PLUGIN_ID

    override val pluginOptions: Collection<AbstractCliOption> = listOf(
        CliOption(
            optionName = RESOURCE_OUTPUT_DIR,
            valueDescription = "Resource directory for generated classes",
            description = "Output directory for generated resources",
        ),
    )

    override fun processOption(option: AbstractCliOption, value: String, configuration: CompilerConfiguration) {
        when (option.optionName) {
            RESOURCE_OUTPUT_DIR -> configuration.put(RESOURCE_OUTPUT_DIR_KEY, value)
        }
    }
}
