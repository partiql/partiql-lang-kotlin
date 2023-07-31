package org.partiql.transpiler.targets

import org.partiql.transpiler.Dialect
import org.partiql.transpiler.TranspilerTarget
import org.partiql.transpiler.dialects.PartiQLDialect

/**
 * Default PartiQL Target
 */
public object PartiQLTarget : TranspilerTarget() {

    override val target: String = "partiql"

    override val version: String = "0.0"

    override val dialect: Dialect = PartiQLDialect.INSTANCE
}

// // Statement
//
// override fun visitPartiQLPlan(node: PartiQLPlan, nil: Schema): AstNode {
//     return super.visitStatement(node.statement, emptyList())
// }
//
// override fun visitStatementQuery(node: Statement.Query, nil: Schema) = unplan {
//     val expr = visitRex(node.root, nil)
//     statementQuery(expr)
// }
//
// // Rex
//
// override fun visitRex(node: Rex, schema: Schema): Expr {
//     verifyType(node.type)
//     return visitRexOp(node.op, schema)
// }
//
// override fun visitRexOp(node: Rex.Op, schema: Schema) = super.visitRexOp(node, schema) as Expr
//
// @OptIn(PartiQLValueExperimental::class)
// override fun visitRexOpLit(node: Rex.Op.Lit, schema: Schema) = unplan {
//     exprLit(node.value)
// }
//
// override fun visitRexOpVarResolved(node: Rex.Op.Var.Resolved, schema: Schema) = unplan {
//     if (node.ref < 0 || schema.size <= node.ref) {
//         // consider better debug
//         throw IllegalArgumentException("Undefined variable (var .. ${node.ref})")
//     }
//     val binding = schema[node.ref]
//     val identifier = unplan(binding.name)
//     exprVar(identifier, Expr.Var.Scope.DEFAULT)
// }
//
// override fun visitRexOpVarUnresolved(node: Rex.Op.Var.Unresolved, schema: Schema): AstNode {
//     error("Unresolved variable ${node.identifier}")
// }
//
// override fun visitRexOpGlobal(node: Rex.Op.Global, schema: Schema) = unplan {
//     if (node.ref < 0 || globals.size <= node.ref) {
//         // consider better debug
//         throw IllegalArgumentException("Undefined global (global .. ${node.ref})")
//     }
//     val binding = globals[node.ref]
//     val identifier = unplan(binding.first)
//     exprVar(identifier, Expr.Var.Scope.DEFAULT)
// }
//
// // <rex>(.<step>)*
// override fun visitRexOpPath(node: Rex.Op.Path, schema: Schema) = unplan {
//     val root = visitRex(node.root, schema)
//     val steps = node.steps.map { visitRexOpPathStep(it, schema) }
//     exprPath(root, steps)
// }
//
// // <rex>.<step>
// override fun visitRexOpPathStep(node: Rex.Op.Path.Step, schema: Schema) =
//     super.visitRexOpPathStep(node, schema) as Expr.Path.Step
//
// // x[<rex>]
// override fun visitRexOpPathStepIndex(node: Rex.Op.Path.Step.Index, schema: Schema) = unplan {
//     val key = visitRex(node.key, schema)
//     exprPathStepIndex(key)
// }
//
// // x[*]
// override fun visitRexOpPathStepWildcard(node: Rex.Op.Path.Step.Wildcard, schema: Schema) = unplan {
//     exprPathStepWildcard()
// }
//
// // x.*
// override fun visitRexOpPathStepUnpivot(node: Rex.Op.Path.Step.Unpivot, schema: Schema) = unplan {
//     exprPathStepUnpivot()
// }
//
// // This is simplified because I do not have a lowered function catalog
// override fun visitRexOpCall(node: Rex.Op.Call, schema: Schema) = unplan {
//     val fn = verifyFn(node.fn)
//     when (fn.id) {
//         "like" -> callLike(node.args, schema)
//         "between" -> callBetween(node.args, schema)
//         "in_collection" -> callInCollection(node.args, schema)
//         "is_type" -> callIsType(node.args, schema)
//         "coalesce" -> callCoalesce(node.args, schema)
//         "null_if" -> callNullIf(node.args, schema)
//         "substring" -> callSubstring(node.args, schema)
//         "position" -> callPosition(node.args, schema)
//         "trim" -> callTrim(node.args, schema)
//         "overlay" -> callOverlay(node.args, schema)
//         "extract" -> callExtract(node.args, schema)
//         "cast" -> callCast(node.args, schema)
//         "can_lossless_cast" -> callCanLosslessCast(node.args, schema)
//         "date_add" -> callDateAdd(node.args, schema)
//         "date_diff" -> callDateDiff(node.args, schema)
//         "outer_union" -> callOuterUnion(node.args, schema)
//         "outer_union_distinct" -> callOuterUnionDistinct(node.args, schema)
//         "outer_intersect" -> callOuterIntersect(node.args, schema)
//         "outer_intersect_distinct" -> callOuterIntersectDistinct(node.args, schema)
//         "outer_except" -> callOuterExcept(node.args, schema)
//         "outer_except_distinct" -> callOuterExceptDistinct(node.args, schema)
//         else -> {
//             val id = identifierSymbol(fn.id, Identifier.CaseSensitivity.INSENSITIVE)
//             val args = node.args.map {
//                 when (it) {
//                     is Rex.Op.Call.Arg.Type -> error("call arg cannot be a type")
//                     is Rex.Op.Call.Arg.Value -> visitRex(it.rex, schema)
//                 }
//             }
//             exprCall(id, args)
//         }
//     }
// }
//
// // unpack value
// override fun visitRexOpCallArgValue(node: Rex.Op.Call.Arg.Value, schema: Schema) = unplan {
//     visitRex(node.rex, schema)
// }
//
// // unpack type
// override fun visitRexOpCallArgType(node: Rex.Op.Call.Arg.Type, schema: Schema) = unplan {
//     val type = verifyType(node.type)
//     visitType(type, schema)
// }
//
// // { <field>* }
// override fun visitRexOpStruct(node: Rex.Op.Struct, schema: Schema) = unplan {
//     val fields = node.fields.map { visitRexOpStructField(it, schema) }
//     exprStruct(fields)
// }
//
// // <rex>_k : <rex>_v
// override fun visitRexOpStructField(node: Rex.Op.Struct.Field, schema: Schema) = unplan {
//     val k = visitRex(node.k, schema)
//     val v = visitRex(node.v, schema)
//     exprStructField(k, v)
// }
//
// // PIVOT <rex>_k
// override fun visitRexOpPivot(node: Rex.Op.Pivot, schema: Schema) = unplan {
//     val k = visitRex(node.key, schema)
//     val v = visitRex(node.value, schema)
//     selectPivot(k, v)
// }
//
// // unwrap COLL_TO_SCALAR(query)
// override fun visitRexOpCollToScalar(node: Rex.Op.CollToScalar, schema: Schema) = unplan {
//     visitRexOpSelect(node.subquery.select, schema)
// }
//
// // SELECT VALUE
// override fun visitRexOpSelect(node: Rex.Op.Select, schema: Schema) = unplan {
//     val constructor = visitRex(node.constructor, schema)
//     selectValue(constructor, setq = null)
// }
//
// // Special Forms
//
// open fun callLike(args: List<Rex.Op.Call.Arg>, schema: Schema): Expr = unplan { TODO() }
//
// open fun callBetween(args: List<Rex.Op.Call.Arg>, schema: Schema): Expr = unplan { TODO() }
//
// open fun callInCollection(args: List<Rex.Op.Call.Arg>, schema: Schema): Expr = unplan { TODO() }
//
// open fun callIsType(args: List<Rex.Op.Call.Arg>, schema: Schema): Expr = unplan { TODO() }
//
// open fun callCoalesce(args: List<Rex.Op.Call.Arg>, schema: Schema): Expr = unplan { TODO() }
//
// open fun callNullIf(args: List<Rex.Op.Call.Arg>, schema: Schema): Expr = unplan { TODO() }
//
// open fun callSubstring(args: List<Rex.Op.Call.Arg>, schema: Schema): Expr = unplan { TODO() }
//
// open fun callPosition(args: List<Rex.Op.Call.Arg>, schema: Schema): Expr = unplan { TODO() }
//
// open fun callTrim(args: List<Rex.Op.Call.Arg>, schema: Schema): Expr = unplan { TODO() }
//
// open fun callOverlay(args: List<Rex.Op.Call.Arg>, schema: Schema): Expr = unplan { TODO() }
//
// open fun callExtract(args: List<Rex.Op.Call.Arg>, schema: Schema): Expr = unplan { TODO() }
//
// open fun callCast(args: List<Rex.Op.Call.Arg>, schema: Schema): Expr = unplan { TODO() }
//
// open fun callCanLosslessCast(args: List<Rex.Op.Call.Arg>, schema: Schema): Expr = unplan { TODO() }
//
// open fun callDateAdd(args: List<Rex.Op.Call.Arg>, schema: Schema): Expr = unplan { TODO() }
//
// open fun callDateDiff(args: List<Rex.Op.Call.Arg>, schema: Schema): Expr = unplan { TODO() }
//
// open fun callOuterUnion(args: List<Rex.Op.Call.Arg>, schema: Schema): Expr = unplan { TODO() }
//
// open fun callOuterUnionDistinct(args: List<Rex.Op.Call.Arg>, schema: Schema): Expr = unplan { TODO() }
//
// open fun callOuterIntersect(args: List<Rex.Op.Call.Arg>, schema: Schema): Expr = unplan { TODO() }
//
// open fun callOuterIntersectDistinct(args: List<Rex.Op.Call.Arg>, schema: Schema): Expr = unplan { TODO() }
//
// open fun callOuterExcept(args: List<Rex.Op.Call.Arg>, schema: Schema): Expr = unplan { TODO() }
//
// open fun callOuterExceptDistinct(args: List<Rex.Op.Call.Arg>, schema: Schema): Expr = unplan { TODO() }
//
// // Helpers
//
// private fun AstFactory.unplan(identifier: PlanIdentifier): AstIdentifier = when (identifier) {
//     is PlanIdentifier.Qualified -> unplan(identifier)
//     is PlanIdentifier.Symbol -> unplan(identifier)
// }
//
// private fun AstFactory.unplan(identifier: PlanIdentifier.Qualified): AstIdentifier.Qualified {
//     val root = unplan(identifier.root)
//     val steps = identifier.steps.map { unplan(it) }
//     return identifierQualified(root, steps)
// }
//
// private fun AstFactory.unplan(identifier: PlanIdentifier.Symbol): AstIdentifier.Symbol {
//     val symbol = identifier.symbol
//     val case = when (identifier.caseSensitivity) {
//         PlanIdentifier.CaseSensitivity.SENSITIVE -> AstIdentifier.CaseSensitivity.SENSITIVE
//         PlanIdentifier.CaseSensitivity.INSENSITIVE -> AstIdentifier.CaseSensitivity.INSENSITIVE
//     }
//     return Ast.identifierSymbol(symbol, case)
// }
