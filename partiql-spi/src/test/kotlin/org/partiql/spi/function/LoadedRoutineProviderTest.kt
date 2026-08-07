/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package org.partiql.spi.function

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.partiql.spi.catalog.Name
import org.partiql.spi.types.PType
import org.partiql.spi.types.PTypeField
import org.partiql.spi.value.Datum

class LoadedRoutineProviderTest {

    @Test
    fun loadsScalarAggregateAndMixedInventories() {
        val scalar = functionDefinition("math.pow", function("pow"))
        val aggregate = aggregationDefinition("stats.total", aggregation("total"))

        val scalarOnly = LoadedRoutineProvider.load(provider(functions = listOf(scalar)))
        val aggregateOnly = LoadedRoutineProvider.load(provider(aggregations = listOf(aggregate)))
        val mixed = LoadedRoutineProvider.load(provider(listOf(scalar), listOf(aggregate)))

        assertEquals(listOf(Name.of("math", "pow")), scalarOnly.functions.map { it.sourceName })
        assertEquals(emptyList<Name>(), scalarOnly.aggregations.map { it.sourceName })
        assertEquals(emptyList<Name>(), aggregateOnly.functions.map { it.sourceName })
        assertEquals(listOf(Name.of("stats", "total")), aggregateOnly.aggregations.map { it.sourceName })
        assertEquals(listOf(Name.of("math", "pow")), mixed.functions.map { it.sourceName })
        assertEquals(listOf(Name.of("stats", "total")), mixed.aggregations.map { it.sourceName })
    }

    @Test
    fun loadsProviderUsingDefaultEmptyInventories() {
        val loaded = LoadedRoutineProvider.load(object : RoutineProvider {})

        assertTrueEmpty(loaded)
    }

    @Test
    fun permitsSameSourceInScalarAndAggregateInventories() {
        val source = "stats.value"

        val loaded = LoadedRoutineProvider.load(
            provider(
                functions = listOf(functionDefinition(source, function("value"))),
                aggregations = listOf(aggregationDefinition(source, aggregation("value"))),
            ),
        )

        assertEquals(Name.of("stats", "value"), loaded.functions.single().sourceName)
        assertEquals(Name.of("stats", "value"), loaded.aggregations.single().sourceName)
    }

    @Test
    fun snapshotsProviderCollectionsAndOverloadSignatures() {
        val scalarOverload = MutableFnOverload(
            RoutineOverloadSignature("tokenize", listOf(PType.string())),
        )
        val aggregateOverload = MutableAggOverload(
            RoutineOverloadSignature("total", listOf(PType.integer())),
        )
        val functions = mutableListOf(functionDefinition("text.tokenize", scalarOverload))
        val aggregations = mutableListOf(aggregationDefinition("stats.total", aggregateOverload))

        val loaded = LoadedRoutineProvider.load(provider(functions, aggregations))
        functions.clear()
        aggregations.clear()
        scalarOverload.currentSignature =
            RoutineOverloadSignature("changed", listOf(PType.dynamic()))
        aggregateOverload.currentSignature =
            RoutineOverloadSignature("changed", listOf(PType.dynamic()))

        val loadedScalar = loaded.functions.single().overloads.single()
        val loadedAggregate = loaded.aggregations.single().overloads.single()
        assertEquals("tokenize", loadedScalar.signature.name)
        assertEquals(listOf(PType.string()), loadedScalar.signature.parameterTypes)
        assertEquals("total", loadedAggregate.signature.name)
        assertEquals(listOf(PType.integer()), loadedAggregate.signature.parameterTypes)
        assertNotSame(scalarOverload, loadedScalar)
        assertNotSame(aggregateOverload, loadedAggregate)
    }

    @Test
    fun snapshotWrappersDelegateInstanceCreation() {
        val scalar = function("scalar")
        val aggregate = aggregation("aggregate")
        val loaded = LoadedRoutineProvider.load(
            provider(
                functions = listOf(functionDefinition("root.scalar", scalar)),
                aggregations = listOf(aggregationDefinition("root.aggregate", aggregate)),
            ),
        )

        assertSame(
            scalar.getInstance(emptyArray()),
            loaded.functions.single().overloads.single().getInstance(emptyArray()),
        )
        assertSame(
            aggregate.getInstance(emptyArray()),
            loaded.aggregations.single().overloads.single().getInstance(emptyArray()),
        )
    }

    @Test
    fun invokesCallbacksOnceInScalarThenAggregateOrder() {
        val callbacks = mutableListOf<String>()
        val loaded = LoadedRoutineProvider.load(
            object : RoutineProvider {
                override fun getFunctions(): Collection<RoutineDefinition<FnOverload>> {
                    callbacks += "getFunctions"
                    return emptyList()
                }

                override fun getAggregations(): Collection<RoutineDefinition<AggOverload>> {
                    callbacks += "getAggregations"
                    return emptyList()
                }
            },
        )

        assertEquals(
            listOf(
                "getFunctions",
                "getAggregations",
            ),
            callbacks,
        )
        assertTrueEmpty(loaded)
    }

    @Test
    fun firstCallbackFailureSkipsAggregateCallbackAndPreservesCause() {
        val cause = IllegalStateException("functions unavailable")
        var aggregateInvoked = false
        val error = assertThrows<RoutineProviderValidationException> {
            LoadedRoutineProvider.load(
                object : RoutineProvider {
                    override fun getFunctions(): Collection<RoutineDefinition<FnOverload>> = throw cause

                    override fun getAggregations(): Collection<RoutineDefinition<AggOverload>> {
                        aggregateInvoked = true
                        return emptyList()
                    }
                },
            )
        }

        val issue = error.issues.single()
        assertFalse(aggregateInvoked)
        assertSame(cause, error.cause)
        assertEquals("Routine provider validation failed with 1 issue(s).", error.message)
        assertEquals(RoutineProviderValidationReason.PROVIDER_ACCESS_FAILED, issue.reason)
        assertFalse(issue.isAggregate)
        assertEquals("getFunctions", issue.callback)
        assertEquals("Provider inventory from getFunctions could not be loaded.", issue.message)
        assertNull(issue.sourceName)
    }

    @Test
    fun fatalProviderFailurePropagates() {
        val cause = OutOfMemoryError("fatal")

        val error = assertThrows<OutOfMemoryError> {
            LoadedRoutineProvider.load(
                object : RoutineProvider {
                    override fun getFunctions(): Collection<RoutineDefinition<FnOverload>> = throw cause
                },
            )
        }

        assertSame(cause, error)
    }

    @Test
    fun metadataFailureIdentifiesInventoryWithoutClaimingCallbackFailure() {
        val cause = IllegalStateException("signature unavailable")
        val overload = object : FnOverload() {
            override fun getSignature(): RoutineOverloadSignature = throw cause

            override fun getInstance(args: Array<PType>): Fn? = null
        }

        val error = validationError(
            provider(
                functions = listOf(RoutineDefinition(Name.of("root", "broken"), listOf(overload))),
            ),
        )

        val issue = error.issues.single()
        assertSame(cause, error.cause)
        assertEquals(RoutineProviderValidationReason.PROVIDER_ACCESS_FAILED, issue.reason)
        assertEquals("getFunctions", issue.callback)
        assertEquals("Provider inventory from getFunctions could not be loaded.", issue.message)
    }

    @Test
    fun secondCallbackFailureIdentifiesAggregateInventory() {
        val cause = IllegalStateException("aggregations unavailable")
        val error = assertThrows<RoutineProviderValidationException> {
            LoadedRoutineProvider.load(
                object : RoutineProvider {
                    override fun getFunctions(): Collection<RoutineDefinition<FnOverload>> = emptyList()

                    override fun getAggregations(): Collection<RoutineDefinition<AggOverload>> = throw cause
                },
            )
        }

        val issue = error.issues.single()
        assertSame(cause, error.cause)
        assertTrue(issue.isAggregate)
        assertEquals("getAggregations", issue.callback)
    }

    @Test
    fun rejectsEmptySourceSegmentAndEmptyOverloads() {
        val error = validationError(
            provider(
                functions = listOf(
                    RoutineDefinition(Name.of("root", ""), listOf(function(""))),
                    RoutineDefinition(Name.of("root", "empty"), emptyList()),
                ),
            ),
        )

        assertEquals(
            listOf(
                RoutineProviderValidationReason.EMPTY_SOURCE_SEGMENT,
                RoutineProviderValidationReason.EMPTY_OVERLOADS,
            ),
            error.issues.map { it.reason },
        )
        assertFalse(error.issues.first().isAggregate)
        assertEquals(Name.of("root", ""), error.issues.first().sourceName)
        assertEquals(
            "Routine definition \"root\".\"empty\" has no overloads.",
            error.issues.last().message,
        )
    }

    @Test
    fun rejectsDuplicateSourceWithinOneKind() {
        val sourceName = Name.of("root", "routine")
        val error = validationError(
            provider(
                functions = listOf(
                    RoutineDefinition(sourceName, listOf(function("routine"))),
                    RoutineDefinition(sourceName, listOf(function("routine", PType.string()))),
                ),
            ),
        )

        val issue = error.issues.single()
        assertEquals(RoutineProviderValidationReason.DUPLICATE_SOURCE_NAME, issue.reason)
        assertFalse(issue.isAggregate)
        assertEquals(sourceName, issue.sourceName)
        assertEquals(sourceName, issue.conflictingSourceName)
        assertNotSame(sourceName, issue.sourceName)
        assertEquals(
            "Source name \"root\".\"routine\" is declared more than once in the scalar inventory.",
            issue.message,
        )
    }

    @Test
    fun rejectsSignatureNameMismatch() {
        val error = validationError(
            provider(
                functions = listOf(functionDefinition("text.tokenize", function("other"))),
            ),
        )

        val issue = error.issues.single()
        assertEquals(RoutineProviderValidationReason.SIGNATURE_NAME_MISMATCH, issue.reason)
        assertEquals("other", issue.signatureName)
        assertEquals(
            "Routine definition \"text\".\"tokenize\" overload name other does not equal source leaf tokenize.",
            issue.message,
        )
    }

    @Test
    fun rejectsDuplicateParameterSignaturesIncludingReturnOnlyDifferences() {
        val first = function("tokenize", PType.string(), returns = PType.string())
        val second = function("tokenize", PType.string(), returns = PType.integer())
        val error = validationError(
            provider(
                functions = listOf(functionDefinition("text.tokenize", first, second)),
            ),
        )

        val issue = error.issues.single()
        assertEquals(RoutineProviderValidationReason.DUPLICATE_OVERLOAD_SIGNATURE, issue.reason)
        assertEquals(listOf(PType.string()), issue.parameterTypes)
        assertEquals(
            "Routine definition \"text\".\"tokenize\" contains a duplicate overload signature.",
            issue.message,
        )
        assertThrows<UnsupportedOperationException> {
            (issue.parameterTypes as MutableList<PType>).add(PType.dynamic())
        }
    }

    @Test
    fun rejectsDuplicateRowParameterSignatures() {
        val firstRow = PType.row(PTypeField.of("value", PType.integer()))
        val secondRow = PType.row(PTypeField.of("value", PType.integer()))
        assertEquals(firstRow, secondRow)

        val error = validationError(
            provider(
                functions = listOf(
                    functionDefinition(
                        "root.inspect",
                        function("inspect", firstRow),
                        function("inspect", secondRow),
                    ),
                ),
            ),
        )

        val issue = error.issues.single()
        assertEquals(RoutineProviderValidationReason.DUPLICATE_OVERLOAD_SIGNATURE, issue.reason)
        assertEquals(listOf(firstRow), issue.parameterTypes)
    }

    @Test
    fun validationIssueDoesNotExposeItsParameterTypeSnapshot() {
        val error = validationError(
            provider(
                functions = listOf(
                    functionDefinition(
                        "root.duplicate",
                        function("duplicate", PType.string()),
                        function("duplicate", PType.string()),
                    ),
                ),
            ),
        )
        val issue = error.issues.single()

        val firstRead = issue.parameterTypes!!.single()
        firstRead.metas["changed"] = true
        val secondRead = issue.parameterTypes!!.single()

        assertNotSame(firstRead, secondRead)
        assertFalse(secondRead.metas.containsKey("changed"))
    }

    @Test
    fun reportsIssuesInScalarThenAggregateAndExactSourceOrder() {
        val error = validationError(
            provider(
                functions = listOf(
                    functionDefinition("zeta.bad", function("wrong"), function("wrong")),
                    RoutineDefinition(Name.of("alpha", "empty"), emptyList()),
                    functionDefinition("zeta.bad", function("bad", PType.string())),
                ),
                aggregations = listOf(
                    aggregationDefinition("alpha.total", aggregation("wrong")),
                ),
            ),
        )

        assertEquals(
            listOf(
                Triple(false, Name.of("alpha", "empty"), RoutineProviderValidationReason.EMPTY_OVERLOADS),
                Triple(false, Name.of("zeta", "bad"), RoutineProviderValidationReason.SIGNATURE_NAME_MISMATCH),
                Triple(false, Name.of("zeta", "bad"), RoutineProviderValidationReason.SIGNATURE_NAME_MISMATCH),
                Triple(
                    false,
                    Name.of("zeta", "bad"),
                    RoutineProviderValidationReason.DUPLICATE_OVERLOAD_SIGNATURE,
                ),
                Triple(false, Name.of("zeta", "bad"), RoutineProviderValidationReason.DUPLICATE_SOURCE_NAME),
                Triple(
                    true,
                    Name.of("alpha", "total"),
                    RoutineProviderValidationReason.SIGNATURE_NAME_MISMATCH,
                ),
            ),
            error.issues.map { Triple(it.isAggregate, it.sourceName, it.reason) },
        )
    }

    @Test
    fun exceptionIssuesAreJavaUnmodifiable() {
        val error = validationError(
            provider(
                functions = listOf(RoutineDefinition(Name.of("root", "empty"), emptyList())),
            ),
        )

        assertThrows<UnsupportedOperationException> {
            (error.issues as MutableList<RoutineProviderValidationIssue>).add(error.issues.single())
        }
    }

    private fun validationError(provider: RoutineProvider): RoutineProviderValidationException =
        assertThrows {
            LoadedRoutineProvider.load(provider)
        }

    private fun provider(
        functions: Collection<RoutineDefinition<FnOverload>> = emptyList(),
        aggregations: Collection<RoutineDefinition<AggOverload>> = emptyList(),
    ): RoutineProvider =
        object : RoutineProvider {
            override fun getFunctions(): Collection<RoutineDefinition<FnOverload>> = functions

            override fun getAggregations(): Collection<RoutineDefinition<AggOverload>> = aggregations
        }

    private fun functionDefinition(
        sourceName: String,
        vararg overloads: FnOverload,
    ): RoutineDefinition<FnOverload> =
        RoutineDefinition(Name.of(sourceName.split(".")), overloads.toList())

    private fun aggregationDefinition(
        sourceName: String,
        vararg overloads: AggOverload,
    ): RoutineDefinition<AggOverload> =
        RoutineDefinition(Name.of(sourceName.split(".")), overloads.toList())

    private fun function(
        name: String,
        vararg parameters: PType,
        returns: PType = PType.dynamic(),
    ): FnOverload {
        val builder = FnOverload.Builder(name)
            .returns(returns)
            .body { Datum.missing() }
        parameters.forEach(builder::addParameter)
        return builder.build()
    }

    private fun aggregation(name: String, vararg parameters: PType): AggOverload {
        val builder = AggOverload.Builder(name).returns(PType.dynamic())
        parameters.forEach(builder::addParameter)
        return builder.build()
    }

    private fun assertTrueEmpty(loaded: LoadedRoutineProvider) {
        assertEquals(emptyList<RoutineDefinition<FnOverload>>(), loaded.functions)
        assertEquals(emptyList<RoutineDefinition<AggOverload>>(), loaded.aggregations)
    }

    private class MutableFnOverload(
        var currentSignature: RoutineOverloadSignature,
    ) : FnOverload() {
        override fun getSignature(): RoutineOverloadSignature = currentSignature

        override fun getInstance(args: Array<PType>): Fn? = null
    }

    private class MutableAggOverload(
        var currentSignature: RoutineOverloadSignature,
    ) : AggOverload() {
        override fun getSignature(): RoutineOverloadSignature = currentSignature

        override fun getInstance(args: Array<PType>): Agg? = null
    }
}
