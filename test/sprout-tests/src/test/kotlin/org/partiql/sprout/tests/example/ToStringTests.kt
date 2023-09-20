/*
 * Copyright Amazon.com, Inc. or its affiliates.  All rights reserved.
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

package org.partiql.sprout.tests.example

import com.amazon.ionelement.api.createIonElementLoader
import org.junit.jupiter.api.Test
import org.partiql.sprout.tests.example.builder.ExampleFactoryImpl
import kotlin.test.assertEquals

/**
 * While toString isn't a contract, here are some tests for making sure at least some things work.
 *
 * Notably, the following definitely won't get properly converted to Ion:
 * - Maps
 * - Imported Types
 * - Escape Characters
 */
class ToStringTests {
    private val factory = ExampleFactoryImpl()
    private val loader = createIonElementLoader()

    @Test
    fun simpleProductAndEnum() {
        val product = factory.identifierSymbol(
            symbol = "helloworld!",
            caseSensitivity = Identifier.CaseSensitivity.SENSITIVE
        )
        val expected = loader.loadSingleElement("IdentifierSymbol::{ symbol: \"helloworld!\", caseSensitivity: IdentifierCaseSensitivity::SENSITIVE }")
        val actual = loader.loadSingleElement(product.toString())
        assertEquals(expected, actual)
    }

    @Test
    fun emptyProduct() {
        val product = factory.exprEmpty()
        val expected = loader.loadSingleElement("ExprEmpty::{ }")
        val actual = loader.loadSingleElement(product.toString())
        assertEquals(expected, actual)
    }

    @Test
    fun list() {
        val product = factory.identifierQualified(
            root = factory.identifierSymbol(
                symbol = "hello",
                caseSensitivity = Identifier.CaseSensitivity.INSENSITIVE
            ),
            steps = listOf(
                factory.identifierSymbol(
                    symbol = "world",
                    caseSensitivity = Identifier.CaseSensitivity.SENSITIVE
                ),
            )
        )
        val expectedString = """
            IdentifierQualified::{
                root: IdentifierSymbol::{ symbol: "hello", caseSensitivity: IdentifierCaseSensitivity::INSENSITIVE },
                steps: [
                    IdentifierSymbol::{ symbol: "world", caseSensitivity: IdentifierCaseSensitivity::SENSITIVE },
                ]
            }
        """.trimIndent()
        val expected = loader.loadSingleElement(expectedString)
        val actual = loader.loadSingleElement(product.toString())
        assertEquals(expected, actual)
    }
}
