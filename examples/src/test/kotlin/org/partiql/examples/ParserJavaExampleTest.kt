package org.partiql.examples

import org.partiql.examples.util.Example
import java.io.PrintStream

class ParserJavaExampleTest : BaseExampleTest() {
    override fun example(out: PrintStream): Example = ParserJavaExample(out)

    override val expected =
"""PartiQL query
    SELECT exampleField FROM exampleTable WHERE anotherField > 10
Serialized AST
    
    (
      query
      (
        select
        (
          project
          (
            project_list
            (
              project_expr
              (
                vr
                (
                  id
                  exampleField
                  (
                    regular
                  )
                )
                (
                  unqualified
                )
              )
              null
            )
          )
        )
        (
          from
          (
            scan
            (
              vr
              (
                id
                exampleTable
                (
                  regular
                )
              )
              (
                unqualified
              )
            )
            null
            null
            null
          )
        )
        (
          where
          (
            gt
            (
              vr
              (
                id
                anotherField
                (
                  regular
                )
              )
              (
                unqualified
              )
            )
            (
              lit
              10
            )
          )
        )
      )
    )
"""
}
