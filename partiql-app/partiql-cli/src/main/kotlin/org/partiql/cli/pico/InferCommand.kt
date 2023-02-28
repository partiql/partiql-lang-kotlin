package org.partiql.cli.pico

import org.partiql.plan.PartiQLSchemaInferencer
import org.partiql.plan.PlannerSession
import org.partiql.spi.Plugin
import org.partiql.spi.sources.TableSchema
import org.partiql.lang.types.DecimalType
import org.partiql.lang.types.StaticType
import picocli.CommandLine
import java.io.File
import java.time.Instant

@CommandLine.Command(
    name = "infer",
    mixinStandardHelpOptions = true,
    versionProvider = PartiQLVersionProvider::class,
    description = [
        "Infers the output schema of a PartiQL Query."
    ],
    showDefaultValues = true
)
internal class InferCommand(
    private val plugins: List<Plugin>
) : Runnable {

    @CommandLine.Parameters(arity = "0..1", index = "0..1", description = ["The filepath of the PartiQL query to infer"], paramLabel = "PARTIQL_FILE")
    var queryFile: File? = null

    @CommandLine.Option(names = ["-c", "--catalog"], description = ["Current catalog"], paramLabel = "CATALOG_NAME")
    var catalog: String? = null

    @CommandLine.Option(names = ["-s", "--schema"], description = ["Current schema"], paramLabel = "SCHEMA_NAME")
    var schema: String? = null

    @CommandLine.Option(names = ["-m", "--catalog-map"], description = ["Catalog map"], paramLabel = "SCHEMA_NAME")
    var catalogMap: Map<String, String> = emptyMap()

    override fun run() {
        val query = queryFile!!.inputStream().readBytes().toString(Charsets.UTF_8)
        val userId = System.getProperty("user.name") ?: "UNKNOWN_USER"
        val session = PlannerSession(query.hashCode().toString(), userId, catalog, schema, catalogMap, Instant.now())
        val schema = PartiQLSchemaInferencer.infer(query, session, plugins)
        val output = getSchemaString(schema)
        println(output)
        println("Inference completed in ${calculateInstantDiff(session.instant)} ms.")
    }

    private fun calculateInstantDiff(instant: Instant): Long {
        return Instant.now().toEpochMilli() - instant.toEpochMilli()
    }

    private fun getSchemaString(schema: TableSchema): String {
        val fields = schema.attributes
        return buildString {
            val title = "Schema Name: ${schema.root}"
            val titleLength = title.length + 6
            val typePairs = fields.map { field ->
                Pair(field.name, getTypeString(field.type))
            }
            val maxLhs = typePairs.maxOfOrNull { it.first.length } ?: 0
            val paddedTypePairs = typePairs.map {
                val newName = it.first.padEnd(maxLhs)
                Pair(newName, it.second)
            }
            val maxTypeLength = paddedTypePairs.maxOfOrNull { it.first.length + it.second.length + 11 } ?: 0
            val totalMax = maxOf(maxTypeLength, titleLength)
            val crossLine = buildString {
                repeat(totalMax) {
                    append("-")
                }
            }

            appendLine(crossLine)
            val newTitle = title.padEnd(totalMax - 6)
            appendLine("|  $newTitle  |")
            appendLine(crossLine)
            paddedTypePairs.forEach {
                val toRemove = maxLhs + 11
                val newSecond = it.second.padEnd(totalMax - toRemove)
                appendLine("|  ${it.first}  |  $newSecond  |")
            }
            append(crossLine)
        }
    }

    private fun getTypeString(type: StaticType): String = when (type) {
        is DecimalType -> {
            val suffix = when (val constraint = type.precisionScaleConstraint) {
                is DecimalType.PrecisionScaleConstraint.Unconstrained -> ""
                is DecimalType.PrecisionScaleConstraint.Constrained -> "(${constraint.precision}, ${constraint.scale})"
            }
            "decimal $suffix"
        }
        else -> type.toString()
    }
}
