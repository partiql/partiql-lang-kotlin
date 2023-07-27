package org.partiql.transpiler.block

/**
 * Representation of some textual corpus; akin to Wadler's "A prettier printer" Document type.
 */
sealed interface Block {

    public override fun toString(): String

    public fun <R, C> accept(visitor: BlockVisitor<R, C>, ctx: C): R

    public object Nil : Block {

        override fun toString() = ""

        override fun <R, C> accept(visitor: BlockVisitor<R, C>, ctx: C): R = visitor.visitNil(this, ctx)
    }

    public object NL : Block {

        override fun toString() = "\n"

        override fun <R, C> accept(visitor: BlockVisitor<R, C>, ctx: C): R = visitor.visitNewline(this, ctx)
    }

    public class Text(val text: String) : Block {

        override fun toString() = text

        override fun <R, C> accept(visitor: BlockVisitor<R, C>, ctx: C): R = visitor.visitText(this, ctx)
    }

    public class Nest(val child: Block) : Block {

        override fun toString() = child.toString()

        override fun <R, C> accept(visitor: BlockVisitor<R, C>, ctx: C): R = visitor.visitNest(this, ctx)
    }

    // Use link block rather than linked-list block.next as it makes pre-order traversal trivial
    public class Link(val lhs: Block, val rhs: Block) : Block {

        override fun toString() = lhs.toString() + rhs.toString()

        override fun <R, C> accept(visitor: BlockVisitor<R, C>, ctx: C): R = visitor.visitLink(this, ctx)
    }
}

public interface BlockVisitor<R, C> {

    public fun visit(block: Block, ctx: C): R

    public fun visitNil(block: Block.Nil, ctx: C): R

    public fun visitNewline(block: Block.NL, ctx: C): R

    public fun visitText(block: Block.Text, ctx: C): R

    public fun visitNest(block: Block.Nest, ctx: C): R

    public fun visitLink(block: Block.Link, ctx: C): R
}

public abstract class BlockBaseVisitor<R, C> : BlockVisitor<R, C> {

    public abstract fun defaultReturn(block: Block, ctx: C): R

    public open fun defaultVisit(block: Block, ctx: C) = defaultReturn(block, ctx)

    public override fun visit(block: Block, ctx: C): R = block.accept(this, ctx)

    public override fun visitNil(block: Block.Nil, ctx: C): R = defaultVisit(block, ctx)

    public override fun visitNewline(block: Block.NL, ctx: C): R = defaultVisit(block, ctx)

    public override fun visitText(block: Block.Text, ctx: C): R = defaultVisit(block, ctx)

    public override fun visitNest(block: Block.Nest, ctx: C): R = defaultVisit(block, ctx)

    public override fun visitLink(block: Block.Link, ctx: C): R = defaultVisit(block, ctx)
}
