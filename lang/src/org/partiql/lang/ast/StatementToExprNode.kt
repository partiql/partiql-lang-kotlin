package org.partiql.lang.ast

import com.amazon.ion.IonSystem
import com.amazon.ionelement.api.toIonValue
import org.partiql.lang.domains.PartiqlAst.CaseSensitivity
import org.partiql.lang.domains.PartiqlAst.DdlOp
import org.partiql.lang.domains.PartiqlAst.DmlOp
import org.partiql.lang.domains.PartiqlAst.Expr
import org.partiql.lang.domains.PartiqlAst.FromSource
import org.partiql.lang.domains.PartiqlAst.GroupBy
import org.partiql.lang.domains.PartiqlAst.GroupingStrategy
import org.partiql.lang.domains.PartiqlAst.JoinType
import org.partiql.lang.domains.PartiqlAst.Let
import org.partiql.lang.domains.PartiqlAst.PathStep
import org.partiql.lang.domains.PartiqlAst.ProjectItem
import org.partiql.lang.domains.PartiqlAst.Projection
import org.partiql.lang.domains.PartiqlAst.ScopeQualifier
import org.partiql.lang.domains.PartiqlAst.SetQuantifier
import org.partiql.lang.domains.PartiqlAst.Statement
import org.partiql.lang.domains.PartiqlAst.Type
import org.partiql.pig.runtime.SymbolPrimitive

internal typealias PartiQlMetaContainer = org.partiql.lang.ast.MetaContainer
internal typealias IonElementMetaContainer = com.amazon.ionelement.api.MetaContainer

/** Converts a [partiql_ast.statement] to an [ExprNode], preserving all metas where possible. */
fun Statement.toExprNode(ion: IonSystem): ExprNode =
    StatementTransformer(ion).transform(this)

internal fun Expr.toExprNode(ion: IonSystem): ExprNode {
    return StatementTransformer(ion).transform(this)
}

internal fun SetQuantifier.toSetQuantifier(): org.partiql.lang.ast.SetQuantifier =
    when (this) {
        is SetQuantifier.All -> org.partiql.lang.ast.SetQuantifier.ALL
        is SetQuantifier.Distinct -> org.partiql.lang.ast.SetQuantifier.DISTINCT
    }

internal fun com.amazon.ionelement.api.MetaContainer.toPartiQlMetaContainer(): org.partiql.lang.ast.MetaContainer {
    val nonLocationMetas: List<Meta> = this.values.map {
        // We may need to account for this in the future, but for now we require that all metas placed
        // on any `partiql_ast` instances to implement Meta.  It's not clear how to deal with that now
        // so we should wait until it's needed.
        val partiQlMeta = it as? Meta ?: error("The meta was not an instance of Meta.")
        partiQlMeta
    }

    return org.partiql.lang.ast.metaContainerOf(nonLocationMetas)
}

private class StatementTransformer(val ion: IonSystem) {
    fun transform(stmt: Statement): ExprNode =
        when (stmt) {
            is Statement.Query -> stmt.toExprNode()
            is Statement.Dml -> stmt.toExprNode()
            is Statement.Ddl -> stmt.toExprNode()
        }

    fun transform(stmt: Expr): ExprNode =
        stmt.toExprNode()


    private fun Statement.Query.toExprNode(): ExprNode {
        return this.expr.toExprNode()
    }

    private fun List<Expr>.toExprNodeList(): List<ExprNode> =
        this.map { it.toExprNode() }

    private fun Expr.toExprNode(): ExprNode {
        val metas = this.metas.toPartiQlMetaContainer()
        return when (this) {
            is Expr.Missing -> LiteralMissing(metas)
            // https://github.com/amzn/ion-element-kotlin/issues/35, .asAnyElement() is unfortunately needed for now
            is Expr.Lit -> Literal(value.asAnyElement().toIonValue(ion), metas)
            is Expr.Id -> VariableReference(name.text, case.toCaseSensitivity(), qualifier.toScopeQualifier(), metas)
            is Expr.Parameter -> Parameter(index.value.toInt(), metas)
            is Expr.Not -> NAry(NAryOp.NOT, listOf(expr.toExprNode()), metas)
            is Expr.Pos -> expr.toExprNode()
            is Expr.Neg -> NAry(NAryOp.SUB, listOf(expr.toExprNode()), metas)
            is Expr.Plus -> NAry(NAryOp.ADD, operands.toExprNodeList(), metas)
            is Expr.Minus -> NAry(NAryOp.SUB, operands.toExprNodeList(), metas)
            is Expr.Times -> NAry(NAryOp.MUL, operands.toExprNodeList(), metas)
            is Expr.Divide -> NAry(NAryOp.DIV, operands.toExprNodeList(), metas)
            is Expr.Modulo -> NAry(NAryOp.MOD, operands.toExprNodeList(), metas)
            is Expr.Concat -> NAry(NAryOp.STRING_CONCAT, operands.toExprNodeList(), metas)
            is Expr.And -> NAry(NAryOp.AND, operands.toExprNodeList(), metas)
            is Expr.Or -> NAry(NAryOp.OR, operands.toExprNodeList(), metas)
            is Expr.Eq -> NAry(NAryOp.EQ, operands.toExprNodeList(), metas)
            is Expr.Ne -> NAry(NAryOp.NE, operands.toExprNodeList(), metas)
            is Expr.Gt -> NAry(NAryOp.GT, operands.toExprNodeList(), metas)
            is Expr.Gte -> NAry(NAryOp.GTE, operands.toExprNodeList(), metas)
            is Expr.Lt -> NAry(NAryOp.LT, operands.toExprNodeList(), metas)
            is Expr.Lte -> NAry(NAryOp.LTE, operands.toExprNodeList(), metas)

            is Expr.Union ->
                NAry(
                    when(setq) {
                        is SetQuantifier.Distinct -> NAryOp.UNION
                        is SetQuantifier.All -> NAryOp.UNION_ALL
                    },
                    operands.toExprNodeList(),
                    metas)
            is Expr.Intersect ->
                NAry(
                    when(setq) {
                        is SetQuantifier.Distinct -> NAryOp.INTERSECT
                        is SetQuantifier.All -> NAryOp.INTERSECT_ALL
                    },
                    operands.toExprNodeList(),
                    metas)
            is Expr.Except  ->
                NAry(
                    when(setq) {
                        is SetQuantifier.Distinct -> NAryOp.EXCEPT
                        is SetQuantifier.All -> NAryOp.EXCEPT_ALL
                    },
                    operands.toExprNodeList(),
                    metas)


            is Expr.Like -> NAry(NAryOp.LIKE, listOfNotNull(value.toExprNode(), pattern.toExprNode(), escape?.toExprNode()), metas)
            is Expr.Between -> NAry(NAryOp.BETWEEN, listOf(value.toExprNode(), from.toExprNode(), to.toExprNode()), metas)
            is Expr.InCollection -> NAry(NAryOp.IN, operands.toExprNodeList(), metas)
            is Expr.IsType -> Typed(TypedOp.IS, value.toExprNode(), type.toExprNodeType(), metas)
            is Expr.Cast -> Typed(TypedOp.CAST, value.toExprNode(), asType.toExprNodeType(), metas)

            is Expr.SimpleCase ->
                SimpleCase(
                    expr.toExprNode(),
                    cases.pairs.map { SimpleCaseWhen(it.first.toExprNode(), it.second.toExprNode()) },
                    default?.toExprNode(),
                    metas)
            is Expr.SearchedCase ->
                SearchedCase(
                    cases.pairs.map { SearchedCaseWhen(it.first.toExprNode(), it.second.toExprNode()) },
                    this.default?.toExprNode(),
                    metas)
            is Expr.Struct -> Struct(this.fields.map { StructField(it.first.toExprNode(), it.second.toExprNode()) }, metas)
            is Expr.Bag -> Seq(SeqType.BAG, values.toExprNodeList(), metas)
            is Expr.List -> Seq(SeqType.LIST, values.toExprNodeList(), metas)
            is Expr.Sexp -> Seq(SeqType.SEXP, values.toExprNodeList(), metas)
            is Expr.Path ->
                Path(
                    root.toExprNode(),
                    steps.map {
                        val componentMetas = it.metas.toPartiQlMetaContainer()
                        when (it) {
                            is PathStep.PathExpr ->
                                PathComponentExpr(
                                    it.index.toExprNode(),
                                    it.case.toCaseSensitivity())
                            is PathStep.PathUnpivot -> PathComponentUnpivot(componentMetas)
                            is PathStep.PathWildcard -> PathComponentWildcard(componentMetas)
                        }
                    },
                    metas)
            is Expr.Call ->
                NAry(
                    NAryOp.CALL,
                    listOf(
                        VariableReference(
                            funcName.text,
                            org.partiql.lang.ast.CaseSensitivity.INSENSITIVE,
                            org.partiql.lang.ast.ScopeQualifier.UNQUALIFIED,
                            emptyMetaContainer)
                    ) + args.map { it.toExprNode() },
                    metas)
            is Expr.CallAgg ->
                CallAgg(
                    VariableReference(
                        funcName.text,
                        org.partiql.lang.ast.CaseSensitivity.INSENSITIVE,
                        org.partiql.lang.ast.ScopeQualifier.UNQUALIFIED,
                        emptyMetaContainer),
                    setq.toSetQuantifier(),
                    arg.toExprNode(),
                    metas)
            is Expr.Select ->
                Select(
                    setQuantifier = setq?.toSetQuantifier() ?: org.partiql.lang.ast.SetQuantifier.ALL,
                    projection = project.toSelectProjection(),
                    from = from.toFromSource(),
                    fromLet = fromLet?.toLetSource(),
                    where = where?.toExprNode(),
                    groupBy = group?.toGroupBy(),
                    having = having?.toExprNode(),
                    limit = limit?.toExprNode(),
                    metas = metas
            )
        }
    }

    private fun Projection.toSelectProjection(): SelectProjection {
        val metas = this.metas.toPartiQlMetaContainer()
        return when (this) {
            is Projection.ProjectStar -> SelectProjectionList(listOf(SelectListItemStar(metas)))
            is Projection.ProjectValue -> SelectProjectionValue(this.value.toExprNode())
            is Projection.ProjectPivot -> SelectProjectionPivot(this.value.toExprNode(), this.key.toExprNode())
            is Projection.ProjectList ->
                SelectProjectionList(
                    this.projectItems.map {
                        when (it) {
                            is ProjectItem.ProjectAll -> SelectListItemProjectAll(it.expr.toExprNode())
                            is ProjectItem.ProjectExpr ->
                                SelectListItemExpr(
                                    it.expr.toExprNode(),
                                    it.asAlias?.toSymbolicName())
                        }
                    })
        }
    }

    private fun FromSource.toFromSource(): org.partiql.lang.ast.FromSource {
        val metas = this.metas.toPartiQlMetaContainer()
        return when (this) {
            is FromSource.Scan ->
                FromSourceExpr(
                    expr = expr.toExprNode(),
                    variables = LetVariables(
                        asName = asAlias?.toSymbolicName(),
                        atName = atAlias?.toSymbolicName(),
                        byName = byAlias?.toSymbolicName()))
            is FromSource.Unpivot ->
                FromSourceUnpivot(
                    expr = expr.toExprNode(),
                    variables = LetVariables(
                        asName = asAlias?.toSymbolicName(),
                        atName = atAlias?.toSymbolicName(),
                        byName = byAlias?.toSymbolicName()),
                    metas = metas)
            is FromSource.Join ->
                FromSourceJoin(
                    joinOp = type.toJoinOp(),
                    leftRef = left.toFromSource(),
                    rightRef = right.toFromSource(),
                    condition = predicate?.toExprNode() ?: Literal(ion.newBool(true), emptyMetaContainer),
                    metas = metas)
        }
    }

    private fun JoinType.toJoinOp(): JoinOp =
        when (this) {
            is JoinType.Inner -> JoinOp.INNER
            is JoinType.Left -> JoinOp.LEFT
            is JoinType.Right -> JoinOp.RIGHT
            is JoinType.Full -> JoinOp.OUTER
        }

    private fun Let.toLetSource(): LetSource {
        return LetSource(
            this.letBindings.map {
                LetBinding(
                    it.expr.toExprNode(),
                    it.name.toSymbolicName()
                )
            }
        )
    }

    private fun SymbolPrimitive.toSymbolicName() = SymbolicName(this.text, this.metas.toPartiQlMetaContainer())

    private fun GroupBy.toGroupBy(): org.partiql.lang.ast.GroupBy =
        GroupBy(
            grouping = strategy.toGroupingStrategy(),
            groupByItems = keyList.keys.map {
                GroupByItem(
                    it.expr.toExprNode(),
                    it.asAlias?.toSymbolicName())
            },
            groupName = groupAsAlias?.toSymbolicName())

    private fun GroupingStrategy.toGroupingStrategy(): org.partiql.lang.ast.GroupingStrategy =
        when(this) {
            is GroupingStrategy.GroupFull-> org.partiql.lang.ast.GroupingStrategy.FULL
            is GroupingStrategy.GroupPartial -> org.partiql.lang.ast.GroupingStrategy.PARTIAL
        }

    private fun Type.toExprNodeType(): DataType {
        val metas = this.metas.toPartiQlMetaContainer()

        return when (this) {
            is Type.NullType -> DataType(SqlDataType.NULL, listOf(), metas)
            is Type.MissingType -> DataType(SqlDataType.MISSING, listOf(), metas)
            is Type.BooleanType -> DataType(SqlDataType.BOOLEAN, listOf(), metas)
            is Type.IntegerType -> DataType(SqlDataType.INTEGER, listOf(), metas)
            is Type.SmallintType -> DataType(SqlDataType.SMALLINT, listOf(), metas)
            is Type.FloatType -> DataType(SqlDataType.FLOAT, listOfNotNull(precision?.value), metas)
            is Type.RealType -> DataType(SqlDataType.REAL, listOf(), metas)
            is Type.DoublePrecisionType -> DataType(SqlDataType.DOUBLE_PRECISION, listOf(), metas)
            is Type.DecimalType -> DataType(SqlDataType.DECIMAL, listOfNotNull(precision?.value, scale?.value), metas)
            is Type.NumericType -> DataType(SqlDataType.NUMERIC, listOfNotNull(precision?.value, scale?.value), metas)
            is Type.TimestampType -> DataType(SqlDataType.TIMESTAMP, listOf(), metas)
            is Type.CharacterType -> DataType(SqlDataType.CHARACTER, listOfNotNull(length?.value), metas)
            is Type.CharacterVaryingType -> DataType(SqlDataType.CHARACTER_VARYING, listOfNotNull(length?.value), metas)
            is Type.StringType -> DataType(SqlDataType.STRING, listOf(), metas)
            is Type.SymbolType -> DataType(SqlDataType.SYMBOL, listOf(), metas)
            is Type.BlobType -> DataType(SqlDataType.BLOB, listOf(), metas)
            is Type.ClobType -> DataType(SqlDataType.CLOB, listOf(), metas)
            is Type.StructType -> DataType(SqlDataType.STRUCT, listOf(), metas)
            is Type.TupleType -> DataType(SqlDataType.TUPLE, listOf(), metas)
            is Type.ListType -> DataType(SqlDataType.LIST, listOf(), metas)
            is Type.SexpType -> DataType(SqlDataType.SEXP, listOf(), metas)
            is Type.BagType -> DataType(SqlDataType.BAG, listOf(), metas)
        }
    }

    private fun SetQuantifier.toSetQuantifier(): org.partiql.lang.ast.SetQuantifier =
        when (this) {
            is SetQuantifier.All -> org.partiql.lang.ast.SetQuantifier.ALL
            is SetQuantifier.Distinct -> org.partiql.lang.ast.SetQuantifier.DISTINCT
        }

    private fun ScopeQualifier.toScopeQualifier(): org.partiql.lang.ast.ScopeQualifier =
        when (this) {
            is ScopeQualifier.Unqualified -> org.partiql.lang.ast.ScopeQualifier.UNQUALIFIED
            is ScopeQualifier.LocalsFirst -> org.partiql.lang.ast.ScopeQualifier.LEXICAL
        }

    private fun CaseSensitivity.toCaseSensitivity(): org.partiql.lang.ast.CaseSensitivity =
        when (this) {
            is CaseSensitivity.CaseSensitive -> org.partiql.lang.ast.CaseSensitivity.SENSITIVE
            is CaseSensitivity.CaseInsensitive -> org.partiql.lang.ast.CaseSensitivity.INSENSITIVE
        }

    private fun Statement.Dml.toExprNode(): ExprNode {
        val fromSource = this.from?.toFromSource()
        val where = this.where?.toExprNode()
        val op = this.operation
        val dmlOp = when (op) {
            is DmlOp.Insert -> InsertOp(op.target.toExprNode(), op.values.toExprNode())
            is DmlOp.InsertValue -> InsertValueOp(op.target.toExprNode(), op.value.toExprNode(), op.index?.toExprNode())
            is DmlOp.Set -> AssignmentOp(op.assignments.map { Assignment(it.target.toExprNode(), it.value.toExprNode()) })
            is DmlOp.Remove -> RemoveOp(op.target.toExprNode())
            is DmlOp.Delete -> DeleteOp()
        }

        return DataManipulation(dmlOp, fromSource, where, this.metas.toPartiQlMetaContainer())
    }

    private fun Statement.Ddl.toExprNode(): ExprNode {
        val op = this.op
        val metas = this.metas.toPartiQlMetaContainer()
        return when(op) {
            is DdlOp.CreateTable -> CreateTable(op.tableName.text, metas)
            is DdlOp.DropTable -> DropTable(op.tableName.name.text, metas)
            is DdlOp.CreateIndex -> CreateIndex(op.indexName.name.text, op.fields.map { it.toExprNode() }, metas)
            is DdlOp.DropIndex ->
                DropIndex(
                    tableName = op.table.name.text,
                    identifier = VariableReference(
                        id = op.keys.name.text,
                        case = op.keys.case.toCaseSensitivity(),
                        scopeQualifier = org.partiql.lang.ast.ScopeQualifier.UNQUALIFIED,
                        metas = emptyMetaContainer
                    ),
                    metas = metas)
        }
    }
}
