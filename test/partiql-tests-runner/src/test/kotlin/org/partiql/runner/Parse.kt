package org.partiql.runner

import com.amazon.ion.IonList
import com.amazon.ion.IonStruct
import com.amazon.ion.IonSymbol
import com.amazon.ion.IonType
import com.amazon.ion.IonValue
import org.partiql.lang.eval.CompileOptions
import org.partiql.lang.eval.TypingMode
import org.partiql.lang.util.asIonStruct
import org.partiql.lang.util.stringValue

/**
 * Parses the [testStruct] to a list of [TestCase]s with respect to the environments and equivalence classes provided
 * in the [curNamespace].
 */
private fun parseTestCase(testStruct: IonStruct, curNamespace: Namespace): List<TestCase> {
    val testCases = mutableListOf<TestCase>()
    val name = testStruct.get("name").stringValue() ?: error("Expected test case to have field `name`")
    val statement = testStruct.get("statement") ?: error("Expected test case to have field `statement`")
    val env = testStruct.get("env") ?: curNamespace.env
    val assertList = when (val assert = testStruct.get("assert") ?: error("Expected test case to have field `assert`")) {
        is IonStruct -> listOf(assert)
        is IonList -> assert.toList()
        else -> error("Expect `assert` field to be an IonStruct or IonList")
    }

    testCases += assertList.map { assertion ->
        val assertionStruct = assertion as IonStruct
        val evalModeList = when (val evalModeIonValue = assertionStruct.get("evalMode")) {
            is IonSymbol -> listOf(evalModeIonValue.stringValue())
            is IonList -> evalModeIonValue.toList().map { it.stringValue() }
            else -> error("evalMode expects IonSymbol or IonList")
        }

        evalModeList.map { evalMode ->
            val compileOption = when (evalMode) {
                "EvalModeError" -> CompileOptions.build { typingMode(TypingMode.LEGACY) }
                "EvalModeCoerce" -> CompileOptions.build { typingMode(TypingMode.PERMISSIVE) }
                else -> error("unsupported eval modes")
            }
            val evalResult: Assertion = when (assertionStruct.get("result").stringValue()) {
                "EvaluationSuccess" -> Assertion.EvaluationSuccess(assertionStruct.get("output"))
                "EvaluationFail" -> Assertion.EvaluationFailure
                else -> error("expected one of EvaluationSuccess or EvaluationFail")
            }

            when (statement.type) {
                // statement being an IonString indicates that this is an Eval test case
                IonType.STRING -> EvalTestCase(
                    name = name,
                    statement = statement.stringValue() ?: error("Expected `statement` to be a string"),
                    env = env.asIonStruct(),
                    compileOptions = compileOption,
                    assertion = evalResult
                )
                // statement being an IonSymbol indicates that this is an eval equivalence test case
                IonType.SYMBOL -> {
                    val equivClassId = statement.stringValue() ?: error("Expected `statement` to be a symbol")
                    EvalEquivTestCase(
                        name = name,
                        statements = curNamespace.equivClasses[equivClassId] ?: error("Equiv class $equivClassId not defined in current namespace"),
                        env = env.asIonStruct(),
                        compileOptions = compileOption,
                        assertion = evalResult
                    )
                }
                else -> TODO("Support other test case categories: https://github.com/partiql/partiql-tests/issues/35")
            }
        }
    }.flatten()
    return testCases
}

private fun parseEquivalenceClass(equivClassStruct: IonStruct): EquivalenceClass {
    val id = equivClassStruct.get("id") ?: error("Expected field `id` for equivalence class struct: $equivClassStruct ")
    val statements = equivClassStruct.get("statements") ?: error("Expected field `statements` for equivalence class struct: $equivClassStruct")

    val idAsString = id.stringValue() ?: error("Expected `id` to be an IonSymbol")
    val statementsAsStrings = (statements as IonList).map { statement ->
        statement.stringValue() ?: error("Expected each statement within equivalence class to be a string $statement")
    }
    return EquivalenceClass(
        idAsString,
        statementsAsStrings
    )
}

/**
 * Parses [data] with the [curNamespace] into a new [Namespace].
 */
internal fun parseNamespace(curNamespace: Namespace, data: IonValue): Namespace {
    return when (data) {
        is IonList -> {
            val newNamespace = Namespace(
                env = curNamespace.env,
                namespaces = mutableListOf(),
                testCases = mutableListOf(),
                equivClasses = mutableMapOf()
            )
            data.forEach { d ->
                parseNamespace(newNamespace, d)
            }
            curNamespace.namespaces.add(newNamespace)
            curNamespace
        }
        is IonStruct -> {
            // environment, equivalence class, or test case. add to current namespace
            val annotations = data.typeAnnotations
            when {
                annotations.contains("envs") -> {
                    curNamespace.env = data
                }
                annotations.contains("equiv_class") -> {
                    // equivalence class
                    val equivClass = parseEquivalenceClass(data)
                    curNamespace.equivClasses[equivClass.id] = equivClass.statements
                }
                annotations.isEmpty() -> {
                    // test case
                    curNamespace.testCases.addAll(parseTestCase(data, curNamespace))
                }
            }
            curNamespace
        }
        else -> error("Document parsing requires an IonList or IonStruct")
    }
}
