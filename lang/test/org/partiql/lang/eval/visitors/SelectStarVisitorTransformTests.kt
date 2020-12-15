package org.partiql.lang.eval.visitors

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import org.partiql.lang.util.ArgumentsProviderBase

class SelectStarVisitorTransformTests : VisitorTransformTestBase() {
    // SelectStar visitor transform depends on UniqueNameMetas injected by GroupByItemAliasVisitorTransform.
    private val visitorTransforms = PipelinedVisitorTransform(GroupByItemAliasVisitorTransform(), SelectStarVisitorTransform())

    class ArgsProvider : ArgumentsProviderBase() {
        override fun getParameters(): List<Any> = listOf(
            TransformTestCase(
            """
                SELECT * 
                FROM foo AS f
            """,
            """
                SELECT "f".* 
                FROM foo AS f
            """
            ),
            TransformTestCase(
                """
                    SELECT DISTINCT * 
                    FROM foo AS f
                """,
                """
                    SELECT DISTINCT 
                        "f".* 
                    FROM foo AS f
                """
            ),
            TransformTestCase(
                """
                    SELECT * 
                    FROM foo AS f AT idx
                """,
                """
                    SELECT 
                        "f".*, 
                        "idx" AS idx 
                    FROM foo AS f AT idx
                """
            ),
            TransformTestCase(
                """
                    SELECT * 
                    FROM foo AS f AT idx BY addr
                """,
                """
                    SELECT 
                        "f".*, 
                        "idx" AS idx, 
                        "addr" as addr 
                    FROM foo AS f AT idx BY addr
                """
            ),
            TransformTestCase(
                """
                    SELECT * 
                    FROM foo AS f, 
                         bar AS b
                """,
                """
                    SELECT 
                        "f".*, 
                        "b".* 
                    FROM foo AS f, 
                         bar AS b
                """
            ),
            TransformTestCase(
                """
                    SELECT * 
                    FROM foo AS f AT f_idx, 
                         bar AS b AT b_idx
                """,
                """
                    SELECT 
                        "f".*, 
                        "f_idx" AS f_idx, 
                        "b".*, 
                        "b_idx" AS b_idx 
                    FROM foo AS f AT f_idx, 
                         bar AS b AT b_idx
                """
            ),
            TransformTestCase(
                """
                    SELECT * 
                    FROM foo AS f AT f_idx BY f_addr, 
                         bar AS b AT b_idx BY b_addr
                """,
                """
                    SELECT 
                        "f".*, 
                        "f_idx" AS f_idx, 
                        "f_addr" AS f_addr, 
                        "b".*, 
                        "b_idx" AS b_idx,
                        "b_addr" AS b_addr
                    FROM foo AS f AT f_idx BY f_addr, 
                         bar AS b AT b_idx BY b_addr
                """
            ),
            TransformTestCase(
                """
                    SELECT * 
                    FROM foo AS f
                    GROUP BY a AS a, b AS b, c AS c 
                """,
                """
                    SELECT 
                        "${'$'}__partiql__group_by_1_item_0" AS a,
                        "${'$'}__partiql__group_by_1_item_1" AS b,
                        "${'$'}__partiql__group_by_1_item_2" AS c
                    FROM foo AS f
                    GROUP BY a AS a, b AS b, c AS c 
                """
            ),
            TransformTestCase(
                """
                    SELECT * 
                    FROM foo AS f
                    GROUP BY a AS a, b AS b, c AS c GROUP AS g
                """,
                """
                    SELECT 
                        "${'$'}__partiql__group_by_1_item_0" AS a,
                        "${'$'}__partiql__group_by_1_item_1" AS b,
                        "${'$'}__partiql__group_by_1_item_2" AS c,
                        "g" AS g
                    FROM foo AS f
                    GROUP BY a AS a, b AS b, c AS c GROUP AS g
                """
            ),
            TransformTestCase(
                """
                    SELECT * 
                    FROM foo AS f
                    GROUP BY a AS dup, b AS dup, c AS dup GROUP AS dup
                """,
                """
                    SELECT 
                        "${'$'}__partiql__group_by_1_item_0" AS dup,
                        "${'$'}__partiql__group_by_1_item_1" AS dup,
                        "${'$'}__partiql__group_by_1_item_2" AS dup,
                        "dup" AS dup
                    FROM foo AS f
                    GROUP BY a AS dup, b AS dup, c AS dup GROUP AS dup
                """
            )
        )
    }

    @ParameterizedTest
    @ArgumentsSource(ArgsProvider::class)
    fun test(tc: TransformTestCase) = runTestForIdempotentTransform(tc, visitorTransforms)
}
