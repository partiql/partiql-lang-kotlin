package org.partiql.runner.executor

import com.amazon.ion.IonStruct
import com.amazon.ion.IonValue
import com.amazon.ionelement.api.AnyElement
import com.amazon.ionelement.api.ElementType
import com.amazon.ionelement.api.StructElement
import com.amazon.ionelement.api.toIonElement
import com.amazon.ionelement.api.toIonValue
import org.partiql.eval.PartiQLEngine
import org.partiql.eval.PartiQLResult
import org.partiql.eval.PartiQLStatement
import org.partiql.parser.PartiQLParser
import org.partiql.plan.Statement
import org.partiql.planner.PartiQLPlanner
import org.partiql.planner.internal.SqlPlannerV1
import org.partiql.plugins.memory.MemoryCatalog
import org.partiql.plugins.memory.MemoryTable
import org.partiql.runner.CompileType
import org.partiql.runner.ION
import org.partiql.runner.test.TestExecutor
import org.partiql.spi.catalog.Catalog
import org.partiql.spi.catalog.Name
import org.partiql.spi.catalog.Session
import org.partiql.spi.value.Datum
import org.partiql.types.PType
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.io.PartiQLValueIonReaderBuilder
import org.partiql.value.toIon

@OptIn(PartiQLValueExperimental::class)
class EvalExecutor(
    private val session: Session,
    private val mode: PartiQLEngine.Mode,
) : TestExecutor<PartiQLStatement, PartiQLResult> {

    override fun prepare(statement: String): PartiQLStatement {
        val stmt = parser.parse(statement).root
        // TODO TEMPORARY REMOVE SqlPlannerV1 once existing public plan is removed.
        val plan = SqlPlannerV1.plan(stmt, session) {}
        return engine.prepare(plan, mode, session)
    }

    override fun execute(statement: PartiQLStatement): PartiQLResult {
        return statement.execute(session)
    }

    override fun fromIon(value: IonValue): PartiQLResult {
        val partiql = PartiQLValueIonReaderBuilder.standard().build(value.toIonElement()).read()

        return PartiQLResult.Value(partiql)
    }

    override fun toIon(value: PartiQLResult): IonValue {
        if (value is PartiQLResult.Value) {
            return value.value.toIon().toIonValue(ION)
        }
        error("PartiQLResult cannot be converted to Ion")
    }

    override fun compare(actual: PartiQLResult, expect: PartiQLResult): Boolean {
        if (actual is PartiQLResult.Value && expect is PartiQLResult.Value) {
            return valueComparison(actual.value, expect.value)
        }
        if (actual is PartiQLResult.Error) {
            throw actual.cause
        }
        val errorMessage = buildString {
            appendLine("Cannot compare different types of PartiQLResult.")
            appendLine(" - Expected : $expect")
            appendLine(" - Actual   : $actual")
        }
        error(errorMessage)
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
        val parser = PartiQLParser.default()
        val planner = PartiQLPlanner.standard()
        val engine = PartiQLEngine.standard()
        // TODO REPLACE WITH DATUM COMPARATOR
        val comparator = PartiQLValue.comparator()
    }

    object Factory : TestExecutor.Factory<PartiQLStatement, PartiQLResult> {

        override fun create(env: IonStruct, options: CompileType): TestExecutor<PartiQLStatement, PartiQLResult> {
            // infer catalog from conformance test `env`
            val catalog = infer(env.toIonElement() as StructElement)
            val session = Session.builder()
                .catalog("default")
                .catalogs(catalog)
                .build()
            val mode = when (options) {
                CompileType.PERMISSIVE -> PartiQLEngine.Mode.PERMISSIVE
                CompileType.STRICT -> PartiQLEngine.Mode.STRICT
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
            return MemoryCatalog.builder()
                .name("default")
                .apply { load(env) }
                .build()
        }

        /**
         * Uses the planner to infer the type of the environment.
         */
        private fun inferEnv(env: AnyElement): PType {
            val catalog = MemoryCatalog.builder().name("default").build()
            val session = Session.builder()
                .catalog("default")
                .catalogs(catalog)
                .build()
            val stmt = parser.parse("`$env`").root
            val plan = planner.plan(stmt, session)
            return (plan.plan.statement as Statement.Query).root.type
        }

        /**
         * Loads each declared global of the catalog from the data element.
         *
         * TODO until this point, PartiQL Kotlin has only done top-level bindings.
         */
        private fun MemoryCatalog.Builder.load(env: StructElement) {
            for (f in env.fields) {
                val name = Name.of(f.name)

                // TODO REMOVE SHIM
                val value = PartiQLValueIonReaderBuilder.standard().build(f.value).read()
                val datum = Datum.of(value)

                // TODO REMOVE SHIM
                // val datum = IonDatum.of(f.value)

                val table = MemoryTable.of(
                    name = name,
                    schema = PType.dynamic(),
                    datum = datum,
                )
                define(table)
            }
        }
    }
}
