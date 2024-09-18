package org.partiql.runner.schema

import com.amazon.ion.IonStruct
import org.partiql_v0_14_8.lang.eval.CompileOptions

sealed class TestCase {
    abstract val name: String
    abstract val env: IonStruct
    abstract val compileOptions: CompileOptions
    abstract val assertion: Assertion

    data class Equiv(
        override val name: String,
        val statements: List<String>,
        override val env: IonStruct,
        override val compileOptions: CompileOptions,
        override val assertion: Assertion
    ) : TestCase() {
        override fun toString(): String {
            return name + ", compileOption: " + compileOptions.typingMode
        }
    }

    data class Eval(
        override val name: String,
        val statement: String,
        override val env: IonStruct,
        override val compileOptions: CompileOptions,
        override val assertion: Assertion
    ) : TestCase() {
        override fun toString(): String {
            return name + ", compileOption: " + compileOptions.typingMode
        }
    }
}
