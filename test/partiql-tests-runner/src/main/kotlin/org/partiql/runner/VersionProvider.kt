package org.partiql.runner

import java.util.Properties

/**
 * Reads the version and git hash from the generated properties file.
 */
internal class VersionProvider {
    companion object {
        fun getPartiQLVersion(): String {
            val properties = Properties()
            properties.load(this.javaClass.getResourceAsStream("/partiql.properties"))
            return "${properties.getProperty("version")}-${properties.getProperty("commit")}"
        }

        fun getJavaVersion(): String {
            return System.getProperty("java.version")
        }
    }
}
