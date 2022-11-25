package org.partiql.examples

import org.partiql.examples.util.Example
import java.io.PrintStream

class CsvExprValueExampleTest : BaseExampleTest() {
    override fun example(out: PrintStream): Example = CsvExprValueExample(out)

    override val expected = """
        |CSV file:
        |    Cat,Nibbler,F
        |    Cat,Hobbes,M
        |    Dog,Fido,M
        |PartiQL query:
        |    SELECT _1, _2, _3 FROM csv_data
        |result:
        |    <<
        |      {
        |        '_1': 'Cat',
        |        '_2': 'Nibbler',
        |        '_3': 'F'
        |      },
        |      {
        |        '_1': 'Cat',
        |        '_2': 'Hobbes',
        |        '_3': 'M'
        |      },
        |      {
        |        '_1': 'Dog',
        |        '_2': 'Fido',
        |        '_3': 'M'
        |      }
        |    >>
        |
    """.trimMargin()
}
