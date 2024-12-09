package org.partiql.runner.schema

import com.amazon.ion.IonStruct
import org.partiql.runner.CompileType

sealed class TestCase {
    abstract val name: String
    abstract val env: IonStruct
    abstract val compileOptions: CompileType
    abstract val assertion: Assertion

    data class Equiv(
        override val name: String,
        val statements: List<String>,
        override val env: IonStruct,
        override val compileOptions: CompileType,
        override val assertion: Assertion
    ) : TestCase() {
        override fun toString(): String {
            return "$name, compileOption: $compileOptions"
        }
    }

    data class Eval(
        override val name: String,
        val statement: String,
        override val env: IonStruct,
        override val compileOptions: CompileType,
        override val assertion: Assertion
    ) : TestCase() {
        override fun toString(): String {
            return "$name, compileOption: $compileOptions"
        }
    }
}
