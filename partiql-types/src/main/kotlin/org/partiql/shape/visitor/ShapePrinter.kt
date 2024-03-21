package org.partiql.shape.visitor

import org.partiql.shape.AnyOf
import org.partiql.shape.Element
import org.partiql.shape.Fields
import org.partiql.shape.Meta
import org.partiql.shape.NotNull
import org.partiql.shape.PShape
import org.partiql.shape.ShapeNode
import org.partiql.shape.visitor.ShapePrinter.Visitor.start
import java.lang.StringBuilder

public object ShapePrinter {

    public fun stringify(shape: ShapeNode, pretty: Boolean = false): String {
        val builder = StringBuilder()
        val ctx = Context(builder, pretty = pretty)
        Visitor.visit(shape, ctx)
        return builder.toString()
    }

    public fun append(out: Appendable, shape: ShapeNode, pretty: Boolean = false) {
        val ctx = Context(out, pretty = pretty)
        Visitor.visit(shape, ctx)
    }

    private data class Context(
        val out: Appendable,
        val indent: Int = 0,
        var ignoreFirstPrefix: Boolean = false,
        val pretty: Boolean = false
    ) {
        fun prefix(): String {
            var str = ""
            repeat(indent) { str += "\t" }
            return str
        }
    }

    private object Visitor : ShapeBaseVisitor<Unit, Context>() {

        private fun Context.start(str: String) {
            when (this.pretty) {
                true -> when (ignoreFirstPrefix) {
                    true -> this.out.append(str).also { this.ignoreFirstPrefix = false }
                    false -> this.out.append("${prefix()}$str")
                }
                false -> this.append(str)
            }
        }

        private fun Context.startLine(str: String) {
            when (this.pretty) {
                true -> when (ignoreFirstPrefix) {
                    true -> this.out.appendLine(str).also { this.ignoreFirstPrefix = false }
                    false -> this.out.appendLine("${prefix()}$str")
                }
                false -> this.appendLine(str)
            }
        }

        private fun Context.append(str: String) {
            this.out.append(str)
        }

        private fun Context.appendLine(str: String) {
            when (this.pretty) {
                true -> this.out.appendLine(str)
                false -> {
                    this.out.append(str)
                }
            }
        }

        private fun Context.indent(): Context {
            return this.copy(indent = this.indent + 1)
        }

        fun stringify(shape: ShapeNode, indent: Int, pretty: Boolean = false): String {
            val builder = StringBuilder()
            val ctx = Context(
                out = builder,
                indent = indent,
                pretty = pretty
            )
            visit(shape, ctx)
            return builder.toString()
        }

        override fun defaultVisit(node: ShapeNode, ctx: Context) {
            error("This shouldn't have happened.")
        }

        override fun visitShape(node: PShape, ctx: Context) {
            ctx.start("${node.type}")
            // Sort the constraints
            val constraints = node.constraints.map { c ->
                stringify(c, ctx.indent + 1, ctx.pretty)
            }.sortedBy { c ->
                c.lines().size
            }
            if (constraints.isNotEmpty()) {
                ctx.appendLine(" WITH CONSTRAINTS (")
                constraints.forEachIndexed { index, c ->
                    ctx.append(c)
                    if (index != constraints.lastIndex) {
                        ctx.append(",")
                        if (!ctx.pretty) {
                            ctx.append(" ")
                        }
                    }
                    ctx.appendLine("")
                }
                ctx.start(")")
            }

            if (node.metas.isNotEmpty()) {
                ctx.appendLine(" WITH METAS (")
                node.metas.forEachIndexed { index, m ->
                    val newCtx = ctx.indent()
                    visitMeta(m, newCtx)
                    if (index != node.metas.size - 1) {
                        newCtx.append(",")
                        if (!newCtx.pretty) {
                            newCtx.append(" ")
                        }
                    }
                    newCtx.appendLine("")
                }
                ctx.start(")")
            }
        }

        override fun visitConstraintElement(node: Element, ctx: Context) {
            ctx.startLine("ELEMENT (")
            val newCtx = ctx.indent()
            visitShape(node.shape, newCtx)
            ctx.appendLine("")
            ctx.start(")")
        }

        override fun visitConstraintAnyOf(node: AnyOf, ctx: Context) {
            ctx.startLine("ANY OF (")
            val newCtx = ctx.indent()
            node.shapes.forEachIndexed { index, s ->
                visitShape(s, newCtx)
                if (index != node.shapes.size - 1) {
                    ctx.append(",")
                    if (!ctx.pretty) {
                        ctx.append(" ")
                    }
                }
                ctx.appendLine("")
            }
            ctx.start(")")
        }

        override fun visitConstraintFields(node: Fields, ctx: Context) {
            if (node.isClosed) {
                ctx.startLine("CLOSED FIELDS (")
            } else {
                ctx.startLine("OPEN FIELDS (")
            }
            val newCtx = ctx.indent()
            node.fields.forEachIndexed { index, f ->
                newCtx.start(f.key)
                newCtx.append(": ")
                newCtx.ignoreFirstPrefix = true
                visitShape(f.value, newCtx)
                if (index != node.fields.lastIndex) {
                    ctx.append(",")
                    if (!ctx.pretty) {
                        ctx.append(" ")
                    }
                }
                ctx.appendLine("")
            }
            ctx.start(")")
        }

        override fun visitConstraintNotNull(node: NotNull, ctx: Context) {
            ctx.start("NOT NULL")
        }

        override fun visitMeta(node: Meta, ctx: Context) {
            ctx.start(node.toString())
        }
    }
}
