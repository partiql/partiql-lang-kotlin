package org.partiql.runner

import java.util.Properties

/**
 * Provides version information for PartiQL and Java runtime environment.
 */
internal class VersionProvider {
    companion object {
        /**
         * Returns the PartiQL version string including git commit hash.
         *
         * @return Version string in format "version-commit"
         */
        fun getPartiQLVersion(): String {
            val properties = Properties()
            properties.load(this.javaClass.getResourceAsStream("/partiql.properties"))
            return "${properties.getProperty("version")}-${properties.getProperty("commit")}"
        }

        /**
         * Returns the Java runtime version.
         *
         * @return Java version string from system properties
         */
        fun getJavaVersion(): String {
            return System.getProperty("java.version")
        }
    }
}
