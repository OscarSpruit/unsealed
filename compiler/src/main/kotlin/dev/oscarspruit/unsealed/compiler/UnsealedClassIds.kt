/*
 * Copyright (c) 2026 Oscar Spruit
 *
 * This file is open source and available under the Apache 2.0 license. See the LICENSE file for more info.
 */

package dev.oscarspruit.unsealed.compiler

import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

public object UnsealedClassIds {

    private val RUNTIME_PACKAGE: FqName = FqName("dev.oscarspruit.unsealed.runtime")

    public val UNSEALED_ROOT: ClassId = ClassId(RUNTIME_PACKAGE, Name.identifier("UnsealedRoot"))

    public val UNSEALED_LEAF: ClassId = ClassId(RUNTIME_PACKAGE, Name.identifier("UnsealedLeaf"))

    internal fun classIdToResourceName(classId: ClassId): String {
        val pkg = classId.packageFqName.asString()
        val cls = classId.relativeClassName.asString().replace('.', '$')
        return if (pkg.isEmpty()) cls else "$pkg.$cls"
    }

    public fun classIdFromFqn(fqn: String): ClassId? {
        val dollarIndex = fqn.indexOf('$')
        val lastDot = if (dollarIndex >= 0) {
            fqn.lastIndexOf('.', dollarIndex)
        } else {
            fqn.lastIndexOf('.')
        }
        if (lastDot < 0) return null
        val pkg = fqn.substring(0, lastDot)
        val cls = fqn.substring(lastDot + 1).replace('$', '.')
        return ClassId(FqName(pkg), FqName(cls), false)
    }
}
