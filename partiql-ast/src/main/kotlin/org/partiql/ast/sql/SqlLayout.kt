package org.partiql.ast.sql

/**
 * [SqlLayout] determines how an [SqlBlock] tree is transformed in SQL text.
 */
public abstract class SqlLayout {

    abstract val indent: Indent

    public open fun format(root: SqlBlock): String {
        val ctx = Ctx.empty()
        root.accept(Formatter(), ctx)
        return ctx.toString()
    }

    public companion object {

        /**
         * Default SQL format.
         */
        public val DEFAULT = object : SqlLayout() {

            override val indent = Indent(2, Indent.Type.SPACE)
        }

        /**
         * Write SQL statement on one line.
         */
        public val ONELINE = object : SqlLayout() {

            override val indent = Indent(2, Indent.Type.SPACE)

            override fun format(root: SqlBlock): String = root.toString().replace("\n", "")
        }
    }

    /**
     * [SqlLayout] indent configuration.
     *
     * @property count
     * @property type
     */
    public class Indent(
        public val count: Int,
        public val type: Type,
    ) {

        enum class Type(val char: Char) {
            TAB(Char(9)),
            SPACE(Char(32)), ;
        }

        override fun toString() = type.char.toString().repeat(count)
    }

    private class Ctx private constructor(val out: StringBuilder, val level: Int) {
        fun nest() = Ctx(out, level + 1)

        override fun toString() = out.toString()

        companion object {
            fun empty() = Ctx(StringBuilder(), 0)
        }
    }

    private inner class Formatter : BlockBaseVisitor<Unit, Ctx>() {

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

        private fun lead(ctx: Ctx) = indent.toString().repeat(ctx.level)
    }
}
