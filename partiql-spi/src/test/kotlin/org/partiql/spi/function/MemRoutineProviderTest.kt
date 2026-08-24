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
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.partiql.spi.catalog.Identifier
import org.partiql.spi.catalog.Name
import org.partiql.spi.catalog.Namespace
import org.partiql.spi.types.PType
import org.partiql.spi.types.PTypeField
import java.util.concurrent.Callable
import java.util.concurrent.Executors

class MemRoutineProviderTest {

    @Test
    fun emptyBuilderProducesEmptyProvider() {
        val provider = MemRoutineProvider.builder().build()
        val identifier = Identifier.regular("missing")

        assertTrue(provider.getFunctions(identifier).isEmpty())
        assertTrue(provider.getAggregations(identifier).isEmpty())
    }

    @Test
    fun registersBothKindsWithEveryNameForm() {
        val rootFunction = function("root_fn")
        val namespacedFunction = function("namespaced_fn")
        val exactFunction = function("exact_fn")
        val rootAggregate = aggregation("root_agg")
        val namespacedAggregate = aggregation("namespaced_agg")
        val exactAggregate = aggregation("exact_agg")
        val provider = MemRoutineProvider.builder()
            .register(rootFunction)
            .register(namespacedFunction, Namespace.of("analytics"))
            .register(exactFunction, Name.of("custom", "exact_fn"))
            .register(rootAggregate, Namespace.empty())
            .register(namespacedAggregate, Namespace.of("analytics"))
            .register(exactAggregate, Name.of("custom", "exact_agg"))
            .build()

        assertSame(rootFunction, function(provider, "root_fn"))
        assertSame(namespacedFunction, function(provider, "analytics", "namespaced_fn"))
        assertSame(exactFunction, function(provider, "custom", "exact_fn"))
        assertSame(rootAggregate, aggregation(provider, "root_agg"))
        assertSame(namespacedAggregate, aggregation(provider, "analytics", "namespaced_agg"))
        assertSame(exactAggregate, aggregation(provider, "custom", "exact_agg"))
    }

    @Test
    fun matchesCompleteIdentifiersWithPerPartSqlCaseRules() {
        val function = function("Pow")
        val provider = MemRoutineProvider.builder()
            .register(function, Name.of("Analytics", "Math", "Pow"))
            .build()

        assertSame(function, function(provider, "analytics", "math", "pow"))
        assertSame(
            function,
            provider.getFunctions(
                Identifier.of(
                    Identifier.Simple.regular("analytics"),
                    Identifier.Simple.delimited("Math"),
                    Identifier.Simple.regular("pow"),
                ),
            ).single().overloads.single(),
        )
        assertTrue(
            provider.getFunctions(
                Identifier.of(
                    Identifier.Simple.regular("analytics"),
                    Identifier.Simple.delimited("math"),
                    Identifier.Simple.regular("pow"),
                ),
            ).isEmpty(),
        )
        assertTrue(provider.getFunctions(Identifier.delimited("Analytics", "Math", "pow")).isEmpty())
        assertTrue(provider.getFunctions(Identifier.regular("math", "pow")).isEmpty())
        assertTrue(provider.getFunctions(Identifier.regular("other", "analytics", "math", "pow")).isEmpty())
    }

    @Test
    fun composesDistinctSignaturesInRegistrationOrder() {
        val integer = function("pow", PType.integer(), PType.integer())
        val double = function("pow", PType.doublePrecision(), PType.doublePrecision())
        val provider = MemRoutineProvider.builder()
            .register(integer, Namespace.of("math"))
            .register(double, Namespace.of("math"))
            .build()

        val binding = provider.getFunctions(Identifier.regular("math", "pow")).single()

        assertEquals(Name.of("math", "pow"), binding.canonicalName)
        assertEquals(listOf(integer, double), binding.overloads)
        assertSame(integer, binding.overloads[0])
        assertSame(double, binding.overloads[1])
    }

    @Test
    fun scalarAndAggregateBindingsRemainSeparateAtTheSameName() {
        val function = function("shared", PType.integer())
        val aggregation = aggregation("shared", PType.integer())
        val provider = MemRoutineProvider.builder()
            .register(function, Namespace.of("stats"))
            .register(aggregation, Namespace.of("stats"))
            .build()

        assertSame(function, function(provider, "stats", "shared"))
        assertSame(aggregation, aggregation(provider, "stats", "shared"))
    }

    @Test
    fun sameOverloadMayBeRegisteredAtMultipleNames() {
        val function = function("pow")
        val provider = MemRoutineProvider.builder()
            .register(function, Namespace.of("math"))
            .register(function, Namespace.of("experimental"))
            .build()

        assertSame(function, function(provider, "math", "pow"))
        assertSame(function, function(provider, "experimental", "pow"))
    }

    @Test
    fun rejectsEmptyNameSegmentsBeforeOtherSemanticErrors() {
        val error = assertThrows<IllegalArgumentException> {
            MemRoutineProvider.builder()
                .register(function("pow"), Name.of("math", "", "power"))
                .build()
        }

        assertTrue(error.message!!.contains("empty segment"))
    }

    @Test
    fun validatesScalarNamesBeforeAggregateNames() {
        val error = assertThrows<IllegalArgumentException> {
            MemRoutineProvider.builder()
                .register(aggregation("aggregate"), Name.of("aggregate", ""))
                .register(function("scalar"), Name.of("scalar", ""))
                .build()
        }

        assertTrue(error.message!!.contains("\"scalar\""))
    }

    @Test
    fun rejectsLeafRenamingForBothKinds() {
        val scalarError = assertThrows<IllegalArgumentException> {
            MemRoutineProvider.builder()
                .register(function("pow"), Name.of("math", "POW"))
                .build()
        }
        val aggregateError = assertThrows<IllegalArgumentException> {
            MemRoutineProvider.builder()
                .register(aggregation("total"), Name.of("stats", "sum"))
                .build()
        }

        assertTrue(scalarError.message!!.contains("does not match overload name"))
        assertTrue(aggregateError.message!!.contains("does not match overload name"))
    }

    @Test
    fun rejectsRegularEquivalentCanonicalNamesWithinAndAcrossKinds() {
        val sameKindError = assertThrows<IllegalArgumentException> {
            MemRoutineProvider.builder()
                .register(function("pow"), Name.of("math", "pow"))
                .register(function("Pow", PType.integer()), Name.of("Math", "Pow"))
                .build()
        }
        val crossKindError = assertThrows<IllegalArgumentException> {
            MemRoutineProvider.builder()
                .register(function("pow"), Name.of("math", "pow"))
                .register(aggregation("Pow"), Name.of("Math", "Pow"))
                .build()
        }

        assertTrue(sameKindError.message!!.contains("conflict under regular SQL matching"))
        assertTrue(crossKindError.message!!.contains("conflict under regular SQL matching"))
    }

    @Test
    fun rejectsEqualParameterSequencesEvenWhenRowHashesDiffer() {
        val firstRow = PType.row(PTypeField.of("value", PType.string()))
        val secondRow = PType.row(PTypeField.of("value", PType.string()))
        val error = assertThrows<IllegalArgumentException> {
            MemRoutineProvider.builder()
                .register(function("inspect", firstRow))
                .register(function("inspect", secondRow))
                .build()
        }

        assertEquals(firstRow, secondRow)
        assertTrue(error.message!!.contains("Duplicate scalar routine signature"))
    }

    @Test
    fun rejectsDuplicateAggregateSignatures() {
        val error = assertThrows<IllegalArgumentException> {
            MemRoutineProvider.builder()
                .register(aggregation("total", PType.integer()))
                .register(aggregation("total", PType.integer()))
                .build()
        }

        assertTrue(error.message!!.contains("Duplicate aggregate routine signature"))
    }

    @Test
    fun rejectsSignaturesDistinguishedOnlyByReturnType() {
        val error = assertThrows<IllegalArgumentException> {
            MemRoutineProvider.builder()
                .register(functionReturning("convert", PType.integer(), PType.string()))
                .register(functionReturning("convert", PType.string(), PType.string()))
                .build()
        }

        assertTrue(error.message!!.contains("Duplicate scalar routine signature"))
    }

    @Test
    fun rejectsDuplicateRegistrationOfTheSameObjectAtOneName() {
        val function = function("pow", PType.integer())
        val error = assertThrows<IllegalArgumentException> {
            MemRoutineProvider.builder()
                .register(function)
                .register(function)
                .build()
        }

        assertTrue(error.message!!.contains("Duplicate scalar routine signature"))
    }

    @Test
    fun eachBuildSnapshotsCurrentBuilderState() {
        val firstFunction = function("first")
        val secondFunction = function("second")
        val builder = MemRoutineProvider.builder().register(firstFunction)
        val first = builder.build()

        builder.register(secondFunction)
        val second = builder.build()

        assertTrue(first.getFunctions(Identifier.regular("second")).isEmpty())
        assertSame(firstFunction, function(first, "first"))
        assertSame(firstFunction, function(second, "first"))
        assertSame(secondFunction, function(second, "second"))
    }

    @Test
    fun copiesRegistrationNameInputs() {
        val namespacedFunction = function("pow")
        val exactFunction = function("mean")
        val namespace = Namespace.of("math")
        val exactName = Name.of("stats", "mean")
        val builder = MemRoutineProvider.builder()
            .register(namespacedFunction, namespace)
            .register(exactFunction, exactName)

        namespace.getLevels()[0] = "changed"
        exactName.getNamespace().getLevels()[0] = "changed"
        val provider = builder.build()

        assertSame(namespacedFunction, function(provider, "math", "pow"))
        assertSame(exactFunction, function(provider, "stats", "mean"))
    }

    @Test
    fun supportsConcurrentReads() {
        val function = function("pow", PType.integer(), PType.integer())
        val provider = MemRoutineProvider.builder()
            .register(function, Namespace.of("math"))
            .build()
        val executor = Executors.newFixedThreadPool(8)

        try {
            val lookups = List(256) {
                Callable {
                    provider.getFunctions(Identifier.regular("math", "pow"))
                        .single()
                        .overloads
                        .single()
                }
            }

            executor.invokeAll(lookups).forEach { assertSame(function, it.get()) }
        } finally {
            executor.shutdownNow()
        }
    }

    private fun function(
        provider: RoutineProvider,
        vararg name: String,
    ): FnOverload =
        provider.getFunctions(Identifier.regular(*name)).single().overloads.single()

    private fun aggregation(
        provider: RoutineProvider,
        vararg name: String,
    ): AggOverload =
        provider.getAggregations(Identifier.regular(*name)).single().overloads.single()

    private fun function(
        name: String,
        vararg parameters: PType,
    ): FnOverload =
        FnOverload.Builder(name)
            .addParameters(*parameters)
            .build()

    private fun functionReturning(
        name: String,
        returns: PType,
        vararg parameters: PType,
    ): FnOverload =
        FnOverload.Builder(name)
            .addParameters(*parameters)
            .returns(returns)
            .build()

    private fun aggregation(
        name: String,
        vararg parameters: PType,
    ): AggOverload =
        AggOverload.Builder(name)
            .addParameters(*parameters)
            .build()
}
