/*
 * Copyright (c) 2026 Oscar Spruit
 *
 * This file is open source and available under the Apache 2.0 license. See the LICENSE file for more info.
 */

package dev.oscarspruit.unsealed.compiler

import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class UnsealedBranchAnalysisTest {

    private val leafA = classId("com.example", "LeafA")
    private val leafB = classId("com.example", "LeafB")
    private val leafC = classId("com.example", "LeafC")

    @Nested
    inner class FindMissingBranches {

        @Test
        fun `returns all leaves when none are covered`() {
            val allLeaves = setOf(leafA, leafB, leafC)

            val result = UnsealedBranchAnalysis.findMissingBranches(allLeaves, emptySet(), false)

            assertEquals(setOf(leafA, leafB, leafC), result)
        }

        @Test
        fun `returns uncovered leaves`() {
            val allLeaves = setOf(leafA, leafB, leafC)
            val covered = setOf(leafA)

            val result = UnsealedBranchAnalysis.findMissingBranches(allLeaves, covered, false)

            assertEquals(setOf(leafB, leafC), result)
        }

        @Test
        fun `returns empty set when all leaves are covered`() {
            val allLeaves = setOf(leafA, leafB)
            val covered = setOf(leafA, leafB)

            val result = UnsealedBranchAnalysis.findMissingBranches(allLeaves, covered, false)

            assertTrue(result.isEmpty())
        }

        @Test
        fun `returns empty set when else branch is present`() {
            val allLeaves = setOf(leafA, leafB, leafC)

            val result = UnsealedBranchAnalysis.findMissingBranches(allLeaves, emptySet(), true)

            assertTrue(result.isEmpty())
        }

        @Test
        fun `returns empty set when else branch is present even with partial coverage`() {
            val allLeaves = setOf(leafA, leafB)
            val covered = setOf(leafA)

            val result = UnsealedBranchAnalysis.findMissingBranches(allLeaves, covered, true)

            assertTrue(result.isEmpty())
        }

        @Test
        fun `ignores extra covered branches not in allLeaves`() {
            val allLeaves = setOf(leafA)
            val covered = setOf(leafA, leafB)

            val result = UnsealedBranchAnalysis.findMissingBranches(allLeaves, covered, false)

            assertTrue(result.isEmpty())
        }

        @Test
        fun `returns empty set when allLeaves is empty`() {
            val result = UnsealedBranchAnalysis.findMissingBranches(emptySet(), emptySet(), false)

            assertTrue(result.isEmpty())
        }
    }

    private fun classId(pkg: String, name: String): ClassId =
        ClassId(FqName(pkg), Name.identifier(name))
}
