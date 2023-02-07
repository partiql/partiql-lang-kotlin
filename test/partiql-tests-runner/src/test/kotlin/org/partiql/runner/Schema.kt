package org.partiql.runner

import com.amazon.ion.IonStruct
import com.amazon.ion.IonValue
import org.partiql.lang.eval.CompileOptions

data class Namespace(
    var env: IonStruct,
    val namespaces: MutableList<Namespace>,
    val testCases: MutableList<TestCase>,
    val equivClasses: MutableMap<String, List<String>>
)

data class EquivalenceClass(val id: String, val statements: List<String>)

sealed class Assertion {
    data class EvaluationSuccess(val expectedResult: IonValue) : Assertion()
    object EvaluationFailure : Assertion()
    // TODO: other assertion and test categories: https://github.com/partiql/partiql-tests/issues/35
}

sealed class TestCase {
    abstract val name: String
    abstract val env: IonStruct
    abstract val compileOptions: CompileOptions
    abstract val assertion: Assertion
}

data class EvalTestCase(
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

data class EvalEquivTestCase(
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
