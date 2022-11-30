/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package org.partiql.gradle.plugin.pig

import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.partiql.pig.main
import java.io.File
import javax.inject.Inject

/**
 * Imported from https://github.com/partiql/partiql-ir-generator/tree/main/pig-gradle-plugin
 *
 * TODO REMOVE ONCE `pig-gradle-plugin` is published
 */
abstract class PigPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        // Ensure `sourceSets` extension exists
        target.pluginManager.apply(JavaPlugin::class.java)

        // Adds pig source set extension to all source sets
        target.sourceSets().forEach { sourceSet ->
            val name = sourceSet.name
            val sds = target.objects.sourceDirectorySet(name, "$name PIG source")
            sds.srcDir("src/$name/pig")
            sds.include("**/*.ion")
            sourceSet.extensions.add("pig", sds)
        }

        // Extensions for pig compiler arguments
        val ext = target.extensions.create("pig", PigExtension::class.java)

        // Create tasks after source sets have been evaluated
        target.afterEvaluate {
            target.sourceSets().forEach { sourceSet ->
                // Pig generate all for the given source set
                val pigAllTaskName = getPigAllTaskName(sourceSet)
                val pigAllTask = target.tasks.create(pigAllTaskName) {
                    this.group = "pig"
                    this.description = "Generate all PIG sources for ${sourceSet.name} source set"
                }

                val language = ext.language
                val namespace = ext.namespace
                var outDir = ext.outputDir

                // The `kotlin` target without an `outputDir` will use the package path as the output dir
                if (outDir == null && language == "kotlin") {
                    val base = "${target.buildDir}/generated-src"
                    outDir = File("$base/${namespace.replace('.', '/')}")
                    // Eventually this will need to be sourceSet.kotlin, but that's currently not possible
                    // sourceSet.java.srcDir(base)
                }

                // Create a pig task for each type universe and each source set
                (sourceSet.extensions.getByName("pig") as SourceDirectorySet).files.forEach { file ->
                    val universeName = file.name.removeSuffix(".ion").lowerToCamelCase().capitalize()
                    val pigTask = target.tasks.create(pigAllTaskName + universeName, PigTask::class.java) {
                        val task = this
                        task.description = "Generated PIG sources for $file"
                        task.universe.set(file.absolutePath)
                        task.target.set(language)
                        if (namespace.isNotEmpty()) {
                            task.namespace.set(namespace)
                        }
                        if (outDir != null) {
                            task.outputDir.set(outDir.absolutePath)
                        }
                        if (ext.outputFile != null) {
                            task.outputFile.set(ext.outputFile!!.absolutePath)
                        }
                        if (ext.template.isNotEmpty()) {
                            task.template.set(ext.template)
                        }
                    }
                    pigAllTask.dependsOn(pigTask)

                    if (language == "kotlin") {
                        target.tasks.named("compileKotlin") {
                            dependsOn(pigAllTask)
                        }
                    }
                }
            }
        }
    }

    private fun Project.sourceSets(): List<SourceSet> = extensions.getByType(SourceSetContainer::class.java).toList()

    private fun getPigAllTaskName(sourceSet: SourceSet) = when (SourceSet.isMain(sourceSet)) {
        true -> "generatePigSource"
        else -> "generatePig${sourceSet.name.capitalize()}Source"
    }

    /**
     * Type Universe files are lower hyphen, but Gradle tasks are lower camel
     */
    private fun String.lowerToCamelCase(): String =
        this.split('-')
            .filter { it.isNotEmpty() }
            .mapIndexed { i, str ->
                when (i) {
                    0 -> str
                    else -> str.capitalize()
                }
            }
            .joinToString(separator = "")
}

abstract class PigTask : DefaultTask() {

    init {
        group = "pig"
    }

    @get:Input
    @get:Option(
        option = "universe",
        description = "Type universe input file"
    )
    abstract val universe: Property<String>

    @get:Input
    @get:Option(
        option = "target",
        description = "Target language"
    )
    abstract val target: Property<String>

    @get:Input
    @get:Optional
    @get:Option(
        option = "outputFile",
        description = "Generated output file (for targets that output a single file)"
    )
    abstract val outputFile: Property<String>

    @get:Input
    @get:Optional
    @get:Option(
        option = "outputDir",
        description = "Generated output directory (for targets that output multiple files)"
    )
    abstract val outputDir: Property<String>

    @get:Input
    @get:Optional
    @get:Option(
        option = "namespace",
        description = "Namespace for generated code"
    )
    abstract val namespace: Property<String>

    @get:Input
    @get:Optional
    @get:Option(
        option = "template",
        description = "Path to an Apache FreeMarker template"
    )
    abstract val template: Property<String>

    @TaskAction
    fun action() {
        val args = mutableListOf<String>()
        // required args
        args += listOf("-u", universe.get())
        args += listOf("-t", target.get())
        // optional args
        if (outputFile.isPresent) {
            args += listOf("-o", outputFile.get())
        }
        if (outputDir.isPresent) {
            args += listOf("-d", outputDir.get())
        }
        if (namespace.isPresent) {
            args += listOf("-n", namespace.get())
        }
        if (template.isPresent) {
            args += listOf("-e", template.get())
        }
        // invoke pig compiler, offloads all arg handling to the application
        // also invoking via the public interface for consistency
        println("pig ${args.joinToString(" ")}")
        main(args.toTypedArray())
    }
}

abstract class PigExtension {
    var language: String = "kotlin"
    var outputFile: File? = null
    var outputDir: File? = null
    var namespace: String = ""
    var template: String = ""
}
