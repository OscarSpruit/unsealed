/*
 * Copyright (c) 2026 Oscar Spruit
 *
 * This file is open source and available under the Apache 2.0 license. See the LICENSE file for more info.
 */

package dev.oscarspruit.unsealed.compiler

import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactoryToRendererMap
import org.jetbrains.kotlin.diagnostics.KtDiagnosticRenderers.TO_STRING
import org.jetbrains.kotlin.diagnostics.KtDiagnosticsContainer
import org.jetbrains.kotlin.diagnostics.error1
import org.jetbrains.kotlin.diagnostics.rendering.BaseDiagnosticRendererFactory

@Suppress("ktlint:standard:property-naming")
internal object UnsealedDiagnostics : KtDiagnosticsContainer() {

    val NON_EXHAUSTIVE_WHEN by error1<PsiElement, String>()

    override fun getRendererFactory(): BaseDiagnosticRendererFactory {
        return RendererFactory
    }

    private object RendererFactory : BaseDiagnosticRendererFactory() {
        override val MAP by KtDiagnosticFactoryToRendererMap("Unsealed") { map ->
            map.put(
                factory = NON_EXHAUSTIVE_WHEN,
                message = "'when' expression on @UnsealedRoot type must be exhaustive. Missing branches: {0}",
                rendererA = TO_STRING,
            )
        }
    }
}
