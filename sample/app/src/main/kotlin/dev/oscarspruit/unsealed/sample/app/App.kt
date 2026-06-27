/*
 * Copyright (c) 2026 Oscar Spruit
 *
 * This file is open source and available under the Apache 2.0 license. See the LICENSE file for more info.
 */

package dev.oscarspruit.unsealed.sample.app

import dev.oscarspruit.unsealed.sample.core.SampleRoot
import dev.oscarspruit.unsealed.sample.feature1.SampleLeafOne
import dev.oscarspruit.unsealed.sample.feature2.SampleLeafTwo

public class App {

    public fun run(testSample: SampleRoot) {
        when (testSample) {
            is SampleLeafOne -> println("TestSample1")
            is SampleLeafTwo -> println("TestSample2")
        }
    }
}
