/*
 * Copyright (c) 2026 Oscar Spruit
 *
 * This file is open source and available under the Apache 2.0 license. See the LICENSE file for more info.
 */

package dev.oscarspruit.unsealed.sample

import dev.oscarspruit.unsealed.runtime.UnsealedLeaf
import dev.oscarspruit.unsealed.runtime.UnsealedRoot

@UnsealedRoot
public interface SampleRoot

@UnsealedLeaf
public interface SampleLeafOne : SampleRoot

@UnsealedLeaf
public interface SampleLeafTwo : SampleRoot
