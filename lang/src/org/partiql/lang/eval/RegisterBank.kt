/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates.  All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 *  You may not use this file except in compliance with the License.
 * A copy of the License is located at:
 *
 *      http://aws.amazon.com/apache2.0/
 *
 *  or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 *  language governing permissions and limitations under the License.
 */

package org.partiql.lang.eval

/**
 * Represents a set of internal compiler slots for intermediate execution states.
 */
class RegisterBank(size: Int) {

    private val bank: Array<Register> = Array(size) { Register.EMPTY }

    /** Retrieves the register stored at the given index. */
    operator fun get(index: Int) = bank[index]

    /** Stores the given [ExprAggregator] into the given register index. */
    operator fun set(index: Int, aggregator: ExprAggregator) {
        bank[index] = object : Register() {
            override val aggregator = aggregator
        }
    }
}
