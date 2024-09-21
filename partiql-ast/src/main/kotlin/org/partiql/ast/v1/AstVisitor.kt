package org.partiql.ast.v1

import org.partiql.ast.v1.expr.Expr
import org.partiql.ast.v1.expr.ExprAnd
import org.partiql.ast.v1.expr.ExprBetween
import org.partiql.ast.v1.expr.ExprCall
import org.partiql.ast.v1.expr.ExprCase
import org.partiql.ast.v1.expr.ExprCast
import org.partiql.ast.v1.expr.ExprCoalesce
import org.partiql.ast.v1.expr.ExprCollection
import org.partiql.ast.v1.expr.ExprDateAdd
import org.partiql.ast.v1.expr.ExprDateDiff
import org.partiql.ast.v1.expr.ExprExtract
import org.partiql.ast.v1.expr.ExprInCollection
import org.partiql.ast.v1.expr.ExprIon
import org.partiql.ast.v1.expr.ExprIsType
import org.partiql.ast.v1.expr.ExprLike
import org.partiql.ast.v1.expr.ExprLit
import org.partiql.ast.v1.expr.ExprMatch
import org.partiql.ast.v1.expr.ExprNot
import org.partiql.ast.v1.expr.ExprNullIf
import org.partiql.ast.v1.expr.ExprOperator
import org.partiql.ast.v1.expr.ExprOr
import org.partiql.ast.v1.expr.ExprOverlay
import org.partiql.ast.v1.expr.ExprParameter
import org.partiql.ast.v1.expr.ExprPath
import org.partiql.ast.v1.expr.ExprPathStep
import org.partiql.ast.v1.expr.ExprPosition
import org.partiql.ast.v1.expr.ExprQuerySet
import org.partiql.ast.v1.expr.ExprSessionAttribute
import org.partiql.ast.v1.expr.ExprStruct
import org.partiql.ast.v1.expr.ExprSubstring
import org.partiql.ast.v1.expr.ExprTrim
import org.partiql.ast.v1.expr.ExprValues
import org.partiql.ast.v1.expr.ExprVar
import org.partiql.ast.v1.expr.ExprWindow
import org.partiql.ast.v1.graph.GraphLabel
import org.partiql.ast.v1.graph.GraphMatch
import org.partiql.ast.v1.graph.GraphPart
import org.partiql.ast.v1.graph.GraphPattern
import org.partiql.ast.v1.graph.GraphQuantifier
import org.partiql.ast.v1.graph.GraphSelector
import org.partiql.ast.v1.type.Type
import org.partiql.ast.v1.type.TypeAny
import org.partiql.ast.v1.type.TypeBag
import org.partiql.ast.v1.type.TypeBigint
import org.partiql.ast.v1.type.TypeBit
import org.partiql.ast.v1.type.TypeBitVarying
import org.partiql.ast.v1.type.TypeBlob
import org.partiql.ast.v1.type.TypeBool
import org.partiql.ast.v1.type.TypeByteString
import org.partiql.ast.v1.type.TypeChar
import org.partiql.ast.v1.type.TypeClob
import org.partiql.ast.v1.type.TypeCustom
import org.partiql.ast.v1.type.TypeDate
import org.partiql.ast.v1.type.TypeDecimal
import org.partiql.ast.v1.type.TypeFloat32
import org.partiql.ast.v1.type.TypeFloat64
import org.partiql.ast.v1.type.TypeInt
import org.partiql.ast.v1.type.TypeInt2
import org.partiql.ast.v1.type.TypeInt4
import org.partiql.ast.v1.type.TypeInt8
import org.partiql.ast.v1.type.TypeInterval
import org.partiql.ast.v1.type.TypeList
import org.partiql.ast.v1.type.TypeMissing
import org.partiql.ast.v1.type.TypeNull
import org.partiql.ast.v1.type.TypeNumeric
import org.partiql.ast.v1.type.TypeReal
import org.partiql.ast.v1.type.TypeSexp
import org.partiql.ast.v1.type.TypeSmallint
import org.partiql.ast.v1.type.TypeString
import org.partiql.ast.v1.type.TypeStruct
import org.partiql.ast.v1.type.TypeSymbol
import org.partiql.ast.v1.type.TypeTime
import org.partiql.ast.v1.type.TypeTimeWithTz
import org.partiql.ast.v1.type.TypeTimestamp
import org.partiql.ast.v1.type.TypeTimestampWithTz
import org.partiql.ast.v1.type.TypeTinyint
import org.partiql.ast.v1.type.TypeTuple
import org.partiql.ast.v1.type.TypeVarchar

/**
 * TODO docs, equals, hashcode
 */
public interface AstVisitor<R, C> {
    public fun visit(node: AstNode, ctx: C): R

    public fun visitStatement(node: Statement, ctx: C): R

    public fun visitQuery(node: Query, ctx: C): R

    public fun visitDDL(node: DDL, ctx: C): R

    public fun visitCreateTable(node: CreateTable, ctx: C): R

    public fun visitCreateIndex(node: CreateIndex, ctx: C): R

    public fun visitDropTable(node: DropTable, ctx: C): R

    public fun visitDropIndex(node: DropIndex, ctx: C): R

    public fun visitExplain(node: Explain, ctx: C): R

    public fun visitTarget(node: Target, ctx: C): R

    public fun visitTargetDomain(node: Target.Domain, ctx: C): R

    public fun visitType(node: Type, ctx: C): R

    public fun visitTypeNull(node: TypeNull, ctx: C): R

    public fun visitTypeMissing(node: TypeMissing, ctx: C): R

    public fun visitTypeBool(node: TypeBool, ctx: C): R

    public fun visitTypeTinyint(node: TypeTinyint, ctx: C): R

    public fun visitTypeSmallint(node: TypeSmallint, ctx: C): R

    public fun visitTypeInt2(node: TypeInt2, ctx: C): R

    public fun visitTypeInt4(node: TypeInt4, ctx: C): R

    public fun visitTypeBigint(node: TypeBigint, ctx: C): R

    public fun visitTypeInt8(node: TypeInt8, ctx: C): R

    public fun visitTypeInt(node: TypeInt, ctx: C): R

    public fun visitTypeReal(node: TypeReal, ctx: C): R

    public fun visitTypeFloat32(node: TypeFloat32, ctx: C): R

    public fun visitTypeFloat64(node: TypeFloat64, ctx: C): R

    public fun visitTypeDecimal(node: TypeDecimal, ctx: C): R

    public fun visitTypeNumeric(node: TypeNumeric, ctx: C): R

    public fun visitTypeChar(node: TypeChar, ctx: C): R

    public fun visitTypeVarchar(node: TypeVarchar, ctx: C): R

    public fun visitTypeString(node: TypeString, ctx: C): R

    public fun visitTypeSymbol(node: TypeSymbol, ctx: C): R

    public fun visitTypeBit(node: TypeBit, ctx: C): R

    public fun visitTypeBitVarying(node: TypeBitVarying, ctx: C): R

    public fun visitTypeByteString(node: TypeByteString, ctx: C): R

    public fun visitTypeBlob(node: TypeBlob, ctx: C): R

    public fun visitTypeClob(node: TypeClob, ctx: C): R

    public fun visitTypeDate(node: TypeDate, ctx: C): R

    public fun visitTypeTime(node: TypeTime, ctx: C): R

    public fun visitTypeTimeWithTz(node: TypeTimeWithTz, ctx: C): R

    public fun visitTypeTimestamp(node: TypeTimestamp, ctx: C): R

    public fun visitTypeTimestampWithTz(node: TypeTimestampWithTz, ctx: C): R

    public fun visitTypeInterval(node: TypeInterval, ctx: C): R

    public fun visitTypeBag(node: TypeBag, ctx: C): R

    public fun visitTypeList(node: TypeList, ctx: C): R

    public fun visitTypeSexp(node: TypeSexp, ctx: C): R

    public fun visitTypeTuple(node: TypeTuple, ctx: C): R

    public fun visitTypeStruct(node: TypeStruct, ctx: C): R

    public fun visitTypeAny(node: TypeAny, ctx: C): R

    public fun visitTypeCustom(node: TypeCustom, ctx: C): R

    public fun visitIdentifier(node: Identifier, ctx: C): R

    public fun visitIdentifierSymbol(node: Identifier.Symbol, ctx: C): R

    public fun visitIdentifierQualified(node: Identifier.Qualified, ctx: C): R

    public fun visitPathLit(node: PathLit, ctx: C): R

    public fun visitPathLitStep(node: PathLitStep, ctx: C): R

    public fun visitPathLitStepSymbol(node: PathLitStep.Symbol, ctx: C): R

    public fun visitPathLitStepIndex(node: PathLitStep.Index, ctx: C): R

    public fun visitExpr(node: Expr, ctx: C): R

    public fun visitExprLit(node: ExprLit, ctx: C): R

    public fun visitExprIon(node: ExprIon, ctx: C): R

    public fun visitExprVar(node: ExprVar, ctx: C): R

    public fun visitExprSessionAttribute(node: ExprSessionAttribute, ctx: C): R

    public fun visitExprPath(node: ExprPath, ctx: C): R

    public fun visitExprPathStep(node: ExprPathStep, ctx: C): R

    public fun visitExprPathStepSymbol(node: ExprPathStep.Symbol, ctx: C): R

    public fun visitExprPathStepIndex(node: ExprPathStep.Index, ctx: C): R

    public fun visitExprPathStepWildcard(node: ExprPathStep.Wildcard, ctx: C): R

    public fun visitExprPathStepUnpivot(node: ExprPathStep.Unpivot, ctx: C): R

    public fun visitExprCall(node: ExprCall, ctx: C): R

    public fun visitExprParameter(node: ExprParameter, ctx: C): R

    public fun visitExprOperator(node: ExprOperator, ctx: C): R

    public fun visitExprNot(node: ExprNot, ctx: C): R

    public fun visitExprAnd(node: ExprAnd, ctx: C): R

    public fun visitExprOr(node: ExprOr, ctx: C): R

    public fun visitExprValues(node: ExprValues, ctx: C): R

    public fun visitExprValuesRow(node: ExprValues.Row, ctx: C): R

    public fun visitExprCollection(node: ExprCollection, ctx: C): R

    public fun visitExprStruct(node: ExprStruct, ctx: C): R

    public fun visitExprStructField(node: ExprStruct.Field, ctx: C): R

    public fun visitExprLike(node: ExprLike, ctx: C): R

    public fun visitExprBetween(node: ExprBetween, ctx: C): R

    public fun visitExprInCollection(node: ExprInCollection, ctx: C): R

    public fun visitExprIsType(node: ExprIsType, ctx: C): R

    public fun visitExprCase(node: ExprCase, ctx: C): R

    public fun visitExprCaseBranch(node: ExprCase.Branch, ctx: C): R

    public fun visitExprCoalesce(node: ExprCoalesce, ctx: C): R

    public fun visitExprNullIf(node: ExprNullIf, ctx: C): R

    public fun visitExprSubstring(node: ExprSubstring, ctx: C): R

    public fun visitExprPosition(node: ExprPosition, ctx: C): R

    public fun visitExprTrim(node: ExprTrim, ctx: C): R

    public fun visitExprOverlay(node: ExprOverlay, ctx: C): R

    public fun visitExprExtract(node: ExprExtract, ctx: C): R

    public fun visitExprCast(node: ExprCast, ctx: C): R

    public fun visitExprDateAdd(node: ExprDateAdd, ctx: C): R

    public fun visitExprDateDiff(node: ExprDateDiff, ctx: C): R

    public fun visitExprQuerySet(node: ExprQuerySet, ctx: C): R

    public fun visitExprMatch(node: ExprMatch, ctx: C): R

    public fun visitExprWindow(node: ExprWindow, ctx: C): R

    public fun visitExprWindowOver(node: ExprWindow.Over, ctx: C): R

    public fun visitQueryBody(node: QueryBody, ctx: C): R

    public fun visitQueryBodySFW(node: QueryBody.SFW, ctx: C): R

    public fun visitQueryBodySetOp(node: QueryBody.SetOp, ctx: C): R

    public fun visitSelect(node: Select, ctx: C): R

    public fun visitSelectStar(node: SelectStar, ctx: C): R

    public fun visitSelectProject(node: SelectProject, ctx: C): R

    public fun visitProjectItem(node: ProjectItem, ctx: C): R

    public fun visitProjectItemAll(node: ProjectItem.All, ctx: C): R

    public fun visitProjectItemExpression(node: ProjectItem.Expression, ctx: C): R

    public fun visitSelectPivot(node: SelectPivot, ctx: C): R

    public fun visitSelectValue(node: SelectValue, ctx: C): R

    public fun visitExclude(node: Exclude, ctx: C): R

    public fun visitExcludePath(node: ExcludePath, ctx: C): R

    public fun visitExcludeStep(node: ExcludeStep, ctx: C): R

    public fun visitExcludeStepStructField(node: ExcludeStep.StructField, ctx: C): R

    public fun visitExcludeStepCollIndex(node: ExcludeStep.CollIndex, ctx: C): R

    public fun visitExcludeStepStructWildcard(node: ExcludeStep.StructWildcard, ctx: C): R

    public fun visitExcludeStepCollWildcard(node: ExcludeStep.CollWildcard, ctx: C): R

    public fun visitFrom(node: From, ctx: C): R

    public fun visitFromValue(node: FromValue, ctx: C): R

    public fun visitFromJoin(node: FromJoin, ctx: C): R

    public fun visitLet(node: Let, ctx: C): R

    public fun visitLetBinding(node: Let.Binding, ctx: C): R

    public fun visitGroupBy(node: GroupBy, ctx: C): R

    public fun visitGroupByKey(node: GroupBy.Key, ctx: C): R

    public fun visitOrderBy(node: OrderBy, ctx: C): R

    public fun visitSort(node: Sort, ctx: C): R

    public fun visitSetOp(node: SetOp, ctx: C): R

    public fun visitGraphMatch(node: GraphMatch, ctx: C): R

    public fun visitGraphMatchPattern(node: GraphPattern, ctx: C): R

    public fun visitGraphPart(node: GraphPart, ctx: C): R

    public fun visitGraphPartNode(node: GraphPart.Node, ctx: C): R

    public fun visitGraphPartEdge(node: GraphPart.Edge, ctx: C): R

    public fun visitGraphPartPattern(node: GraphPart.Pattern, ctx: C): R

    public fun visitGraphQuantifier(node: GraphQuantifier, ctx: C): R

    public fun visitGraphSelector(node: GraphSelector, ctx: C): R

    public fun visitGraphSelectorAnyShortest(node: GraphSelector.AnyShortest, ctx: C): R

    public fun visitGraphSelectorAllShortest(node: GraphSelector.AllShortest, ctx: C): R

    public fun visitGraphSelectorAny(node: GraphSelector.Any, ctx: C): R

    public fun visitGraphSelectorAnyK(node: GraphSelector.AnyK, ctx: C): R

    public fun visitGraphSelectorShortestK(node: GraphSelector.ShortestK, ctx: C): R

    public fun visitGraphSelectorShortestKGroup(
        node: GraphSelector.ShortestKGroup,
        ctx: C
    ): R

    public fun visitGraphLabel(node: GraphLabel, ctx: C): R

    public fun visitGraphLabelName(node: GraphLabel.Name, ctx: C): R

    public fun visitGraphLabelWildcard(node: GraphLabel.Wildcard, ctx: C): R

    public fun visitGraphLabelNegation(node: GraphLabel.Negation, ctx: C): R

    public fun visitGraphLabelConj(node: GraphLabel.Conj, ctx: C): R

    public fun visitGraphLabelDisj(node: GraphLabel.Disj, ctx: C): R

    public fun visitTableDefinition(node: TableDefinition, ctx: C): R

    public fun visitColumn(node: Column, ctx: C): R

    public fun visitConstraint(node: Constraint, ctx: C):
        R

    public fun visitConstraintBody(
        node: ConstraintBody,
        ctx: C
    ): R

    public
    fun visitConstraintBodyNullable(
        node: ConstraintBody.Nullable,
        ctx: C
    ): R

    public
    fun visitConstraintBodyNotNull(
        node: ConstraintBody.NotNull,
        ctx: C
    ): R

    public
    fun visitConstraintBodyCheck(
        node: ConstraintBody.Check,
        ctx: C
    ): R
}
