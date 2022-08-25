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

package org.partiql.lang.syntax

import com.amazon.ion.IonSystem
import com.amazon.ion.system.IonSystemBuilder
import org.partiql.lang.types.CustomType

class PartiQLParserBuilder {

    companion object {
        private val DEFAULT_ION = IonSystemBuilder.standard().build()

        @JvmStatic
        fun standard(): PartiQLParserBuilder {
            return PartiQLParserBuilder().withIonSystem(DEFAULT_ION)
        }
    }

    private var ion: IonSystem = DEFAULT_ION
    private var customTypes: List<CustomType> = emptyList()

    fun withIonSystem(ion: IonSystem): PartiQLParserBuilder {
        this.ion = ion
        return this
    }

    fun withCustomTypes(types: List<CustomType>): PartiQLParserBuilder {
        this.customTypes = types
        return this
    }

    fun build(): Parser {
        return PartiQLParser(this.ion, this.customTypes)
    }
}
