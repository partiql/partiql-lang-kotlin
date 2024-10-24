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
    default R defaultVisit(AstNode node, C ctx) {
        for (AstNode child : node.children()) {
            child.accept(this, ctx);
        }
        return defaultReturn(node, ctx);
    }

    R defaultReturn(AstNode node, C ctx);

    default R visit(AstNode node, C ctx) {
        return node.accept(this, ctx);
    }

    default R visitStatement(Statement node, C ctx) {
        return defaultVisit(node, ctx);
    }

    default R visitQuery(Query node, C ctx) {
        return defaultVisit(node, ctx);
    }

    default R visitExplain(Explain node, C ctx) {
        return defaultVisit(node, ctx);
    }

    default R visitIdentifier(Identifier node, C ctx) {
        return defaultVisit(node, ctx);
    }

    default R visitIdentifierChain(IdentifierChain node, C ctx) {
        return defaultVisit(node, ctx);
    }

    default R visitExpr(Expr node, C ctx) {
        return defaultVisit(node, ctx);
    }

    default R visitExprLit(ExprLit node, C ctx) {
        return defaultVisit(node, ctx);
    }

    default R visitExprVariant(ExprVariant node, C ctx) {
        return defaultVisit(node, ctx);
    }

    default R visitExprVarRef(ExprVarRef node, C ctx) {
        return defaultVisit(node, ctx);
    }

    default R visitExprSessionAttribute(ExprSessionAttribute node, C ctx) {
        return defaultVisit(node, ctx);
    }

    default R visitExprPath(ExprPath node, C ctx) {
        return defaultVisit(node, ctx);
    }

    default R visitPathStep(PathStep node, C ctx) {
        return defaultVisit(node, ctx);
    }

    default R visitPathStepField(PathStep.Field node, C ctx) {
        return defaultVisit(node, ctx);
    }

    default R visitPathStepElement(PathStep.Element node, C ctx) {
        return defaultVisit(node, ctx);
    }

    default R visitPathStepAllElements(PathStep.AllElements node, C ctx) {
        return defaultVisit(node, ctx);
    }

    default R visitPathStepAllFields(PathStep.AllFields node, C ctx) {
        return defaultVisit(node, ctx);
    }

    default R visitExprCall(ExprCall node, C ctx) {
        return defaultVisit(node, ctx);
    }

    default R visitExprParameter(ExprParameter node, C ctx) {
        return defaultVisit(node, ctx);
    }

    default R visitExprOperator(ExprOperator node, C ctx) {
        return defaultVisit(node, ctx);
    }

    default R visitExprNot(ExprNot node, C ctx) {
        return defaultVisit(node, ctx);
    }

    default R visitExprAnd(ExprAnd node, C ctx) {
        return defaultVisit(node, ctx);
    }

    default R visitExprOr(ExprOr node, C ctx) {
        return defaultVisit(node, ctx);
    }

    default R visitExprValues(ExprValues node, C ctx) {
        return defaultVisit(node, ctx);
    }

    default R visitExprValuesRow(ExprValues.Row node, C ctx) {
        return defaultVisit(node, ctx);
    }

    default R visitExprArray(ExprArray node, C ctx) {
        return defaultVisit(node, ctx);
    }

    default R visitExprBag(ExprBag node, C ctx) {
        return defaultVisit(node, ctx);
    }

    default R visitExprStruct(ExprStruct node, C ctx) {
        return defaultVisit(node, ctx);
    }

    default R visitExprStructField(ExprStruct.Field node, C ctx) {
        return defaultVisit(node, ctx);
    }

    default R visitExprLike(ExprLike node, C ctx) {
        return defaultVisit(node, ctx);
    }

    default R visitExprBetween(ExprBetween node, C ctx) {
        return defaultVisit(node, ctx);
    }

    default R visitExprInCollection(ExprInCollection node, C ctx) {
        return defaultVisit(node, ctx);
    }

    default R visitExprIsType(ExprIsType node, C ctx) {
        return defaultVisit(node, ctx);
    }

    default R visitExprCase(ExprCase node, C ctx) {
        return defaultVisit(node, ctx);
    }

    default R visitExprCaseBranch(ExprCase.Branch node, C ctx) {
        return defaultVisit(node, ctx);
    }

    default R visitExprCoalesce(ExprCoalesce node, C ctx) {
        return defaultVisit(node, ctx);
    }

    default R visitExprNullIf(ExprNullIf node, C ctx) {
        return defaultVisit(node, ctx);
    }

    default R visitExprSubstring(ExprSubstring node, C ctx) {
        return defaultVisit(node, ctx);
    }

    default R visitExprPosition(ExprPosition node, C ctx) {
        return defaultVisit(node, ctx);
    }

    default R visitExprTrim(ExprTrim node, C ctx) {
        return defaultVisit(node, ctx);
    }

    default R visitExprOverlay(ExprOverlay node, C ctx) {
        return defaultVisit(node, ctx);
    }

    default R visitExprExtract(ExprExtract node, C ctx) {
        return defaultVisit(node, ctx);
    }

    default R visitExprCast(ExprCast node, C ctx) {
        return defaultVisit(node, ctx);
    }

    default R visitExprQuerySet(ExprQuerySet node, C ctx) {
        return defaultVisit(node, ctx);
    }

    default R visitExprMatch(ExprMatch node, C ctx) {
        return defaultVisit(node, ctx);
    }

    default R visitExprWindow(ExprWindow node, C ctx) {
        return defaultVisit(node, ctx);
    }

    default R visitExprWindowOver(ExprWindow.Over node, C ctx) {
        return defaultVisit(node, ctx);
    }

    default R visitQueryBody(QueryBody node, C ctx) {
        return defaultVisit(node, ctx);
    }

    default R visitQueryBodySFW(QueryBody.SFW node, C ctx) {
        return defaultVisit(node, ctx);
    }

    default R visitQueryBodySetOp(QueryBody.SetOp node, C ctx) {
        return defaultVisit(node, ctx);
    }

    default R visitSelect(Select node, C ctx) {
        return defaultVisit(node, ctx);
    }

    default R visitSelectStar(SelectStar node, C ctx) {
        return defaultVisit(node, ctx);
    }

    default R visitSelectList(SelectList node, C ctx) {
        return defaultVisit(node, ctx);
    }

    default R visitSelectItem(SelectItem node, C ctx) {
        return defaultVisit(node, ctx);
    }

    default R visitSelectItemStar(SelectItem.Star node, C ctx) {
        return defaultVisit(node, ctx);
    }

    default R visitSelectItemExpr(SelectItem.Expr node, C ctx) {
        return defaultVisit(node, ctx);
    }

    default R visitSelectPivot(SelectPivot node, C ctx) {
        return defaultVisit(node, ctx);
    }

    default R visitSelectValue(SelectValue node, C ctx) {
        return defaultVisit(node, ctx);
    }

    default R visitExclude(Exclude node, C ctx) {
        return defaultVisit(node, ctx);
    }

    default R visitExcludePath(ExcludePath node, C ctx) {
        return defaultVisit(node, ctx);
    }

    default R visitExcludeStep(ExcludeStep node, C ctx) {
        return defaultVisit(node, ctx);
    }

    default R visitExcludeStepStructField(ExcludeStep.StructField node, C ctx) {
        return defaultVisit(node, ctx);
    }

    default R visitExcludeStepCollIndex(ExcludeStep.CollIndex node, C ctx) {
        return defaultVisit(node, ctx);
    }

    default R visitExcludeStepStructWildcard(ExcludeStep.StructWildcard node, C ctx) {
        return defaultVisit(node, ctx);
    }

    default R visitExcludeStepCollWildcard(ExcludeStep.CollWildcard node, C ctx) {
        return defaultVisit(node, ctx);
    }

    default R visitFrom(From node, C ctx) {
        return defaultVisit(node, ctx);
    }

    default R visitTableRef(FromTableRef node, C ctx) {
        return defaultVisit(node, ctx);
    }

    default R visitFromExpr(FromExpr node, C ctx) {
        return defaultVisit(node, ctx);
    }

    default R visitFromJoin(FromJoin node, C ctx) {
        return defaultVisit(node, ctx);
    }

    default R visitLet(Let node, C ctx) {
        return defaultVisit(node, ctx);
    }

    default R visitLetBinding(Let.Binding node, C ctx) {
        return defaultVisit(node, ctx);
    }

    default R visitGroupBy(GroupBy node, C ctx) {
        return defaultVisit(node, ctx);
    }

    default R visitGroupByKey(GroupBy.Key node, C ctx) {
        return defaultVisit(node, ctx);
    }

    default R visitOrderBy(OrderBy node, C ctx) {
        return defaultVisit(node, ctx);
    }

    default R visitSort(Sort node, C ctx) {
        return defaultVisit(node, ctx);
    }

    default R visitSetOp(SetOp node, C ctx) {
        return defaultVisit(node, ctx);
    }

    default R visitGraphMatch(GraphMatch node, C ctx) {
        return defaultVisit(node, ctx);
    }

    default R visitGraphMatchPattern(GraphPattern node, C ctx) {
        return defaultVisit(node, ctx);
    }

    default R visitGraphPart(GraphPart node, C ctx) {
        return defaultVisit(node, ctx);
    }

    default R visitGraphPartNode(GraphPart.Node node, C ctx) {
        return defaultVisit(node, ctx);
    }

    default R visitGraphPartEdge(GraphPart.Edge node, C ctx) {
        return defaultVisit(node, ctx);
    }

    default R visitGraphPartPattern(GraphPart.Pattern node, C ctx) {
        return defaultVisit(node, ctx);
    }

    default R visitGraphQuantifier(GraphQuantifier node, C ctx) {
        return defaultVisit(node, ctx);
    }

    default R visitGraphSelector(GraphSelector node, C ctx) {
        return defaultVisit(node, ctx);
    }

    default R visitGraphSelectorAnyShortest(GraphSelector.AnyShortest node, C ctx) {
        return defaultVisit(node, ctx);
    }

    default R visitGraphSelectorAllShortest(GraphSelector.AllShortest node, C ctx) {
        return defaultVisit(node, ctx);
    }

    default R visitGraphSelectorAny(GraphSelector.Any node, C ctx) {
        return defaultVisit(node, ctx);
    }

    default R visitGraphSelectorAnyK(GraphSelector.AnyK node, C ctx) {
        return defaultVisit(node, ctx);
    }

    default R visitGraphSelectorShortestK(GraphSelector.ShortestK node, C ctx) {
        return defaultVisit(node, ctx);
    }

    default R visitGraphSelectorShortestKGroup(GraphSelector.ShortestKGroup node, C ctx) {
        return defaultVisit(node, ctx);
    }

    default R visitGraphLabel(GraphLabel node, C ctx) {
        return defaultVisit(node, ctx);
    }

    default R visitGraphLabelName(GraphLabel.Name node, C ctx) {
        return defaultVisit(node, ctx);
    }

    default R visitGraphLabelWildcard(GraphLabel.Wildcard node, C ctx) {
        return defaultVisit(node, ctx);
    }

    default R visitGraphLabelNegation(GraphLabel.Negation node, C ctx) {
        return defaultVisit(node, ctx);
    }

    default R visitGraphLabelConj(GraphLabel.Conj node, C ctx) {
        return defaultVisit(node, ctx);
    }

    default R visitGraphLabelDisj(GraphLabel.Disj node, C ctx) {
        return defaultVisit(node, ctx);
    }
}
