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
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class UnsealedClassIdsTest {

    @Nested
    inner class ClassIdFromFqn {

        @Test
        fun `parses simple top-level class`() {
            val result = UnsealedClassIds.classIdFromFqn("com.example.MyClass")!!

            assertEquals(FqName("com.example"), result.packageFqName)
            assertEquals("MyClass", result.relativeClassName.asString())
        }

        @Test
        fun `parses single-segment package`() {
            val result = UnsealedClassIds.classIdFromFqn("example.MyClass")!!

            assertEquals(FqName("example"), result.packageFqName)
            assertEquals("MyClass", result.relativeClassName.asString())
        }

        @Test
        fun `parses nested class with dollar separator`() {
            val result = UnsealedClassIds.classIdFromFqn($$"com.example.Outer$Inner")!!

            assertEquals(FqName("com.example"), result.packageFqName)
            assertEquals("Outer.Inner", result.relativeClassName.asString())
        }

        @Test
        fun `parses deeply nested class`() {
            val result = UnsealedClassIds.classIdFromFqn($$"com.example.A$B$C")!!

            assertEquals(FqName("com.example"), result.packageFqName)
            assertEquals("A.B.C", result.relativeClassName.asString())
        }

        @Test
        fun `returns null for no-dot input`() {
            assertNull(UnsealedClassIds.classIdFromFqn("NoDots"))
        }

        @Test
        fun `returns null for empty string`() {
            assertNull(UnsealedClassIds.classIdFromFqn(""))
        }
    }

    @Nested
    inner class ClassIdToResourceName {

        @Test
        fun `formats simple top-level class`() {
            val classId = ClassId(FqName("com.example"), Name.identifier("MyClass"))

            assertEquals("com.example.MyClass", UnsealedClassIds.classIdToResourceName(classId))
        }

        @Test
        fun `formats nested class with dollar`() {
            val classId = ClassId(FqName("com.example"), FqName("Outer.Inner"), false)

            assertEquals($$"com.example.Outer$Inner", UnsealedClassIds.classIdToResourceName(classId))
        }

        @Test
        fun `formats deeply nested class`() {
            val classId = ClassId(FqName("com.example"), FqName("A.B.C"), false)

            assertEquals($$"com.example.A$B$C", UnsealedClassIds.classIdToResourceName(classId))
        }

        @Test
        fun `formats class in default package`() {
            val classId = ClassId(FqName.ROOT, Name.identifier("MyClass"))

            assertEquals("MyClass", UnsealedClassIds.classIdToResourceName(classId))
        }
    }

    @Nested
    inner class RoundTrip {

        @Test
        fun `simple class`() {
            val original = ClassId(FqName("com.example.feature"), Name.identifier("SampleLeaf"))

            val resourceName = UnsealedClassIds.classIdToResourceName(original)
            val parsed = UnsealedClassIds.classIdFromFqn(resourceName)

            assertEquals(original, parsed)
        }

        @Test
        fun `nested class`() {
            val original = ClassId(FqName("com.example"), FqName("Outer.Inner"), false)

            val resourceName = UnsealedClassIds.classIdToResourceName(original)
            val parsed = UnsealedClassIds.classIdFromFqn(resourceName)

            assertEquals(original, parsed)
        }

        @Test
        fun `deeply nested class`() {
            val original = ClassId(FqName("com.example"), FqName("A.B.C"), false)

            val resourceName = UnsealedClassIds.classIdToResourceName(original)
            val parsed = UnsealedClassIds.classIdFromFqn(resourceName)

            assertEquals(original, parsed)
        }
    }
}
