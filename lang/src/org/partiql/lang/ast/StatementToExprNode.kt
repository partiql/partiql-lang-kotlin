package org.partiql.lang.ast

import com.amazon.ionelement.api.toIonValue
import com.amazon.ion.IonSystem
import org.partiql.lang.domains.partiql_ast.case_sensitivity
import org.partiql.lang.domains.partiql_ast.ddl_op
import org.partiql.lang.domains.partiql_ast.dml_op
import org.partiql.lang.domains.partiql_ast.expr
import org.partiql.lang.domains.partiql_ast.from_source
import org.partiql.lang.domains.partiql_ast.group_by
import org.partiql.lang.domains.partiql_ast.grouping_strategy
import org.partiql.lang.domains.partiql_ast.join_type
import org.partiql.lang.domains.partiql_ast.path_step
import org.partiql.lang.domains.partiql_ast.project_item
import org.partiql.lang.domains.partiql_ast.projection
import org.partiql.lang.domains.partiql_ast.scope_qualifier
import org.partiql.lang.domains.partiql_ast.set_quantifier
import org.partiql.lang.domains.partiql_ast.statement
import org.partiql.lang.domains.partiql_ast.type
import org.partiql.pig.runtime.SymbolPrimitive

internal typealias PartiQlMetaContainer = org.partiql.lang.ast.MetaContainer
internal typealias ElectrolyteMetaContainer = com.amazon.ionelement.api.MetaContainer

/** Converts a [partiql_ast.statement] to an [ExprNode], preserving all metas where possible. */
fun statement.toExprNode(ion: IonSystem): ExprNode =
    StatementTransformer(ion).transform(this)

private class StatementTransformer(val ion: IonSystem) {
    fun transform(stmt: statement): ExprNode =
        when (stmt) {
            is statement.query -> stmt.toExprNode()
            is statement.dml -> stmt.toExprNode()
            is statement.ddl -> stmt.toExprNode()
        }

    private fun ElectrolyteMetaContainer.toPartiQlMetaContainer(): PartiQlMetaContainer {
        val nonLocationMetas: List<Meta> = this.values.map {
            // We may need to account for this in the future, but for now we require that all metas placed
            // on any `partiql_ast` instances to implement Meta.  It's not clear how to deal with that now
            // so we should wait until it's needed.
            val partiQlMeta = it as? Meta ?: error("The meta was not an instance of Meta.")
            partiQlMeta
        }

        return org.partiql.lang.ast.metaContainerOf(nonLocationMetas)
    }

    private fun statement.query.toExprNode(): ExprNode {
        return this.expr0.toExprNode()
    }

    private fun List<expr>.toExprNodeList(): List<ExprNode> =
        this.map { it.toExprNode() }

    private fun expr.toExprNode(): ExprNode {
        val metas = this.metas.toPartiQlMetaContainer()
        return when (this) {
            is expr.missing -> LiteralMissing(metas)
            is expr.lit -> Literal(ion0.toIonValue(ion), metas)
            is expr.id -> VariableReference(symbol0.text, case_sensitivity1.toCaseSensitivity(), scope_qualifier2.toScopeQualifier(), metas)
            is expr.parameter -> Parameter(int0.value.toInt(), metas)
            is expr.not -> NAry(NAryOp.NOT, listOf(expr0.toExprNode()), metas)
            is expr.pos -> expr0.toExprNode()
            is expr.neg -> NAry(NAryOp.SUB, listOf(expr0.toExprNode()), metas)
            is expr.plus -> NAry(NAryOp.ADD, expr0.toExprNodeList(), metas)
            is expr.minus -> NAry(NAryOp.SUB, expr0.toExprNodeList(), metas)
            is expr.times -> NAry(NAryOp.MUL, expr0.toExprNodeList(), metas)
            is expr.divide -> NAry(NAryOp.DIV, expr0.toExprNodeList(), metas)
            is expr.modulo -> NAry(NAryOp.MOD, expr0.toExprNodeList(), metas)
            is expr.concat -> NAry(NAryOp.STRING_CONCAT, expr0.toExprNodeList(), metas)
            is expr.and -> NAry(NAryOp.AND, expr0.toExprNodeList(), metas)
            is expr.or -> NAry(NAryOp.OR, expr0.toExprNodeList(), metas)
            is expr.eq -> NAry(NAryOp.EQ, expr0.toExprNodeList(), metas)
            is expr.ne -> NAry(NAryOp.NE, expr0.toExprNodeList(), metas)
            is expr.gt -> NAry(NAryOp.GT, expr0.toExprNodeList(), metas)
            is expr.gte -> NAry(NAryOp.GTE, expr0.toExprNodeList(), metas)
            is expr.lt -> NAry(NAryOp.LT, expr0.toExprNodeList(), metas)
            is expr.lte -> NAry(NAryOp.LTE, expr0.toExprNodeList(), metas)

            is expr.union ->
                NAry(
                    when(set_quantifier0) {
                        is set_quantifier.distinct -> NAryOp.UNION
                        is set_quantifier.all -> NAryOp.UNION_ALL
                    },
                    expr1.toExprNodeList(),
                    metas)
            is expr.intersect ->
                NAry(
                    when(set_quantifier0) {
                        is set_quantifier.distinct -> NAryOp.INTERSECT
                        is set_quantifier.all -> NAryOp.INTERSECT_ALL
                    },
                    expr1.toExprNodeList(),
                    metas)
            is expr.except  ->
                NAry(
                    when(set_quantifier0) {
                        is set_quantifier.distinct -> NAryOp.EXCEPT
                        is set_quantifier.all -> NAryOp.EXCEPT_ALL
                    },
                    expr1.toExprNodeList(),
                    metas)


            is expr.like -> NAry(NAryOp.LIKE, listOfNotNull(expr0.toExprNode(), expr1.toExprNode(), expr2?.toExprNode()), metas)
            is expr.between -> NAry(NAryOp.BETWEEN, listOf(expr0.toExprNode(), expr1.toExprNode(), expr2.toExprNode()), metas)
            is expr.in_collection -> NAry(NAryOp.IN, expr0.toExprNodeList(), metas)
            is expr.is_type -> Typed(TypedOp.IS, expr0.toExprNode(), type1.toExprNodeType(), metas)
            is expr.cast -> Typed(TypedOp.CAST, expr0.toExprNode(), type1.toExprNodeType(), metas)

            is expr.simple_case ->
                SimpleCase(
                    expr0.toExprNode(),
                    expr_pair_list1.expr_pair0.map { SimpleCaseWhen(it.expr0.toExprNode(), it.expr1.toExprNode()) },
                    this.expr2?.toExprNode(),
                    metas)
            is expr.searched_case ->
                SearchedCase(
                    expr_pair_list0.expr_pair0.map { SearchedCaseWhen(it.expr0.toExprNode(), it.expr1.toExprNode()) },
                    this.expr1?.toExprNode(),
                    metas)
            is expr.struct -> Struct(expr_pair0.map { StructField(it.expr0.toExprNode(), it.expr1.toExprNode()) }, metas)
            is expr.bag -> Seq(SeqType.BAG, expr0.toExprNodeList(), metas)
            is expr.list -> Seq(SeqType.LIST, expr0.toExprNodeList(), metas)
            is expr.sexp -> Seq(SeqType.SEXP, expr0.toExprNodeList(), metas)
            is expr.path ->
                Path(
                    expr0.toExprNode(),
                    path_step1.map {
                        val componentMetas = it.metas.toPartiQlMetaContainer()
                        when (it) {
                            is path_step.path_expr ->
                                PathComponentExpr(
                                    it.expr0.toExprNode(),
                                    it.case_sensitivity1.toCaseSensitivity())
                            is path_step.path_unpivot -> PathComponentUnpivot(componentMetas)
                            is path_step.path_wildcard -> PathComponentWildcard(componentMetas)
                        }
                    },
                    metas)
            is expr.call ->
                NAry(
                    NAryOp.CALL,
                    listOf(
                        VariableReference(
                            symbol0.text,
                            CaseSensitivity.INSENSITIVE,
                            ScopeQualifier.UNQUALIFIED,
                            emptyMetaContainer)
                    ) + expr1.map { it.toExprNode() },
                    metas)
            is expr.call_agg ->
                CallAgg(
                    VariableReference(
                        symbol1.text,
                        CaseSensitivity.INSENSITIVE,
                        ScopeQualifier.UNQUALIFIED,
                        emptyMetaContainer),
                    set_quantifier0.toSetQuantifier(),
                    expr2.toExprNode(),
                    metas)
            is expr.select ->
                Select(
                    setQuantifier = setq?.toSetQuantifier() ?: SetQuantifier.ALL,
                    projection = project.toSelectProjection(),
                    from = from.toFromSource(),
                    where = where?.toExprNode(),
                    groupBy = group?.toGroupBy(),
                    having = having?.toExprNode(),
                    limit = limit?.toExprNode(),
                    metas = metas
            )
        }
    }

    private fun projection.toSelectProjection(): SelectProjection {
        val metas = this.metas.toPartiQlMetaContainer()
        return when (this) {
            is projection.project_star -> SelectProjectionList(listOf(SelectListItemStar(metas)))
            is projection.project_value -> SelectProjectionValue(this.expr0.toExprNode())
            is projection.project_pivot -> SelectProjectionPivot(this.expr0.toExprNode(), this.expr1.toExprNode())
            is projection.project_list ->
                SelectProjectionList(
                    this.project_item0.map {
                        when (it) {
                            is project_item.project_all -> SelectListItemProjectAll(it.expr0.toExprNode())
                            is project_item.project_expr ->
                                SelectListItemExpr(
                                    it.expr0.toExprNode(),
                                    it.symbol1?.toSymbolicName())
                        }
                    })
        }
    }

    private fun from_source.toFromSource(): FromSource {
        val metas = this.metas.toPartiQlMetaContainer()
        return when (this) {
            is from_source.scan ->
                FromSourceExpr(
                    expr = expr0.toExprNode(),
                    variables = LetVariables(
                        asName = symbol1?.toSymbolicName(),
                        atName = symbol2?.toSymbolicName(),
                        byName = symbol3?.toSymbolicName()))
            is from_source.unpivot ->
                FromSourceUnpivot(
                    expr = expr0.toExprNode(),
                    variables = LetVariables(
                        asName = symbol1?.toSymbolicName(),
                        atName = symbol2?.toSymbolicName(),
                        byName = symbol3?.toSymbolicName()),
                    metas = metas)
            is from_source.join ->
                FromSourceJoin(
                    joinOp = join_type0.toJoinOp(),
                    leftRef = from_source1.toFromSource(),
                    rightRef = from_source2.toFromSource(),
                    condition = expr3?.toExprNode() ?: Literal(ion.newBool(true), emptyMetaContainer),
                    metas = metas)
        }
    }

    private fun join_type.toJoinOp(): JoinOp =
        when (this) {
            is join_type.inner -> JoinOp.INNER
            is join_type.left -> JoinOp.LEFT
            is join_type.right -> JoinOp.RIGHT
            is join_type.full -> JoinOp.OUTER
        }

    private fun SymbolPrimitive?.toSymbolicName() = this?.let { SymbolicName(it.text, it.metas.toPartiQlMetaContainer()) }

    private fun group_by.toGroupBy(): GroupBy =
        GroupBy(
            grouping = grouping_strategy0.toGroupingStrategy(),
            groupByItems = group_key_list1.group_key0.map {
                GroupByItem(
                    it.expr0.toExprNode(),
                    it.symbol1?.toSymbolicName())
            },
            groupName = symbol2?.toSymbolicName())

    private fun grouping_strategy.toGroupingStrategy(): GroupingStrategy =
        when(this) {
            is grouping_strategy.group_full-> GroupingStrategy.FULL
            is grouping_strategy.group_partial -> GroupingStrategy.PARTIAL
        }

    private fun type.toExprNodeType(): DataType {
        val metas = this.metas.toPartiQlMetaContainer()

        return when (this) {
            is type.null_type -> DataType(SqlDataType.NULL, listOf(), metas)
            is type.missing_type -> DataType(SqlDataType.MISSING, listOf(), metas)
            is type.boolean_type -> DataType(SqlDataType.BOOLEAN, listOf(), metas)
            is type.integer_type -> DataType(SqlDataType.INTEGER, listOf(), metas)
            is type.smallint_type -> DataType(SqlDataType.SMALLINT, listOf(), metas)
            is type.float_type -> DataType(SqlDataType.FLOAT, listOfNotNull(int0?.value), metas)
            is type.real_type -> DataType(SqlDataType.REAL, listOf(), metas)
            is type.double_precision_type -> DataType(SqlDataType.DOUBLE_PRECISION, listOf(), metas)
            is type.decimal_type -> DataType(SqlDataType.DECIMAL, listOfNotNull(int0?.value, int1?.value), metas)
            is type.numeric_type -> DataType(SqlDataType.NUMERIC, listOfNotNull(int0?.value, int1?.value), metas)
            is type.timestamp_type -> DataType(SqlDataType.TIMESTAMP, listOf(), metas)
            is type.character_type -> DataType(SqlDataType.CHARACTER, listOfNotNull(int0?.value), metas)
            is type.character_varying_type -> DataType(SqlDataType.CHARACTER_VARYING, listOfNotNull(int0?.value), metas)
            is type.string_type -> DataType(SqlDataType.STRING, listOf(), metas)
            is type.symbol_type -> DataType(SqlDataType.SYMBOL, listOf(), metas)
            is type.blob_type -> DataType(SqlDataType.BLOB, listOf(), metas)
            is type.clob_type -> DataType(SqlDataType.CLOB, listOf(), metas)
            is type.struct_type -> DataType(SqlDataType.STRUCT, listOf(), metas)
            is type.tuple_type -> DataType(SqlDataType.TUPLE, listOf(), metas)
            is type.list_type -> DataType(SqlDataType.LIST, listOf(), metas)
            is type.sexp_type -> DataType(SqlDataType.SEXP, listOf(), metas)
            is type.bag_type -> DataType(SqlDataType.BAG, listOf(), metas)
        }
    }

    private fun set_quantifier.toSetQuantifier(): SetQuantifier =
        when (this) {
            is set_quantifier.all -> SetQuantifier.ALL
            is set_quantifier.distinct -> SetQuantifier.DISTINCT
        }

    private fun scope_qualifier.toScopeQualifier(): ScopeQualifier =
        when (this) {
            is scope_qualifier.unqualified -> ScopeQualifier.UNQUALIFIED
            is scope_qualifier.locals_first -> ScopeQualifier.LEXICAL
        }

    private fun case_sensitivity.toCaseSensitivity(): CaseSensitivity =
        when (this) {
            is case_sensitivity.case_sensitive -> CaseSensitivity.SENSITIVE
            is case_sensitivity.case_insensitive -> CaseSensitivity.INSENSITIVE
        }

    private fun statement.dml.toExprNode(): ExprNode {
        val fromSource = this.from?.toFromSource()
        val where = this.where?.toExprNode()
        val op = this.operation
        val dmlOp = when (op) {
            is dml_op.insert -> InsertOp(op.expr0.toExprNode(), op.expr1.toExprNode())
            is dml_op.insert_value -> InsertValueOp(op.expr0.toExprNode(), op.expr1.toExprNode(), op.expr2?.toExprNode())
            is dml_op.set -> AssignmentOp(op.assignment0.map { Assignment(it.expr0.toExprNode(), it.expr1.toExprNode()) })
            is dml_op.remove -> RemoveOp(op.expr0.toExprNode())
            is dml_op.delete -> DeleteOp()
        }

        return DataManipulation(dmlOp, fromSource, where, this.metas.toPartiQlMetaContainer())
    }

    private fun statement.ddl.toExprNode(): ExprNode {
        val op = this.ddl_op0
        val metas = this.metas.toPartiQlMetaContainer()
        return when(op) {
            is ddl_op.create_table -> CreateTable(op.symbol0.text, metas)
            is ddl_op.drop_table -> DropTable(op.identifier0.symbol0.text, metas)
            is ddl_op.create_index -> CreateIndex(op.identifier0.symbol0.text, op.expr1.map { it.toExprNode() }, metas)
            is ddl_op.drop_index ->
                DropIndex(
                    tableName = op.table.symbol0.text,
                    identifier = VariableReference(
                        id = op.keys.symbol0.text,
                        case = op.keys.case_sensitivity1.toCaseSensitivity(),
                        scopeQualifier = ScopeQualifier.UNQUALIFIED,
                        metas = emptyMetaContainer
                    ),
                    metas = metas)
        }
    }
}
