package org.partiql.plugins.bananna
import org.partiql.spi.functions.CustomInterface
import java.util.ServiceLoader

fun main() {
    val serviceLoader = ServiceLoader.load(CustomInterface::class.java)
    for (service in serviceLoader) {
        service.performAction()
    }
}
