/*
 * Copyright (c) 2026 Oscar Spruit
 *
 * This file is open source and available under the Apache 2.0 license. See the LICENSE file for more info.
 */

package dev.oscarspruit.unsealed.compiler

import org.jetbrains.kotlin.name.ClassId

public object UnsealedBranchAnalysis {

    public fun findMissingBranches(
        allLeaves: Set<ClassId>,
        coveredBranches: Set<ClassId>,
        hasElseBranch: Boolean,
    ): Set<ClassId> {
        if (hasElseBranch) return emptySet()
        return allLeaves - coveredBranches
    }
}
