package org.partiql.transpiler.sql

import org.junit.jupiter.api.Test
import org.partiql.ast.sql
import org.partiql.ast.sql.SqlLayout
import org.partiql.plan.Global
import org.partiql.plan.Identifier
import org.partiql.plan.Plan
import org.partiql.plan.Rel
import org.partiql.plan.Rex
import org.partiql.plan.Statement
import org.partiql.plan.builder.PlanFactory
import org.partiql.transpiler.ProblemCallback
import org.partiql.types.StaticType
import org.partiql.types.function.FunctionParameter
import org.partiql.types.function.FunctionSignature
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType
import org.partiql.value.stringValue
import kotlin.test.assertEquals

@OptIn(PartiQLValueExperimental::class)
class SqlTransformTest {

    @Test
    fun sanity() {
        // SELECT _1.a, _1.b, _1.c FROM T AS _1

        // SCAN global(T)
        // |> PROJECT var(0).a, var(0).b, var(0).c
        // |> SELECT { 'a': var(0), 'b': var(1), 'c': var(2) }

        val (globals, statement) = with(Plan) {
            // {
            //   T : << >>
            // }
            val globals = listOf(
                global(
                    identifierQualified(
                        root = identifierSymbol("T", Identifier.CaseSensitivity.INSENSITIVE),
                        steps = emptyList(),
                    ),
                    StaticType.BAG
                ),
            )
            val scan = rel(
                type = schema("_1" to StaticType.STRUCT), op = relOpScan(rex(StaticType.STRUCT, rexOpGlobal(0)))
            )

            val var0 = rex(StaticType.STRUCT, rexOpVarResolved(0))

            // PROJECT var(0).a, var(0).b, var(0).c
            val project = rel(
                type = schema(
                    "a" to StaticType.INT,
                    "b" to StaticType.INT,
                    "c" to StaticType.INT,
                ),
                op = relOpProject(
                    input = scan,
                    projections = listOf(
                        rex(StaticType.INT, path(var0, "a")),
                        rex(StaticType.INT, path(var0, "b")),
                        rex(StaticType.INT, path(var0, "c")),
                    )
                )
            )

            val select = rex(
                type = StaticType.BAG,
                op = rexOpSelect(
                    constructor = rex(
                        type = StaticType.STRUCT,
                        op = rexOpStruct(
                            listOf(
                                rexOpStructField(
                                    k = rex(StaticType.STRING, rexOpLit(stringValue("a"))),
                                    v = rex(StaticType.INT, rexOpVarResolved(0)),
                                ),
                                rexOpStructField(
                                    k = rex(StaticType.STRING, rexOpLit(stringValue("b"))),
                                    v = rex(StaticType.INT, rexOpVarResolved(1)),
                                ),
                                rexOpStructField(
                                    k = rex(StaticType.STRING, rexOpLit(stringValue("c"))),
                                    v = rex(StaticType.INT, rexOpVarResolved(2)),
                                ),
                            )
                        )
                    ),
                    rel = project
                )
            )
            val statement = statementQuery(select)

            //
            globals to statement
        }

        // SELECT a, b, c FROM T AS _1
        //
        // Normalized: SELECT _1.a AS a, _1.b AS b, _1.c AS c FROM T AS _1
        //
        // Equivalent: SELECT VALUE { 'a': _1.a, 'b': _1.b, 'c': _1.c } FROM T AS _1

        // SCAN global(T)
        // |> PROJECT var(0).a, var(0).b, var(0).c
        // |> SELECT { 'a': var(0), 'b': var(1), 'c': var(2) }

        val case = Case(
            input = statement,
            expected = "SELECT _1.a AS a, _1.b AS b, _1.c AS c FROM T AS _1",
            globals = globals,
        )
        case.assert()
    }

    @Test
    fun calls() {
        // SELECT a + b, abs(c) FROM T AS _1
        // SELECT _1.a + _1.b AS _1, abs(_1.c) AS _2 FROM T AS _1

        // SCAN global(T)
        // |> PROJECT call(plus, [var(0).a, var(0).b]), call(abs, [var(0).c])
        // |> SELECT { '_1': var(0), '_2': var(1) }

        val (globals, statement) = with(Plan) {
            // {
            //   T : << >>
            // }
            val globals = listOf(
                global(
                    identifierQualified(
                        root = identifierSymbol("T", Identifier.CaseSensitivity.INSENSITIVE),
                        steps = emptyList(),
                    ),
                    StaticType.BAG
                ),
            )
            val scan = rel(
                type = schema("_1" to StaticType.STRUCT), op = relOpScan(rex(StaticType.STRUCT, rexOpGlobal(0)))
            )

            val var0 = rex(StaticType.STRUCT, rexOpVarResolved(0))

            val plus = FunctionSignature(
                name = "plus", returns = PartiQLValueType.INT,
                parameters = listOf(
                    FunctionParameter("lhs", PartiQLValueType.INT),
                    FunctionParameter("rhs", PartiQLValueType.INT),
                )
            )

            val abs = FunctionSignature(
                name = "abs", returns = PartiQLValueType.INT,
                parameters = listOf(
                    FunctionParameter("value", PartiQLValueType.INT),
                )
            )

            // |> PROJECT call(plus, [var(0).a, var(0).b]), call(abs, [var(0).c])
            val project = rel(
                type = schema(
                    "_1" to StaticType.INT,
                    "_2" to StaticType.INT,
                ),
                op = relOpProject(
                    input = scan,
                    projections = listOf(
                        rex(
                            StaticType.INT,
                            rexOpCall(
                                fn = fnResolved(plus),
                                args = listOf(
                                    (rex(StaticType.INT, path(var0, "a"))),
                                    (rex(StaticType.INT, path(var0, "b"))),
                                )
                            )
                        ),
                        rex(
                            StaticType.INT,
                            rexOpCall(
                                fn = fnResolved(abs),
                                args = listOf(
                                    (rex(StaticType.INT, path(var0, "c"))),
                                )
                            )
                        ),
                    )
                )
            )

            val select = rex(
                type = StaticType.BAG,
                op = rexOpSelect(
                    constructor = rex(
                        type = StaticType.STRUCT,
                        op = rexOpStruct(
                            listOf(
                                rexOpStructField(
                                    k = rex(StaticType.STRING, rexOpLit(stringValue("_1"))),
                                    v = rex(StaticType.INT, rexOpVarResolved(0)),
                                ),
                                rexOpStructField(
                                    k = rex(StaticType.STRING, rexOpLit(stringValue("_2"))),
                                    v = rex(StaticType.INT, rexOpVarResolved(1)),
                                ),
                            )
                        )
                    ),
                    rel = project
                )
            )
            val statement = statementQuery(select)

            //
            globals to statement
        }

        // SELECT a, b, c FROM T AS _1
        //
        // Normalized: SELECT _1.a AS a, _1.b AS b, _1.c AS c FROM T AS _1
        //
        // Equivalent: SELECT VALUE { 'a': _1.a, 'b': _1.b, 'c': _1.c } FROM T AS _1

        // SCAN global(T)
        // |> PROJECT var(0).a, var(0).b, var(0).c
        // |> SELECT { 'a': var(0), 'b': var(1), 'c': var(2) }

        val case = Case(
            input = statement,
            expected = "SELECT _1.a + _1.b AS _1, abs(_1.c) AS _2 FROM T AS _1",
            globals = globals,
        )
        case.assert()
    }

    private fun PlanFactory.schema(vararg vars: Pair<String, StaticType>): Rel.Type {
        val schema = vars.map { relBinding(it.first, it.second) }
        return relType(schema, emptySet())
    }

    companion object {

        val problemThrower: ProblemCallback = { it -> error(it.toString()) }
    }

    private class Case(
        private val input: Statement,
        private val expected: String,
        private val globals: List<Global>,
    ) {

        fun assert() {
            val transform = SqlTransform(globals, SqlCalls.DEFAULT, problemThrower)
            val ast = transform.apply(input)
            val actual = ast.sql(SqlLayout.ONELINE)
            assertEquals(expected, actual)
        }
    }

    private fun PlanFactory.path(root: Rex, vararg steps: String) = rexOpPath(
        root = root,
        steps = steps.map { rexOpPathStepIndex(rex(StaticType.STRING, rexOpLit(stringValue(it)))) },
    )
}
