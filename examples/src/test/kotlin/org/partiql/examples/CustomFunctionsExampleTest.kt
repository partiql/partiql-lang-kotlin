package org.partiql.examples

import org.partiql.examples.util.Example
import java.io.PrintStream

class CustomFunctionsExampleTest : BaseExampleTest() {
    override fun example(out: PrintStream): Example = CustomFunctionsExample(out)

    override val expected = """
        |fib_scalar(NULL)
        |    NULL
        |fib_scalar(MISSING)
        |    NULL
        |fib_scalar(0)
        |    0
        |fib_scalar(1)
        |    1
        |fib_scalar(2)
        |    1
        |fib_scalar(3)
        |    2
        |fib_scalar(4)
        |    3
        |fib_scalar(5)
        |    5
        |fib_scalar(6)
        |    8
        |fib_scalar(7)
        |    13
        |fib_scalar(8)
        |    21
        |fib_list(NULL)
        |    NULL
        |fib_list(MISSING)
        |    NULL
        |fib_list(0)
        |    [
        |      {
        |        'n': 0
        |      }
        |    ]
        |fib_list(1)
        |    [
        |      {
        |        'n': 0
        |      },
        |      {
        |        'n': 1
        |      }
        |    ]
        |fib_list(2)
        |    [
        |      {
        |        'n': 0
        |      },
        |      {
        |        'n': 1
        |      },
        |      {
        |        'n': 1
        |      }
        |    ]
        |fib_list(3)
        |    [
        |      {
        |        'n': 0
        |      },
        |      {
        |        'n': 1
        |      },
        |      {
        |        'n': 1
        |      },
        |      {
        |        'n': 2
        |      }
        |    ]
        |fib_list(4)
        |    [
        |      {
        |        'n': 0
        |      },
        |      {
        |        'n': 1
        |      },
        |      {
        |        'n': 1
        |      },
        |      {
        |        'n': 2
        |      },
        |      {
        |        'n': 3
        |      }
        |    ]
        |fib_list(5)
        |    [
        |      {
        |        'n': 0
        |      },
        |      {
        |        'n': 1
        |      },
        |      {
        |        'n': 1
        |      },
        |      {
        |        'n': 2
        |      },
        |      {
        |        'n': 3
        |      },
        |      {
        |        'n': 5
        |      }
        |    ]
        |fib_list(6)
        |    [
        |      {
        |        'n': 0
        |      },
        |      {
        |        'n': 1
        |      },
        |      {
        |        'n': 1
        |      },
        |      {
        |        'n': 2
        |      },
        |      {
        |        'n': 3
        |      },
        |      {
        |        'n': 5
        |      },
        |      {
        |        'n': 8
        |      }
        |    ]
        |fib_list(7)
        |    [
        |      {
        |        'n': 0
        |      },
        |      {
        |        'n': 1
        |      },
        |      {
        |        'n': 1
        |      },
        |      {
        |        'n': 2
        |      },
        |      {
        |        'n': 3
        |      },
        |      {
        |        'n': 5
        |      },
        |      {
        |        'n': 8
        |      },
        |      {
        |        'n': 13
        |      }
        |    ]
        |fib_list(8)
        |    [
        |      {
        |        'n': 0
        |      },
        |      {
        |        'n': 1
        |      },
        |      {
        |        'n': 1
        |      },
        |      {
        |        'n': 2
        |      },
        |      {
        |        'n': 3
        |      },
        |      {
        |        'n': 5
        |      },
        |      {
        |        'n': 8
        |      },
        |      {
        |        'n': 13
        |      },
        |      {
        |        'n': 21
        |      }
        |    ]
        |
    """.trimMargin()
}
