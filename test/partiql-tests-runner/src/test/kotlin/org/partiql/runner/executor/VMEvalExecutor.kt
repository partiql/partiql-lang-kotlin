package org.partiql.runner.executor

import com.amazon.ion.IonStruct
import com.amazon.ion.IonValue
import com.amazon.ionelement.api.AnyElement
import com.amazon.ionelement.api.StructElement
import com.amazon.ionelement.api.toIonElement
import com.amazon.ionelement.api.toIonValue
import org.partiql.eval.ExecutionPlan
import org.partiql.eval.Mode
import org.partiql.eval.PartiQLVM
import org.partiql.eval.compiler.PartiQLCompiler
import org.partiql.parser.PartiQLParser
import org.partiql.plan.Action.Query
import org.partiql.plan.SymbolTable
import org.partiql.planner.PartiQLPlanner
import org.partiql.runner.CompileType
import org.partiql.runner.ION
import org.partiql.runner.test.TestExecutor
import org.partiql.runner.util.toIonElement
import org.partiql.spi.catalog.Catalog
import org.partiql.spi.catalog.ExecutionCatalog
import org.partiql.spi.catalog.Name
import org.partiql.spi.catalog.Session
import org.partiql.spi.catalog.Table
import org.partiql.spi.types.PType
import org.partiql.spi.value.Datum
import org.partiql.value.io.DatumIonReaderBuilder
import kotlin.test.assertEquals

/**
 * Wrapper holding an ExecutionPlan + its SymbolTable for catalog resolution at execute time.
 */
class VMPreparedPlan(val execPlan: ExecutionPlan, val symbols: SymbolTable)

/**
 * VM-based conformance test executor.
 *
 * Uses the thread-safe path: useRefs() planner → compile() → PartiQLVM.execute().
 */
class VMEvalExecutor(
    private val session: Session,
    private val mode: Mode,
) : TestExecutor<VMPreparedPlan, Datum> {

    override fun prepare(input: String): VMPreparedPlan {
        val parseResult = parser.parse(input)
        assertEquals(1, parseResult.statements.size)
        val ast = parseResult.statements[0]
        val result = planner.plan(ast, session)
        val execPlan = compiler.compile(result.plan, mode)
        return VMPreparedPlan(execPlan, result.symbols)
    }

    override fun execute(input: VMPreparedPlan): Datum {
        val catalogs = buildCatalogs(input.symbols, session)
        return vm.execute(input.execPlan, catalogs)
    }

    override fun fromIon(value: IonValue): Datum {
        return DatumIonReaderBuilder.standard().build(value.toIonElement()).read()
    }

    override fun toIon(value: Datum): IonValue {
        return value.toIonElement().toIonValue(ION)
    }

    override fun compare(actual: Datum, expect: Datum): Boolean {
        val actualValue = if (actual.type.code() == PType.VARIANT) actual.lower() else actual
        val expectedValue = if (expect.type.code() == PType.VARIANT) expect.lower() else expect
        return comparator.compare(actualValue, expectedValue) == 0
    }

    companion object {
        val compiler = PartiQLCompiler.standard()
        val parser = PartiQLParser.standard()
        val planner = PartiQLPlanner.builder().useRefs().build()
        val vm = PartiQLVM.standard()
        val comparator = Datum.comparator(true, true)

        private fun buildCatalogs(symbols: SymbolTable, session: Session): Array<ExecutionCatalog> {
            return Array(symbols.catalogCount()) { catalogId ->
                val catalogName = symbols.getCatalogName(catalogId)
                val catalog = session.getCatalogs().getCatalog(catalogName)
                ExecutionCatalog { id ->
                    val entry = symbols.getTables(catalogId)[id]
                    catalog?.getTable(session, entry.name)
                        ?: error("Table '${entry.name}' not found in catalog '$catalogName'")
                }
            }
        }
    }

    object Factory : TestExecutor.Factory<VMPreparedPlan, Datum> {

        override fun create(env: IonStruct, options: CompileType): TestExecutor<VMPreparedPlan, Datum> {
            val catalog = infer(env.toIonElement() as StructElement)
            val session = Session.builder()
                .catalog("default")
                .catalogs(catalog)
                .build()
            val mode = when (options) {
                CompileType.PERMISSIVE -> Mode.PERMISSIVE()
                CompileType.STRICT -> Mode.STRICT()
            }
            return VMEvalExecutor(session, mode)
        }

        private fun infer(env: StructElement): Catalog {
            val map = mutableMapOf<String, PType>()
            env.fields.forEach {
                map[it.name] = inferEnv(it.value)
            }
            return Catalog.builder()
                .name("default")
                .apply { load(env, map) }
                .build()
        }

        private fun inferEnv(env: AnyElement): PType {
            val annotations = env.annotations
            if (annotations.size >= 3 && annotations[annotations.size - 3] == "\$map") {
                val keyType = pTypeFromName(annotations[annotations.size - 2])
                val valueType = pTypeFromName(annotations[annotations.size - 1])
                return PType.map(keyType, valueType)
            }
            val catalog = Catalog.builder().name("default").build()
            val session = Session.builder()
                .catalog("default")
                .catalogs(catalog)
                .build()
            val parseResult = parser.parse("`$env`")
            assertEquals(1, parseResult.statements.size)
            val stmt = parseResult.statements[0]
            val plan = PartiQLPlanner.standard().plan(stmt, session).plan
            return (plan.action as Query).getRex().getType().pType
        }

        private fun pTypeFromName(name: String): PType = when (name.lowercase()) {
            "dynamic" -> PType.dynamic()
            "bool" -> PType.bool()
            "tinyint" -> PType.tinyint()
            "smallint" -> PType.smallint()
            "integer", "int" -> PType.integer()
            "bigint" -> PType.bigint()
            "numeric" -> PType.numeric()
            "decimal" -> PType.decimal(38, 19)
            "real" -> PType.real()
            "double" -> PType.doublePrecision()
            "string" -> PType.string()
            "char" -> PType.character(255)
            "varchar" -> PType.varchar(255)
            "date" -> PType.date()
            "time" -> PType.time(6)
            "timestamp" -> PType.timestamp(6)
            else -> error("unsupported PType name: $name")
        }

        private fun Catalog.Builder.load(env: StructElement, schemas: Map<String, PType>) {
            for (f in env.fields) {
                val name = Name.of(f.name)
                val datum = DatumIonReaderBuilder.standard().build(f.value).read()
                val table = Table.standard(
                    name = name,
                    schema = schemas[f.name] ?: PType.dynamic(),
                    datum = datum,
                )
                define(table)
            }
        }
    }
}
