package org.partiql.ast.v1

/**
 * TODO docs, equals, hashcode
 */
public interface AstVisitor<R, C> {
    public fun visit(node: AstNode, ctx: C): R

    public fun visitStatement(node: Statement, ctx: C): R

    public fun visitStatementQuery(node: Statement.Query, ctx: C): R

    public fun visitStatementDDL(node: Statement.DDL, ctx: C): R

    public fun visitStatementDDLCreateTable(node: Statement.DDL.CreateTable, ctx: C): R

    public fun visitStatementDDLCreateIndex(node: Statement.DDL.CreateIndex, ctx: C): R

    public fun visitStatementDDLDropTable(node: Statement.DDL.DropTable, ctx: C): R

    public fun visitStatementDDLDropIndex(node: Statement.DDL.DropIndex, ctx: C): R

    public fun visitStatementExplain(node: Statement.Explain, ctx: C): R

    public fun visitStatementExplainTarget(node: Statement.Explain.Target, ctx: C): R

    public fun visitStatementExplainTargetDomain(node: Statement.Explain.Target.Domain, ctx: C): R

    public fun visitType(node: Type, ctx: C): R

    public fun visitTypeNull(node: Type.Null, ctx: C): R

    public fun visitTypeMissing(node: Type.Missing, ctx: C): R

    public fun visitTypeBool(node: Type.Bool, ctx: C): R

    public fun visitTypeTinyint(node: Type.Tinyint, ctx: C): R

    public fun visitTypeSmallint(node: Type.Smallint, ctx: C): R

    public fun visitTypeInt2(node: Type.Int2, ctx: C): R

    public fun visitTypeInt4(node: Type.Int4, ctx: C): R

    public fun visitTypeBigint(node: Type.Bigint, ctx: C): R

    public fun visitTypeInt8(node: Type.Int8, ctx: C): R

    public fun visitTypeInt(node: Type.Int, ctx: C): R

    public fun visitTypeReal(node: Type.Real, ctx: C): R

    public fun visitTypeFloat32(node: Type.Float32, ctx: C): R

    public fun visitTypeFloat64(node: Type.Float64, ctx: C): R

    public fun visitTypeDecimal(node: Type.Decimal, ctx: C): R

    public fun visitTypeNumeric(node: Type.Numeric, ctx: C): R

    public fun visitTypeChar(node: Type.Char, ctx: C): R

    public fun visitTypeVarchar(node: Type.Varchar, ctx: C): R

    public fun visitTypeString(node: Type.String, ctx: C): R

    public fun visitTypeSymbol(node: Type.Symbol, ctx: C): R

    public fun visitTypeBit(node: Type.Bit, ctx: C): R

    public fun visitTypeBitVarying(node: Type.BitVarying, ctx: C): R

    public fun visitTypeByteString(node: Type.ByteString, ctx: C): R

    public fun visitTypeBlob(node: Type.Blob, ctx: C): R

    public fun visitTypeClob(node: Type.Clob, ctx: C): R

    public fun visitTypeDate(node: Type.Date, ctx: C): R

    public fun visitTypeTime(node: Type.Time, ctx: C): R

    public fun visitTypeTimeWithTz(node: Type.TimeWithTz, ctx: C): R

    public fun visitTypeTimestamp(node: Type.Timestamp, ctx: C): R

    public fun visitTypeTimestampWithTz(node: Type.TimestampWithTz, ctx: C): R

    public fun visitTypeInterval(node: Type.Interval, ctx: C): R

    public fun visitTypeBag(node: Type.Bag, ctx: C): R

    public fun visitTypeList(node: Type.List, ctx: C): R

    public fun visitTypeSexp(node: Type.Sexp, ctx: C): R

    public fun visitTypeTuple(node: Type.Tuple, ctx: C): R

    public fun visitTypeStruct(node: Type.Struct, ctx: C): R

    public fun visitTypeAny(node: Type.Any, ctx: C): R

    public fun visitTypeCustom(node: Type.Custom, ctx: C): R

    public fun visitIdentifier(node: Identifier, ctx: C): R

    public fun visitIdentifierSymbol(node: Identifier.Symbol, ctx: C): R

    public fun visitIdentifierQualified(node: Identifier.Qualified, ctx: C): R

    public fun visitPath(node: PathLit, ctx: C): R

    public fun visitPathStep(node: PathLit.Step, ctx: C): R

    public fun visitPathStepSymbol(node: PathLit.Step.Symbol, ctx: C): R

    public fun visitPathStepIndex(node: PathLit.Step.Index, ctx: C): R

    public fun visitExpr(node: Expr, ctx: C): R

    public fun visitExprLit(node: Expr.Lit, ctx: C): R

    public fun visitExprIon(node: Expr.Ion, ctx: C): R

    public fun visitExprVar(node: Expr.Var, ctx: C): R

    public fun visitExprSessionAttribute(node: Expr.SessionAttribute, ctx: C): R

    public fun visitExprPath(node: Expr.Path, ctx: C): R

    public fun visitExprPathStep(node: Expr.Path.Step, ctx: C): R

    public fun visitExprPathStepSymbol(node: Expr.Path.Step.Symbol, ctx: C): R

    public fun visitExprPathStepIndex(node: Expr.Path.Step.Index, ctx: C): R

    public fun visitExprPathStepWildcard(node: Expr.Path.Step.Wildcard, ctx: C): R

    public fun visitExprPathStepUnpivot(node: Expr.Path.Step.Unpivot, ctx: C): R

    public fun visitExprCall(node: Expr.Call, ctx: C): R

    public fun visitExprParameter(node: Expr.Parameter, ctx: C): R

    public fun visitExprOperator(node: Expr.Operator, ctx: C): R

    public fun visitExprNot(node: Expr.Not, ctx: C): R

    public fun visitExprAnd(node: Expr.And, ctx: C): R

    public fun visitExprOr(node: Expr.Or, ctx: C): R

    public fun visitExprValues(node: Expr.Values, ctx: C): R

    public fun visitExprValuesRow(node: Expr.Values.Row, ctx: C): R

    public fun visitExprCollection(node: Expr.Collection, ctx: C): R

    public fun visitExprStruct(node: Expr.Struct, ctx: C): R

    public fun visitExprStructField(node: Expr.Struct.Field, ctx: C): R

    public fun visitExprLike(node: Expr.Like, ctx: C): R

    public fun visitExprBetween(node: Expr.Between, ctx: C): R

    public fun visitExprInCollection(node: Expr.InCollection, ctx: C): R

    public fun visitExprIsType(node: Expr.IsType, ctx: C): R

    public fun visitExprCase(node: Expr.Case, ctx: C): R

    public fun visitExprCaseBranch(node: Expr.Case.Branch, ctx: C): R

    public fun visitExprCoalesce(node: Expr.Coalesce, ctx: C): R

    public fun visitExprNullIf(node: Expr.NullIf, ctx: C): R

    public fun visitExprSubstring(node: Expr.Substring, ctx: C): R

    public fun visitExprPosition(node: Expr.Position, ctx: C): R

    public fun visitExprTrim(node: Expr.Trim, ctx: C): R

    public fun visitExprOverlay(node: Expr.Overlay, ctx: C): R

    public fun visitExprExtract(node: Expr.Extract, ctx: C): R

    public fun visitExprCast(node: Expr.Cast, ctx: C): R

    public fun visitExprDateAdd(node: Expr.DateAdd, ctx: C): R

    public fun visitExprDateDiff(node: Expr.DateDiff, ctx: C): R

    public fun visitExprQuerySet(node: Expr.QuerySet, ctx: C): R

    public fun visitExprMatch(node: Expr.Match, ctx: C): R

    public fun visitExprWindow(node: Expr.Window, ctx: C): R

    public fun visitExprWindowOver(node: Expr.Window.Over, ctx: C): R

    public fun visitQueryBody(node: QueryBody, ctx: C): R

    public fun visitQueryBodySFW(node: QueryBody.SFW, ctx: C): R

    public fun visitQueryBodySetOp(node: QueryBody.SetOp, ctx: C): R

    public fun visitSelect(node: Select, ctx: C): R

    public fun visitSelectStar(node: Select.Star, ctx: C): R

    public fun visitSelectProject(node: Select.Project, ctx: C): R

    public fun visitSelectProjectItem(node: Select.Project.Item, ctx: C): R

    public fun visitSelectProjectItemAll(node: Select.Project.Item.All, ctx: C): R

    public fun visitSelectProjectItemExpression(node: Select.Project.Item.Expression, ctx: C): R

    public fun visitSelectPivot(node: Select.Pivot, ctx: C): R

    public fun visitSelectValue(node: Select.Value, ctx: C): R

    public fun visitExclude(node: Exclude, ctx: C): R

    public fun visitExcludeItem(node: Exclude.Item, ctx: C): R

    public fun visitExcludeStep(node: Exclude.Step, ctx: C): R

    public fun visitExcludeStepStructField(node: Exclude.Step.StructField, ctx: C): R

    public fun visitExcludeStepCollIndex(node: Exclude.Step.CollIndex, ctx: C): R

    public fun visitExcludeStepStructWildcard(node: Exclude.Step.StructWildcard, ctx: C): R

    public fun visitExcludeStepCollWildcard(node: Exclude.Step.CollWildcard, ctx: C): R

    public fun visitFrom(node: From, ctx: C): R

    public fun visitFromValue(node: From.Value, ctx: C): R

    public fun visitFromJoin(node: From.Join, ctx: C): R

    public fun visitLet(node: Let, ctx: C): R

    public fun visitLetBinding(node: Let.Binding, ctx: C): R

    public fun visitGroupBy(node: GroupBy, ctx: C): R

    public fun visitGroupByKey(node: GroupBy.Key, ctx: C): R

    public fun visitOrderBy(node: OrderBy, ctx: C): R

    public fun visitSort(node: Sort, ctx: C): R

    public fun visitSetOp(node: SetOp, ctx: C): R

    public fun visitGraphMatch(node: GraphMatch, ctx: C): R

    public fun visitGraphMatchPattern(node: GraphMatch.Pattern, ctx: C): R

    public fun visitGraphMatchPatternPart(node: GraphMatch.Pattern.Part, ctx: C): R

    public fun visitGraphMatchPatternPartNode(node: GraphMatch.Pattern.Part.Node, ctx: C): R

    public fun visitGraphMatchPatternPartEdge(node: GraphMatch.Pattern.Part.Edge, ctx: C): R

    public fun visitGraphMatchPatternPartPattern(node: GraphMatch.Pattern.Part.Pattern, ctx: C): R

    public fun visitGraphMatchQuantifier(node: GraphMatch.Quantifier, ctx: C): R

    public fun visitGraphMatchSelector(node: GraphMatch.Selector, ctx: C): R

    public fun visitGraphMatchSelectorAnyShortest(node: GraphMatch.Selector.AnyShortest, ctx: C): R

    public fun visitGraphMatchSelectorAllShortest(node: GraphMatch.Selector.AllShortest, ctx: C): R

    public fun visitGraphMatchSelectorAny(node: GraphMatch.Selector.Any, ctx: C): R

    public fun visitGraphMatchSelectorAnyK(node: GraphMatch.Selector.AnyK, ctx: C): R

    public fun visitGraphMatchSelectorShortestK(node: GraphMatch.Selector.ShortestK, ctx: C): R

    public fun visitGraphMatchSelectorShortestKGroup(
        node: GraphMatch.Selector.ShortestKGroup,
        ctx: C
    ): R

    public fun visitGraphMatchLabel(node: GraphMatch.Label, ctx: C): R

    public fun visitGraphMatchLabelName(node: GraphMatch.Label.Name, ctx: C): R

    public fun visitGraphMatchLabelWildcard(node: GraphMatch.Label.Wildcard, ctx: C): R

    public fun visitGraphMatchLabelNegation(node: GraphMatch.Label.Negation, ctx: C): R

    public fun visitGraphMatchLabelConj(node: GraphMatch.Label.Conj, ctx: C): R

    public fun visitGraphMatchLabelDisj(node: GraphMatch.Label.Disj, ctx: C): R

    public fun visitTableDefinition(node: TableDefinition, ctx: C): R

    public fun visitTableDefinitionColumn(node: TableDefinition.Column, ctx: C): R

    public fun visitTableDefinitionColumnConstraint(node: TableDefinition.Column.Constraint, ctx: C):
        R

    public fun visitTableDefinitionColumnConstraintBody(
        node: TableDefinition.Column.Constraint.Body,
        ctx: C
    ): R

    public
    fun visitTableDefinitionColumnConstraintBodyNullable(
        node: TableDefinition.Column.Constraint.Body.Nullable,
        ctx: C
    ): R

    public
    fun visitTableDefinitionColumnConstraintBodyNotNull(
        node: TableDefinition.Column.Constraint.Body.NotNull,
        ctx: C
    ): R

    public
    fun visitTableDefinitionColumnConstraintBodyCheck(
        node: TableDefinition.Column.Constraint.Body.Check,
        ctx: C
    ): R
}
