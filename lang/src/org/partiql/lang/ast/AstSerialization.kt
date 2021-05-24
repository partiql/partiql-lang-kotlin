/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates.  All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 *  You may not use this file except in compliance with the License.
 * A copy of the License is located at:
 *
 *      http://aws.amazon.com/apache2.0/
 *
 *  or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 *  language governing permissions and limitations under the License.
 */

package org.partiql.lang.ast

import com.amazon.ion.IonSexp
import com.amazon.ion.IonString
import com.amazon.ion.IonSystem
import org.partiql.lang.util.IonWriterContext
import org.partiql.lang.util.asIonSexp
import org.partiql.lang.util.case
import org.partiql.lang.util.checkThreadInterrupted
import kotlin.UnsupportedOperationException

/**
 * Serializes an instance of [ExprNode] to one of the s-expression based ASTs.
 *
 * Implementations of this should not be assumed to be thread-safe.
 */
@Deprecated("Please use PartiqlAst")
interface AstSerializer {
    fun serialize(exprNode: ExprNode): IonSexp
    companion object {
        /**
         * Given an instance of ExprNode, return its serialized s-expression form.
         *
         * [exprNode] must be valid as determined by [AstSanityVisitor].
         */
        @Deprecated("Please use PartiqlAst")
        @JvmStatic
        fun serialize(exprNode: ExprNode, astVersion: AstVersion, ion: IonSystem): IonSexp {
            return AstSerializerImpl(astVersion, ion).serialize(exprNode)
        }
    }
}

private class AstSerializerImpl(val astVersion: AstVersion, val ion: IonSystem): AstSerializer {
    override fun serialize(exprNode: ExprNode): IonSexp {
        val resultSexp = ion.newEmptySexp()
        val writer = ion.newWriter(resultSexp)
        IonWriterContext(writer).apply {
            this.writeExprNode(exprNode)
        }
        return resultSexp[0].asIonSexp()
    }

    private fun IonWriterContext.writeAsTerm(metas: MetaContainer?, block: IonWriterContext.() -> Unit) {
        val sloc = metas?.find(SourceLocationMeta.TAG) as? SourceLocationMeta
        if(sloc != null) {
            sexp {
                symbol("meta")
                block()
                struct {
                    int("line", sloc.lineNum)
                    int("column", sloc.charOffset)
                }
            }
        }
        else {
            block()
        }
    }

    private fun IonWriterContext.writeExprNode(expr: ExprNode): Unit =
        writeAsTerm(expr.metas) {
            checkThreadInterrupted()
            sexp {
                when (expr) {
                    // Leaf nodes
                    is Literal           -> case { writeLiteral(expr) }
                    is LiteralMissing    -> case { writeLiteralMissing(expr) }
                    is VariableReference -> case { writeVariableReference(expr) }
                    is NAry              -> case { writeNAry(expr) }
                    is CallAgg           -> case { writeCallAgg(expr) }
                    is Typed             -> case { writeTyped(expr) }
                    is Path              -> case { writePath(expr) }
                    is SimpleCase        -> case { writeSimpleCase(expr) }
                    is SearchedCase      -> case { writeSearchedCase(expr) }
                    is Struct            -> case { writeStruct(expr) }
                    is Seq               -> case { writeSeq(expr) }
                    is Select            -> case { writeSelect(expr) }
                    is DataManipulation  -> case { writeDataManipulation(expr) }
                    is CreateTable       -> case { writeCreateTable(expr) }
                    is CreateIndex       -> case { writeCreateIndex(expr) }
                    is DropTable         -> case { writeDropTable(expr) }
                    is DropIndex         -> case { writeDropIndex(expr) }
                    is Parameter         -> case { writeParameter(expr)}
                    is DateTimeType.Date -> throw UnsupportedOperationException("DATE literals not supported by the V0 AST")
                    is DateTimeType.Time -> throw UnsupportedOperationException("TIME literals not supported by the V0 AST")
                    is Exec              -> throw UnsupportedOperationException("EXEC clause not supported by the V0 AST")
                }.toUnit()
            }
        }

    private fun IonWriterContext.writeLiteral(expr: Literal) {
        val (ionValue, _: MetaContainer) = expr
        symbol("lit")
        value(ionValue)
    }

    private fun IonWriterContext.writeLiteralMissing(expr: LiteralMissing) {
        val (_: MetaContainer) = expr
        symbol("missing")
    }

    private fun IonWriterContext.writeVariableReference(expr: VariableReference) {
        val (id, sensitivity, lookup, _: MetaContainer) = expr

        when(astVersion) {
            AstVersion.V0 -> {
                fun writeVarRef() {
                    symbol("id")
                    symbol(id)
                    symbol(sensitivity.toSymbol())
                }
                if (lookup == ScopeQualifier.LEXICAL) {
                    symbol("@")
                    sexp {
                        writeVarRef()
                    }
                }
                else {
                    writeVarRef()
                }
            }
        }

    }

    private fun IonWriterContext.writeCallAgg(expr: CallAgg) {
        val (funcExpr, setQuantifier, arg, metas: MetaContainer) = expr
        when {
            metas.hasMeta(IsCountStarMeta.TAG) -> {
                symbol("call_agg_wildcard")
                symbol("count")
            }
            else -> {
                symbol("call_agg")
                when (astVersion) {
                    AstVersion.V0 -> {
                        symbol(funcExpr.getFuncName())
                        symbol(setQuantifier.toString().toLowerCase())
                    }
                }
                writeExprNode(arg)
            }

        }
    }

    private fun IonWriterContext.writeTyped(expr: Typed) {
        val (op: TypedOp, exp, dataType, _: MetaContainer) = expr
        symbol(op.text)

        writeExprNode(exp)
        writeDataType(dataType)
    }

    private fun IonWriterContext.writeStruct(expr: Struct) {
        val (fields, _: MetaContainer) = expr
        symbol("struct")
        fields.forEach {
            val (name, valueExpr) = it

            when (astVersion) {
                AstVersion.V0 -> {
                    writeExprNode(name)
                    writeExprNode(valueExpr)
                }
            }
        }
    }

    fun IonWriterContext.writeDataType(dataType: DataType) {
        writeAsTerm(dataType.metas) {
            sexp {
                when(astVersion) {
                    AstVersion.V0 -> {
                        symbol("type")
                        symbol(dataType.sqlDataType.typeName)
                        dataType.args.forEach {
                            int(it)
                        }
                    }
                }
            }
        }
    }

    private fun IonWriterContext.writeSeq(expr: Seq) {
        val (type, items, _: MetaContainer) = expr
        symbol(type.typeName)
        items.forEach {
            writeExprNode(it)
        }
    }

    private fun IonWriterContext.writeSelect(expr: Select) {
        val (setQuantifier, projection, from, fromLet, where, groupBy, having, orderBy, limit, _: MetaContainer) = expr
        if (orderBy != null) {
            throw UnsupportedOperationException("ORDER BY clause is not supported by the V0 AST")
        }

        if (fromLet != null) {
            throw UnsupportedOperationException("LET clause is not supported by the V0 AST")
        }

        writeSelectProjection(projection, setQuantifier)

        sexp {
            symbol("from")
            writeFromSource(from)
        }

        where?.let {
            sexp {
                symbol("where")
                writeExprNode(it)
            }
        }

        groupBy?.let {
            val (grouping, groupByItems, groupName) = it

            sexp {
                symbol(
                    when (grouping) {
                        GroupingStrategy.FULL    -> "group"
                        GroupingStrategy.PARTIAL -> "group_partial"
                    }
                )
                sexp {
                    symbol("by")
                    groupByItems.forEach { gbi ->
                        val (itemExpr, asName) = gbi
                        if (asName != null) {
                            writeAsTerm(asName.metas) {
                                sexp {
                                    symbol("as")
                                    symbol(asName.name)
                                    writeExprNode(itemExpr)
                                }
                            }
                        }
                        else {
                            writeExprNode(itemExpr)
                        }
                    }
                }
                groupName?.let { gn ->
                    writeAsTerm(gn.metas) {
                        sexp {
                            symbol("name")
                            symbol(gn.name)
                        }
                    }
                }
            }
        }

        having?.let {
            sexp {
                symbol("having")
                writeExprNode(it)
            }
        }

        limit?.let {
            sexp {
                symbol("limit")
                writeExprNode(limit)
            }
        }
    }

    private fun IonWriterContext.writeDataManipulation(expr: DataManipulation) {
        val (dmlOp, from, where, returning, _: MetaContainer) = expr

        if (returning != null) {
            throw java.lang.UnsupportedOperationException("RETURNING clause not supported by V0 AST.")
        }

        // TODO refactor this along with SELECT to be more functional (i.e. granular)
        symbol("dml")

        when (astVersion) {
            AstVersion.V0 -> writeDataManipulationOperation(dmlOp)
        }

        from?.let {
            sexp {
                symbol("from")
                writeFromSource(it)
            }
        }
        where?.let {
            sexp{
                symbol("where")
                writeExprNode(it)
            }
        }
    }

    private fun IonWriterContext.writeDataManipulationOperation(opList: DmlOpList) {
        // The V0 AST supports multiple set operations, i.e.: SET a = 1, b = 2, c = 3,
        // but it does *not* support mixing multiple DML operations together, i.e.: SET a = 1, REMOVE b is illegal
        // The following bit of code checks to make sure that [dmlOp] only contains items that can be represented in
        // the V0 AST.
        val isSetOnly = if(!opList.ops.any { it !is AssignmentOp }) {
            if(opList.ops.size != 1) {
                throw UnsupportedOperationException(
                    "A single DML statement with multiple operations other than SET cannot be represented with the V0 AST")
            }
            true
        } else {
            false
        }

        if(!isSetOnly) {
            val dmlOp = opList.ops.first()

            sexp {
                symbol(dmlOp.name)

                when (dmlOp) {
                    is InsertOp -> {
                        writeExprNode(dmlOp.lvalue)
                        writeExprNode(dmlOp.values)
                    }
                    is InsertValueOp -> {
                        writeExprNode(dmlOp.lvalue)
                        writeExprNode(dmlOp.value)

                        when (astVersion) {
                            AstVersion.V0 ->
                                dmlOp.position?.let { writeExprNode(it) }
                        }
                    }
                    is RemoveOp -> {
                        writeExprNode(dmlOp.lvalue)
                    }
                    is DeleteOp -> {
                        // no-op - implicit target
                    }
                    is AssignmentOp -> error("should be handled by else block below")
                }.let {}
            }
        } else {
            sexp {
                symbol("set")
                // Cast should always succeed due to checks at top of function.
                opList.ops.map { it as AssignmentOp }.forEach {
                    val assignment = it.assignment
                    sexp {
                        symbol("assignment")
                        writeExprNode(assignment.lvalue)
                        writeExprNode(assignment.rvalue)
                    }
                }
            }

        }
    }


    private fun IonWriterContext.writeSelectProjection(projection: SelectProjection, setQuantifier: SetQuantifier) {
        when (astVersion) {
            AstVersion.V0 ->
                when (projection) {
                    is SelectProjectionValue -> case { writeSelectProjectionValueV0(projection, setQuantifier) }
                    is SelectProjectionPivot -> case { writeSelectProjectionPivotV0(projection) }
                    is SelectProjectionList -> case { writeSelectProjectionListV0(projection, setQuantifier) }
                }.toUnit()
        }
    }

    private fun IonWriterContext.writeSelectProjectionValueV0(
        projection: SelectProjectionValue,
        setQuantifier: SetQuantifier) {
        val (valueExpr) = projection
        symbol("select")
        sexp {
            symbol(
                when (setQuantifier) {
                    SetQuantifier.ALL      -> "project"
                    SetQuantifier.DISTINCT -> "project_distinct"
                }
            )
            sexp {
                symbol("value")
                writeExprNode(valueExpr)
            }
        }
    }

    private fun IonWriterContext.writeSelectProjectionPivotV0(projection: SelectProjectionPivot) {
        val (asExpr, atExpr) = projection
        symbol("pivot")

        sexp {
            symbol("member")
            writeExprNode(asExpr)
            writeExprNode(atExpr)
        }
    }

    private fun IonWriterContext.writeSelectProjectionListV0(
        projection: SelectProjectionList,
        setQuantifier: SetQuantifier) {
        val (items) = projection
        symbol("select")

        sexp {
            symbol(
                when (setQuantifier) {
                    SetQuantifier.ALL      -> "project"
                    SetQuantifier.DISTINCT -> "project_distinct"
                }
            )

            sexp {
                symbol("list")
                items.forEach {
                    when (it) {
                        is SelectListItemStar       -> case { writeSelectListItemStar(it) }
                        is SelectListItemExpr       -> case { writeSelectListItemExpr(it) }
                        is SelectListItemProjectAll -> case { writeSelectListItemProjectAll(it) }
                    }.toUnit()
                }
            }
        }
    }

    private fun IonWriterContext.writeSelectListItemStar(it: SelectListItemStar) {
        writeAsTerm(it.metas) {
            sexp {
                case { symbol("project_all") }.toUnit()
            }
        }
    }

    private fun IonWriterContext.writeSelectListItemExpr(it: SelectListItemExpr) {
        val (itemExpr, asName) = it
        if (asName != null) {
            writeAsTerm(asName.metas) {
                sexp {
                    symbol("as")
                    symbol(asName.name)
                    writeExprNode(itemExpr)
                }
            }
        }
        else {
            writeExprNode(itemExpr)
        }
    }

    private fun IonWriterContext.writeSelectListItemProjectAll(it: SelectListItemProjectAll) {
        val (exp) = it
        sexp {
            symbol("project_all")
            writeExprNode(exp)
        }
    }

    private fun IonWriterContext.writeFromSource(fromSource: FromSource): Unit =
        when(astVersion) {
            AstVersion.V0 -> writeFromSourceV0(fromSource)
        }

    /** Emits an untyped null if a null [name] is passed or a symbol consisting of [name] otherwise. */
    private fun IonWriterContext.writeSymbolicName(name: SymbolicName?): Unit =
        when (name) {
            null -> untypedNull()
            else -> writeAsTerm(name.metas) { symbol(name.name) }
        }

    private fun IonWriterContext.writeFromSourceV0(fromSource: FromSource) {
        when (fromSource) {
            is FromSourceExpr -> case {
                val (exp, aliases) = fromSource
                nestByAlias(aliases) {
                    writeExprNode(exp)
                }
            }
            is FromSourceUnpivot -> case {
                val (exp, aliases, metas) = fromSource
                nestByAlias(aliases) {
                    writeAsTerm(metas) {
                        sexp {
                            symbol("unpivot")
                            writeExprNode(exp)
                        }
                    }
                }
            }
            is FromSourceJoin -> case {
                val (op, leftRef, rightRef, condition) = fromSource
                writeAsTerm(fromSource.metas) {
                    sexp {
                        symbol(
                            when (op) {
                                JoinOp.INNER -> "inner_join"
                                JoinOp.LEFT -> "left_join"
                                JoinOp.RIGHT -> "right_join"
                                JoinOp.OUTER -> "outer_join"
                            }
                        )
                        writeFromSource(leftRef)
                        writeFromSource(rightRef)

                        if (!fromSource.metas.hasMeta(IsImplictJoinMeta.TAG)) {
                            writeExprNode(condition)
                        }
                    }
                }
            }
        }.toUnit()
    }

    private fun IonWriterContext.writeSimpleCase(expr: SimpleCase) {
        symbol("simple_case")
        val (valueExpr, whenClauses, elseExpr, _: MetaContainer) = expr
        writeExprNode(valueExpr)
        when (astVersion) {
            AstVersion.V0 -> {
                whenClauses.forEach {
                    val (whenValueExpr, thenExpr) = it
                    sexp {
                        symbol("when")
                        writeExprNode(whenValueExpr)
                        writeExprNode(thenExpr)
                    }
                }
                elseExpr?.let {
                    sexp {
                        symbol("else")
                        writeExprNode(it)
                    }
                }
            }
        }
    }

    private fun IonWriterContext.writeSearchedCase(expr: SearchedCase) {
        symbol("searched_case")
        val (whenClauses, elseExpr, _: MetaContainer) = expr
        when (astVersion) {
            AstVersion.V0 -> {
                whenClauses.forEach {
                    val (condition, thenExpr) = it
                    sexp {
                        symbol("when")
                        writeExprNode(condition)
                        writeExprNode(thenExpr)
                    }
                }
                elseExpr?.let {
                    sexp {
                        symbol("else")
                        writeExprNode(it)
                    }
                }
            }
        }
    }

    private fun IonWriterContext.writeExprPair(first: ExprNode, second: ExprNode) {
        sexp {
            symbol("expr_pair")
            writeExprNode(first)
            writeExprNode(second)
        }
    }

    private fun IonWriterContext.writeExprPairList(exprPairList: List<Pair<ExprNode, ExprNode>>) {
        sexp {
            symbol("expr_pair_list")
            exprPairList.forEach {
                writeExprPair(it.first, it.second)
            }
        }
    }

    private fun IonWriterContext.writePath(expr: Path) {
        val (root, components, _: MetaContainer) = expr
        symbol("path")
        writeExprNode(root)
        when(astVersion) {
            AstVersion.V0 -> case { writePathComponentsV0(components) }
        }
    }

    private fun IonWriterContext.writePathComponentsV0(components: List<PathComponent>) {
        components.forEach {
            when (it) {
                is PathComponentExpr     -> case { writePathComponentExprV0(it) }
                is PathComponentUnpivot  -> case { writePathComponentUnpivotV0(it) }
                is PathComponentWildcard -> case { writePathComponentWildcardV0(it) }
            }.toUnit()
        }
    }

    private fun IonWriterContext.writePathComponentExprV0(pathComponent: PathComponentExpr) {
        val (exp, case) = pathComponent
        //Only wrap variable references and literal strings in case_[in]sensitive...

        if ((exp is VariableReference) || exp is Literal && exp.ionValue is IonString) {
            sexp {
                symbol(case.toSymbol())
                writeExprNode(exp)
            }
        }
        else {
            writeExprNode(exp)
        }
    }

    private fun IonWriterContext.writePathComponentUnpivotV0(pathComponentUnpivot: PathComponentUnpivot) {
        writeAsTerm(pathComponentUnpivot.metas) {
            sexp {
                symbol("*")
                symbol("unpivot")
            }
        }
    }

    private fun IonWriterContext.writePathComponentWildcardV0(pathComponent: PathComponentWildcard) {
        writeAsTerm(pathComponent.metas) {
            sexp {
                symbol("*")
            }
        }
    }

    private fun IonWriterContext.writeNAry(expr: NAry) {
        val (op, args, _: MetaContainer) = expr

        // The new AST has no equivalent for not_between, not_like and is_not.
        // These are expressed by wrapping between, like and is in an NAry with NAryOp.Not operation.
        // The true branch will unwrap that expression, preserving the original AST form.
        if (astVersion == AstVersion.V0 && op == NAryOp.NOT && expr.metas.hasMeta(LegacyLogicalNotMeta.TAG)) {
            val firstArg = args.first()
            //Note: it is intentional that this is `when` statement and not an expression
            when (firstArg) {
                is NAry  -> {
                    val (argOp, argArgs, _: MetaContainer) = firstArg

                    fun recurseArgs() {
                        argArgs.forEach { writeExprNode(it) }
                    }
                    when (argOp) {
                        NAryOp.BETWEEN -> {
                            symbol("not_between")
                            recurseArgs()
                        }
                        NAryOp.LIKE    -> {
                            symbol("not_like")
                            recurseArgs()
                        }
                        NAryOp.IN      -> {
                            symbol("not_in")
                            recurseArgs()
                        }
                        else           -> {
                            throw IllegalStateException("Invalid NAryOp on argument of `(not )` node decorated with LegacyLogicalNotMeta")
                        }
                    }
                }
                is Typed -> {
                    if (firstArg.op != TypedOp.IS) {
                        throw IllegalStateException("Invalid TypedOp on argument of `(not )` node decorated with LegacyLogicalNotMeta")
                    }
                    symbol("is_not")
                    writeExprNode(firstArg.expr)
                    writeDataType(firstArg.type)
                }
                else     -> {
                    throw IllegalStateException("Invalid node type of of `(not )` node decorated with LegacyLogicalNotMeta")
                }

            }
            return
        }

        val opSymbol = getOpSymbol(op)
        symbol(opSymbol)

        fun writeArgs() = args.forEach {
            writeExprNode(it)
        }

        when(op) {
            NAryOp.CALL -> {
                // Note: we can assume that by this point the AST has been checked
                // for errors.  (in this case that means the arity is at least 1)
                // This means it is safe to access the first element of args below.
                val funcExpr = expr.args.first()
                val funcName = funcExpr.getFuncName()
                symbol(funcName)
                args.drop(1).forEach {
                    writeExprNode(it)
                }
            }
            NAryOp.UNION, NAryOp.EXCEPT, NAryOp.INTERSECT -> {
                writeArgs()
            }
            NAryOp.UNION_ALL, NAryOp.EXCEPT_ALL, NAryOp.INTERSECT_ALL -> {
                writeArgs()
            }
            NAryOp.LIKE -> {
                writeArgs()
            }
            else -> { writeArgs() }
        }
    }

    private fun getOpSymbol(op: NAryOp) =
        when(astVersion) {
            AstVersion.V0 -> op.symbol
        }

    private fun IonWriterContext.writeCreateIndex(expr: CreateIndex) {
        when (astVersion) {
            AstVersion.V0 -> {
                symbol("create")
                symbol(null)
                sexp {
                    symbol("index")
                    symbol(expr.tableName)
                    sexp {
                        symbol("keys")
                        expr.keys.forEach {
                            writeExprNode(it)
                        }
                    }
                }
            }
        }
    }

    private fun IonWriterContext.writeCreateTable(expr: CreateTable) {
        when (astVersion) {
            AstVersion.V0 -> {
                symbol("create")
                symbol(expr.tableName)
                sexp {
                    symbol("table")
                }
            }
        }
    }

    private fun IonWriterContext.writeDropTable(expr: DropTable) {
        when (astVersion) {
            AstVersion.V0 -> {
                symbol("drop_table")
                symbol(expr.tableName)
            }
        }
    }

    private fun IonWriterContext.writeDropIndex(expr: DropIndex) {
        when (astVersion) {
            AstVersion.V0 -> {
                symbol("drop_index")
                symbol(expr.tableName)
                writeExprNode(expr.identifier)
            }
        }
    }

    private fun IonWriterContext.writeDDL(block: () -> Unit) {
        symbol("ddl")
        sexp {
            block()
        }
    }

    private fun IonWriterContext.writeIdentifier(name: String, caseSensitivity: CaseSensitivity) {
        sexp {
            symbol("identifier")
            symbol(name)
            sexp {
                symbol(caseSensitivity.toSymbol())
            }
        }
    }

    private fun IonWriterContext.writeParameter(expr: Parameter) {
        symbol("parameter")
        int(expr.position.toLong())
    }


    private fun IonWriterContext.nestByAlias(variables: LetVariables, block: () -> Unit) {
        if(variables.byName != null) {
            writeAsTerm(variables.byName.metas) {
                sexp {
                    symbol("by")
                    symbol(variables.byName.name)
                    nestAtAlias(variables, block)
                }
            }
        } else {
            nestAtAlias(variables, block)
        }
    }
    private fun IonWriterContext.nestAtAlias(variables: LetVariables, block: () -> Unit) {
        if(variables.atName != null) {
            writeAsTerm(variables.atName.metas) {
                sexp {
                    symbol("at")
                    symbol(variables.atName.name)
                    nestAsAlias(variables, block)
                }
            }
        } else {
            nestAsAlias(variables, block)
        }
    }

    private fun IonWriterContext.nestAsAlias(variables: LetVariables, block: () -> Unit) {
        if(variables.asName != null) {
            writeAsTerm(variables.asName.metas) {
                sexp {
                    symbol("as")
                    symbol(variables.asName.name)
                    block()
                }
            }
        } else {
            block()
        }
    }

    private fun ExprNode.getFuncName(): String {
        return when (this) {
            is VariableReference -> id
            else -> throw UnsupportedOperationException(
                "Using arbitrary expressions to identify a function in a call_agg or call node is not supported. " +
                "Functions must be identified by name only.")
        }
    }

}
