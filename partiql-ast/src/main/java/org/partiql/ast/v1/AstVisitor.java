package org.partiql.ast.v1;

import org.partiql.ast.v1.expr.Expr;
import org.partiql.ast.v1.expr.ExprAnd;
import org.partiql.ast.v1.expr.ExprBetween;
import org.partiql.ast.v1.expr.ExprCall;
import org.partiql.ast.v1.expr.ExprCase;
import org.partiql.ast.v1.expr.ExprCast;
import org.partiql.ast.v1.expr.ExprCoalesce;
import org.partiql.ast.v1.expr.ExprCollection;
import org.partiql.ast.v1.expr.ExprDateAdd;
import org.partiql.ast.v1.expr.ExprDateDiff;
import org.partiql.ast.v1.expr.ExprExtract;
import org.partiql.ast.v1.expr.ExprInCollection;
import org.partiql.ast.v1.expr.ExprIon;
import org.partiql.ast.v1.expr.ExprIsType;
import org.partiql.ast.v1.expr.ExprLike;
import org.partiql.ast.v1.expr.ExprLit;
import org.partiql.ast.v1.expr.ExprMatch;
import org.partiql.ast.v1.expr.ExprNot;
import org.partiql.ast.v1.expr.ExprNullIf;
import org.partiql.ast.v1.expr.ExprOperator;
import org.partiql.ast.v1.expr.ExprOr;
import org.partiql.ast.v1.expr.ExprOverlay;
import org.partiql.ast.v1.expr.ExprParameter;
import org.partiql.ast.v1.expr.ExprPath;
import org.partiql.ast.v1.expr.ExprPathStep;
import org.partiql.ast.v1.expr.ExprPosition;
import org.partiql.ast.v1.expr.ExprQuerySet;
import org.partiql.ast.v1.expr.ExprSessionAttribute;
import org.partiql.ast.v1.expr.ExprStruct;
import org.partiql.ast.v1.expr.ExprSubstring;
import org.partiql.ast.v1.expr.ExprTrim;
import org.partiql.ast.v1.expr.ExprValues;
import org.partiql.ast.v1.expr.ExprVar;
import org.partiql.ast.v1.expr.ExprWindow;
import org.partiql.ast.v1.graph.GraphLabel;
import org.partiql.ast.v1.graph.GraphMatch;
import org.partiql.ast.v1.graph.GraphPart;
import org.partiql.ast.v1.graph.GraphPattern;
import org.partiql.ast.v1.graph.GraphQuantifier;
import org.partiql.ast.v1.graph.GraphSelector;
import org.partiql.ast.v1.type.Type;
import org.partiql.ast.v1.type.TypeAny;
import org.partiql.ast.v1.type.TypeBag;
import org.partiql.ast.v1.type.TypeBigint;
import org.partiql.ast.v1.type.TypeBit;
import org.partiql.ast.v1.type.TypeBitVarying;
import org.partiql.ast.v1.type.TypeBlob;
import org.partiql.ast.v1.type.TypeBool;
import org.partiql.ast.v1.type.TypeByteString;
import org.partiql.ast.v1.type.TypeChar;
import org.partiql.ast.v1.type.TypeClob;
import org.partiql.ast.v1.type.TypeCustom;
import org.partiql.ast.v1.type.TypeDate;
import org.partiql.ast.v1.type.TypeDecimal;
import org.partiql.ast.v1.type.TypeFloat32;
import org.partiql.ast.v1.type.TypeFloat64;
import org.partiql.ast.v1.type.TypeInt;
import org.partiql.ast.v1.type.TypeInt2;
import org.partiql.ast.v1.type.TypeInt4;
import org.partiql.ast.v1.type.TypeInt8;
import org.partiql.ast.v1.type.TypeInterval;
import org.partiql.ast.v1.type.TypeList;
import org.partiql.ast.v1.type.TypeMissing;
import org.partiql.ast.v1.type.TypeNull;
import org.partiql.ast.v1.type.TypeNumeric;
import org.partiql.ast.v1.type.TypeReal;
import org.partiql.ast.v1.type.TypeSexp;
import org.partiql.ast.v1.type.TypeSmallint;
import org.partiql.ast.v1.type.TypeString;
import org.partiql.ast.v1.type.TypeStruct;
import org.partiql.ast.v1.type.TypeSymbol;
import org.partiql.ast.v1.type.TypeTime;
import org.partiql.ast.v1.type.TypeTimeWithTz;
import org.partiql.ast.v1.type.TypeTimestamp;
import org.partiql.ast.v1.type.TypeTimestampWithTz;
import org.partiql.ast.v1.type.TypeTinyint;
import org.partiql.ast.v1.type.TypeTuple;
import org.partiql.ast.v1.type.TypeVarchar;

public interface AstVisitor<R, C> {
    R visit(AstNode node, C ctx);

    R visitStatement(Statement node, C ctx);

    R visitQuery(Query node, C ctx);

    R visitExplain(Explain node, C ctx);

    R visitTarget(Target node, C ctx);

    R visitTargetDomain(Target.Domain node, C ctx);

    R visitType(Type node, C ctx);

    R visitTypeNull(TypeNull node, C ctx);

    R visitTypeMissing(TypeMissing node, C ctx);

    R visitTypeBool(TypeBool node, C ctx);

    R visitTypeTinyint(TypeTinyint node, C ctx);

    R visitTypeSmallint(TypeSmallint node, C ctx);

    R visitTypeInt2(TypeInt2 node, C ctx);

    R visitTypeInt4(TypeInt4 node, C ctx);

    R visitTypeBigint(TypeBigint node, C ctx);

    R visitTypeInt8(TypeInt8 node, C ctx);

    R visitTypeInt(TypeInt node, C ctx);

    R visitTypeReal(TypeReal node, C ctx);

    R visitTypeFloat32(TypeFloat32 node, C ctx);

    R visitTypeFloat64(TypeFloat64 node, C ctx);

    R visitTypeDecimal(TypeDecimal node, C ctx);

    R visitTypeNumeric(TypeNumeric node, C ctx);

    R visitTypeChar(TypeChar node, C ctx);

    R visitTypeVarchar(TypeVarchar node, C ctx);

    R visitTypeString(TypeString node, C ctx);

    R visitTypeSymbol(TypeSymbol node, C ctx);

    R visitTypeBit(TypeBit node, C ctx);

    R visitTypeBitVarying(TypeBitVarying node, C ctx);

    R visitTypeByteString(TypeByteString node, C ctx);

    R visitTypeBlob(TypeBlob node, C ctx);

    R visitTypeClob(TypeClob node, C ctx);

    R visitTypeDate(TypeDate node, C ctx);

    R visitTypeTime(TypeTime node, C ctx);

    R visitTypeTimeWithTz(TypeTimeWithTz node, C ctx);

    R visitTypeTimestamp(TypeTimestamp node, C ctx);

    R visitTypeTimestampWithTz(TypeTimestampWithTz node, C ctx);

    R visitTypeInterval(TypeInterval node, C ctx);

    R visitTypeBag(TypeBag node, C ctx);

    R visitTypeList(TypeList node, C ctx);

    R visitTypeSexp(TypeSexp node, C ctx);

    R visitTypeTuple(TypeTuple node, C ctx);

    R visitTypeStruct(TypeStruct node, C ctx);

    R visitTypeAny(TypeAny node, C ctx);

    R visitTypeCustom(TypeCustom node, C ctx);

    R visitIdentifier(Identifier node, C ctx);

    R visitIdentifierSymbol(Identifier.Symbol node, C ctx);

    R visitIdentifierQualified(Identifier.Qualified node, C ctx);

    R visitPathLit(PathLit node, C ctx);

    R visitPathLitStep(PathLitStep node, C ctx);

    R visitPathLitStepSymbol(PathLitStep.Symbol node, C ctx);

    R visitPathLitStepIndex(PathLitStep.Index node, C ctx);

    R visitExpr(Expr node, C ctx);

    R visitExprLit(ExprLit node, C ctx);

    R visitExprIon(ExprIon node, C ctx);

    R visitExprVar(ExprVar node, C ctx);

    R visitExprSessionAttribute(ExprSessionAttribute node, C ctx);

    R visitExprPath(ExprPath node, C ctx);

    R visitExprPathStep(ExprPathStep node, C ctx);

    R visitExprPathStepSymbol(ExprPathStep.Symbol node, C ctx);

    R visitExprPathStepIndex(ExprPathStep.Index node, C ctx);

    R visitExprPathStepWildcard(ExprPathStep.Wildcard node, C ctx);

    R visitExprPathStepUnpivot(ExprPathStep.Unpivot node, C ctx);

    R visitExprCall(ExprCall node, C ctx);

    R visitExprParameter(ExprParameter node, C ctx);

    R visitExprOperator(ExprOperator node, C ctx);

    R visitExprNot(ExprNot node, C ctx);

    R visitExprAnd(ExprAnd node, C ctx);

    R visitExprOr(ExprOr node, C ctx);

    R visitExprValues(ExprValues node, C ctx);

    R visitExprValuesRow(ExprValues.Row node, C ctx);

    R visitExprCollection(ExprCollection node, C ctx);

    R visitExprStruct(ExprStruct node, C ctx);

    R visitExprStructField(ExprStruct.Field node, C ctx);

    R visitExprLike(ExprLike node, C ctx);

    R visitExprBetween(ExprBetween node, C ctx);

    R visitExprInCollection(ExprInCollection node, C ctx);

    R visitExprIsType(ExprIsType node, C ctx);

    R visitExprCase(ExprCase node, C ctx);

    R visitExprCaseBranch(ExprCase.Branch node, C ctx);

    R visitExprCoalesce(ExprCoalesce node, C ctx);

    R visitExprNullIf(ExprNullIf node, C ctx);

    R visitExprSubstring(ExprSubstring node, C ctx);

    R visitExprPosition(ExprPosition node, C ctx);

    R visitExprTrim(ExprTrim node, C ctx);

    R visitExprOverlay(ExprOverlay node, C ctx);

    R visitExprExtract(ExprExtract node, C ctx);

    R visitExprCast(ExprCast node, C ctx);

    R visitExprDateAdd(ExprDateAdd node, C ctx);

    R visitExprDateDiff(ExprDateDiff node, C ctx);

    R visitExprQuerySet(ExprQuerySet node, C ctx);

    R visitExprMatch(ExprMatch node, C ctx);

    R visitExprWindow(ExprWindow node, C ctx);

    R visitExprWindowOver(ExprWindow.Over node, C ctx);

    R visitQueryBody(QueryBody node, C ctx);

    R visitQueryBodySFW(QueryBody.SFW node, C ctx);

    R visitQueryBodySetOp(QueryBody.SetOp node, C ctx);

    R visitSelect(Select node, C ctx);

    R visitSelectStar(SelectStar node, C ctx);

    R visitSelectProject(SelectProject node, C ctx);

    R visitProjectItem(ProjectItem node, C ctx);

    R visitProjectItemAll(ProjectItem.All node, C ctx);

    R visitProjectItemExpression(ProjectItem.Expression node, C ctx);

    R visitSelectPivot(SelectPivot node, C ctx);

    R visitSelectValue(SelectValue node, C ctx);

    R visitExclude(Exclude node, C ctx);

    R visitExcludePath(ExcludePath node, C ctx);

    R visitExcludeStep(ExcludeStep node, C ctx);

    R visitExcludeStepStructField(ExcludeStep.StructField node, C ctx);

    R visitExcludeStepCollIndex(ExcludeStep.CollIndex node, C ctx);

    R visitExcludeStepStructWildcard(ExcludeStep.StructWildcard node, C ctx);

    R visitExcludeStepCollWildcard(ExcludeStep.CollWildcard node, C ctx);

    R visitFrom(From node, C ctx);

    R visitFromValue(FromValue node, C ctx);

    R visitFromJoin(FromJoin node, C ctx);

    R visitLet(Let node, C ctx);

    R visitLetBinding(Let.Binding node, C ctx);

    R visitGroupBy(GroupBy node, C ctx);

    R visitGroupByKey(GroupBy.Key node, C ctx);

    R visitOrderBy(OrderBy node, C ctx);

    R visitSort(Sort node, C ctx);

    R visitSetOp(SetOp node, C ctx);

    R visitGraphMatch(GraphMatch node, C ctx);

    R visitGraphMatchPattern(GraphPattern node, C ctx);

    R visitGraphPart(GraphPart node, C ctx);

    R visitGraphPartNode(GraphPart.Node node, C ctx);

    R visitGraphPartEdge(GraphPart.Edge node, C ctx);

    R visitGraphPartPattern(GraphPart.Pattern node, C ctx);

    R visitGraphQuantifier(GraphQuantifier node, C ctx);

    R visitGraphSelector(GraphSelector node, C ctx);

    R visitGraphSelectorAnyShortest(GraphSelector.AnyShortest node, C ctx);

    R visitGraphSelectorAllShortest(GraphSelector.AllShortest node, C ctx);

    R visitGraphSelectorAny(GraphSelector.Any node, C ctx);

    R visitGraphSelectorAnyK(GraphSelector.AnyK node, C ctx);

    R visitGraphSelectorShortestK(GraphSelector.ShortestK node, C ctx);

    R visitGraphSelectorShortestKGroup(
            GraphSelector.ShortestKGroup node,
            C ctx
    );

    R visitGraphLabel(GraphLabel node, C ctx);

    R visitGraphLabelName(GraphLabel.Name node, C ctx);

    R visitGraphLabelWildcard(GraphLabel.Wildcard node, C ctx);

    R visitGraphLabelNegation(GraphLabel.Negation node, C ctx);

    R visitGraphLabelConj(GraphLabel.Conj node, C ctx);

    R visitGraphLabelDisj(GraphLabel.Disj node, C ctx);
}
