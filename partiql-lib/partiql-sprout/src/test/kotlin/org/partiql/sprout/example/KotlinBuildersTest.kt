package org.partiql.sprout.example

import com.amazon.ionelement.api.ionTimestamp
import org.junit.jupiter.api.Test
import org.partiql.sprout.test.generated.Inlines
import org.partiql.sprout.test.generated.Node
import org.partiql.sprout.test.generated.builder.sproutTest
import kotlin.test.assertEquals

class KotlinBuildersTest {

    @Test
    internal fun example() {

        val node: Node = sproutTest {
            node {
                a = false
                b = 1
                c = "C Value"
                e = ionTimestamp("2009-01-01T00:00Z")
            }
        }

        assertEquals(false, node.a)
        assertEquals(1, node.b)
        assertEquals("C Value", node.c)
        assertEquals(ionTimestamp("2009-01-01T00:00Z"), node.e)

        val foo = sproutTest {
            inlinesFoo {
                x = -1
                y = Inlines.Bar.A
            }
        }

        assertEquals(-1, foo.x)
        assertEquals(Inlines.Bar.A, foo.y)
    }
}
