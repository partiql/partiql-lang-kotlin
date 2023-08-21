package org.partiql.ast.sql

/**
 * Internal methods for SQL formatting.
 */
internal object SqlWriter {

    /**
     * Write a block tree without formatting.
     */
    internal fun write(root: SqlBlock): String = root.toString().replace("\n", "")

    /**
     * Write a block tree with formatting.
     */
    @JvmOverloads
    internal fun format(root: SqlBlock, layout: SqlLayout = SqlLayout.DEFAULT): String {
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

    private class Formatter(val layout: SqlLayout) : BlockBaseVisitor<Unit, Ctx>() {

        private inline fun write(ctx: Ctx, f: () -> String) {
            if (ctx.level > 0) ctx.out.append(lead(ctx))
            ctx.out.append(f())
        }

        override fun defaultReturn(block: SqlBlock, ctx: Ctx) = write(ctx) {
            block.toString()
        }

        override fun visitNil(block: SqlBlock.Nil, ctx: Ctx) {}

        override fun visitNewline(block: SqlBlock.NL, ctx: Ctx) {
            ctx.out.appendLine()
        }

        override fun visitText(block: SqlBlock.Text, ctx: Ctx) = write(ctx) { block.text }

        override fun visitNest(block: SqlBlock.Nest, ctx: Ctx) {
            block.child.accept(this, ctx.nest())
        }

        override fun visitLink(block: SqlBlock.Link, ctx: Ctx) {
            block.lhs.accept(this, ctx)
            block.rhs.accept(this, ctx)
        }

        private fun lead(ctx: Ctx) = layout.indent.toString().repeat(ctx.level)
    }
}
