package org.partiql.runner.executor

import com.amazon.ion.IonStruct
import com.amazon.ion.IonValue
import com.amazon.ionelement.api.AnyElement
import com.amazon.ionelement.api.ElementType
import com.amazon.ionelement.api.StructElement
import com.amazon.ionelement.api.toIonElement
import com.amazon.ionelement.api.toIonValue
import org.partiql.eval.Mode
import org.partiql.eval.Statement
import org.partiql.eval.compiler.PartiQLCompiler
import org.partiql.parser.PartiQLParserV1
import org.partiql.plan.Operation.Query
import org.partiql.planner.PartiQLPlanner
import org.partiql.runner.CompileType
import org.partiql.runner.ION
import org.partiql.runner.test.TestExecutor
import org.partiql.spi.Context
import org.partiql.spi.catalog.Catalog
import org.partiql.spi.catalog.Name
import org.partiql.spi.catalog.Session
import org.partiql.spi.catalog.Table
import org.partiql.spi.errors.PError
import org.partiql.spi.errors.PErrorException
import org.partiql.spi.errors.PErrorListener
import org.partiql.spi.errors.Severity
import org.partiql.spi.value.Datum
import org.partiql.types.PType
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.io.PartiQLValueIonReaderBuilder
import org.partiql.value.toIon

/**
 * @property session
 * @property mode
 */
@OptIn(PartiQLValueExperimental::class)
class EvalExecutor(
    private val session: Session,
    private val mode: Mode,
) : TestExecutor<Statement, Datum> {

    override fun prepare(input: String): Statement {
        val listener = getErrorListener(mode)
        val ctx = Context.of(listener)
        val parseResult = parser.parseSingle(input, ctx)
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
                        Severity.ERROR -> throw PErrorException(error)
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
        val partiql = PartiQLValueIonReaderBuilder.standard().build(value.toIonElement()).read()
        val datum = Datum.of(partiql)
        return datum
    }

    override fun toIon(value: Datum): IonValue {
        val partiql = value.toPartiQLValue()
        return partiql.toIon().toIonValue(ION)
    }

    // TODO: Use DATUM
    override fun compare(actual: Datum, expect: Datum): Boolean {
        return valueComparison(actual.toPartiQLValue(), expect.toPartiQLValue())
    }

    // Value comparison of PartiQL Value that utilized Ion Hashcode.
    // in here, null.bool is considered equivalent to null
    // missing is considered different from null
    // annotation::1 is considered different from 1
    // 1 of type INT is considered the same as 1 of type INT32
    // we should probably consider adding our own hashcode implementation
    private fun valueComparison(v1: PartiQLValue, v2: PartiQLValue): Boolean {
        // Additional check to put on annotation
        // we want to have
        // annotation::null.int == annotation::null.bool  <- True
        // annotation::null.int == other::null.int <- False
        if (v1.annotations != v2.annotations) {
            return false
        }
        if (v1.isNull && v2.isNull) {
            return true
        }
        // TODO: this comparator is designed for order by
        //  One of the false result it might produce is that
        //  it treats MISSING and NULL equally.
        //  we should move to hash or equals in value class once
        //  we finished implementing those.
        if (comparator.compare(v1, v2) == 0) {
            return true
        }
        if (v1.toIon().hashCode() == v2.toIon().hashCode()) {
            return true
        }
        // Ion element hash code contains a bug
        // Hashcode of BigIntIntElementImpl(BigInteger.ONE) is not the same as that of LongIntElementImpl(1)
        if (v1.toIon().type == ElementType.INT && v2.toIon().type == ElementType.INT) {
            return v1.toIon().asAnyElement().bigIntegerValue == v2.toIon().asAnyElement().bigIntegerValue
        }
        return false
    }

    companion object {
        val compiler = PartiQLCompiler.standard()
        val parser = PartiQLParserV1.standard()
        val planner = PartiQLPlanner.standard()
        // TODO REPLACE WITH DATUM COMPARATOR
        val comparator = PartiQLValue.comparator()
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
            val parseResult = parser.parseSingle("`$env`")
            val stmt = parseResult.statements[0]
            val plan = planner.plan(stmt, session).plan
            return (plan.getOperation() as Query).getRex().getType().getPType()
        }

        /**
         * Loads each declared global of the catalog from the data element.
         *
         * TODO until this point, PartiQL Kotlin has only done top-level bindings.
         * TODO https://github.com/partiql/partiql-tests/issues/127
         *
         * Test data is "PartiQL encoded as Ion" hence we need the PartiQLValueIonReader.
         */
        private fun Catalog.Builder.load(env: StructElement) {
            for (f in env.fields) {
                val name = Name.of(f.name)
                val value = PartiQLValueIonReaderBuilder.standard().build(f.value).read()
                val datum = Datum.of(value)
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
