package org.partiql.lang.util.testdsl

import org.partiql.lang.eval.CompileOptions
import org.partiql.lang.eval.ExprValue

interface GroupBuilder {
    /** Defines a test to be added to the current group. */
    fun test(
        name: String,
        sql: String,
        expectedLegacyModeIonResult: String,
        expectedPermissiveModeIonResult: String = expectedLegacyModeIonResult,
        compileOptionsBuilderBlock: CompileOptions.Builder.() -> Unit = { },
        extraAssertions: (ExprValue) -> Unit = { }
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
        expectedLegacyModeIonResult: String,
        expectedPermissiveModeIonResult: String,
        compileOptionsBuilderBlock: CompileOptions.Builder.() -> Unit,
        extraAssertions: (ExprValue) -> Unit
    ) {
        tests.add(
            IonResultTestCase(
                name = name,
                sqlUnderTest = sql,
                expectedLegacyModeIonResult = expectedLegacyModeIonResult,
                expectedPermissiveModeIonResult = expectedPermissiveModeIonResult,
                compileOptionsBuilderBlock = compileOptionsBuilderBlock,
                extraAssertions = extraAssertions
            )
        )
    }

    fun build() = IonResultTestGroup(groupName, tests)
}
