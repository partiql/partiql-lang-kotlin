package org.partiql.gradle.plugin.coverage

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin

/**
 * TODO
 */
abstract class CoveragePlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply(JavaPlugin::class.java)
        val ext = extensions.create("coverage", CoveragePluginExtension::class.java)
        println("COVERAGE PLUGIN LOADED!")
    }
}
