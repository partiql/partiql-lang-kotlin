package org.partiql.sprout.example;


import com.amazon.ionelement.api.Ion;
import org.junit.jupiter.api.Test;
import org.partiql.sprout.test.generated.Inlines;
import org.partiql.sprout.test.generated.Node;

class JavaBuildersTest {

    @Test
    void example() {
        // basic example
        Node node = Node.builder()
                .a(false)
                .b(1)
                .c("C Value")
                .e(Ion.ionTimestamp("2009-01-01T00:00Z"))
                .build();

        // nested example
        Inlines.Foo foo = Inlines.Foo.builder()
                .x(-1)
                .y(Inlines.Bar.A)
                .build();
    }
}
