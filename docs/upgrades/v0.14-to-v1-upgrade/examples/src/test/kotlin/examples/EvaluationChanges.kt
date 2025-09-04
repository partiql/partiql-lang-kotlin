package examples

import org.partiql.lang.CompilerPipeline
import org.partiql.lang.eval.CompileOptions
import org.partiql.lang.eval.EvaluationSession
import org.partiql.lang.eval.ExprFunction
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.ExprValueType
import org.partiql.lang.eval.PartiQLResult
import org.partiql.lang.eval.TypingMode
import org.partiql.lang.eval.namedValue
import org.partiql.lang.types.FunctionSignature
import org.partiql.types.StaticType.Companion.STRING
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class EvaluationChanges {
    @Test
    fun `safe exprvalue access pattern`() {
        // In PLK v0.14.9, values are represented as ExprValue
        // Safe access requires checking type and using scalar interface
        fun getDataAsString(value: ExprValue): String? {
            val type = value.type
            // Check type and handle null/missing manually
            return when (type) {
                ExprValueType.NULL -> "null"
                ExprValueType.MISSING -> "missing"
                ExprValueType.INT -> value.scalar.numberValue().toString()
                ExprValueType.STRING -> value.scalar.stringValue()
                ExprValueType.BOOL -> value.scalar.booleanValue().toString()
                ExprValueType.FLOAT -> value.scalar.numberValue().toString()
                ExprValueType.DECIMAL -> value.scalar.numberValue().toString()
                else -> throw RuntimeException("Unsupported type: $type")
            }
        }
        assertEquals("42", getDataAsString(ExprValue.newInt(42)))
        assertEquals("hello", getDataAsString(ExprValue.newString("hello")))
        assertEquals("null", getDataAsString(ExprValue.nullValue))
        assertEquals("missing", getDataAsString(ExprValue.missingValue))
    }

    // Helper functions for orderly materialization test
    // These helper functions memoize the data after iterating by return an ExprValue,
    // which ensuring that the data is backed by in-memory data structures instead of PartiQL engine.
    // Dispatches to appropriate materialization function based on data type
    fun materializeInOrder(value: ExprValue): ExprValue {
        val type = value.type
        return when (type) {
            ExprValueType.LIST -> materializeList(value)
            ExprValueType.BAG -> materializeBag(value)
            ExprValueType.SEXP -> materializeSexp(value)
            ExprValueType.STRUCT -> materializeStruct(value)
            else -> materializeScalar(value)
        }
    }

    // Materializes list elements in order by iterating through each element
    // Back the elements with a List
    fun materializeList(value: ExprValue): ExprValue {
        val elements = mutableListOf<ExprValue>()
        value.forEach { element ->
            elements.add(materializeInOrder(element))
        }
        return ExprValue.newList(elements)
    }

    // Materializes bag elements in order by iterating through each element
    // Back the elements with a List
    fun materializeBag(value: ExprValue): ExprValue {
        val elements = mutableListOf<ExprValue>()
        value.forEach { element ->
            elements.add(materializeInOrder(element))
        }
        return ExprValue.newBag(elements)
    }

    // Materializes sexp elements in order by iterating through each element
    // Back the elements with a List
    fun materializeSexp(value: ExprValue): ExprValue {
        val elements = mutableListOf<ExprValue>()
        value.forEach { element ->
            elements.add(materializeInOrder(element))
        }
        return ExprValue.newSexp(elements)
    }

    // Materializes struct fields in order by iterating through each field value
    // Back the fields with a List
    fun materializeStruct(value: ExprValue): ExprValue {
        val fields = mutableListOf<ExprValue>()
        for (field in value) {
            val materializedField = materializeInOrder(field)
            fields.add(materializedField)
        }
        return ExprValue.newStruct(fields, org.partiql.lang.eval.StructOrdering.UNORDERED)
    }

    // Materializes scalar values by accessing their primitive data
    fun materializeScalar(value: ExprValue): ExprValue {
        if (value.type == ExprValueType.NULL || value.type == ExprValueType.MISSING) {
            return value
        }
        val type = value.type
        when (type) {
            ExprValueType.INT -> value.scalar.numberValue()
            ExprValueType.STRING -> value.scalar.stringValue()
            ExprValueType.BOOL -> value.scalar.booleanValue()
            // ... rest of scalar types
            else -> throw RuntimeException("Unsupported type: $type")
        }
        return value
    }

    @Test
    fun `orderly materialization of semi-structured data`() {
        // In PLK 0.14.9, semi-structured data must be materialized in a predictable order
        // This is critical for stateful processes that depend on consistent traversal patterns
        // which means for semi-structured type, we need to fully materialize its first element
        // before the second one (and so on).
        val bagValue = ExprValue.newBag(
            listOf(
                ExprValue.newInt(42),
                ExprValue.newStruct(
                    listOf(
                        ExprValue.newString("value").namedValue(ExprValue.newString("nested"))
                    ),
                    org.partiql.lang.eval.StructOrdering.UNORDERED
                )
            )
        )
        val structValue = ExprValue.newStruct(
            listOf(
                ExprValue.newInt(42).namedValue(ExprValue.newString("id")),
                ExprValue.newList(listOf(ExprValue.newString("a"), ExprValue.newString("b")))
                    .namedValue(ExprValue.newString("items"))
            ),
            org.partiql.lang.eval.StructOrdering.UNORDERED
        )
        // Materialize in order for semi-structured data
        materializeInOrder(bagValue)
        materializeInOrder(structValue)
        assertEquals(2, bagValue.count())
        assertEquals(2, structValue.count())
    }

    @Test
    fun `working with evaluation environment`() {
        // In PLK 0.14.9, evaluation context is managed through EvaluationSession
        // Create sessions with parameters
        val session1 = EvaluationSession.build {
            parameters(listOf(ExprValue.newString("a"), ExprValue.newInt(1)))
        }
        val session2 = EvaluationSession.build {
            parameters(listOf(ExprValue.newString("b"), ExprValue.newInt(2), ExprValue.newBoolean(true)))
        }
        // Combine sessions -- create a new session with combined parameters
        val combinedParams = session1.parameters + session2.parameters
        val combinedSession = EvaluationSession.build {
            parameters(combinedParams)
        }
        assertEquals(true, combinedSession.parameters[4].scalar.booleanValue())
        // Access parameters by index
        val param1 = session1.parameters[0]
        val param2 = session2.parameters[1]
        assertEquals("a", param1.scalar.stringValue())
        assertEquals(2L, param2.scalar.numberValue())
    }

    @Test
    fun `working with compiler pipeline builder`() {
        // In PLK 0.14.9, compiler pipeline customization is done through custom functions and procedures
        val pipeline = CompilerPipeline.builder()
            .addFunction(object : ExprFunction {
                override val signature = FunctionSignature(
                    name = "custom_strategy",
                    requiredParameters = listOf(),
                    returnType = STRING
                )
                override fun callWithRequired(
                    session: EvaluationSession,
                    required: List<ExprValue>
                ): ExprValue {
                    return ExprValue.newString("custom strategy")
                }
            })
            .build()
        // Create a simple expression and evaluate it using the custom function
        val session = EvaluationSession.standard()
        val expression = pipeline.compile("custom_strategy()")
        val result = expression.evaluate(session)
        assertEquals("custom strategy", (result as PartiQLResult.Value).value.scalar.stringValue())
    }

    @Test
    fun `working with evaluation modes`() {
        // In PLK 0.14.9, typing behavior controlled via CompileOptions.typingMode
        // PERMISSIVE: Returns missing for type mismatches
        // LEGACY: Throws exceptions for type mismatches
        val permissivePipeline = CompilerPipeline.builder()
            .compileOptions(
                CompileOptions.build {
                    typingMode(TypingMode.PERMISSIVE)
                }
            )
            .build()
        val legacyPipeline = CompilerPipeline.builder()
            .compileOptions(
                CompileOptions.build {
                    typingMode(TypingMode.LEGACY)
                }
            )
            .build()
        val session = EvaluationSession.standard()
        // PERMISSIVE: type error returns NULL
        val permissiveExpression = permissivePipeline.compile("1 + 'a'")
        val permissiveResult = permissiveExpression.evaluate(session)
        assertEquals(ExprValueType.MISSING, (permissiveResult as PartiQLResult.Value).value.type)
        // LEGACY: type error throws exception
        val legacyExpression = legacyPipeline.compile("1 + 'a'")
        assertFailsWith<Exception> {
            legacyExpression.evaluate(session)
        }
    }
}
