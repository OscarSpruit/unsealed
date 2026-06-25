/*
 * Copyright (c) 2026 Oscar Spruit
 *
 * This file is open source and available under the Apache 2.0 license. See the LICENSE file for more info.
 */

package dev.oscarspruit.unsealed.runtime

import kotlin.reflect.KClass

public interface UnsealedLeafProvider {
    public val root: KClass<*>
    public val leaves: List<KClass<*>>
}
