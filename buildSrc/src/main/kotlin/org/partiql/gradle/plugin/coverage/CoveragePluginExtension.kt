package org.partiql.gradle.plugin.coverage

/**
 * TODO
 */
abstract class CoveragePluginExtension {
    var artifactId: String = ""
    var name: String = ""
    var description: String = ""
    var url: String = "https://github.com/partiql/partiql-lang-kotlin"
    override fun toString(): String {
        return "CoveragePluginExtension(artifactId='$artifactId', name='$name', description='$description', url='$url')"
    }
}
