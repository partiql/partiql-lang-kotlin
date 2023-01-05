package org.partiql.examples

import org.partiql.examples.util.Example
import java.io.PrintStream

class ExperimentalFeatureJavaExampleTest : BaseExampleTest() {

    override fun example(out: PrintStream): Example = ExperimentalFeatureJavaExample(out)

    override val expected = """
        |PartiQL query:
        |    SELECT t.name FROM myTable AS t WHERE t.age > 20
        |result
        |    <<
        |      {
        |        'name': 'tim'
        |      }
        |    >>
        |
    """.trimMargin()
}
