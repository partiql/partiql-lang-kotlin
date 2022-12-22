package org.partiql.sprout.generator

import org.junit.jupiter.api.Test
import org.partiql.sprout.parser.SproutParser

internal class GeneratorTest {

    @Test
    fun generate() {
        val input = GeneratorTest::class.java.getResource("/test.ion")!!.readText()
        val parser = SproutParser.default()
        val generator = Generator(
            options = Options(
                packageRoot = "org.partiql.sprout.test.generated",
                node = NodeOptions(
                    modifier = NodeOptions.Modifier.DATA
                )
            )
        )
        val universe = parser.parse("sprout_test", input)
        println(universe)
        val result = generator.generate(universe)
        result.write {
            println("---[${it.name}]----------------")
            it.writeTo(System.out)
            println()
        }
    }
}
