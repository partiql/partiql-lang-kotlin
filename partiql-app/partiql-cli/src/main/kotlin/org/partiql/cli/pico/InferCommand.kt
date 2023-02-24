package org.partiql.cli.pico

import org.partiql.lang.eval.BindingCase
import org.partiql.lang.eval.BindingName
import org.partiql.lang.infer.Metadata
import org.partiql.lang.infer.PlannerContext
import org.partiql.lang.infer.QualifiedObjectName
import org.partiql.lang.infer.Session
import org.partiql.lang.infer.TableHandle
import org.partiql.lang.plugin.PluginManager
import org.partiql.plan.PartiQLSchemaInferencer
import org.partiql.spi.connector.ConnectorMetadata
import org.partiql.spi.connector.ConnectorSession
import org.partiql.spi.sources.TableSchema
import org.partiql.spi.types.DecimalType
import org.partiql.spi.types.StaticType
import picocli.CommandLine
import java.io.File

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
    private val manager: PluginManager
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
        val session = Session(query.hashCode().toString(), catalog, schema)
        val plannerCtx = PlannerContext(metadata = MetadataSimple(manager, catalogMap))
        val schema = PartiQLSchemaInferencer.infer(query, session, plannerCtx)
        val output = getSchemaString(schema)
        println(output)
    }

    private class MetadataSimple(
        private val manager: PluginManager,
        private val catalogMap: Map<String, String>
    ) : Metadata {
        override fun catalogExists(session: Session, catalogName: String): Boolean {
            return this.catalogMap.containsKey(catalogName)
        }

        override fun schemaExists(session: Session, catalogName: String, schemaName: String): Boolean {
            val connectorSession = session.toConnectorSession()
            val metadata = getMetadata(session.toConnectorSession(), catalogName)
            return metadata.schemaExists(connectorSession, BindingName(schemaName, BindingCase.SENSITIVE))
        }

        override fun getTableHandle(session: Session, tableName: QualifiedObjectName): TableHandle? {
            val connectorSession = session.toConnectorSession()
            val catalogName = tableName.catalogName?.name!!
            val metadata = getMetadata(session.toConnectorSession(), catalogName)
            return metadata.getTableHandle(connectorSession, tableName.schemaName!!, tableName.objectName!!)?.let {
                TableHandle(
                    connectorHandle = it,
                    catalogName = catalogName
                )
            }
        }

        override fun getTableSchema(session: Session, handle: TableHandle): TableSchema {
            val connectorSession = session.toConnectorSession()
            val metadata = getMetadata(session.toConnectorSession(), handle.catalogName)
            return metadata.getTableSchema(connectorSession, handle.connectorHandle)!!
        }

        private fun getMetadata(connectorSession: ConnectorSession, catalogName: String): ConnectorMetadata {
            val connectorName = catalogMap[catalogName]!!
            val connectorFactory = manager.connectorFactories.first { it.getName() == connectorName }
            val connector = connectorFactory.create()
            return connector.getMetadata(session = connectorSession)
        }
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
            appendLine(crossLine)
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
