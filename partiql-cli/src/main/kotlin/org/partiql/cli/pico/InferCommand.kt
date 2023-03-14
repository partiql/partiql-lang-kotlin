/*
 * Copyright Amazon.com, Inc. or its affiliates.  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License").
 *  You may not use this file except in compliance with the License.
 *  A copy of the License is located at:
 *
 *       http://aws.amazon.com/apache2.0/
 *
 *  or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 *  language governing permissions and limitations under the License.
 */

package org.partiql.cli.pico

import com.amazon.ion.system.IonReaderBuilder
import com.amazon.ionelement.api.StructElement
import com.amazon.ionelement.api.createIonElementLoader
import org.partiql.lang.planner.transforms.PartiQLSchemaInferencer
import org.partiql.lang.planner.transforms.PlannerSession
import org.partiql.spi.Plugin
import org.partiql.spi.sources.ValueDescriptor
import org.partiql.spi.sources.ValueDescriptor.TableDescriptor
import org.partiql.spi.sources.ValueDescriptor.TypeDescriptor
import org.partiql.types.DecimalType
import org.partiql.types.StaticType
import picocli.CommandLine
import java.io.File
import java.nio.file.Paths
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

    @CommandLine.Option(names = ["-d", "--directory"], description = ["Default directory"], paramLabel = "DIRECTORY")
    var namespace: List<String> = emptyList()

    @CommandLine.Option(names = ["-p", "--plugin-config"], description = ["Plugins configuration directory"], paramLabel = "PATH")
    var pluginConfigDirectory: File? = null

    override fun run() {
        val query = queryFile!!.inputStream().readBytes().toString(Charsets.UTF_8)
        val userId = System.getProperty("user.name") ?: "UNKNOWN_USER"
        val pluginConfigDirectory = pluginConfigDirectory ?: Paths.get(System.getProperty("user.home")).resolve(".partiql/plugins").toFile()
        val ionReaderBuilder = IonReaderBuilder.standard()
        val pluginConfigs = pluginConfigDirectory.listFiles()?.associate {
            val reader = ionReaderBuilder.build(it.readText())
            val element = createIonElementLoader().loadSingleElement(reader) as StructElement
            it.nameWithoutExtension to element
        } ?: emptyMap()
        val session = PlannerSession(query.hashCode().toString(), userId, catalog, namespace, pluginConfigs, Instant.now())
        val descriptor = infer(query, session)
        val output = getDescriptorString(descriptor)
        println(output)
        println("Inference completed in ${calculateInstantDiff(session.instant)} ms.")
    }

    internal fun infer(query: String, session: PlannerSession): ValueDescriptor {
        return PartiQLSchemaInferencer.infer(query, PartiQLSchemaInferencer.Context(session, plugins))
    }

    private fun calculateInstantDiff(instant: Instant): Long {
        return Instant.now().toEpochMilli() - instant.toEpochMilli()
    }

    private fun getDescriptorString(descriptor: ValueDescriptor): String = when (descriptor) {
        is TableDescriptor -> getSchemaString(descriptor)
        is TypeDescriptor -> descriptor.toString()
    }

    private fun getSchemaString(descriptor: TableDescriptor): String {
        val fields = descriptor.attributes
        return buildString {
            val title = "Schema Name: ${descriptor.name}"
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
