@file:Suppress("UnusedImport")

package org.partiql.lang.ast

import com.amazon.ion.IonSystem
import com.amazon.ionelement.api.toIonValue
import org.partiql.lang.domains.PartiqlAst
import org.partiql.lang.types.StaticType
import org.partiql.lang.util.checkThreadInterrupted
import org.partiql.lang.util.toIntExact
import org.partiql.pig.runtime.SymbolPrimitive

internal typealias PartiQlMetaContainer = org.partiql.lang.ast.MetaContainer
internal typealias IonElementMetaContainer = com.amazon.ionelement.api.MetaContainer

/** Converts a [partiql_ast.statement] to an [ExprNode], preserving all metas where possible. */
fun PartiqlAst.Statement.toExprNode(ion: IonSystem): ExprNode =
    StatementTransformer(ion).transform(this)

internal fun PartiqlAst.Expr.toExprNode(ion: IonSystem): ExprNode {
    return StatementTransformer(ion).transform(this)
}

internal fun PartiqlAst.SetQuantifier.toExprNodeSetQuantifier(): SetQuantifier  =
    when (this) {
        is PartiqlAst.SetQuantifier.All -> SetQuantifier.ALL
        is PartiqlAst.SetQuantifier.Distinct -> SetQuantifier.DISTINCT
    }

internal fun com.amazon.ionelement.api.MetaContainer.toPartiQlMetaContainer(): PartiQlMetaContainer {
    val nonLocationMetas: List<Meta> = this.values.map {
        // We may need to account for this in the future, but for now we require that all metas placed
        // on any `partiql_ast` instances to implement Meta.  It's not clear how to deal with that now
        // so we should wait until it's needed.
        val partiQlMeta = it as? Meta ?: error("The meta was not an instance of Meta.")
        partiQlMeta
    }

    return metaContainerOf(nonLocationMetas)
}

private class StatementTransformer(val ion: IonSystem) {
    fun transform(stmt: PartiqlAst.Statement): ExprNode =
        when (stmt) {
            is PartiqlAst.Statement.Query -> stmt.toExprNode()
            is PartiqlAst.Statement.Dml -> stmt.toExprNode()
            is PartiqlAst.Statement.Ddl -> stmt.toExprNode()
            is PartiqlAst.Statement.Exec -> stmt.toExprNode()
        }

    fun transform(stmt: PartiqlAst.Expr): ExprNode =
        stmt.toExprNode()


    private fun PartiqlAst.Statement.Query.toExprNode(): ExprNode {
        return this.expr.toExprNode()
    }

    private fun List<PartiqlAst.Expr>.toExprNodeList(): List<ExprNode> =
        this.map { it.toExprNode() }

    private fun PartiqlAst.Expr.toExprNode(): ExprNode {
        checkThreadInterrupted()
        val metas = this.metas.toPartiQlMetaContainer()
        return when (this) {
            is PartiqlAst.Expr.Missing -> LiteralMissing(metas)
            // https://github.com/amzn/ion-element-kotlin/issues/35, .asAnyElement() is unfortunately needed for now
            is PartiqlAst.Expr.Lit -> Literal(value.asAnyElement().toIonValue(ion), metas)
            is PartiqlAst.Expr.Id -> VariableReference(name.text, case.toCaseSensitivity(), qualifier.toScopeQualifier(), metas)
            is PartiqlAst.Expr.Parameter -> Parameter(index.value.toInt(), metas)
            is PartiqlAst.Expr.Not -> NAry(NAryOp.NOT, listOf(expr.toExprNode()), metas)
            is PartiqlAst.Expr.Pos -> expr.toExprNode()
            is PartiqlAst.Expr.Neg -> NAry(NAryOp.SUB, listOf(expr.toExprNode()), metas)
            is PartiqlAst.Expr.Plus -> NAry(NAryOp.ADD, operands.toExprNodeList(), metas)
            is PartiqlAst.Expr.Minus -> NAry(NAryOp.SUB, operands.toExprNodeList(), metas)
            is PartiqlAst.Expr.Times -> NAry(NAryOp.MUL, operands.toExprNodeList(), metas)
            is PartiqlAst.Expr.Divide -> NAry(NAryOp.DIV, operands.toExprNodeList(), metas)
            is PartiqlAst.Expr.Modulo -> NAry(NAryOp.MOD, operands.toExprNodeList(), metas)
            is PartiqlAst.Expr.Concat -> NAry(NAryOp.STRING_CONCAT, operands.toExprNodeList(), metas)
            is PartiqlAst.Expr.And -> NAry(NAryOp.AND, operands.toExprNodeList(), metas)
            is PartiqlAst.Expr.Or -> NAry(NAryOp.OR, operands.toExprNodeList(), metas)
            is PartiqlAst.Expr.Eq -> NAry(NAryOp.EQ, operands.toExprNodeList(), metas)
            is PartiqlAst.Expr.Ne -> NAry(NAryOp.NE, operands.toExprNodeList(), metas)
            is PartiqlAst.Expr.Gt -> NAry(NAryOp.GT, operands.toExprNodeList(), metas)
            is PartiqlAst.Expr.Gte -> NAry(NAryOp.GTE, operands.toExprNodeList(), metas)
            is PartiqlAst.Expr.Lt -> NAry(NAryOp.LT, operands.toExprNodeList(), metas)
            is PartiqlAst.Expr.Lte -> NAry(NAryOp.LTE, operands.toExprNodeList(), metas)

            is PartiqlAst.Expr.Union ->
                NAry(
                    when (setq) {
                        is PartiqlAst.SetQuantifier.Distinct -> NAryOp.UNION
                        is PartiqlAst.SetQuantifier.All -> NAryOp.UNION_ALL
                    },
                    operands.toExprNodeList(),
                    metas)
            is PartiqlAst.Expr.Intersect ->
                NAry(
                    when (setq) {
                        is PartiqlAst.SetQuantifier.Distinct -> NAryOp.INTERSECT
                        is PartiqlAst.SetQuantifier.All -> NAryOp.INTERSECT_ALL
                    },
                    operands.toExprNodeList(),
                    metas)
            is PartiqlAst.Expr.Except ->
                NAry(
                    when (setq) {
                        is PartiqlAst.SetQuantifier.Distinct -> NAryOp.EXCEPT
                        is PartiqlAst.SetQuantifier.All -> NAryOp.EXCEPT_ALL
                    },
                    operands.toExprNodeList(),
                    metas)
            is PartiqlAst.Expr.Like -> NAry(NAryOp.LIKE, listOfNotNull(value.toExprNode(), pattern.toExprNode(), escape?.toExprNode()), metas)
            is PartiqlAst.Expr.Between -> NAry(NAryOp.BETWEEN, listOf(value.toExprNode(), from.toExprNode(), to.toExprNode()), metas)
            is PartiqlAst.Expr.InCollection -> NAry(NAryOp.IN, operands.toExprNodeList(), metas)
            is PartiqlAst.Expr.IsType -> Typed(TypedOp.IS, value.toExprNode(), type.toExprNodeType(), metas)
            is PartiqlAst.Expr.Cast -> Typed(TypedOp.CAST, value.toExprNode(), asType.toExprNodeType(), metas)
            is PartiqlAst.Expr.CanCast -> Typed(TypedOp.CAN_CAST, value.toExprNode(), asType.toExprNodeType(), metas)
            is PartiqlAst.Expr.CanLosslessCast -> Typed(TypedOp.CAN_LOSSLESS_CAST, value.toExprNode(), asType.toExprNodeType(), metas)

            is PartiqlAst.Expr.SimpleCase ->
                SimpleCase(
                    expr.toExprNode(),
                    cases.pairs.map { SimpleCaseWhen(it.first.toExprNode(), it.second.toExprNode()) },
                    default?.toExprNode(),
                    metas)
            is PartiqlAst.Expr.SearchedCase ->
                SearchedCase(
                    cases.pairs.map { SearchedCaseWhen(it.first.toExprNode(), it.second.toExprNode()) },
                    this.default?.toExprNode(),
                    metas)
            is PartiqlAst.Expr.Struct -> Struct(this.fields.map { StructField(it.first.toExprNode(), it.second.toExprNode()) }, metas)
            is PartiqlAst.Expr.Bag -> Seq(SeqType.BAG, values.toExprNodeList(), metas)
            is PartiqlAst.Expr.List -> Seq(SeqType.LIST, values.toExprNodeList(), metas)
            is PartiqlAst.Expr.Sexp -> Seq(SeqType.SEXP, values.toExprNodeList(), metas)
            is PartiqlAst.Expr.Path ->
                Path(
                    root.toExprNode(),
                    steps.map {
                        val componentMetas = it.metas.toPartiQlMetaContainer()
                        when (it) {
                            is PartiqlAst.PathStep.PathExpr ->
                                PathComponentExpr(
                                    it.index.toExprNode(),
                                    it.case.toCaseSensitivity(),
                                    componentMetas)
                            is PartiqlAst.PathStep.PathUnpivot -> PathComponentUnpivot(componentMetas)
                            is PartiqlAst.PathStep.PathWildcard -> PathComponentWildcard(componentMetas)
                        }
                    },
                    metas)
            is PartiqlAst.Expr.Call ->
                NAry(
                    NAryOp.CALL,
                    listOf(
                        VariableReference(
                            funcName.text,
                            CaseSensitivity.INSENSITIVE,
                            ScopeQualifier.UNQUALIFIED,
                            emptyMetaContainer)
                    ) + args.map { it.toExprNode() },
                    metas)
            is PartiqlAst.Expr.CallAgg ->
                CallAgg(
                    VariableReference(
                        funcName.text,
                        CaseSensitivity.INSENSITIVE,
                        ScopeQualifier.UNQUALIFIED,
                        funcName.metas.toPartiQlMetaContainer()),
                    setq.toSetQuantifier(),
                    arg.toExprNode(),
                    metas)
            is PartiqlAst.Expr.Select ->
                Select(
                    setQuantifier = setq?.toSetQuantifier() ?: SetQuantifier.ALL,
                    projection = project.toSelectProjection(),
                    from = from.toFromSource(),
                    fromLet = fromLet?.toLetSource(),
                    where = where?.toExprNode(),
                    groupBy = group?.toGroupBy(),
                    having = having?.toExprNode(),
                    orderBy = order?.toOrderBy(),
                    limit = limit?.toExprNode(),
                    offset = offset?.toExprNode(),
                    metas = metas
            )
            is PartiqlAst.Expr.Date -> DateLiteral(year.value.toInt(), month.value.toInt(), day.value.toInt(), metas)
            is PartiqlAst.Expr.LitTime ->
                TimeLiteral(
                    value.hour.value.toInt(),
                    value.minute.value.toInt(),
                    value.second.value.toInt(),
                    value.nano.value.toInt(),
                    value.precision.value.toInt(),
                    value.withTimeZone.value,
                    value.tzMinutes?.value?.toInt(),
                    metas
                )
            is PartiqlAst.Expr.NullIf -> NullIf(expr1.toExprNode(), expr2.toExprNode(), metas)
            is PartiqlAst.Expr.Coalesce -> Coalesce(args.map { it.toExprNode() }, metas)
        }
    }

    private fun PartiqlAst.Projection.toSelectProjection(): SelectProjection {
        val metas = this.metas.toPartiQlMetaContainer()
        return when (this) {
            is PartiqlAst.Projection.ProjectStar -> SelectProjectionList(listOf(SelectListItemStar(metas)), metas)
            is PartiqlAst.Projection.ProjectValue -> SelectProjectionValue(this.value.toExprNode(), metas)
            is PartiqlAst.Projection.ProjectPivot -> SelectProjectionPivot(this.value.toExprNode(), this.key.toExprNode(), metas)
            is PartiqlAst.Projection.ProjectList ->
                SelectProjectionList(
                    this.projectItems.map {
                        when (it) {
                            is PartiqlAst.ProjectItem.ProjectAll -> SelectListItemProjectAll(it.expr.toExprNode())
                            is PartiqlAst.ProjectItem.ProjectExpr ->
                                SelectListItemExpr(
                                    it.expr.toExprNode(),
                                    it.asAlias?.toSymbolicName())
                        }
                    }, metas)
        }
    }

    private fun PartiqlAst.FromSource.toFromSource(): FromSource {
        val metas = this.metas.toPartiQlMetaContainer()
        return when (this) {
            is PartiqlAst.FromSource.Scan ->
                FromSourceExpr(
                    expr = expr.toExprNode(),
                    variables = LetVariables(
                        asName = asAlias?.toSymbolicName(),
                        atName = atAlias?.toSymbolicName(),
                        byName = byAlias?.toSymbolicName()))
            is PartiqlAst.FromSource.Unpivot ->
                FromSourceUnpivot(
                    expr = expr.toExprNode(),
                    variables = LetVariables(
                        asName = asAlias?.toSymbolicName(),
                        atName = atAlias?.toSymbolicName(),
                        byName = byAlias?.toSymbolicName()),
                    metas = metas)
            is PartiqlAst.FromSource.Join ->
                FromSourceJoin(
                    joinOp = type.toJoinOp(),
                    leftRef = left.toFromSource(),
                    rightRef = right.toFromSource(),
                    // Consider adding StaticTypeMeta here only when static type inference occurs.
                    // See https://github.com/partiql/partiql-lang-kotlin/issues/511
                    condition = predicate?.toExprNode() ?: Literal(ion.newBool(true), metaContainerOf(StaticTypeMeta(StaticType.BOOL))),
                    metas = metas)
        }
    }

    private fun PartiqlAst.JoinType.toJoinOp(): JoinOp =
        when (this) {
            is PartiqlAst.JoinType.Inner -> JoinOp.INNER
            is PartiqlAst.JoinType.Left -> JoinOp.LEFT
            is PartiqlAst.JoinType.Right -> JoinOp.RIGHT
            is PartiqlAst.JoinType.Full -> JoinOp.OUTER
        }

    private fun PartiqlAst.Let.toLetSource(): LetSource {
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

    private fun PartiqlAst.OrderBy.toOrderBy(): OrderBy =
        OrderBy(
            sortSpecItems = this.sortSpecs.map {
                SortSpec(
                    it.expr.toExprNode(),
                    it.orderingSpec.toOrderSpec())})

    private fun PartiqlAst.OrderingSpec?.toOrderSpec(): OrderingSpec =
        when(this) {
            is PartiqlAst.OrderingSpec.Desc -> OrderingSpec.DESC
            else -> OrderingSpec.ASC
        }

    private fun PartiqlAst.GroupBy.toGroupBy(): GroupBy =
        GroupBy(
            grouping = strategy.toGroupingStrategy(),
            groupByItems = keyList.keys.map {
                GroupByItem(
                    it.expr.toExprNode(),
                    it.asAlias?.toSymbolicName())
            },
            groupName = groupAsAlias?.toSymbolicName())

    private fun PartiqlAst.GroupingStrategy.toGroupingStrategy(): GroupingStrategy =
        when(this) {
            is PartiqlAst.GroupingStrategy.GroupFull-> GroupingStrategy.FULL
            is PartiqlAst.GroupingStrategy.GroupPartial -> GroupingStrategy.PARTIAL
        }

    private fun PartiqlAst.Type.toExprNodeType(): DataType {
        val metas = this.metas.toPartiQlMetaContainer()

        return when (this) {
            is PartiqlAst.Type.NullType -> DataType(SqlDataType.NULL, listOf(), metas)
            is PartiqlAst.Type.MissingType -> DataType(SqlDataType.MISSING, listOf(), metas)
            is PartiqlAst.Type.BooleanType -> DataType(SqlDataType.BOOLEAN, listOf(), metas)
            is PartiqlAst.Type.IntegerType -> DataType(SqlDataType.INTEGER, listOf(), metas)
            is PartiqlAst.Type.SmallintType -> DataType(SqlDataType.SMALLINT, listOf(), metas)
            is PartiqlAst.Type.Integer4Type -> DataType(SqlDataType.INTEGER4, listOf(), metas)
            is PartiqlAst.Type.Integer8Type -> DataType(SqlDataType.INTEGER8, listOf(), metas)
            is PartiqlAst.Type.FloatType -> DataType(SqlDataType.FLOAT, listOfNotNull(precision?.value?.toIntExact()), metas)
            is PartiqlAst.Type.RealType -> DataType(SqlDataType.REAL, listOf(), metas)
            is PartiqlAst.Type.DoublePrecisionType -> DataType(SqlDataType.DOUBLE_PRECISION, listOf(), metas)
            is PartiqlAst.Type.DecimalType -> DataType(SqlDataType.DECIMAL, listOfNotNull(precision?.value?.toIntExact(), scale?.value?.toIntExact()), metas)
            is PartiqlAst.Type.NumericType -> DataType(SqlDataType.NUMERIC, listOfNotNull(precision?.value?.toIntExact(), scale?.value?.toIntExact()), metas)
            is PartiqlAst.Type.TimestampType -> DataType(SqlDataType.TIMESTAMP, listOf(), metas)
            is PartiqlAst.Type.CharacterType -> DataType(SqlDataType.CHARACTER, listOfNotNull(length?.value?.toIntExact()), metas)
            is PartiqlAst.Type.CharacterVaryingType -> DataType(SqlDataType.CHARACTER_VARYING, listOfNotNull(length?.value?.toIntExact()), metas)
            is PartiqlAst.Type.StringType -> DataType(SqlDataType.STRING, listOf(), metas)
            is PartiqlAst.Type.SymbolType -> DataType(SqlDataType.SYMBOL, listOf(), metas)
            is PartiqlAst.Type.BlobType -> DataType(SqlDataType.BLOB, listOf(), metas)
            is PartiqlAst.Type.ClobType -> DataType(SqlDataType.CLOB, listOf(), metas)
            is PartiqlAst.Type.StructType -> DataType(SqlDataType.STRUCT, listOf(), metas)
            is PartiqlAst.Type.TupleType -> DataType(SqlDataType.TUPLE, listOf(), metas)
            is PartiqlAst.Type.ListType -> DataType(SqlDataType.LIST, listOf(), metas)
            is PartiqlAst.Type.SexpType -> DataType(SqlDataType.SEXP, listOf(), metas)
            is PartiqlAst.Type.BagType -> DataType(SqlDataType.BAG, listOf(), metas)
            is PartiqlAst.Type.AnyType -> DataType(SqlDataType.ANY, listOf(), metas)
            is PartiqlAst.Type.CustomType -> DataType(
                SqlDataType.CustomDataType(this.name.text),
                args.map { it.value.toInt() },
                metas
            )
            is PartiqlAst.Type.DateType -> DataType(SqlDataType.DATE, listOf(), metas)
            is PartiqlAst.Type.TimeType -> DataType(SqlDataType.TIME, listOfNotNull(precision?.value?.toIntExact()), metas)
            is PartiqlAst.Type.TimeWithTimeZoneType -> DataType(SqlDataType.TIME_WITH_TIME_ZONE, listOfNotNull(precision?.value?.toIntExact()), metas)
            // TODO: Remove these hardcoded nodes from the PIG domain once [https://github.com/partiql/partiql-lang-kotlin/issues/510] is resolved.
            is PartiqlAst.Type.EsBoolean,
            is PartiqlAst.Type.EsInteger,
            is PartiqlAst.Type.EsText,
            is PartiqlAst.Type.EsAny,
            is PartiqlAst.Type.EsFloat,
            is PartiqlAst.Type.RsBigint,
            is PartiqlAst.Type.RsBoolean,
            is PartiqlAst.Type.RsDoublePrecision,
            is PartiqlAst.Type.RsInteger,
            is PartiqlAst.Type.RsReal,
            is PartiqlAst.Type.RsVarcharMax,
            is PartiqlAst.Type.SparkBoolean,
            is PartiqlAst.Type.SparkDouble,
            is PartiqlAst.Type.SparkFloat,
            is PartiqlAst.Type.SparkInteger,
            is PartiqlAst.Type.SparkLong,
            is PartiqlAst.Type.SparkShort -> error("$this node should not be present in PartiQLAST. Consider transforming the AST using CustomTypeVisitorTransform.")
            is PartiqlAst.Type.NullType -> DataType(SqlDataType.NULL, listOf(), metas)
            is PartiqlAst.Type.MissingType -> DataType(SqlDataType.MISSING, listOf(), metas)
            is PartiqlAst.Type.BooleanType -> DataType(SqlDataType.BOOLEAN, listOf(), metas)
            is PartiqlAst.Type.IntegerType -> DataType(SqlDataType.INTEGER, listOf(), metas)
            is PartiqlAst.Type.SmallintType -> DataType(SqlDataType.SMALLINT, listOf(), metas)
            is PartiqlAst.Type.Integer4Type -> DataType(SqlDataType.INTEGER4, listOf(), metas)
            is PartiqlAst.Type.FloatType -> DataType(SqlDataType.FLOAT, listOfNotNull(precision?.value?.toIntExact()), metas)
            is PartiqlAst.Type.RealType -> DataType(SqlDataType.REAL, listOf(), metas)
            is PartiqlAst.Type.DoublePrecisionType -> DataType(SqlDataType.DOUBLE_PRECISION, listOf(), metas)
            is PartiqlAst.Type.DecimalType -> DataType(SqlDataType.DECIMAL, listOfNotNull(precision?.value?.toIntExact(), scale?.value?.toIntExact()), metas)
            is PartiqlAst.Type.NumericType -> DataType(SqlDataType.NUMERIC, listOfNotNull(precision?.value?.toIntExact(), scale?.value?.toIntExact()), metas)
            is PartiqlAst.Type.TimestampType -> DataType(SqlDataType.TIMESTAMP, listOf(), metas)
            is PartiqlAst.Type.CharacterType -> DataType(SqlDataType.CHARACTER, listOfNotNull(length?.value?.toIntExact()), metas)
            is PartiqlAst.Type.CharacterVaryingType -> DataType(SqlDataType.CHARACTER_VARYING, listOfNotNull(length?.value?.toIntExact()), metas)
            is PartiqlAst.Type.StringType -> DataType(SqlDataType.STRING, listOf(), metas)
            is PartiqlAst.Type.SymbolType -> DataType(SqlDataType.SYMBOL, listOf(), metas)
            is PartiqlAst.Type.BlobType -> DataType(SqlDataType.BLOB, listOf(), metas)
            is PartiqlAst.Type.ClobType -> DataType(SqlDataType.CLOB, listOf(), metas)
            is PartiqlAst.Type.StructType -> DataType(SqlDataType.STRUCT, listOf(), metas)
            is PartiqlAst.Type.TupleType -> DataType(SqlDataType.TUPLE, listOf(), metas)
            is PartiqlAst.Type.ListType -> DataType(SqlDataType.LIST, listOf(), metas)
            is PartiqlAst.Type.SexpType -> DataType(SqlDataType.SEXP, listOf(), metas)
            is PartiqlAst.Type.BagType -> DataType(SqlDataType.BAG, listOf(), metas)
            is PartiqlAst.Type.AnyType -> DataType(SqlDataType.ANY, listOf(), metas)
            is PartiqlAst.Type.CustomType -> DataType(
                SqlDataType.CustomDataType(this.name.text),
                args.map { it.value.toInt() },
                metas
            )
            is PartiqlAst.Type.DateType -> DataType(SqlDataType.DATE, listOf(), metas)
            is PartiqlAst.Type.TimeType -> DataType(SqlDataType.TIME, listOfNotNull(precision?.value?.toIntExact()), metas)
            is PartiqlAst.Type.TimeWithTimeZoneType -> DataType(SqlDataType.TIME_WITH_TIME_ZONE, listOfNotNull(precision?.value?.toIntExact()), metas)
            // TODO: Remove these hardcoded nodes from the PIG domain once [https://issues.amazon.com/HARRY-6393] is resolved.
            is PartiqlAst.Type.EsBoolean,
            is PartiqlAst.Type.EsInteger,
            is PartiqlAst.Type.EsText,
            is PartiqlAst.Type.EsAny,
            is PartiqlAst.Type.EsFloat,
            is PartiqlAst.Type.RsBigint,
            is PartiqlAst.Type.RsBoolean,
            is PartiqlAst.Type.RsDoublePrecision,
            is PartiqlAst.Type.RsInteger,
            is PartiqlAst.Type.RsReal,
            is PartiqlAst.Type.RsVarcharMax,
            is PartiqlAst.Type.SparkBoolean,
            is PartiqlAst.Type.SparkDouble,
            is PartiqlAst.Type.SparkFloat,
            is PartiqlAst.Type.SparkInteger,
            is PartiqlAst.Type.SparkLong,
            is PartiqlAst.Type.SparkShort -> error("$this node should not be present in PartiQLAST. Consider transforming the AST using CustomTypeVisitorTransform.")
        }
    }

    private fun PartiqlAst.SetQuantifier.toSetQuantifier(): SetQuantifier =
        when (this) {
            is PartiqlAst.SetQuantifier.All -> SetQuantifier.ALL
            is PartiqlAst.SetQuantifier.Distinct -> SetQuantifier.DISTINCT
        }

    private fun PartiqlAst.ScopeQualifier.toScopeQualifier(): ScopeQualifier =
        when (this) {
            is PartiqlAst.ScopeQualifier.Unqualified -> ScopeQualifier.UNQUALIFIED
            is PartiqlAst.ScopeQualifier.LocalsFirst -> ScopeQualifier.LEXICAL
        }

    private fun PartiqlAst.CaseSensitivity.toCaseSensitivity(): CaseSensitivity =
        when (this) {
            is PartiqlAst.CaseSensitivity.CaseSensitive -> CaseSensitivity.SENSITIVE
            is PartiqlAst.CaseSensitivity.CaseInsensitive -> CaseSensitivity.INSENSITIVE
        }

    private fun PartiqlAst.OnConflict.toOnConflictNode(): OnConflict {
        return when(this.conflictAction) {
            is PartiqlAst.ConflictAction.DoNothing -> OnConflict(this.expr.toExprNode(), ConflictAction.DO_NOTHING)
        }
    }

    private fun PartiqlAst.Statement.Dml.toExprNode(): ExprNode {
        val fromSource = this.from?.toFromSource()
        val where = this.where?.toExprNode()
        val returningExpr = this.returning?.toReturningExpr()
        val ops = this.operations
        val dmlOps = ops.toDmlOps()

        return DataManipulation(dmlOps, fromSource, where, returningExpr, this.metas.toPartiQlMetaContainer())
    }

    private fun PartiqlAst.DmlOpList.toDmlOps(): DmlOpList =
        DmlOpList(this.ops.map { it.toDmlOp() })

    private fun PartiqlAst.DmlOp.toDmlOp(): DataManipulationOperation =
        when (this) {
            is PartiqlAst.DmlOp.Insert -> InsertOp(target.toExprNode(), values.toExprNode())
            is PartiqlAst.DmlOp.InsertValue ->
                InsertValueOp(
                    lvalue = target.toExprNode(),
                    value = value.toExprNode(),
                    position = this.index?.toExprNode(),
                    onConflict = onConflict?.toOnConflictNode())
            is PartiqlAst.DmlOp.Set ->
                AssignmentOp(
                    assignment = Assignment(
                        lvalue = this.assignment.target.toExprNode(),
                        rvalue = assignment.value.toExprNode()))

            is PartiqlAst.DmlOp.Remove ->
                RemoveOp(target.toExprNode())

            is PartiqlAst.DmlOp.Delete ->
                DeleteOp()
        }

    private fun PartiqlAst.ReturningExpr.toReturningExpr(): ReturningExpr =
        ReturningExpr(
            returningElems = elems.map {
                ReturningElem(
                   it.mapping.toExprNodeReturningMapping(),
                   it.column.toColumnComponent()
                )
            }
        )

    private fun PartiqlAst.ColumnComponent.toColumnComponent(): ColumnComponent {
        val metas = this.metas.toPartiQlMetaContainer()
        return when (this) {
            is PartiqlAst.ColumnComponent.ReturningColumn -> ReturningColumn(this.expr.toExprNode())
            is PartiqlAst.ColumnComponent.ReturningWildcard -> ReturningWildcard(metas)
        }
    }

    private fun PartiqlAst.ReturningMapping.toExprNodeReturningMapping(): ReturningMapping =
            when(this) {
                is PartiqlAst.ReturningMapping.ModifiedOld -> ReturningMapping.MODIFIED_OLD
                is PartiqlAst.ReturningMapping.ModifiedNew -> ReturningMapping.MODIFIED_NEW
                is PartiqlAst.ReturningMapping.AllOld -> ReturningMapping.ALL_OLD
                is PartiqlAst.ReturningMapping.AllNew -> ReturningMapping.ALL_NEW
            }

    private fun PartiqlAst.Statement.Ddl.toExprNode(): ExprNode {
        val op = this.op
        val metas = this.metas.toPartiQlMetaContainer()
        return when(op) {
            is PartiqlAst.DdlOp.CreateTable -> CreateTable(op.tableName.text, metas)
            is PartiqlAst.DdlOp.DropTable ->
                DropTable(
                    tableId = Identifier(
                        id = op.tableName.name.text,
                        case = op.tableName.case.toCaseSensitivity(),
                        metas = emptyMetaContainer
                    ),
                    metas = metas)
            is PartiqlAst.DdlOp.CreateIndex ->
                CreateIndex(
                    tableId = Identifier(
                        id = op.indexName.name.text,
                        case = op.indexName.case.toCaseSensitivity(),
                        metas = emptyMetaContainer
                    ),
                    keys = op.fields.map { it.toExprNode() },
                    metas = metas)
            is PartiqlAst.DdlOp.DropIndex ->
                DropIndex(
                    tableId = Identifier(
                        id = op.table.name.text,
                        case = op.table.case.toCaseSensitivity(),
                        metas = emptyMetaContainer
                    ),
                    indexId = Identifier(
                        id = op.keys.name.text,
                        case = op.keys.case.toCaseSensitivity(),
                        metas = emptyMetaContainer
                    ),
                    metas = metas)
        }
    }

    private fun PartiqlAst.Statement.Exec.toExprNode(): ExprNode {
        return Exec(procedureName.toSymbolicName(), this.args.toExprNodeList(), metas.toPartiQlMetaContainer())
    }
}
