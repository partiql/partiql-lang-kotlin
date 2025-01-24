/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates.  All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at:
 *
 *      http://aws.amazon.com/apache2.0/
 *
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */

package org.partiql.spi.function.builtins.internal

import org.partiql.spi.function.Accumulator
import org.partiql.spi.value.Datum

internal abstract class Accumulator : Accumulator {

    override fun next(args: Array<Datum>) {
        val value = args[0]
        if (value.isNull || value.isMissing) return
        nextValue(value)
    }

    abstract fun nextValue(value: Datum)
}
