/*
 * Copyright (c) 2026 Oscar Spruit
 *
 * This file is open source and available under the Apache 2.0 license. See the LICENSE file for more info.
 */

package dev.oscarspruit.unsealed.compiler

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirExpressionChecker
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.expressions.FirOperation
import org.jetbrains.kotlin.fir.expressions.FirTypeOperatorCall
import org.jetbrains.kotlin.fir.expressions.FirWhenExpression
import org.jetbrains.kotlin.fir.expressions.impl.FirElseIfTrueCondition
import org.jetbrains.kotlin.fir.extensions.PluginServicesInitialization
import org.jetbrains.kotlin.fir.extensions.predicateBasedProvider
import org.jetbrains.kotlin.fir.resolve.toClassSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.coneTypeOrNull
import org.jetbrains.kotlin.fir.types.lowerBoundIfFlexible
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

internal class UnsealedWhenExhaustivenessChecker(
    private val treeRegistry: UnsealedTreeRegistry,
) : FirExpressionChecker<FirWhenExpression>(MppCheckerKind.Common) {

    @OptIn(SymbolInternals::class, PluginServicesInitialization::class)
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirWhenExpression) {
        val unsealedRootClassId = ClassId(
            FqName("dev.oscarspruit.unsealed.runtime"),
            FqName("UnsealedRoot"),
            isLocal = false,
        )
        val subjectType = expression.subjectVariable?.returnTypeRef?.coneTypeOrNull
        val classSymbol = subjectType?.lowerBoundIfFlexible()?.toClassSymbol(context.session)
        val isAnnotated = classSymbol?.annotations?.hasAnnotation(unsealedRootClassId, context.session)

        if (isAnnotated != true) return

        val allLeaves = getAllLeaves(context, classSymbol)

        val missingLeaves = findMissingLeaves(context, expression, allLeaves)
        if (missingLeaves.isNotEmpty()) {
            reportDiagnostic(context, expression, reporter, missingLeaves)
        }
    }

    @OptIn(PluginServicesInitialization::class)
    private fun getAllLeaves(context: CheckerContext, classSymbol: FirClassSymbol<*>): Set<ClassId> {
        val dependencyLeaves = treeRegistry.getLeavesForRoot(classSymbol.classId)

        val predicateProvider = context.session.predicateBasedProvider
        val currentModuleLeaves = predicateProvider
            .getSymbolsByPredicate(UnsealedPredicateRegistrar.PREDICATE)
            .filterIsInstance<FirRegularClassSymbol>()
            .filter { leafSymbol ->
                leafSymbol.resolvedSuperTypes.any { superType ->
                    superType.toClassSymbol(context.session)?.classId == classSymbol.classId
                }
            }
            .map { it.classId }

        return dependencyLeaves + currentModuleLeaves
    }

    private fun findMissingLeaves(
        context: CheckerContext,
        expression: FirWhenExpression,
        allLeaves: Set<ClassId>,
    ): Set<ClassId> {
        val coveredLeaves = mutableSetOf<ClassId>()

        expression.branches.forEach { branch ->
            when (val condition = branch.condition) {
                is FirElseIfTrueCondition -> {
                    // Else branch covers everything
                    return emptySet()
                }

                is FirTypeOperatorCall -> {
                    if (condition.operation == FirOperation.IS) {
                        val checkedType = condition.conversionTypeRef.coneTypeOrNull
                        val checkedClassId = checkedType
                            ?.lowerBoundIfFlexible()
                            ?.toClassSymbol(context.session)
                            ?.classId
                        if (checkedClassId != null) {
                            coveredLeaves.add(checkedClassId)
                        }
                    }
                }
            }
        }

        return allLeaves - coveredLeaves
    }

    private fun reportDiagnostic(
        context: CheckerContext,
        expression: FirWhenExpression,
        reporter: DiagnosticReporter,
        missingLeaves: Set<ClassId>,
    ) {
        val missingNames = missingLeaves.joinToString { "'${it.shortClassName.asString()}'" }
        val branchWord = if (missingLeaves.size == 1) "branch" else "branches"
        reporter.reportOn(
            source = expression.source,
            factory = UnsealedDiagnostics.NON_EXHAUSTIVE_WHEN,
            a = missingNames,
            b = branchWord,
            context = context,
        )
    }
}
