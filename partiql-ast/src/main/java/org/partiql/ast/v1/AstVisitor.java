package org.partiql.ast.v1;

import org.partiql.ast.v1.expr.Expr;
import org.partiql.ast.v1.expr.ExprAnd;
import org.partiql.ast.v1.expr.ExprArray;
import org.partiql.ast.v1.expr.ExprBag;
import org.partiql.ast.v1.expr.ExprBetween;
import org.partiql.ast.v1.expr.ExprCall;
import org.partiql.ast.v1.expr.ExprCase;
import org.partiql.ast.v1.expr.ExprCast;
import org.partiql.ast.v1.expr.ExprCoalesce;
import org.partiql.ast.v1.expr.ExprExtract;
import org.partiql.ast.v1.expr.ExprInCollection;
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
import org.partiql.ast.v1.expr.PathStep;
import org.partiql.ast.v1.expr.ExprPosition;
import org.partiql.ast.v1.expr.ExprQuerySet;
import org.partiql.ast.v1.expr.ExprSessionAttribute;
import org.partiql.ast.v1.expr.ExprStruct;
import org.partiql.ast.v1.expr.ExprSubstring;
import org.partiql.ast.v1.expr.ExprTrim;
import org.partiql.ast.v1.expr.ExprValues;
import org.partiql.ast.v1.expr.ExprVarRef;
import org.partiql.ast.v1.expr.ExprVariant;
import org.partiql.ast.v1.expr.ExprWindow;
import org.partiql.ast.v1.graph.GraphLabel;
import org.partiql.ast.v1.graph.GraphMatch;
import org.partiql.ast.v1.graph.GraphPart;
import org.partiql.ast.v1.graph.GraphPattern;
import org.partiql.ast.v1.graph.GraphQuantifier;
import org.partiql.ast.v1.graph.GraphSelector;

public interface AstVisitor<R, C> {
    R visit(AstNode node, C ctx);

    R visitStatement(Statement node, C ctx);

    R visitQuery(Query node, C ctx);

    R visitExplain(Explain node, C ctx);

    R visitIdentifier(Identifier node, C ctx);

    R visitIdentifierChain(IdentifierChain node, C ctx);

    R visitExpr(Expr node, C ctx);

    R visitExprLit(ExprLit node, C ctx);

    R visitExprVariant(ExprVariant node, C ctx);

    R visitExprVarRef(ExprVarRef node, C ctx);

    R visitExprSessionAttribute(ExprSessionAttribute node, C ctx);

    R visitExprPath(ExprPath node, C ctx);

    R visitPathStep(PathStep node, C ctx);

    R visitPathStepField(PathStep.Field node, C ctx);

    R visitPathStepElement(PathStep.Element node, C ctx);

    R visitPathStepAllElements(PathStep.AllElements node, C ctx);

    R visitPathStepAllFields(PathStep.AllFields node, C ctx);

    R visitExprCall(ExprCall node, C ctx);

    R visitExprParameter(ExprParameter node, C ctx);

    R visitExprOperator(ExprOperator node, C ctx);

    R visitExprNot(ExprNot node, C ctx);

    R visitExprAnd(ExprAnd node, C ctx);

    R visitExprOr(ExprOr node, C ctx);

    R visitExprValues(ExprValues node, C ctx);

    R visitExprValuesRow(ExprValues.Row node, C ctx);

    R visitExprArray(ExprArray node, C ctx);

    R visitExprBag(ExprBag node, C ctx);

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

    R visitProjectItemExpr(ProjectItem.Expr node, C ctx);

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

    R visitFromExpr(FromExpr node, C ctx);

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
