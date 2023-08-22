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

import com.amazon.ionelement.api.ionInt
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import org.partiql.sprout.tests.ArgumentsProviderBase
import org.partiql.sprout.tests.example.builder.ExampleFactoryImpl
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

/**
 * Tests the generated equals and hashCode methods
 */
class EqualityTests {

    @ParameterizedTest
    @ArgumentsSource(EqualArgumentsProvider::class)
    fun testEquality(tc: EqualArgumentsProvider.TestCase) {
        assertEquals(tc.first, tc.second)
        assertEquals(tc.first.hashCode(), tc.second.hashCode())
    }

    /**
     * We do not check that hashCode produces unequal results due to the `hashCode()` JavaDocs:
     * > It is not required that if two objects are unequal according to the equals(java.lang.Object) method, then
     * > calling the hashCode method on each of the two objects must produce distinct integer results.
     */
    @ParameterizedTest
    @ArgumentsSource(NotEqualArgumentsProvider::class)
    fun testNotEquality(tc: NotEqualArgumentsProvider.TestCase) {
        assertNotEquals(tc.first, tc.second)
    }

    class EqualArgumentsProvider : ArgumentsProviderBase() {
        private val factory = ExampleFactoryImpl()
        override fun getParameters(): List<TestCase> = listOf(
            TestCase(factory.exprEmpty(), factory.exprEmpty()),
            TestCase(factory.exprIon(ionInt(1)), factory.exprIon(ionInt(1))),
            TestCase(
                factory.identifierQualified(
                    factory.identifierSymbol("hello", Identifier.CaseSensitivity.INSENSITIVE),
                    emptyList()
                ),
                factory.identifierQualified(
                    factory.identifierSymbol("hello", Identifier.CaseSensitivity.INSENSITIVE),
                    emptyList()
                )
            ),
            TestCase(
                factory.identifierQualified(
                    factory.identifierSymbol("hello", Identifier.CaseSensitivity.INSENSITIVE),
                    listOf(
                        factory.identifierSymbol("world", Identifier.CaseSensitivity.SENSITIVE),
                        factory.identifierSymbol("yeah", Identifier.CaseSensitivity.INSENSITIVE),
                        factory.identifierSymbol("foliage", Identifier.CaseSensitivity.INSENSITIVE),
                    )
                ),
                factory.identifierQualified(
                    factory.identifierSymbol("hello", Identifier.CaseSensitivity.INSENSITIVE),
                    listOf(
                        factory.identifierSymbol("world", Identifier.CaseSensitivity.SENSITIVE),
                        factory.identifierSymbol("yeah", Identifier.CaseSensitivity.INSENSITIVE),
                        factory.identifierSymbol("foliage", Identifier.CaseSensitivity.INSENSITIVE),
                    )
                )
            ),
            TestCase(
                factory.statementQuery(factory.exprEmpty()),
                factory.statementQuery(factory.exprEmpty())
            ),
            // Tests deep equality of LISTS
            TestCase(
                factory.exprNested(
                    itemsList = listOf(
                        listOf(
                            factory.exprEmpty(),
                            factory.exprIon(ionInt(1))
                        ),
                        listOf(
                            factory.exprIon(ionInt(3))
                        )
                    ),
                    itemsSet = emptySet(),
                    itemsMap = emptyMap()
                ),
                factory.exprNested(
                    itemsList = listOf(
                        listOf(
                            factory.exprEmpty(),
                            factory.exprIon(ionInt(1))
                        ),
                        listOf(
                            factory.exprIon(ionInt(3))
                        )
                    ),
                    itemsSet = emptySet(),
                    itemsMap = emptyMap()
                ),
            ),
            // Tests deep equality of SETS
            TestCase(
                first = factory.exprNested(
                    itemsList = emptyList(),
                    itemsSet = setOf(
                        setOf(),
                        setOf(factory.exprEmpty()),
                        setOf(factory.exprIon(ionInt(1)), factory.exprIon(ionInt(2)))
                    ),
                    itemsMap = emptyMap()
                ),
                second = factory.exprNested(
                    itemsList = emptyList(),
                    itemsSet = setOf(
                        setOf(),
                        setOf(factory.exprEmpty()),
                        setOf(factory.exprIon(ionInt(1)), factory.exprIon(ionInt(2)))
                    ),
                    itemsMap = emptyMap()
                ),
            ),
            // Tests deep equality of MAPS
            TestCase(
                first = factory.exprNested(
                    itemsList = emptyList(),
                    itemsSet = emptySet(),
                    itemsMap = mapOf(
                        "hello" to mapOf(
                            "world" to factory.exprEmpty(),
                            "!" to factory.exprIon(ionInt(1))
                        ),
                        "goodbye" to mapOf(
                            "friend" to factory.exprIon(ionInt(2))
                        )
                    )
                ),
                second = factory.exprNested(
                    itemsList = emptyList(),
                    itemsSet = emptySet(),
                    itemsMap = mapOf(
                        "hello" to mapOf(
                            "world" to factory.exprEmpty(),
                            "!" to factory.exprIon(ionInt(1))
                        ),
                        "goodbye" to mapOf(
                            "friend" to factory.exprIon(ionInt(2))
                        )
                    )
                ),
            )
        )

        class TestCase(
            val first: ExampleNode,
            val second: ExampleNode
        )
    }

    class NotEqualArgumentsProvider : ArgumentsProviderBase() {
        private val factory = ExampleFactoryImpl()
        override fun getParameters(): List<TestCase> = listOf(
            TestCase(factory.exprEmpty(), factory.exprIon(ionInt(1))),
            TestCase(factory.exprIon(ionInt(1)), factory.exprIon(ionInt(2))),
            TestCase(
                factory.identifierSymbol("hello", Identifier.CaseSensitivity.INSENSITIVE),
                factory.identifierSymbol("hello", Identifier.CaseSensitivity.SENSITIVE)
            ),
            TestCase(
                factory.identifierQualified(
                    factory.identifierSymbol("hello", Identifier.CaseSensitivity.INSENSITIVE),
                    listOf(
                        factory.identifierSymbol("world", Identifier.CaseSensitivity.SENSITIVE),
                        factory.identifierSymbol("yeah", Identifier.CaseSensitivity.INSENSITIVE),
                        factory.identifierSymbol("foliage", Identifier.CaseSensitivity.INSENSITIVE),
                    )
                ),
                factory.identifierQualified(
                    factory.identifierSymbol("hello", Identifier.CaseSensitivity.INSENSITIVE),
                    listOf(
                        factory.identifierSymbol("NOT_WORLD", Identifier.CaseSensitivity.SENSITIVE),
                        factory.identifierSymbol("yeah", Identifier.CaseSensitivity.INSENSITIVE),
                        factory.identifierSymbol("foliage", Identifier.CaseSensitivity.INSENSITIVE),
                    )
                )
            ),
            // Tests deep equality of LISTS
            TestCase(
                factory.exprNested(
                    itemsList = listOf(
                        listOf(
                            factory.exprEmpty(),
                            factory.exprIon(ionInt(1))
                        ),
                        listOf(
                            factory.exprIon(ionInt(3))
                        )
                    ),
                    itemsSet = emptySet(),
                    itemsMap = emptyMap()
                ),
                factory.exprNested(
                    itemsList = listOf(
                        listOf(
                            factory.exprEmpty(),
                            factory.exprIon(ionInt(2))
                        ),
                        listOf(
                            factory.exprIon(ionInt(3))
                        )
                    ),
                    itemsSet = emptySet(),
                    itemsMap = emptyMap()
                ),
            ),
            // Tests deep equality of SETS
            TestCase(
                first = factory.exprNested(
                    itemsList = emptyList(),
                    itemsSet = setOf(
                        setOf(),
                        setOf(factory.exprEmpty()),
                        setOf(factory.exprIon(ionInt(1)), factory.exprIon(ionInt(2)))
                    ),
                    itemsMap = emptyMap()
                ),
                second = factory.exprNested(
                    itemsList = emptyList(),
                    itemsSet = setOf(
                        setOf(),
                        setOf(factory.exprEmpty()),
                        setOf(factory.exprIon(ionInt(1)), factory.exprIon(ionInt(3)))
                    ),
                    itemsMap = emptyMap()
                ),
            ),
            // Tests deep equality of MAPS
            TestCase(
                first = factory.exprNested(
                    itemsList = emptyList(),
                    itemsSet = emptySet(),
                    itemsMap = mapOf(
                        "hello" to mapOf(
                            "world" to factory.exprEmpty(),
                            "!" to factory.exprIon(ionInt(1))
                        ),
                        "goodbye" to mapOf(
                            "friend" to factory.exprIon(ionInt(2))
                        )
                    )
                ),
                second = factory.exprNested(
                    itemsList = emptyList(),
                    itemsSet = emptySet(),
                    itemsMap = mapOf(
                        "hello" to mapOf(
                            "world" to factory.exprEmpty(),
                            "!" to factory.exprIon(ionInt(1))
                        ),
                        "goodbye" to mapOf(
                            "friend" to factory.exprIon(ionInt(3))
                        )
                    )
                ),
            )
        )

        class TestCase(
            val first: ExampleNode,
            val second: ExampleNode
        )
    }
}
