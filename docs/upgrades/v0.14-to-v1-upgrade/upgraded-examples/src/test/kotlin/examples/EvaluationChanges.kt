package examples

import org.partiql.ast.Literal
import org.partiql.ast.Query
import org.partiql.ast.expr.ExprLit
import org.partiql.ast.expr.ExprOperator
import org.partiql.eval.Environment
import org.partiql.eval.Expr
import org.partiql.eval.ExprValue
import org.partiql.eval.Mode
import org.partiql.eval.Row
import org.partiql.eval.compiler.Match
import org.partiql.eval.compiler.PartiQLCompiler
import org.partiql.eval.compiler.Pattern
import org.partiql.eval.compiler.Strategy
import org.partiql.plan.Operator
import org.partiql.planner.PartiQLPlanner
import org.partiql.spi.catalog.Session
import org.partiql.spi.types.PType
import org.partiql.spi.value.Datum
import org.partiql.spi.value.Field
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class EvaluationChanges {
    @Test
    fun `safe datum access pattern`() {
        // In PLK v1, values are represented as Datum
        // Safe access requires checking type, null, and missing
        fun getDataAsString(value: Datum): String {
            val type = value.type // using getType() method
            val typeCode = type.code()
            // Check if null, using isNull() method
            if (value.isNull) {
                return "null"
            }
            // Check if missing, using isMissing() method
            if (value.isMissing) {
                return "missing"
            }
            // Check the type of the value
            return when (typeCode) {
                PType.INTEGER -> value.int.toString()
                PType.BIGINT -> value.long.toString()
                PType.SMALLINT -> value.short.toString()
                PType.STRING -> value.string
                PType.BOOL -> value.boolean.toString()
                else -> throw RuntimeException("Unsupported type: $type")
            }
        }
        assertEquals("42", getDataAsString(Datum.integer(42)))
        assertEquals("hello", getDataAsString(Datum.string("hello")))
        assertEquals("null", getDataAsString(Datum.nullValue()))
        assertEquals("missing", getDataAsString(Datum.missing()))
    }

    // Helper functions for orderly materialization test
    // These helper functions memoize the data after iterating by return a Datum,
    // which ensuring that the data is backed by in-memory data structures instead of PartiQL engine.
    // Dispatches to appropriate materialization function based on data type
    fun materializeInOrder(d: Datum): Datum {
        val type = d.type
        val typeCode = type.code()
        return when (typeCode) {
            PType.ARRAY -> materializeArray(d)
            PType.BAG -> materializeBag(d)
            PType.ROW -> materializeRow(d)
            PType.STRUCT -> materializeStruct(d)
            else -> materializeScalar(d)
        }
    }

    // Materializes array elements in order by iterating through each element
    // Back the elements with a List
    fun materializeArray(d: Datum): Datum {
        val elements = mutableListOf<Datum>()
        d.forEach { element ->
            elements.add(materializeInOrder(element))
        }
        return Datum.array(elements)
    }

    // Materializes bag elements in order by iterating through each element
    // Back the elements with a List
    fun materializeBag(d: Datum): Datum {
        val elements = mutableListOf<Datum>()
        d.forEach { element ->
            elements.add(materializeInOrder(element))
        }
        return Datum.bag(elements)
    }

    // Materializes row fields in order by iterating through each field value
    // Back the fields with a List
    fun materializeRow(d: Datum): Datum {
        val fields = mutableListOf<Field>()
        d.fields.forEach { field ->
            val newValue = materializeInOrder(field.value)
            fields.add(Field.of(field.name, newValue))
        }
        return Datum.row(fields)
    }

    // Materializes struct fields in order by iterating through each field value
    // Back the fields with a List
    fun materializeStruct(d: Datum): Datum {
        val fields = mutableListOf<Field>()
        d.fields.forEach { field ->
            val newValue = materializeInOrder(field.value)
            fields.add(Field.of(field.name, newValue))
        }
        return Datum.struct(fields)
    }

    // Materializes scalar values by accessing their primitive data
    fun materializeScalar(d: Datum): Datum {
        if (d.isNull || d.isMissing) {
            return d
        }
        val type = d.type
        val typeCode = d.type.code()
        when (typeCode) {
            PType.INTEGER -> d.int
            PType.STRING -> d.string
            PType.BOOL -> d.boolean
            // ... rest of Scalar types
            else -> throw RuntimeException("Unsupported type: $type")
        }
        return d
    }

    @Test
    fun `orderly materialization of semi-structured data`() {
        // In PLK v1, semi-structured data must be materialized in a predictable order
        // This is critical for stateful processes that depend on consistent traversal patterns
        // which means for semi-structured type, we need to fully materialize its first element
        // before the second one (and so on).
        val bagValue = Datum.bag(
            listOf(
                Datum.integer(42),
                Datum.struct(
                    Field.of("nested", Datum.string("value"))
                )
            )
        )
        val structValue = Datum.struct(
            Field.of("id", Datum.integer(42)),
            Field.of("items", Datum.array(listOf(Datum.string("a"), Datum.string("b"))))
        )
        // Materialize in order for semi-structured data
        materializeInOrder(bagValue)
        materializeInOrder(structValue)
        assertEquals(2, bagValue.count())
        assertEquals(2, structValue.fields.asSequence().count())
    }

    @Test
    fun `working with evaluation environment`() {
        // In PLK v1, evaluation context is managed through Environment and Row
        // Create an empty environment
        val emptyEnv = Environment()
        // Create a row with Datum[]
        val row1 = Row(arrayOf(Datum.string("a"), Datum.integer(1)))
        // Create a row with multiple Datum values
        val row2 = Row.of(Datum.string("b"), Datum.integer(2), Datum.bool(true))
        // Manipulate rows - concatenation
        val row3 = row1.concat(row2)
        assertEquals(true, row3.values[4].boolean)
        // Environment stack operations - push returns new environment
        val env1 = emptyEnv.push(row1)
        val env2 = env1.push(row2)
        // Access variables by depth and offset
        val param1 = env2.get(0, 0)
        val param2 = env2.get(1, 1)
        assertEquals("b", param1.string)
        assertEquals(1, param2.int)
    }

    @Test
    fun `working with compiler builder and strategies`() {
        // In PLK v1, PartiQLCompiler can be built with custom strategies
        val compiler = PartiQLCompiler.builder()
            .addStrategy(object : Strategy(
                Pattern(Operator::class.java)
            ) {
                    override fun apply(
                        match: Match,
                        mode: Mode,
                        callback: Callback
                    ): Expr {
                        return ExprValue { Datum.string("custom strategy") }
                    }
                }
            )
            .build()
        assertTrue(compiler is PartiQLCompiler)
        // Create a simple plan and statement to test the custom strategy
        val literal = Literal.string("hello")
        val exprLit = ExprLit(literal)
        val query = Query.builder().expr(exprLit).build()
        val planner = PartiQLPlanner.standard()
        val session = Session.empty()
        val planResult = planner.plan(query, session)
        val plan = planResult.plan
        val statement = compiler.prepare(plan, Mode.PERMISSIVE())
        val result = statement.execute()
        assertEquals("custom strategy", result.string)
    }

    @Test
    fun `working with evaluation modes`() {
        // In PLK v1, Mode enum controls evaluation behavior
        // PERMISSIVE: Returns missing for type mismatches
        // STRICT: Throws exceptions for type mismatches
        val compiler = PartiQLCompiler.standard()
        val planner = PartiQLPlanner.standard()
        val session = Session.empty()
        // Create a query that subtracts string from integer: "1-a"
        val query = Query.builder().expr(
            ExprOperator(
                "-",
                ExprLit(Literal.intNum(1L)),
                ExprLit(Literal.string("a"))
            )
        ).build()
        val planResult = planner.plan(query, session)
        val plan = planResult.plan
        // PERMISSIVE mode: returns missing for type mismatch
        val permissiveStatement = compiler.prepare(plan, Mode.PERMISSIVE())
        val permissiveResult = permissiveStatement.execute()
        assertTrue(permissiveResult.isMissing)
        // STRICT mode: throws exception for type mismatch
        val strictStatement = compiler.prepare(plan, Mode.STRICT())
        assertFailsWith<Exception> {
            strictStatement.execute()
        }
    }
}
