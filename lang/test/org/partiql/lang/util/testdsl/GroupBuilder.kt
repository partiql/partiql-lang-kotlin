package org.partiql.lang.util.testdsl

import org.partiql.lang.eval.CompileOptions
import org.partiql.lang.eval.ExprValue

interface GroupBuilder {
    /** Defines a test to be added to the current group. */
    fun test(
        name: String,
        sql: String,
        expected: String?,
        compileOptions: CompileOptions = CompileOptions.standard(),
        extraAssertions: ((ExprValue, CompileOptions) -> Unit)? = null
    )
}

/** Builds an [IonResultTestGroup]. */
@TestDslMarker
class GroupBuilderImpl(private val groupName: String) : GroupBuilder {
    private val tests = mutableListOf<IonResultTestCase>()

    /** Defines a test to be added to the current group. */
    override fun test(
        name: String,
        sql: String,
        expected: String?,
        compileOptions: CompileOptions,
        extraAssertions: ((ExprValue, CompileOptions) -> Unit)?
    ) {
        tests.add(
            IonResultTestCase(
                name = name,
                sqlUnderTest = sql,
                expectedIonResult = expected,
                compileOptions = compileOptions,
                extraAssertions = extraAssertions
            )
        )
    }

    fun build() = IonResultTestGroup(groupName, tests)
}
