/*
 * Copyright (c) 2026 Oscar Spruit
 *
 * This file is open source and available under the Apache 2.0 license. See the LICENSE file for more info.
 */

package dev.oscarspruit.unsealed.compiler

import dev.oscarspruit.unsealed.compiler.Constants.RESOURCE_OUTPUT_DIR_KEY
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.jvm.config.JvmClasspathRoot
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrarAdapter

@OptIn(ExperimentalCompilerApi::class)
public class UnsealedCompilerPluginRegistrar : CompilerPluginRegistrar() {

    override val pluginId: String = Constants.PLUGIN_ID

    override val supportsK2: Boolean = true

    override fun ExtensionStorage.registerExtensions(
        configuration: CompilerConfiguration
    ) {
        val classpathEntries = configuration.getList(CLIConfigurationKeys.CONTENT_ROOTS)
            .filterIsInstance<JvmClasspathRoot>()
            .map { it.file }
        FirExtensionRegistrarAdapter.registerExtension(UnsealedFirExtensionRegistrar(classpathEntries))

        val resourceOutputDir = configuration[RESOURCE_OUTPUT_DIR_KEY]
            ?: error("Resource output directory not specified")
        IrGenerationExtension.registerExtension(UnsealedIrGenerationExtension(resourceOutputDir))
    }
}
