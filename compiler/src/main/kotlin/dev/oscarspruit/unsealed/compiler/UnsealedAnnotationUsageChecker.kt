/*
 * Copyright (c) 2026 Oscar Spruit
 *
 * This file is open source and available under the Apache 2.0 license. See the LICENSE file for more info.
 */

package dev.oscarspruit.unsealed.compiler

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirRegularClassChecker
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.resolve.toClassSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.types.coneTypeOrNull
import org.jetbrains.kotlin.fir.types.lowerBoundIfFlexible

internal class UnsealedAnnotationUsageChecker : FirRegularClassChecker(MppCheckerKind.Common) {

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirRegularClass) {
        val hasRoot = declaration.annotations.hasAnnotation(UnsealedClassIds.UNSEALED_ROOT, context.session)
        val hasLeaf = declaration.annotations.hasAnnotation(UnsealedClassIds.UNSEALED_LEAF, context.session)

        if (hasRoot) {
            checkRootUsage(declaration)
        }
        if (hasLeaf) {
            checkLeafUsage(declaration)
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkRootUsage(declaration: FirRegularClass) {
        val isExtendable = when (declaration.classKind) {
            ClassKind.INTERFACE -> true
            ClassKind.CLASS -> declaration.status.modality != Modality.FINAL
            else -> false
        }

        if (!isExtendable) {
            reporter.reportOn(
                source = declaration.source,
                factory = UnsealedDiagnostics.ROOT_NOT_EXTENDABLE,
                context = context,
            )
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkLeafUsage(declaration: FirRegularClass) {
        val hasRootSuperType = declaration.superTypeRefs.any { superTypeRef ->
            val classSymbol = superTypeRef.coneTypeOrNull
                ?.lowerBoundIfFlexible()
                ?.toClassSymbol(context.session)

            @OptIn(SymbolInternals::class)
            classSymbol?.annotations?.hasAnnotation(UnsealedClassIds.UNSEALED_ROOT, context.session) == true
        }

        if (!hasRootSuperType) {
            reporter.reportOn(
                source = declaration.source,
                factory = UnsealedDiagnostics.LEAF_WITHOUT_ROOT,
                context = context,
            )
        }
    }
}
