package org.partiql.lang.eval.visitors

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import org.partiql.lang.eval.builtins.ExprFunctionCurrentUser
import org.partiql.lang.util.ArgumentsProviderBase

class SystemFunctionsVisitorTransformTests : VisitorTransformTestBase() {

    @ParameterizedTest
    @ArgumentsSource(ArgsProvider::class)
    fun test(tc: TransformTestCase) = runTestForIdempotentTransform(tc, SystemFunctionsVisitorTransform)

    class ArgsProvider : ArgumentsProviderBase() {
        private val currentUserFunction = "${ExprFunctionCurrentUser.NAME}()"
        override fun getParameters(): List<Any> = listOf(
            // singular variable references
            TransformTestCase(
                """
                SELECT 
                    CURRENT_USER
                FROM foo
                """,
                """
                SELECT 
                    $currentUserFunction
                FROM foo
                """
            ),
            TransformTestCase(
                """
                SELECT 
                    *
                FROM foo
                WHERE a = CURRENT_USER
                """,
                """
                SELECT 
                    *
                FROM foo
                WHERE a = $currentUserFunction
                """
            ),
            TransformTestCase(
                """
                SELECT 
                    (SELECT CURRENT_USER FROM bar)
                FROM
                    (SELECT CURRENT_USER FROM zar)
                WHERE a = CURRENT_USER
                """,
                """
                SELECT 
                    (SELECT $currentUserFunction FROM bar)
                FROM 
                    (SELECT $currentUserFunction FROM zar)
                WHERE a = $currentUserFunction
                """
            ),
        )
    }
}
