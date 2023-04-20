package org.partiql.lang.ast

import com.amazon.ionelement.api.ionSymbol
import org.partiql.ast.AstNode
import org.partiql.ast.Case
import org.partiql.ast.Expr
import org.partiql.ast.From
import org.partiql.ast.GroupBy
import org.partiql.ast.Let
import org.partiql.ast.OnConflict
import org.partiql.ast.OrderBy
import org.partiql.ast.Returning
import org.partiql.ast.Select
import org.partiql.ast.SetQuantifier
import org.partiql.ast.Statement
import org.partiql.ast.Type
import org.partiql.ast.visitor.AstBaseVisitor
import org.partiql.lang.domains.PartiqlAst
import org.partiql.parser.PartiQLParser
import kotlin.reflect.KClass
import kotlin.reflect.cast

/**
 * Several PIG calls have special forms we need to recreate
 */
private typealias SpecialForm = (exprs: List<PartiqlAst.Expr>) -> Pair<String, List<Any>>

/**
 * Translates an [AstNode] tree to the legacy PIG AST
 */
object AstToPigTranslator {

    /**
     * Translates an [AstNode] tree to the legacy PIG AST
     */
    fun translate(ast: AstNode, locations: PartiQLParser.SourceLocations? = null): PartiqlAst.PartiqlAstNode {
        val translator = Translator(locations)
        val node = ast.accept(translator, Ctx())
        return node
    }

    /**
     * Undo lowering
     */
    private val specialCalls: Map<String, SpecialForm> = mapOf(
        "trim_whitespace_both" to { args -> "trim" to listOf("both", args[0]) },
        "trim_whitespace_leading" to { args -> "trim" to listOf("leading", args[0]) },
        "trim_whitespace_trailing" to { args -> "trim" to listOf("trailing", args[0]) },
        "trim_chars_both" to { args -> "trim" to listOf("both", args[0], args[1]) },
        "trim_chars_leading" to { args -> "trim" to listOf("leading", args[0], args[1]) },
        "trim_chars_trailing" to { args -> "trim" to listOf("trailing", args[0], args[1]) },
    )

    /**
     * Visitor method arguments
     */
    private class Ctx

    private class Translator(val locations: PartiQLParser.SourceLocations?) : AstBaseVisitor<PartiqlAst.PartiqlAstNode, Ctx>() {

        private val factory = PartiqlAst.BUILDER()

        /**
         * Builds a PIG node with a reconstructed SourceLocation
         */
        inline fun <T : PartiqlAst.PartiqlAstNode> translate(
            node: AstNode,
            block: PartiqlAst.Builder.() -> T,
        ): T {
            val piggy = factory.block()
            val location = when (val l = locations?.get(node.id)) {
                null -> UNKNOWN_SOURCE_LOCATION
                else -> SourceLocationMeta(l.line.toLong(), l.offset.toLong(), l.length.toLong())
            }
            @Suppress("UNCHECKED_CAST") return piggy.withMeta(SourceLocationMeta.TAG, location) as T
        }

        /**
         * Builds a PIG node only if it's not synthetic
         */
        inline fun <T : PartiqlAst.PartiqlAstNode> optional(
            node: AstNode,
            offset: Int = 0,
            block: PartiqlAst.Builder.() -> T,
        ): T? = when (locations?.isSynthetic(node.id, offset)) {
            true -> null
            else -> factory.block()
        }

        override fun visitExpr(node: Expr, ctx: Ctx) = super.visitExpr(node, ctx) as PartiqlAst.Expr

        override fun visitStatementQuery(node: Statement.Query, ctx: Ctx) = translate(node) {
            val expr = visitExpr(node.expr, ctx)
            query(expr)
        }

        override fun visitStatementDMLInsert(node: Statement.DML.Insert, ctx: Ctx) = translate(node) {
            val target = visitExpr(node.target.table, ctx)
            val values = visitExpr(node.value, ctx)
            val conflictAction = node.onConflict?.let { visitOnConflictAction(it.action, ctx) }
            val op = insert(target, values, conflictAction)
            dml(dmlOpList(op), null, null, null)
        }

        override fun visitStatementDMLInsertValue(
            node: Statement.DML.InsertValue,
            ctx: Ctx
        ) = translate(node) {
            val target = visitExpr(node.target.table, ctx)
            val values = visitExpr(node.value, ctx)
            val index = node.index?.let { visitExpr(it, ctx) }
            val onConflict = node.onConflict?.let { visitOnConflict(it, ctx) }
            val op = insertValue(target, values, index, onConflict)
            dml(dmlOpList(op), null, null, null)
        }

        override fun visitStatementDMLUpsert(node: Statement.DML.Upsert, ctx: Ctx) = translate(node) {
            val target = visitExpr(node.target, ctx)
            val values = visitExpr(node.value, ctx)
            val conflictAction = doUpdate(excluded())
            // UPSERT overloads legacy INSERT
            val op = insert(target, values, conflictAction)
            dml(dmlOpList(op), null, null, null)
        }

        override fun visitStatementDMLReplace(node: Statement.DML.Replace, ctx: Ctx) = translate(node) {
            val target = visitExpr(node.target, ctx)
            val values = visitExpr(node.value, ctx)
            val conflictAction = doUpdate(excluded())
            // REPLACE overloads legacy INSERT
            val op = insert(target, values, conflictAction)
            dml(dmlOpList(op), null, null, null)
        }

        override fun visitStatementDMLUpdate(node: Statement.DML.Update, ctx: Ctx) = translate(node) {
            val from = visitStatementDMLTarget(node.target, ctx)
            // UPDATE becomes multiple sets
            val operations = node.assignments.map {
                val assignment = visitStatementDMLUpdateAssignment(it, ctx)
                set(assignment)
            }
            dml(dmlOpList(operations), from, null, null)
        }

        override fun visitStatementDMLUpdateAssignment(
            node: Statement.DML.Update.Assignment,
            ctx: Ctx
        ) = translate(node) {
            val path = visitExprPath(node.target, ctx)
            // PIG models target as Expr, unpack the path if it's only one node
            val target = if (path.steps.isEmpty()) path.root else path
            val value = visitExpr(node.value, ctx)
            assignment(target, value)
        }

        override fun visitStatementDMLRemove(node: Statement.DML.Remove, ctx: Ctx) = translate(node) {
            val target = visitExpr(node.target.root, ctx)
            val op = remove(target)
            dml(dmlOpList(op), null, null, null)
        }

        override fun visitStatementDMLDelete(node: Statement.DML.Delete, ctx: Ctx) = translate(node) {
            val from = visitStatementDMLTarget(node.from, ctx)
            val where = node.where?.let { visitExpr(it, ctx) }
            val returning = node.returning?.let { visitReturning(it, ctx) }
            val op = delete()
            dml(dmlOpList(op), from, where, returning)
        }

        override fun visitStatementDMLBatch(node: Statement.DML.Batch, ctx: Ctx) = translate(node) {
            val from = visitFrom(node.from, ctx)
            val ops = translate(node.ops, ctx, PartiqlAst.DmlOp::class)
            val where = node.where?.let { visitExpr(it, ctx) }
            val returning = node.returning?.let { visitReturning(it, ctx) }
            dml(dmlOpList(ops), from, where, returning)
        }

        override fun visitStatementDMLBatchOp(node: Statement.DML.Batch.Op, ctx: Ctx) =
            super.visitStatementDMLBatchOp(node, ctx) as PartiqlAst.DmlOp

        override fun visitStatementDMLBatchOpSet(
            node: Statement.DML.Batch.Op.Set,
            ctx: Ctx
        ) = translate(node) {
            if (node.assignments.size != 1) {
                throw IllegalArgumentException("dml_op.set must have only one assignment")
            }
            val assignment = visitStatementDMLUpdateAssignment(node.assignments[0], ctx)
            set(assignment)
        }

        override fun visitStatementDMLBatchOpRemove(
            node: Statement.DML.Batch.Op.Remove,
            ctx: Ctx
        ) = translate(node) {
            val target = visitExprPath(node.target, ctx)
            remove(target)
        }

        override fun visitStatementDMLBatchOpDelete(
            node: Statement.DML.Batch.Op.Delete,
            ctx: Ctx
        ) = translate(node) {
            delete()
        }

        override fun visitStatementDMLTarget(node: Statement.DML.Target, ctx: Ctx) = translate(node) {
            // PIG models a target as a FROM source
            val expr = visitExpr(node.table, ctx)
            scan(expr, null, null, null)
        }

        override fun visitOnConflict(node: OnConflict, ctx: Ctx) = translate(node) {
            val expr = visitOnConflictTarget(node.target, ctx)
            val action = visitOnConflictAction(node.action, ctx)
            onConflict(expr, action)
        }

        override fun visitOnConflictTarget(node: OnConflict.Target, ctx: Ctx) =
            super.visitOnConflictTarget(node, ctx) as PartiqlAst.Expr

        override fun visitOnConflictTargetCondition(
            node: OnConflict.Target.Condition,
            ctx: Ctx
        ) = translate(node) {
            visitExpr(node.condition, ctx)
        }

        override fun visitOnConflictTargetSymbols(
            node: OnConflict.Target.Symbols,
            ctx: Ctx
        ) = translate(node) {
            list(node.symbols.map { lit(ionSymbol(it)) })
        }

        override fun visitOnConflictTargetConstraint(
            node: OnConflict.Target.Constraint,
            ctx: Ctx
        ) = translate(node) {
            lit(ionSymbol(node.constraint))
        }

        override fun visitOnConflictAction(node: OnConflict.Action, ctx: Ctx) =
            super.visitOnConflictAction(node, ctx) as PartiqlAst.ConflictAction

        override fun visitOnConflictActionDoReplace(
            node: OnConflict.Action.DoReplace,
            ctx: Ctx
        ) = translate(node) {
            doReplace(excluded())
        }

        override fun visitOnConflictActionDoUpdate(
            node: OnConflict.Action.DoUpdate,
            ctx: Ctx
        ) = translate(node) {
            doUpdate(excluded())
        }

        override fun visitOnConflictActionDoNothing(
            node: OnConflict.Action.DoNothing,
            ctx: Ctx
        ) = translate(node) {
            doNothing()
        }

        override fun visitReturning(node: Returning, ctx: Ctx) = translate(node) {
            val elems = translate(node.columns, ctx, PartiqlAst.ReturningElem::class)
            returningExpr(elems)
        }

        override fun visitReturningColumn(node: Returning.Column, ctx: Ctx) = translate(node) {
            // a fine example of `when` is `if`, not pattern matching
            val mapping = when (node.status) {
                Returning.Column.Status.MODIFIED -> when (node.age) {
                    Returning.Column.Age.OLD -> modifiedOld()
                    Returning.Column.Age.NEW -> modifiedNew()
                }
                Returning.Column.Status.ALL -> when (node.age) {
                    Returning.Column.Age.OLD -> allOld()
                    Returning.Column.Age.NEW -> allNew()
                }
            }
            val column = visitReturningColumnValue(node.value, ctx)
            returningElem(mapping, column)
        }

        override fun visitReturningColumnValue(node: Returning.Column.Value, ctx: Ctx) =
            super.visitReturningColumnValue(node, ctx) as PartiqlAst.ColumnComponent

        override fun visitReturningColumnValueWildcard(
            node: Returning.Column.Value.Wildcard,
            ctx: Ctx
        ) = translate(node) {
            returningWildcard()
        }

        override fun visitReturningColumnValueExpression(
            node: Returning.Column.Value.Expression,
            ctx: Ctx
        ) = translate(node) {
            val expr = visitExpr(node.expr, ctx)
            returningColumn(expr)
        }

        override fun visitStatementExec(node: Statement.Exec, ctx: Ctx) = translate(node) {
            val procedureName = node.procedure
            val args = translate(node.args, ctx, PartiqlAst.Expr::class)
            exec(procedureName, args)
        }

        override fun visitType(node: Type, ctx: Ctx) = translate(node) {
            // parameters are only integers for now
            val parameters = node.parameters.map { it.asAnyElement().longValue }
            when (node.identifier) {
                "null" -> nullType()
                "missing" -> missingType()
                "any" -> anyType()
                "blob" -> blobType()
                "bool" -> booleanType()
                "bag" -> bagType()
                "array" -> listType()
                "sexp" -> sexpType()
                "date" -> dateType()
                "time" -> timeType()
                "timestamp" -> timestampType()
                "numeric" -> {
                    when (parameters.size) {
                        0 -> numericType()
                        1 -> numericType(parameters[0])
                        2 -> numericType(parameters[0], parameters[1])
                        else -> throw IllegalArgumentException("Too many parameters for numeric type")
                    }
                }
                "decimal" -> {
                    when (parameters.size) {
                        0 -> decimalType()
                        1 -> decimalType(parameters[0])
                        2 -> decimalType(parameters[0], parameters[1])
                        else -> throw IllegalArgumentException("Too many parameters for decimal type")
                    }
                }
                "float" -> floatType()
                "int" -> integerType()
                "varchar" -> {
                    if (parameters.isNotEmpty()) {
                        characterVaryingType(parameters[0])
                    } else {
                        characterVaryingType()
                    }
                }
                "tuple" -> structType()
                else -> throw IllegalArgumentException("Type ${node.identifier} does not exist in the PIG AST")
            }
        }

        override fun visitExprMissing(node: Expr.Missing, ctx: Ctx) = translate(node) {
            missing()
        }

        override fun visitExprLit(node: Expr.Lit, ctx: Ctx) = translate(node) {
            lit(node.value)
        }

        override fun visitExprIdentifier(node: Expr.Identifier, ctx: Ctx) = translate(node) {
            val case = when (node.case) {
                Case.SENSITIVE -> caseSensitive()
                Case.INSENSITIVE -> caseInsensitive()
            }
            val qualifier = when (node.scope) {
                Expr.Identifier.Scope.UNQUALIFIED -> unqualified()
                Expr.Identifier.Scope.LOCALS_FIRST -> localsFirst()
            }
            id(node.name, case, qualifier)
        }

        override fun visitExprPath(node: Expr.Path, ctx: Ctx) = translate(node) {
            val root = visitExpr(node.root, ctx)
            val steps = translate(node.steps, ctx, PartiqlAst.PathStep::class)
            path(root, steps)
        }

        override fun visitExprPathStep(node: Expr.Path.Step, ctx: Ctx) =
            super.visitExprPathStep(node, ctx) as PartiqlAst.PathStep

        override fun visitExprPathStepIndex(node: Expr.Path.Step.Index, ctx: Ctx) = translate(node) {
            val key = visitExpr(node.key, ctx)
            val case = when (node.case) {
                Case.SENSITIVE -> caseSensitive()
                Case.INSENSITIVE -> caseInsensitive()
            }
            pathExpr(key, case)
        }

        override fun visitExprPathStepWildcard(node: Expr.Path.Step.Wildcard, ctx: Ctx) =
            translate(node) {
                pathWildcard()
            }

        override fun visitExprPathStepUnpivot(node: Expr.Path.Step.Unpivot, ctx: Ctx) = translate(node) {
            pathUnpivot()
        }

        override fun visitExprCall(node: Expr.Call, ctx: Ctx) = translate(node) {
            val funcName = node.function
            var args = translate(node.args, ctx, PartiqlAst.Expr::class)
            when (val form = specialCalls[funcName]) {
                null -> call(funcName, args)
                else -> {
                    val rewriter = form(args)
                    args = rewriter.second.mapIndexedNotNull { i, a ->
                        optional(node, i + 1) {
                            when (a) {
                                is PartiqlAst.Expr -> a
                                is String -> lit(ionSymbol(a))
                                else -> throw IllegalArgumentException("argument must be an expression or symbol")
                            }
                        }
                    }
                    call(rewriter.first, args)
                }
            }
        }

        override fun visitExprAgg(node: Expr.Agg, ctx: Ctx) = translate(node) {
            val setq = translate(node.quantifier)
            val funcName = node.function
            val arg = visitExpr(node.args[0], ctx) // PIG callAgg only has one arg
            callAgg(setq, funcName, arg)
        }

        override fun visitExprParameter(node: Expr.Parameter, ctx: Ctx) = translate(node) {
            parameter(node.index.toLong())
        }

        override fun visitExprUnary(node: Expr.Unary, ctx: Ctx) = translate(node) {
            val expr = visitExpr(node.expr, ctx)
            when (node.op) {
                Expr.Unary.Op.NOT -> not(expr)
                Expr.Unary.Op.POS -> pos(expr)
                Expr.Unary.Op.NEG -> neg(expr)
            }
        }

        override fun visitExprBinary(node: Expr.Binary, ctx: Ctx) = translate(node) {
            val lhs = visitExpr(node.lhs, ctx)
            val rhs = visitExpr(node.rhs, ctx)
            val operands = listOf(lhs, rhs)
            when (node.op) {
                Expr.Binary.Op.PLUS -> plus(operands)
                Expr.Binary.Op.MINUS -> minus(operands)
                Expr.Binary.Op.TIMES -> times(operands)
                Expr.Binary.Op.DIVIDE -> divide(operands)
                Expr.Binary.Op.MODULO -> modulo(operands)
                Expr.Binary.Op.CONCAT -> concat(operands)
                Expr.Binary.Op.AND -> and(operands)
                Expr.Binary.Op.OR -> or(operands)
                Expr.Binary.Op.EQ -> eq(operands)
                Expr.Binary.Op.NE -> ne(operands)
                Expr.Binary.Op.GT -> gt(operands)
                Expr.Binary.Op.GTE -> gte(operands)
                Expr.Binary.Op.LT -> lt(operands)
                Expr.Binary.Op.LTE -> lte(operands)
            }
        }

        override fun visitExprCollection(node: Expr.Collection, ctx: Ctx) = translate(node) {
            val values = translate(node.values, ctx, PartiqlAst.Expr::class)
            when (node.type) {
                Expr.Collection.Type.BAG -> bag(values)
                Expr.Collection.Type.ARRAY -> list(values)
                Expr.Collection.Type.LIST -> list(values).withMeta(IsListParenthesizedMeta.tag, IsListParenthesizedMeta)
                Expr.Collection.Type.SEXP -> sexp(values)
            }
        }

        override fun visitExprTuple(node: Expr.Tuple, ctx: Ctx) = translate(node) {
            val fields = translate(node.fields, ctx, PartiqlAst.ExprPair::class)
            struct(fields)
        }

        override fun visitExprTupleField(node: Expr.Tuple.Field, ctx: Ctx) = translate(node) {
            val first = visitExpr(node.name, ctx)
            val second = visitExpr(node.value, ctx)
            exprPair(first, second)
        }

        override fun visitExprDate(node: Expr.Date, ctx: Ctx) = translate(node) {
            date(node.year, node.month, node.day)
        }

        override fun visitExprTime(node: Expr.Time, ctx: Ctx) = translate(node) {
            timeValue(
                node.hour,
                node.minute,
                node.second,
                node.nano,
                node.precision,
                node.tzOffsetMinutes != null,
                node.tzOffsetMinutes
            )
        }

        override fun visitExprLike(node: Expr.Like, ctx: Ctx) = translate(node) {
            val value = visitExpr(node.value, ctx)
            val pattern = visitExpr(node.pattern, ctx)
            val escape = node.escape?.let { visitExpr(it, ctx) }
            like(value, pattern, escape)
        }

        override fun visitExprBetween(node: Expr.Between, ctx: Ctx) = translate(node) {
            val value = visitExpr(node.value, ctx)
            val from = visitExpr(node.from, ctx)
            val to = visitExpr(node.to, ctx)
            between(value, from, to)
        }

        override fun visitExprInCollection(node: Expr.InCollection, ctx: Ctx) = translate(node) {
            val lhs = visitExpr(node.lhs, ctx)
            val rhs = visitExpr(node.rhs, ctx)
            val operands = listOf(lhs, rhs)
            inCollection(operands)
        }

        override fun visitExprIsType(node: Expr.IsType, ctx: Ctx) = translate(node) {
            val value = visitExpr(node.value, ctx)
            val type = visitType(node.type, ctx)
            isType(value, type)
        }

        override fun visitExprSwitch(node: Expr.Switch, ctx: Ctx) = translate(node) {
            val pairs = exprPairList(translate(node.branches, ctx, PartiqlAst.ExprPair::class))
            val default = node.default?.let { visitExpr(it, ctx) }
            when (node.expr) {
                null -> searchedCase(pairs, default)
                else -> simpleCase(visitExpr(node.expr!!, ctx), pairs, default)
            }
        }

        override fun visitExprSwitchBranch(node: Expr.Switch.Branch, ctx: Ctx) = translate(node) {
            val first = visitExpr(node.condition, ctx)
            val second = visitExpr(node.expr, ctx)
            exprPair(first, second)
        }

        override fun visitExprCoalesce(node: Expr.Coalesce, ctx: Ctx) = translate(node) {
            val args = translate(node.args, ctx, PartiqlAst.Expr::class)
            coalesce(args)
        }

        override fun visitExprNullIf(node: Expr.NullIf, ctx: Ctx) = translate(node) {
            val expr1 = visitExpr(node.expr0, ctx)
            val expr2 = visitExpr(node.expr1, ctx)
            nullIf(expr1, expr2)
        }

        override fun visitExprCast(node: Expr.Cast, ctx: Ctx) = translate(node) {
            val value = visitExpr(node.value, ctx)
            val asType = visitType(node.asType, ctx)
            cast(value, asType)
        }

        override fun visitExprCanCast(node: Expr.CanCast, ctx: Ctx) = translate(node) {
            val value = visitExpr(node.value, ctx)
            val asType = visitType(node.asType, ctx)
            canCast(value, asType)
        }

        override fun visitExprCanLosslessCast(node: Expr.CanLosslessCast, ctx: Ctx) = translate(node) {
            val value = visitExpr(node.value, ctx)
            val asType = visitType(node.asType, ctx)
            canLosslessCast(value, asType)
        }

        override fun visitExprSet(node: Expr.Set, ctx: Ctx) = translate(node) {
            val quantifier = translate(node.quantifier)
            val op = when (node.op) {
                Expr.Set.Op.UNION -> if (node.outer) outerUnion() else union()
                Expr.Set.Op.INTERSECT -> if (node.outer) outerIntersect() else intersect()
                Expr.Set.Op.EXCEPT -> if (node.outer) outerExcept() else except()
            }
            val lhs = visitExpr(node.lhs, ctx)
            val rhs = visitExpr(node.rhs, ctx)
            val operands = listOf(lhs, rhs)
            bagOp(op, quantifier, operands)
        }

        override fun visitExprSFW(node: Expr.SFW, ctx: Ctx) = translate(node) {
            var setq = when (val s = node.select) {
                is Select.Pivot -> null
                is Select.Project -> translate(s.quantifier)
                is Select.Star -> null
                is Select.Value -> translate(s.quantifier)
            }
            // PIG AST omits setq if ALL
            if (setq is PartiqlAst.SetQuantifier.All) {
                setq = null
            }
            val project = visitSelect(node.select, ctx)
            val from = visitFrom(node.from, ctx)
            val fromLet = node.let?.let { visitLet(it, ctx) }
            val where = node.where?.let { visitExpr(it, ctx) }
            val groupBy = node.groupBy?.let { visitGroupBy(it, ctx) }
            val having = node.having?.let { visitExpr(it, ctx) }
            val orderBy = node.orderBy?.let { visitOrderBy(it, ctx) }
            val limit = node.limit?.let { visitExpr(it, ctx) }
            val offset = node.offset?.let { visitExpr(it, ctx) }
            select(setq, project, from, fromLet, where, groupBy, having, orderBy, limit, offset)
        }

        override fun visitExprMatch(node: Expr.Match, ctx: Ctx) = translate(node) {
            TODO("GPML Translation not implemented")
        }

        override fun visitExprWindow(node: Expr.Window, ctx: Ctx) = translate(node) {
            TODO("WINDOW Translation not implemented")
        }

        override fun visitSelect(node: Select, ctx: Ctx) = super.visitSelect(node, ctx) as PartiqlAst.Projection

        override fun visitSelectStar(node: Select.Star, ctx: Ctx) = translate(node) {
            projectStar()
        }

        override fun visitSelectProject(node: Select.Project, ctx: Ctx) = translate(node) {
            val items = translate(node.items, ctx, PartiqlAst.ProjectItem::class)
            projectList(items)
        }

        override fun visitSelectProjectItemAll(node: Select.Project.Item.All, ctx: Ctx) = translate(node) {
            val expr = visitExpr(node.expr, ctx)
            projectAll(expr)
        }

        override fun visitSelectProjectItemVar(node: Select.Project.Item.Var, ctx: Ctx) = translate(node) {
            val expr = visitExpr(node.expr, ctx)
            val asAlias = node.asAlias
            projectExpr(expr, asAlias)
        }

        override fun visitSelectPivot(node: Select.Pivot, ctx: Ctx) = translate(node) {
            val value = visitExpr(node.value, ctx)
            val key = visitExpr(node.key, ctx)
            projectPivot(value, key)
        }

        override fun visitSelectValue(node: Select.Value, ctx: Ctx) = translate(node) {
            val value = visitExpr(node.constructor, ctx)
            projectValue(value)
        }

        override fun visitFrom(node: From, ctx: Ctx) = super.visitFrom(node, ctx) as PartiqlAst.FromSource

        override fun visitFromCollection(node: From.Collection, ctx: Ctx) = translate(node) {
            val expr = visitExpr(node.expr, ctx)
            val asAlias = node.asAlias
            val atAlias = node.atAlias
            val byAlias = node.byAlias
            when (node.unpivot) {
                true -> unpivot(expr, asAlias, atAlias, byAlias)
                else -> scan(expr, asAlias, atAlias, byAlias)
            }
        }

        override fun visitFromJoin(node: From.Join, ctx: Ctx) = translate(node) {
            val type = when (node.type) {
                From.Join.Type.INNER -> inner()
                From.Join.Type.LEFT -> left()
                From.Join.Type.RIGHT -> right()
                From.Join.Type.FULL -> full()
            }
            val left = visitFrom(node.lhs, ctx)
            val right = visitFrom(node.rhs, ctx)
            val predicate = node.condition?.let { visitExpr(it, ctx) }
            join(type, left, right, predicate)
        }

        override fun visitLet(node: Let, ctx: Ctx) = translate(node) {
            val bindings = translate(node.bindings, ctx, PartiqlAst.LetBinding::class)
            let(bindings)
        }

        override fun visitLetBinding(node: Let.Binding, ctx: Ctx) = translate(node) {
            val expr = visitExpr(node.expr, ctx)
            val name = node.asAlias
            letBinding(expr, name)
        }

        override fun visitGroupBy(node: GroupBy, ctx: Ctx) = translate(node) {
            val strategy = when (node.strategy) {
                GroupBy.Strategy.FULL -> groupFull()
                GroupBy.Strategy.PARTIAL -> groupPartial()
            }
            val keyList = groupKeyList(translate(node.keys, ctx, PartiqlAst.GroupKey::class))
            val groupAsAlias = node.asAlias
            groupBy(strategy, keyList, groupAsAlias)
        }

        override fun visitGroupByKey(node: GroupBy.Key, ctx: Ctx) = translate(node) {
            val expr = visitExpr(node.expr, ctx)
            val asAlias = node.asAlias
            groupKey(expr, asAlias)
        }

        override fun visitOrderBy(node: OrderBy, ctx: Ctx) = translate(node) {
            val sortSpecs = translate(node.sorts, ctx, PartiqlAst.SortSpec::class)
            orderBy(sortSpecs)
        }

        override fun visitOrderBySort(node: OrderBy.Sort, ctx: Ctx) = translate(node) {
            val expr = visitExpr(node.expr, ctx)
            val orderingSpec = optional(node, 2) {
                when (node.dir) {
                    OrderBy.Sort.Dir.ASC -> asc()
                    OrderBy.Sort.Dir.DESC -> desc()
                }
            }
            val nullsSpec = optional(node, 3) {
                when (node.nulls) {
                    OrderBy.Sort.Nulls.FIRST -> nullsFirst()
                    OrderBy.Sort.Nulls.LAST -> nullsLast()
                }
            }
            sortSpec(expr, orderingSpec, nullsSpec)
        }

        override fun defaultReturn(node: AstNode, ctx: Ctx) = translate(node) {
            TODO("Not yet implemented")
        }

        // -----

        private fun <T : PartiqlAst.PartiqlAstNode> translate(
            nodes: List<AstNode>,
            ctx: Ctx,
            clazz: KClass<T>
        ): List<T> {
            return nodes.map { clazz.cast(visit(it, ctx)) }
        }

        private fun translate(quantifier: SetQuantifier) = when (quantifier) {
            SetQuantifier.ALL -> PartiqlAst.SetQuantifier.All()
            SetQuantifier.DISTINCT -> PartiqlAst.SetQuantifier.Distinct()
        }
    }
}
