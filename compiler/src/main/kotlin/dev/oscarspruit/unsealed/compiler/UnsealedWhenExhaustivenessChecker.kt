/*
 * Copyright (c) 2026 Oscar Spruit
 *
 * This file is open source and available under the Apache 2.0 license. See the LICENSE file for more info.
 */

package dev.oscarspruit.unsealed.compiler

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirExpressionChecker
import org.jetbrains.kotlin.fir.expressions.FirWhenExpression

internal object UnsealedWhenExhaustivenessChecker : FirExpressionChecker<FirWhenExpression>(MppCheckerKind.Common) {

    context(context: CheckerContext, reporter: DiagnosticReporter)

    override fun check(expression: FirWhenExpression) {
        TODO("Not yet implemented")
    }
}
