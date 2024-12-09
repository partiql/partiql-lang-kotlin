import io.github.gradlenexus.publishplugin.NexusPublishExtension
import java.time.Duration

plugins {
    id("io.github.gradle-nexus.publish-plugin")
}

// We use gradle-nexus's publish-plugin to publish all of our published artifacts to Maven using OSSRH.
// Documentation for this plugin, see https://github.com/gradle-nexus/publish-plugin/blob/v2.0.0/README.md
// This plugin must be applied at the root project, so we include the following block around the nexus publish
// extension.
rootProject.run {
    plugins.apply("io.github.gradle-nexus.publish-plugin")
    extensions.getByType(NexusPublishExtension::class.java).run {
        this.repositories {
            sonatype {
                nexusUrl.set(uri("https://aws.oss.sonatype.org/service/local/"))
                username.set(properties["ossrhUsername"].toString())
                password.set(properties["ossrhPassword"].toString())
            }
        }

        // these are not strictly required. The default timeouts are set to 1 minute. But Sonatype can be really slow.
        // If you get the error "java.net.SocketTimeoutException: timeout", these lines will help.
        connectTimeout.set(Duration.ofMinutes(3))
        clientTimeout.set(Duration.ofMinutes(3))
    }
}
