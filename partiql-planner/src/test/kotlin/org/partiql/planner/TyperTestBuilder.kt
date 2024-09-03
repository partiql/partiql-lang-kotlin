package org.partiql.planner

import com.amazon.ionelement.api.AnyElement
import com.amazon.ionelement.api.ElementType
import com.amazon.ionelement.api.SexpElement
import com.amazon.ionelement.api.StructElement
import org.partiql.planner.test.PartiQLTest
import org.partiql.planner.test.PartiQLTestProvider
import org.partiql.planner.test.Test
import org.partiql.planner.test.TestBuilder
import org.partiql.planner.test.TestId
import org.partiql.types.Field
import org.partiql.types.PType

/**
 * This parses an Ion test configuration where the test type is set to "type". Based on the test body's [FIELD_STATUS],
 * this will either return a [TyperTest.Success] or [TyperTest.Failure].
 * @see FIELD_STATUS
 * @see STATUS_SUCCESS
 * @see STATUS_FAILURE
 * @see TyperTest
 * @see TyperTest.Success
 * @see TyperTest.Failure
 */
class TyperTestBuilder : TestBuilder {

    private var name: TestId? = null
    private var expectedType: PType? = null
    private var isSuccess: Boolean = true
    private var statement: String? = null
    private var sessionCatalog: String? = null
    private var sessionDirectory: List<String>? = null
    private var inputProvider: PartiQLTestProvider? = null
    private var assertion: ProblemAssertion? = null

    companion object {
        const val STATUS_SUCCESS = "SUCCESS"
        const val STATUS_FAILURE = "FAILURE"
        const val FIELD_EXPECTED = "expected"
        const val FIELD_STATUS = "status"
        const val FIELD_STATEMENT = "statement"
        const val FIELD_SESSION = "session"
        const val FIELD_CATALOG = "catalog"
        const val FIELD_CWD = "cwd"
        const val FIELD_ASSERT_PROBLEM_EXISTS = "assertProblemExists"
    }

    override fun id(id: TestId): TestBuilder = this.apply {
        this.name = id
    }

    override fun config(body: StructElement, statementProvider: PartiQLTestProvider): TestBuilder = this.apply {
        this.inputProvider = statementProvider
        val session = body[FIELD_SESSION].asStruct()
        this.sessionCatalog = session[FIELD_CATALOG].asString().textValue
        this.sessionDirectory = session[FIELD_CWD].asList().values.map { it.asString().textValue }
        this.statement = getStatement(body[FIELD_STATEMENT])
        val status = body[FIELD_STATUS].asSymbol().textValue
        when (status) {
            STATUS_SUCCESS -> {
                this.isSuccess = true
                this.expectedType = TypeParser.parse(body[FIELD_EXPECTED])
            }
            STATUS_FAILURE -> {
                this.isSuccess = false
                if (body.containsField(FIELD_EXPECTED)) {
                    this.expectedType = TypeParser.parse(body[FIELD_EXPECTED])
                }
                if (body.containsField(FIELD_ASSERT_PROBLEM_EXISTS)) {
                    val sexp = body[FIELD_ASSERT_PROBLEM_EXISTS].asSexp()
                    val severity = sexp.values[0].textValue
                    val message = sexp.values[1].textValue
                    this.assertion = ProblemAssertion(severity, message)
                }
            }
        }
    }

    override fun build(): Test {
        return when (this.isSuccess) {
            true -> TyperTest.Success(this.name.toString(), this.expectedType!!, this.statement!!, this.sessionCatalog!!, this.sessionDirectory!!)
            false -> TyperTest.Failure(this.name.toString(), this.expectedType, this.assertion, this.statement!!, this.sessionCatalog!!, this.sessionDirectory!!)
        }
    }

    private fun getStatement(element: AnyElement): String {
        return when (element.type) {
            ElementType.STRING, ElementType.SYMBOL -> element.textValue
            ElementType.SEXP -> {
                val sexp = element.asSexp()
                val values = sexp.values
                assert(values[0].textValue == "@")
                val remaining = values.subList(1, values.size)
                assert(remaining.size == 3)
                assert(remaining[0].textValue == "\$inputs")
                val group = remaining[1].textValue
                val name = remaining[2].textValue
                return inputProvider?.get(PartiQLTest.Key(group, name))?.statement ?: error("Input Provider: $inputProvider")
            }
            else -> "Unsupported element type for statement: ${element.type}"
        }
    }

    private object TypeParser {
        fun parse(element: AnyElement): PType {
            return when (element.type) {
                ElementType.STRING, ElementType.SYMBOL -> parseTypeSimple(element.textValue.trim())
                ElementType.SEXP -> parseTypeComplex(element.asSexp())
                else -> error("Cannot parse type from ${element.type}.")
            }
        }

        val SIMPLE_TYPE_MAP = mapOf(
            PType.Kind.DECIMAL to PType.decimal(),
            PType.Kind.STRING to PType.string(),
            PType.Kind.BOOL to PType.bool(),
            PType.Kind.TINYINT to PType.tinyint(),
            PType.Kind.SMALLINT to PType.smallint(),
            PType.Kind.INTEGER to PType.integer(),
            PType.Kind.BIGINT to PType.bigint(),
            PType.Kind.NUMERIC to PType.numeric(),
            PType.Kind.DECIMAL_ARBITRARY to PType.decimal(),
            PType.Kind.STRING to PType.string(),
            PType.Kind.STRUCT to PType.struct(),
            PType.Kind.ARRAY to PType.array(),
            PType.Kind.BAG to PType.bag(),
            PType.Kind.BOOL to PType.bool(),
            PType.Kind.DYNAMIC to PType.dynamic(),
            PType.Kind.DOUBLE to PType.doublePrecision(),
            PType.Kind.DATE to PType.date()
        )

        val COMPLEX_TYPE_MAP = mapOf(
            PType.Kind.DECIMAL to TypeParser::typeDecimal,
            PType.Kind.CHAR to { params -> typeWithParamInt(params, PType::character) },
            PType.Kind.VARCHAR to { params -> typeWithParamInt(params, PType::varchar) },
            PType.Kind.CLOB to { params -> typeWithParamInt(params, PType::clob) },
            PType.Kind.BLOB to { params -> typeWithParamInt(params, PType::blob) },
            PType.Kind.TIME to { params -> typeWithParamInt(params, PType::time) },
            PType.Kind.TIMEZ to { params -> typeWithParamInt(params, PType::timez) },
            PType.Kind.TIMESTAMP to { params -> typeWithParamInt(params, PType::timestamp) },
            PType.Kind.TIMESTAMPZ to { params -> typeWithParamInt(params, PType::timestampz) },
            PType.Kind.BAG to { params -> typeWithParamType(params, PType::bag) },
            PType.Kind.ARRAY to { params -> typeWithParamType(params, PType::array) },
            PType.Kind.SEXP to { params -> typeWithParamType(params, PType::sexp) },
            PType.Kind.ROW to TypeParser::typeRow,
        )

        private fun parseTypeComplex(element: SexpElement): PType {
            val values = element.values
            val kind = PType.Kind.valueOf(values.first().textValue.trim())
            val params = values.drop(1)
            return COMPLEX_TYPE_MAP[kind]?.invoke(params) ?: error("Unsupported type: $kind")
        }

        private fun typeDecimal(params: List<AnyElement>): PType {
            assert(params.size == 2)
            val precision = params[0].asInt().longValue.toInt()
            val scale = params[1].asInt().longValue.toInt()
            return PType.decimal(precision, scale)
        }

        private fun typeWithParamInt(params: List<AnyElement>, typeInit: (Int) -> PType): PType {
            assert(params.size == 1)
            val length = params[0].asInt().longValue.toInt()
            return typeInit(length)
        }

        private fun typeWithParamType(params: List<AnyElement>, typeInit: (PType) -> PType): PType {
            assert(params.size == 1)
            val typeParam = parse(params[0])
            return typeInit(typeParam)
        }

        private fun typeRow(params: List<AnyElement>): PType {
            assert(params.size % 2 == 0)
            val fields = mutableListOf<Field>()
            for (i in params.indices.filter { it % 2 == 0 }) {
                val name = params[i].textValue
                val type = parse(params[i + 1])
                fields.add(Field.of(name, type))
            }
            return PType.row(fields)
        }

        private fun parseTypeSimple(name: String): PType {
            val kind = PType.Kind.valueOf(name)
            return SIMPLE_TYPE_MAP[kind] ?: error("Unsupported type: $name")
        }
    }

    class ProblemAssertion(
        val severity: String,
        val message: String
    )
}
