package org.partiql.runner.executor

import com.amazon.ion.IonStruct
import com.amazon.ion.IonValue
import com.amazon.ionelement.api.AnyElement
import com.amazon.ionelement.api.StructElement
import com.amazon.ionelement.api.toIonElement
import com.amazon.ionelement.api.toIonValue
import org.partiql.eval.Mode
import org.partiql.eval.Statement
import org.partiql.eval.compiler.PartiQLCompiler
import org.partiql.parser.PartiQLParser
import org.partiql.plan.Action.Query
import org.partiql.planner.PartiQLPlanner
import org.partiql.runner.CompileType
import org.partiql.runner.ION
import org.partiql.runner.test.TestExecutor
import org.partiql.runner.util.toIonElement
import org.partiql.spi.Context
import org.partiql.spi.catalog.Catalog
import org.partiql.spi.catalog.Name
import org.partiql.spi.catalog.Session
import org.partiql.spi.catalog.Table
import org.partiql.spi.errors.PError
import org.partiql.spi.errors.PErrorListener
import org.partiql.spi.errors.PRuntimeException
import org.partiql.spi.errors.Severity
import org.partiql.spi.types.PType
import org.partiql.spi.value.Datum
import org.partiql.value.io.DatumIonReaderBuilder
import kotlin.test.assertEquals

/**
 * @property session
 * @property mode
 */
class EvalExecutor(
    private val session: Session,
    private val mode: Mode,
) : TestExecutor<Statement, Datum> {

    override fun prepare(input: String): Statement {
        val listener = getErrorListener(mode)
        val ctx = Context.of(listener)
        val parseResult = parser.parse(input, ctx)
        assertEquals(1, parseResult.statements.size)
        val ast = parseResult.statements[0]
        val plan = planner.plan(ast, session, ctx).plan
        return compiler.prepare(plan, mode, ctx)
    }

    private fun getErrorListener(mode: Mode): PErrorListener {
        return when (mode.code()) {
            Mode.PERMISSIVE -> PErrorListener.abortOnError()
            Mode.STRICT -> object : PErrorListener {
                override fun report(error: PError) {
                    when (error.severity.code()) {
                        Severity.ERROR -> throw PRuntimeException(error)
                        Severity.WARNING -> warning(error)
                        else -> error("Unhandled severity.")
                    }
                }

                private fun warning(error: PError) {
                    when (error.code()) {
                        PError.PATH_KEY_NEVER_SUCCEEDS,
                        PError.PATH_INDEX_NEVER_SUCCEEDS,
                        PError.VAR_REF_NOT_FOUND,
                        PError.PATH_SYMBOL_NEVER_SUCCEEDS -> {
                            error.severity = Severity.ERROR()
                            report(error)
                        }

                        else -> {
                            // Do nothing
                        }
                    }
                }
            }
            else -> error("This mode is not handled.")
        }
    }

    override fun execute(input: Statement): Datum {
        return input.execute()
    }

    override fun fromIon(value: IonValue): Datum {
        return DatumIonReaderBuilder.standard().build(value.toIonElement()).read()
    }

    override fun toIon(value: Datum): IonValue {
        return value.toIonElement().toIonValue(ION)
    }

    override fun compare(actual: Datum, expect: Datum): Boolean {
        // If the type is Ion, convert to lower case for comparison
        val actualValue = if (actual.type.code() == PType.VARIANT) actual.lower() else actual
        val expectedValue = if (expect.type.code() == PType.VARIANT) expect.lower() else expect
        return comparator.compare(actualValue, expectedValue) == 0
    }

    companion object {
        val compiler = PartiQLCompiler.standard()
        val parser = PartiQLParser.standard()
        val planner = PartiQLPlanner.standard()
        val comparator = Datum.comparator()
    }

    object Factory : TestExecutor.Factory<Statement, Datum> {

        override fun create(env: IonStruct, options: CompileType): TestExecutor<Statement, Datum> {
            // infer catalog from conformance test `env`
            val catalog = infer(env.toIonElement() as StructElement)
            val session = Session.builder()
                .catalog("default")
                .catalogs(catalog)
                .build()
            val mode = when (options) {
                CompileType.PERMISSIVE -> Mode.PERMISSIVE()
                CompileType.STRICT -> Mode.STRICT()
            }
            return EvalExecutor(session, mode)
        }

        /**
         * Produces an inferred catalog from the environment.
         *
         * @param env
         * @return
         */
        private fun infer(env: StructElement): Catalog {
            val map = mutableMapOf<String, PType>()
            env.fields.forEach {
                map[it.name] = inferEnv(it.value)
            }
            return Catalog.builder()
                .name("default")
                .apply { load(env) }
                .build()
        }

        /**
         * Uses the planner to infer the type of the environment.
         */
        private fun inferEnv(env: AnyElement): PType {
            val catalog = Catalog.builder().name("default").build()
            val session = Session.builder()
                .catalog("default")
                .catalogs(catalog)
                .build()
            val parseResult = parser.parse("`$env`")
            assertEquals(1, parseResult.statements.size)
            val stmt = parseResult.statements[0]
            val plan = planner.plan(stmt, session).plan
            return (plan.action as Query).getRex().getType().getPType()
        }

        /**
         * Loads each declared global of the catalog from the data element.
         *
         * TODO until this point, PartiQL Kotlin has only done top-level bindings.
         * TODO https://github.com/partiql/partiql-tests/issues/127
         *
         * Test data is "PartiQL encoded as Ion" hence we need the DatumIonReader.
         */
        private fun Catalog.Builder.load(env: StructElement) {
            for (f in env.fields) {
                val name = Name.of(f.name)
                val datum = DatumIonReaderBuilder.standard().build(f.value).read()
                val table = Table.standard(
                    name = name,
                    schema = PType.dynamic(),
                    datum = datum,
                )
                define(table)
            }
        }
    }
}
