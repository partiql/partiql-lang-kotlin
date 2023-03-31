package org.partiql.sprout.generator.target.kotlin

import org.junit.jupiter.api.Test
import org.partiql.sprout.parser.SproutParser

internal class KotlinGeneratorTest {

    @Test
    fun generate() {
        val input = KotlinGeneratorTest::class.java.getResource("/test.ion")!!.readText()
        val parser = SproutParser.default()
        val generator = KotlinGenerator(
            options = KotlinOptions(
                packageRoot = "org.partiql.sprout.test.generated",
                poems = listOf("visitor", "builder"),
                node = KotlinNodeOptions(
                    modifier = KotlinNodeOptions.Modifier.DATA
                ),
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
