package org.partiql.examples

import org.partiql.examples.util.Example
import java.io.PrintStream

class CSVJavaExampleTest : BaseExampleTest() {
    override fun example(out: PrintStream): Example = CSVJavaExample(out)

    override val expected = """
            |CSV Data:
            |    person_1,32,tag_1
            |    person_1,27,tag_1
            |    person_2,24,tag_1,tag_2
            |PartiQL query:
            |    SELECT * FROM myCsvDocument csv WHERE CAST(csv._1 AS INT) < 30
            |PartiQL query result:
            |    <<
            |      {
            |        '_0': 'person_1',
            |        '_1': '27',
            |        '_2': 'tag_1'
            |      },
            |      {
            |        '_0': 'person_2',
            |        '_1': '24',
            |        '_2': 'tag_1',
            |        '_3': 'tag_2'
            |      }
            |    >>
            |
        """.trimMargin()
}
