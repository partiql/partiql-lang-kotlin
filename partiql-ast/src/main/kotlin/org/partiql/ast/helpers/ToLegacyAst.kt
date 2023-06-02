@file:JvmName("ToLegacyAst")

package org.partiql.ast.helpers

import com.amazon.ionelement.api.MetaContainer
import com.amazon.ionelement.api.emptyMetaContainer
import com.amazon.ionelement.api.ionNull
import org.partiql.ast.AstNode
import org.partiql.ast.Expr
import org.partiql.ast.Statement
import org.partiql.ast.visitor.AstBaseVisitor
import org.partiql.lang.domains.PartiqlAst

/**
 * Translates an [AstNode] tree to the legacy PIG AST.
 *
 * Optionally, you can provide a Map of MetaContainers to attach to the legacy AST nodes.
 */
public fun AstNode.toLegacyAst(metas: Map<String, MetaContainer> = emptyMap()): PartiqlAst.PartiqlAstNode {
    val translator = AstTranslator(metas)
    return accept(translator, Ctx())
}

/**
 * Empty visitor method arguments
 */
private class Ctx

/**
 * Traverses an [AstNode] tree, folding to a [PartiqlAst.PartiqlAstNode] tree.
 */
private class AstTranslator(val metas: Map<String, MetaContainer>) : AstBaseVisitor<PartiqlAst.PartiqlAstNode, Ctx>() {

    private val pig = PartiqlAst.BUILDER()

    override fun defaultReturn(node: AstNode, ctx: Ctx): Nothing {
        val fromClass = node::class.qualifiedName
        val toClass = PartiqlAst.PartiqlAstNode::class.qualifiedName
        throw IllegalArgumentException("$fromClass cannot be translated to $toClass")
    }

    /**
     * Attach Metas if-any
     */
    private inline fun <T : PartiqlAst.PartiqlAstNode> translate(
        node: AstNode,
        block: PartiqlAst.Builder.(metas: MetaContainer) -> T,
    ): T {
        val metas = metas[node._id] ?: emptyMetaContainer()
        return pig.block(metas)
    }

    override fun visitStatement(node: Statement, ctx: Ctx) = super.visitStatement(node, ctx) as PartiqlAst.Statement

    override fun visitStatementQuery(node: Statement.Query, ctx: Ctx) = translate(node) { metas ->
        val expr = visitExpr(node.expr, ctx)
        query(expr, metas)
    }

    override fun visitExpr(node: Expr, ctx: Ctx): PartiqlAst.Expr = super.visitExpr(node, ctx) as PartiqlAst.Expr

    override fun visitExprMissingValue(node: Expr.MissingValue, ctx: Ctx) = translate(node) { metas ->
        lit(ionNull().withAnnotations("\$missing"), metas)
    }

    override fun visitExprNullValue(node: Expr.NullValue, ctx: Ctx) = translate(node) { metas ->
        lit(ionNull(), metas)
    }

    override fun visitExprLiteral(node: Expr.Literal, ctx: Ctx) = translate(node) { metas ->
        lit(node.value, metas)
    }
}

// /**
//  * Builds a PIG node with a reconstructed SourceLocation
//  */
// inline fun <T : PartiqlAst.PartiqlAstNode> translate(
//     node: AstNode,
//     block: PartiqlAst.Builder.() -> T,
// ): T {
//     val piggy = factory.block()
//     val location = when (val l = locations?.get(node.id)) {
//             =======
//             private class Ctx(val locations: PartiQLParser.SourceLocations?)
//
//             private object Visitor : AstBaseVisitor<PartiqlAst.PartiqlAstNode, Ctx>() {
//
//             private val factory = PartiqlAst.BUILDER()
//
//             private inline fun <T : PartiqlAst.PartiqlAstNode> translate(
//                 node: AstNode,
//                 ctx: Ctx,
//                 block: PartiqlAst.Builder.() -> T,
//             ): T {
//                 val piggy = factory.block()
//                 val location = when (val l = ctx.locations?.get(node.id)) {
//                         >>>>>>> f4175c5e (Initial Ast to PIG Ast conversion)
//                     null -> UNKNOWN_SOURCE_LOCATION
//                     else -> SourceLocationMeta(l.line.toLong(), l.offset.toLong(), l.length.toLong())
//                 }
//                 @Suppress("UNCHECKED_CAST") return piggy.withMeta(SourceLocationMeta.TAG, location) as T
//             }
//
//             <<<<<<< HEAD
//             /**
//              * Builds a PIG node only if it's not synthetic
//              */
//             inline fun <T : PartiqlAst.PartiqlAstNode> optional(
//                 node: AstNode,
//                 offset: Int = 0,
//                 block: PartiqlAst.Builder.() -> T,
//             ): T? = when (locations?.isSynthetic(node.id, offset)) {
//                 true -> null
//                 else -> factory.block()
//             }
