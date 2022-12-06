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

package org.partiql.gradle.plugin.publish

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin
import org.gradle.api.publish.maven.tasks.PublishToMavenRepository
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.getByName
import org.gradle.kotlin.dsl.provideDelegate
import org.gradle.plugins.signing.SigningExtension
import org.gradle.plugins.signing.SigningPlugin
import org.jetbrains.dokka.gradle.DokkaPlugin
import org.jetbrains.dokka.gradle.DokkaTask
import java.io.File

/**
 * Gradle plugin to consolidates the following publishing logic
 * - Maven Publising
 * - Signing
 * - SourcesJar
 * - Dokka + JavadocJar
 */
abstract class PublishPlugin : Plugin<Project> {

    override fun apply(target: Project): Unit = with(target) {
        pluginManager.apply(JavaPlugin::class.java)
        pluginManager.apply(MavenPublishPlugin::class.java)
        pluginManager.apply(SigningPlugin::class.java)
        pluginManager.apply(DokkaPlugin::class.java)
        val ext = extensions.create("publish", PublishExtension::class.java)
        target.afterEvaluate { publish(ext) }
    }

    private fun Project.publish(ext: PublishExtension) {
        val releaseVersion = !version.toString().endsWith("-SNAPSHOT")

        // Generate "javadoc"
        tasks.getByName<DokkaTask>("dokkaHtml") {
            outputDirectory.set(File("${buildDir}/javadoc"))
        }

        // Include "sources" and "javadoc" in the JAR
        extensions.getByType(JavaPluginExtension::class.java).run {
            withSourcesJar()
            withJavadocJar()
        }

        // Setup Maven Central Publishing
        val publishing = extensions.getByType(PublishingExtension::class.java).apply {
            publications {
                create<MavenPublication>("maven") {
                    artifactId = ext.artifactId
                    from(components["java"])
                    pom {
                        packaging = "jar"
                        name.set(ext.name)
                        description.set(ext.description)
                        url.set(ext.url)
                        scm {
                            connection.set("scm:git@github.com:partiql/partiql-lang-kotlin.git")
                            developerConnection.set("scm:git@github.com:partiql/partiql-lang-kotlin.git")
                            url.set("git@github.com:partiql/partiql-lang-kotlin.git")
                        }
                        licenses {
                            license {
                                name.set("The Apache License, Version 2.0")
                                url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                            }
                        }
                        developers {
                            developer {
                                name.set("PartiQL Team")
                                email.set("partiql-dev@amazon.com")
                                organization.set("PartiQL")
                                organizationUrl.set("https://github.com/partiql")
                            }
                        }
                    }
                }
            }
            repositories {
                maven {
                    url = uri("https://aws.oss.sonatype.org/service/local/staging/deploy/maven2")
                    credentials {
                        val ossrhUsername: String by rootProject
                        val ossrhPassword: String by rootProject
                        username = ossrhUsername
                        password = ossrhPassword
                    }
                }
            }
        }

        // Sign only if publishing to Maven Central
        extensions.getByType(SigningExtension::class.java).run {
            setRequired {
                releaseVersion && gradle.taskGraph.allTasks.any { it is PublishToMavenRepository }
            }
            sign(publishing.publications["maven"])
        }
    }
}

abstract class PublishExtension {
    var artifactId: String = ""
    var name: String = ""
    var description: String = ""
    var url: String = "https://github.com/partiql/partiql-lang-kotlin"
    override fun toString(): String {
        return "PublishExtension(artifactId='$artifactId', name='$name', description='$description', url='$url')"
    }
}
