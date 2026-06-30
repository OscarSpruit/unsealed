/*
 * Copyright (c) 2026 Oscar Spruit
 *
 * This file is open source and available under the Apache 2.0 license. See the LICENSE file for more info.
 */

package dev.oscarspruit.unsealed.compiler

import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactoryToRendererMap
import org.jetbrains.kotlin.diagnostics.KtDiagnosticsContainer
import org.jetbrains.kotlin.diagnostics.error0
import org.jetbrains.kotlin.diagnostics.error2
import org.jetbrains.kotlin.diagnostics.rendering.BaseDiagnosticRendererFactory
import org.jetbrains.kotlin.diagnostics.rendering.CommonRenderers.STRING
import org.jetbrains.kotlin.diagnostics.rendering.Renderers.TO_STRING
import org.jetbrains.kotlin.psi.KtElement

@Suppress("ktlint:standard:property-naming")
internal object UnsealedDiagnostics : KtDiagnosticsContainer() {

    val NON_EXHAUSTIVE_WHEN by error2<KtElement, String, String>()

    val ROOT_NOT_EXTENDABLE by error0<KtElement>()

    val LEAF_WITHOUT_ROOT by error0<KtElement>()

    override fun getRendererFactory(): BaseDiagnosticRendererFactory {
        return RendererFactory
    }

    private object RendererFactory : BaseDiagnosticRendererFactory() {
        override val MAP by KtDiagnosticFactoryToRendererMap("Unsealed") { map ->
            map.put(
                factory = NON_EXHAUSTIVE_WHEN,
                message = "''when'' expression must be exhaustive. Add the {0} {1} or an ''else'' branch.",
                rendererA = TO_STRING,
                rendererB = STRING,
            )
            map.put(
                factory = ROOT_NOT_EXTENDABLE,
                message = "''@UnsealedRoot'' should only be applied to interfaces or open/abstract classes.",
            )
            map.put(
                factory = LEAF_WITHOUT_ROOT,
                message = "''@UnsealedLeaf'' class does not extend any ''@UnsealedRoot'' type.",
            )
        }
    }
}
