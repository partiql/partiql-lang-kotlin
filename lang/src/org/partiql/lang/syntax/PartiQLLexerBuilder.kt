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

/**
 * A builder class to instantiate a [Lexer].
 *
 * Example usages:
 *
 * ```
 * val lexer = PartiQLLexerBuilder.standard().build()
 * val lexer = PartiQLLexerBuilder().ionSystem(ion).build()
 * ```
 */
public class PartiQLLexerBuilder {

    companion object {
        private val DEFAULT_ION = IonSystemBuilder.standard().build()

        @JvmStatic
        public fun standard(): PartiQLLexerBuilder {
            return PartiQLLexerBuilder().ionSystem(DEFAULT_ION)
        }
    }

    private var ion: IonSystem = DEFAULT_ION

    public fun ionSystem(ion: IonSystem): PartiQLLexerBuilder = this.apply { this.ion = ion }

    public fun build(): Lexer = PartiQLLexer(this.ion)
}
