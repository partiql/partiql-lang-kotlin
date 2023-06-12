
package org.partiql.cli
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Test
import org.partiql.plugins.bananna.MyCustomFunction
import org.partiql.spi.functions.CustomInterface
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.util.ServiceLoader
class ServiceLoaderTest {
    @Test
    fun `ServiceLoader should load CustomFunction`() {
        val loader = ServiceLoader.load(CustomInterface::class.java)
        val customImplementation = loader.firstOrNull()

        customImplementation.shouldBeInstanceOf<MyCustomFunction>()
        customImplementation.shouldBeInstanceOf<CustomInterface>()

        // Capture the output stream
        val outContent = ByteArrayOutputStream()
        System.setOut(PrintStream(outContent))

        // Call the method
        customImplementation?.performAction()

        // Verify the output
        outContent.toString().trim() shouldBe "Action performed by MyCustomFunction"

        // Reset the System.out
        System.setOut(System.out)
    }
}
