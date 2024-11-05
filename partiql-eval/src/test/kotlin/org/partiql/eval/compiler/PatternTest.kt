package org.partiql.eval.compiler

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.partiql.plan.Operator
import org.partiql.plan.builder.RelBuilder
import org.partiql.plan.builder.RexBuilder
import org.partiql.plan.rel.RelFilter
import org.partiql.plan.rel.RelScan
import org.partiql.spi.catalog.Table
import org.partiql.types.Field
import org.partiql.types.PType
import java.util.Stack

/**
 * The PatternTest tests the pattern matching algorithm against a hand-constructed tree.
 */
public class PatternTest {

    /**
     * CREATE TABLE jobs (id INT, complete BOOL);
     */
    private val table = Table.builder()
        .name("T")
        .schema(
            PType.row(
                Field.of("id", PType.integer()),
                Field.of("complete", PType.bool()),
            )
        )
        .build()

    /**
     * Some simple tree to test patterns against.
     *
     * RelProject(id)           -> var(0).id
     *  \
     *   RelFilter(complete)    -> var(0).complete
     *    \
     *     RelScan(T)           -> < 0: T >
     */
    private val tree: Operator = RelBuilder
        .scan(RexBuilder.table(table))
        .filter(RexBuilder.variable(0).path("complete"))
        .project(RexBuilder.variable(0).path("id"))
        .build()

    @Test
    fun matchSingle() {
        // curr should be what we want to match
        val pattern = Pattern.match(RelFilter::class.java).build()
        // traverse the tree until we hit what we want to match.
        var curr = tree
        val stack = Stack<Operator>()
        stack.push(curr)
        while (stack.isNotEmpty()) {
            curr = stack.pop()
            if (curr is RelFilter) {
                break
            }
            for (next in curr.getChildren()) {
                stack.push(next)
            }
        }
        pattern.matches(curr)
    }

    @Test
    @Disabled("Matching search not implemented")
    fun predicatePushDown() {
        // filter(scan(T))
        val pattern = Pattern
            .match(RelFilter::class.java)
            .child(RelScan::class.java)
            .build()
        assert(match(tree, pattern) != null)
    }

    @Test
    @Disabled("Matching search not implemented")
    fun projectionPushDown() {
        // project(any(scan(T)))
        val pattern = Pattern
            .match(RelFilter::class.java)
            .child(Pattern.any().child(RelScan::class.java).build())
            .build()
        assert(match(tree, pattern) != null)
    }

    // working on the match algorithm
    private fun match(tree: Operator, pattern: Pattern): Match? {
        TODO("adapt DFS tracking child position")
    }
}
