package org.partiql.transpiler.block

object BlockWriter {

    /**
     * Write a block tree without formatting.
     */
    fun write(root: Block): String = root.toString().replace("\n", "")

    /**
     * Write a block tree with formatting.
     */
    @JvmOverloads
    fun format(root: Block, layout: Layout = Layout.DEFAULT): String {
        val formatter = Formatter(layout)
        val ctx = Ctx.empty()
        root.accept(formatter, ctx)
        return ctx.toString()
    }

    private class Ctx private constructor(
        val out: StringBuilder,
        val level: Int,
    ) {
        fun nest() = Ctx(out, level + 1)

        override fun toString() = out.toString()

        companion object {
            fun empty() = Ctx(StringBuilder(), 0)
        }
    }

    private class Formatter(val layout: Layout) : BlockBaseVisitor<Unit, Ctx>() {

        private inline fun write(ctx: Ctx, f: () -> String) {
            if (ctx.level > 0) ctx.out.append(lead(ctx))
            ctx.out.append(f())
        }

        override fun defaultReturn(block: Block, ctx: Ctx) = write(ctx) {
            block.toString()
        }

        override fun visitNil(block: Block.Nil, ctx: Ctx) {}

        override fun visitNewline(block: Block.NL, ctx: Ctx) {
            ctx.out.appendLine()
        }

        override fun visitText(block: Block.Text, ctx: Ctx) = write(ctx) { block.text }

        override fun visitNest(block: Block.Nest, ctx: Ctx) {
            block.child.accept(this, ctx.nest())
        }

        override fun visitLink(block: Block.Link, ctx: Ctx) {
            block.lhs.accept(this, ctx)
            block.rhs.accept(this, ctx)
        }

        private fun lead(ctx: Ctx) = layout.indent.toString().repeat(ctx.level)
    }
}
