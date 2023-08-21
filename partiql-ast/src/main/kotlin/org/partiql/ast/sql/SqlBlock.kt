package org.partiql.ast.sql

/**
 * Write this [SqlBlock] tree as SQL text with the given [SqlLayout].
 *
 * @param layout    SQL formatting ruleset
 * @return          SQL text
 */
public fun SqlBlock.sql(layout: SqlLayout = SqlLayout.DEFAULT): String = SqlWriter.format(this, layout)

/**
 * Representation of some textual corpus; akin to Wadler's "A prettier printer" Document type.
 */
sealed interface SqlBlock {

    public override fun toString(): String

    public fun <R, C> accept(visitor: BlockVisitor<R, C>, ctx: C): R

    public object Nil : SqlBlock {

        override fun toString() = ""

        override fun <R, C> accept(visitor: BlockVisitor<R, C>, ctx: C): R = visitor.visitNil(this, ctx)
    }

    public object NL : SqlBlock {

        override fun toString() = "\n"

        override fun <R, C> accept(visitor: BlockVisitor<R, C>, ctx: C): R = visitor.visitNewline(this, ctx)
    }

    public class Text(val text: String) : SqlBlock {

        override fun toString() = text

        override fun <R, C> accept(visitor: BlockVisitor<R, C>, ctx: C): R = visitor.visitText(this, ctx)
    }

    public class Nest(val child: SqlBlock) : SqlBlock {

        override fun toString() = child.toString()

        override fun <R, C> accept(visitor: BlockVisitor<R, C>, ctx: C): R = visitor.visitNest(this, ctx)
    }

    // Use link block rather than linked-list block.next as it makes pre-order traversal trivial
    public class Link(val lhs: SqlBlock, val rhs: SqlBlock) : SqlBlock {

        override fun toString() = lhs.toString() + rhs.toString()

        override fun <R, C> accept(visitor: BlockVisitor<R, C>, ctx: C): R = visitor.visitLink(this, ctx)
    }
}

public interface BlockVisitor<R, C> {

    public fun visit(block: SqlBlock, ctx: C): R

    public fun visitNil(block: SqlBlock.Nil, ctx: C): R

    public fun visitNewline(block: SqlBlock.NL, ctx: C): R

    public fun visitText(block: SqlBlock.Text, ctx: C): R

    public fun visitNest(block: SqlBlock.Nest, ctx: C): R

    public fun visitLink(block: SqlBlock.Link, ctx: C): R
}

public abstract class BlockBaseVisitor<R, C> : BlockVisitor<R, C> {

    public abstract fun defaultReturn(block: SqlBlock, ctx: C): R

    public open fun defaultVisit(block: SqlBlock, ctx: C) = defaultReturn(block, ctx)

    public override fun visit(block: SqlBlock, ctx: C): R = block.accept(this, ctx)

    public override fun visitNil(block: SqlBlock.Nil, ctx: C): R = defaultVisit(block, ctx)

    public override fun visitNewline(block: SqlBlock.NL, ctx: C): R = defaultVisit(block, ctx)

    public override fun visitText(block: SqlBlock.Text, ctx: C): R = defaultVisit(block, ctx)

    public override fun visitNest(block: SqlBlock.Nest, ctx: C): R = defaultVisit(block, ctx)

    public override fun visitLink(block: SqlBlock.Link, ctx: C): R = defaultVisit(block, ctx)
}
