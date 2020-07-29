package org.partiql.examples

import org.partiql.examples.util.Example
import java.io.PrintStream

class ParserExampleTest : BaseExampleTest() {
    override fun example(out: PrintStream): Example = ParserExample(out)

    override val expected =
"""PartiQL query
    SELECT exampleField FROM exampleTable WHERE anotherField > 10
Serialized AST
    
    (
      meta
      (
        select
        (
          project
          (
            project_list
            (
              project_expr
              (
                meta
                (
                  id
                  exampleField
                  (
                    case_insensitive
                  )
                  (
                    unqualified
                  )
                )
                {
                  line:1,
                  column:8
                }
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
              meta
              (
                id
                exampleTable
                (
                  case_insensitive
                )
                (
                  unqualified
                )
              )
              {
                line:1,
                column:26
              }
            )
            null
            null
            null
          )
        )
        (
          where
          (
            meta
            (
              gt
              (
                meta
                (
                  id
                  anotherField
                  (
                    case_insensitive
                  )
                  (
                    unqualified
                  )
                )
                {
                  line:1,
                  column:45
                }
              )
              (
                meta
                (
                  lit
                  10
                )
                {
                  line:1,
                  column:60
                }
              )
            )
            {
              line:1,
              column:58
            }
          )
        )
      )
      {
        line:1,
        column:1
      }
    )
"""
}
