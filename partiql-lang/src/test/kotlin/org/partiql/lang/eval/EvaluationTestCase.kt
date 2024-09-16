package org.partiql.lang.eval

import com.amazon.ion.IonType
import com.amazon.ion.IonWriter
import com.amazon.ion.system.IonReaderBuilder
import com.amazon.ion.system.IonSystemBuilder
import com.amazon.ion.system.IonTextWriterBuilder
import org.partiql.errors.ErrorCode
import org.partiql.errors.PropertyValueMap
import org.partiql.lang.CompilerPipeline
import org.partiql.lang.eval.CastTestBase.Companion.toSession
import org.partiql.lang.eval.evaluatortestframework.EvaluatorErrorTestCase
import org.partiql.lang.eval.evaluatortestframework.EvaluatorTestCase
import org.partiql.lang.eval.evaluatortestframework.EvaluatorTestTarget
import org.partiql.lang.eval.evaluatortestframework.ExpectedResultFormat
import java.io.File
import java.io.FileWriter
import java.nio.file.Paths

class EvaluationTestCase(
    val name: String,
    val statement: String,
    val assert: Assertion
) {

    fun append() {
        val writer = IonTextWriterBuilder.pretty().build(System.out as Appendable)
        append(writer)
    }

    /**
     * This appends to the file if it exists.
     */
    fun append(path: String) {
        val writer = writer(path)
        append(writer)
        writer.close()
    }

    fun append(writer: IonWriter) {
        writer.stepIn(IonType.STRUCT)
        writer.setFieldName("name")
        writer.writeString(name)
        writer.setFieldName("statement")
        writer.writeString(statement)
        writer.setFieldName("assert")
        assert.print(writer)
        writer.stepOut()
    }

    companion object {
        val COERCE: List<Mode> = listOf(Mode.EvalModeCoerce)
        val ERROR: List<Mode> = listOf(Mode.EvalModeError)
        val ALL_MODES: List<Mode> = listOf(Mode.EvalModeCoerce, Mode.EvalModeError)

        private val root = "../test/partiql-tests-runner/src/test/resources/ported/eval"

        fun runEvaluatorTestCase(
            query: String,
            session: EvaluationSession = EvaluationSession.standard(),
            expectedResult: String,
            expectedResultFormat: ExpectedResultFormat = ExpectedResultFormat.ION,
            target: EvaluatorTestTarget = EvaluatorTestTarget.ALL_PIPELINES,
        ): EvaluationTestCase {
            val assertion = when (expectedResultFormat) {
                ExpectedResultFormat.STRICT, ExpectedResultFormat.PARTIQL -> EvaluationTestCase.Assertion.SuccessPartiQL(
                    EvaluationTestCase.ALL_MODES, expectedResult, emptyMap()
                )
                ExpectedResultFormat.ION -> EvaluationTestCase.Assertion.Success(
                    EvaluationTestCase.ALL_MODES, expectedResult
                )
            }
            return EvaluationTestCase(query, query, assertion)
        }

        fun runEvaluatorErrorTestCase(
            query: String,
            expectedErrorCode: ErrorCode,
            expectedErrorContext: PropertyValueMap? = null,
            expectedPermissiveModeResult: String? = null,
        ): EvaluationTestCase {
            val newQuery = cleanPartiQL(query)
            val assertion = EvaluationTestCase.Assertion.Multi(
                listOfNotNull(
                    EvaluationTestCase.Assertion.Failure(
                        EvaluationTestCase.ERROR
                    ),
                    expectedPermissiveModeResult?.let {
                        EvaluationTestCase.Assertion.SuccessPartiQL(
                            EvaluationTestCase.COERCE,
                            expectedPermissiveModeResult,
                            emptyMap()
                        )
                    }
                )
            )
            return EvaluationTestCase(newQuery, newQuery, assertion)
        }

        fun print(path: String, cases: List<EvaluationTestCase>, env: Map<String, String>) {
            val file = File(root, path)
            if (file.exists()) {
                file.writeText("")
                file.createNewFile()
            } else {
                file.parentFile.mkdirs()
                file.createNewFile()
            }
            val writer = writer(path)
            printEnv(writer, env)
            cases.forEach {
                it.append(writer)
            }
            writer.close()
        }

        fun print(cases: List<EvaluationTestCase>, env: Map<String, String>) {
            val writer = writer()
            printEnv(writer, env)
            cases.forEach {
                it.append(writer)
            }
        }

        fun printEnv(writer: IonWriter, env: Map<String, String>) {
            if (env.isEmpty()) {
                return
            }
            writer.addTypeAnnotation("envs")
            writer.stepIn(IonType.STRUCT)
            env.forEach { key, value ->
                writer.setFieldName(key)
                val reader = IonReaderBuilder.standard().build(value)
                reader.next()
                writer.writeValue(reader)
            }
            writer.stepOut()
        }

        fun writer(): IonWriter {
            return IonTextWriterBuilder.pretty().build(System.out as Appendable)
        }

        fun writer(path: String): IonWriter {
            val prefix = root
            val nPath = Paths.get(prefix, path)
            val file = File(nPath.toUri())
            if (!file.exists()) {
                file.parentFile.mkdirs()
                file.createNewFile()
            }
            val fileWriter = FileWriter(file, true)
            return IonTextWriterBuilder.pretty().build(fileWriter)
        }

        /**
         * Removes comments. Cleans up new lines. Etc.
         */
        fun cleanPartiQL(input: String): String {
            return input.lines().map {
                // Remove comments
                val index = it.indexOf("--")
                when (index < 0) {
                    true -> it
                    false -> it.substring(0, index)
                }
            }.map { it.trim() }.joinToString(" ")
        }

        fun fromEvaluatorTestCase(tc: EvaluatorErrorTestCase): EvaluationTestCase {
            val name = tc.query
            val statement = tc.query
            val assertion = when (tc.expectedPermissiveModeResult) {
                null -> Assertion.Failure(ALL_MODES)
                else -> {
                    Assertion.Multi(
                        listOf(
                            Assertion.Failure(ERROR),
                            Assertion.SuccessPartiQL(COERCE, tc.expectedPermissiveModeResult!!, emptyMap())
                        )
                    )
                }
            }
            return EvaluationTestCase(name, statement, assertion)
        }

        fun fromEvaluatorTestCase(tc: EvaluatorTestCase): EvaluationTestCase {
            val queryClean = cleanPartiQL(tc.query)
            val newQuery: String
            val outputStrict: String
            val outputPermissive: String
            when (tc.expectedResultFormat) {
                ExpectedResultFormat.ION -> {
                    newQuery = queryClean
                    outputStrict = tc.expectedResult
                    outputPermissive = tc.expectedPermissiveModeResult
                }
                // The conformance test runner doesn't interpret PartiQL input
                ExpectedResultFormat.STRICT, ExpectedResultFormat.PARTIQL -> {
                    val ionSystem = IonSystemBuilder.standard().build()
                    val expectedResult = CompilerPipeline.standard().compile(tc.expectedResult).evaluate(EvaluationSession.standard()) as PartiQLResult.Value
                    val expectedIon = expectedResult.value.toIonValue(ionSystem)
                    val expectedPermissiveResult = CompilerPipeline.standard().compile(tc.expectedResult).evaluate(EvaluationSession.standard()) as PartiQLResult.Value
                    val expectedPermissiveIon = expectedPermissiveResult.value.toIonValue(ionSystem)
                    newQuery = queryClean
                    outputStrict = expectedIon.toString()
                    outputPermissive = expectedPermissiveIon.toString()
                }
            }
            val assertion = when (tc.expectedPermissiveModeResult == tc.expectedResult) {
                true -> Assertion.Success(ALL_MODES, outputStrict)
                false -> Assertion.Multi(
                    listOf(
                        Assertion.Success(ERROR, outputStrict),
                        Assertion.Success(COERCE, outputPermissive),
                    )
                )
            }
            return EvaluationTestCase(newQuery, newQuery, assertion)
        }
    }

    sealed class Assertion {
        abstract fun print(writer: IonWriter)

        protected fun printModes(writer: IonWriter, mode: List<Mode>) {
            when (mode.size) {
                0 -> error("No mode specified")
                1 -> writer.writeSymbol(mode[0].name)
                else -> {
                    writer.stepIn(IonType.LIST)
                    mode.forEach { writer.writeSymbol(it.name) }
                    writer.stepOut()
                }
            }
        }

        abstract class _Success : Assertion() {
            abstract val modes: List<Mode>
            abstract val output: String

            override fun print(writer: IonWriter) {
                writer.stepIn(IonType.STRUCT)
                writer.setFieldName("result")
                writer.writeSymbol("EvaluationSuccess")
                writer.setFieldName("evalMode")
                printModes(writer, modes)
                writer.setFieldName("output")
                val reader = IonReaderBuilder.standard().build(output)
                reader.next()
                writer.writeValue(reader)
                writer.stepOut()
            }
        }

        class SuccessPartiQL(
            override val modes: List<Mode>,
            output: String,
            val env: Map<String, String>
        ) : _Success() {
            override val output: String = kotlin.run {
                val session = env.toSession()
                val result = CompilerPipeline.standard().compile(output).evaluate(session) as PartiQLResult.Value
                result.value.toIonValue(IonSystemBuilder.standard().build()).toString()
            }

            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (other !is SuccessPartiQL) return false

                if (modes != other.modes) return false
                if (env != other.env) return false
                if (output != other.output) return false

                return true
            }

            override fun hashCode(): Int {
                var result = modes.hashCode()
                result = 31 * result + env.hashCode()
                result = 31 * result + output.hashCode()
                return result
            }
        }

        class Success(
            override val modes: List<Mode>,
            override val output: String
        ) : _Success() {
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (other !is Success) return false

                if (modes != other.modes) return false
                if (output != other.output) return false

                return true
            }

            override fun hashCode(): Int {
                var result = modes.hashCode()
                result = 31 * result + output.hashCode()
                return result
            }
        }

        class Failure(
            val modes: List<Mode>
        ) : Assertion() {
            override fun print(writer: IonWriter) {
                writer.stepIn(IonType.STRUCT)
                writer.setFieldName("result")
                writer.writeSymbol("EvaluationFail")
                writer.setFieldName("evalMode")
                printModes(writer, modes)
                writer.stepOut()
            }

            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (other !is Failure) return false

                if (modes != other.modes) return false

                return true
            }

            override fun hashCode(): Int {
                return modes.hashCode()
            }
        }

        class Multi(
            val cases: List<Assertion>
        ) : Assertion() {

            constructor(vararg cases: Assertion) : this(cases.toList())

            override fun print(writer: IonWriter) {
                writer.stepIn(IonType.LIST)
                cases.forEach { it.print(writer) }
                writer.stepOut()
            }
        }
    }

    enum class Mode {
        EvalModeCoerce,
        EvalModeError
    }
}
