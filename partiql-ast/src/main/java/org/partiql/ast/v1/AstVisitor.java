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
import org.partiql.ast.v1.expr.ExprPosition;
import org.partiql.ast.v1.expr.ExprQuerySet;
import org.partiql.ast.v1.expr.ExprRowValue;
import org.partiql.ast.v1.expr.ExprSessionAttribute;
import org.partiql.ast.v1.expr.ExprStruct;
import org.partiql.ast.v1.expr.ExprSubstring;
import org.partiql.ast.v1.expr.ExprTrim;
import org.partiql.ast.v1.expr.ExprValues;
import org.partiql.ast.v1.expr.ExprVarRef;
import org.partiql.ast.v1.expr.ExprVariant;
import org.partiql.ast.v1.expr.ExprWindow;
import org.partiql.ast.v1.expr.PathStep;
import org.partiql.ast.v1.graph.GraphLabel;
import org.partiql.ast.v1.graph.GraphMatch;
import org.partiql.ast.v1.graph.GraphPart;
import org.partiql.ast.v1.graph.GraphPattern;
import org.partiql.ast.v1.graph.GraphQuantifier;
import org.partiql.ast.v1.graph.GraphSelector;

// TODO docs
//  Also include docs on how a library user could create a new variant for sum types and which methods to override
public abstract class AstVisitor<R, C> {
    public R defaultVisit(AstNode node, C ctx) {
        for (AstNode child : node.children()) {
            child.accept(this, ctx);
        }
        return defaultReturn(node, ctx);
    }

    public abstract R defaultReturn(AstNode node, C ctx);

    public R visit(AstNode node, C ctx) {
        return node.accept(this, ctx);
    }

    public R visitStatement(Statement node, C ctx) {
        return node.accept(this, ctx);
    }

    public R visitQuery(Query node, C ctx) {
        return defaultVisit(node, ctx);
    }

    public R visitExplain(Explain node, C ctx) {
        return defaultVisit(node, ctx);
    }

    public R visitIdentifier(Identifier node, C ctx) {
        return defaultVisit(node, ctx);
    }

    public R visitIdentifierChain(IdentifierChain node, C ctx) {
        return defaultVisit(node, ctx);
    }

    public R visitExpr(Expr node, C ctx) {
        return node.accept(this, ctx);
    }

    public R visitExprLit(ExprLit node, C ctx) {
        return defaultVisit(node, ctx);
    }

    public R visitExprVariant(ExprVariant node, C ctx) {
        return defaultVisit(node, ctx);
    }

    public R visitExprVarRef(ExprVarRef node, C ctx) {
        return defaultVisit(node, ctx);
    }

    public R visitExprSessionAttribute(ExprSessionAttribute node, C ctx) {
        return defaultVisit(node, ctx);
    }

    public R visitExprPath(ExprPath node, C ctx) {
        return defaultVisit(node, ctx);
    }

    public R visitPathStep(PathStep node, C ctx) {
        return node.accept(this, ctx);
    }

    public R visitPathStepField(PathStep.Field node, C ctx) {
        return defaultVisit(node, ctx);
    }

    public R visitPathStepElement(PathStep.Element node, C ctx) {
        return defaultVisit(node, ctx);
    }

    public R visitPathStepAllElements(PathStep.AllElements node, C ctx) {
        return defaultVisit(node, ctx);
    }

    public R visitPathStepAllFields(PathStep.AllFields node, C ctx) {
        return defaultVisit(node, ctx);
    }

    public R visitExprCall(ExprCall node, C ctx) {
        return defaultVisit(node, ctx);
    }

    public R visitExprParameter(ExprParameter node, C ctx) {
        return defaultVisit(node, ctx);
    }

    public R visitExprOperator(ExprOperator node, C ctx) {
        return defaultVisit(node, ctx);
    }

    public R visitExprNot(ExprNot node, C ctx) {
        return defaultVisit(node, ctx);
    }

    public R visitExprAnd(ExprAnd node, C ctx) {
        return defaultVisit(node, ctx);
    }

    public R visitExprOr(ExprOr node, C ctx) {
        return defaultVisit(node, ctx);
    }

    public R visitExprValues(ExprValues node, C ctx) {
        return defaultVisit(node, ctx);
    }

    public R visitExprRowValue(ExprRowValue node, C ctx) {
        return defaultVisit(node, ctx);
    }

    public R visitExprArray(ExprArray node, C ctx) {
        return defaultVisit(node, ctx);
    }

    public R visitExprBag(ExprBag node, C ctx) {
        return defaultVisit(node, ctx);
    }

    public R visitExprStruct(ExprStruct node, C ctx) {
        return defaultVisit(node, ctx);
    }

    public R visitExprStructField(ExprStruct.Field node, C ctx) {
        return defaultVisit(node, ctx);
    }

    public R visitExprLike(ExprLike node, C ctx) {
        return defaultVisit(node, ctx);
    }

    public R visitExprBetween(ExprBetween node, C ctx) {
        return defaultVisit(node, ctx);
    }

    public R visitExprInCollection(ExprInCollection node, C ctx) {
        return defaultVisit(node, ctx);
    }

    public R visitExprIsType(ExprIsType node, C ctx) {
        return defaultVisit(node, ctx);
    }

    public R visitExprCase(ExprCase node, C ctx) {
        return defaultVisit(node, ctx);
    }

    public R visitExprCaseBranch(ExprCase.Branch node, C ctx) {
        return defaultVisit(node, ctx);
    }

    public R visitExprCoalesce(ExprCoalesce node, C ctx) {
        return defaultVisit(node, ctx);
    }

    public R visitExprNullIf(ExprNullIf node, C ctx) {
        return defaultVisit(node, ctx);
    }

    public R visitExprSubstring(ExprSubstring node, C ctx) {
        return defaultVisit(node, ctx);
    }

    public R visitExprPosition(ExprPosition node, C ctx) {
        return defaultVisit(node, ctx);
    }

    public R visitExprTrim(ExprTrim node, C ctx) {
        return defaultVisit(node, ctx);
    }

    public R visitExprOverlay(ExprOverlay node, C ctx) {
        return defaultVisit(node, ctx);
    }

    public R visitExprExtract(ExprExtract node, C ctx) {
        return defaultVisit(node, ctx);
    }

    public R visitExprCast(ExprCast node, C ctx) {
        return defaultVisit(node, ctx);
    }

    public R visitExprQuerySet(ExprQuerySet node, C ctx) {
        return defaultVisit(node, ctx);
    }

    public R visitExprMatch(ExprMatch node, C ctx) {
        return defaultVisit(node, ctx);
    }

    public R visitExprWindow(ExprWindow node, C ctx) {
        return defaultVisit(node, ctx);
    }

    public R visitExprWindowOver(ExprWindow.Over node, C ctx) {
        return defaultVisit(node, ctx);
    }

    public R visitQueryBody(QueryBody node, C ctx) {
        return node.accept(this, ctx);
    }

    public R visitQueryBodySFW(QueryBody.SFW node, C ctx) {
        return defaultVisit(node, ctx);
    }

    public R visitQueryBodySetOp(QueryBody.SetOp node, C ctx) {
        return defaultVisit(node, ctx);
    }

    public R visitSelect(Select node, C ctx) {
        return node.accept(this, ctx);
    }

    public R visitSelectStar(SelectStar node, C ctx) {
        return defaultVisit(node, ctx);
    }

    public R visitSelectList(SelectList node, C ctx) {
        return defaultVisit(node, ctx);
    }

    public R visitSelectItem(SelectItem node, C ctx) {
        return node.accept(this, ctx);
    }

    public R visitSelectItemStar(SelectItem.Star node, C ctx) {
        return defaultVisit(node, ctx);
    }

    public R visitSelectItemExpr(SelectItem.Expr node, C ctx) {
        return defaultVisit(node, ctx);
    }

    public R visitSelectPivot(SelectPivot node, C ctx) {
        return defaultVisit(node, ctx);
    }

    public R visitSelectValue(SelectValue node, C ctx) {
        return defaultVisit(node, ctx);
    }

    public R visitExclude(Exclude node, C ctx) {
        return defaultVisit(node, ctx);
    }

    public R visitExcludePath(ExcludePath node, C ctx) {
        return defaultVisit(node, ctx);
    }

    public R visitExcludeStep(ExcludeStep node, C ctx) {
        return node.accept(this, ctx);
    }

    public R visitExcludeStepStructField(ExcludeStep.StructField node, C ctx) {
        return defaultVisit(node, ctx);
    }

    public R visitExcludeStepCollIndex(ExcludeStep.CollIndex node, C ctx) {
        return defaultVisit(node, ctx);
    }

    public R visitExcludeStepStructWildcard(ExcludeStep.StructWildcard node, C ctx) {
        return defaultVisit(node, ctx);
    }

    public R visitExcludeStepCollWildcard(ExcludeStep.CollWildcard node, C ctx) {
        return defaultVisit(node, ctx);
    }

    public R visitFrom(From node, C ctx) {
        return defaultVisit(node, ctx);
    }

    public R visitFromTableRef(FromTableRef node, C ctx) {
        return node.accept(this, ctx);
    }

    public R visitFromExpr(FromExpr node, C ctx) {
        return defaultVisit(node, ctx);
    }

    public R visitFromJoin(FromJoin node, C ctx) {
        return defaultVisit(node, ctx);
    }

    public R visitLet(Let node, C ctx) {
        return defaultVisit(node, ctx);
    }

    public R visitLetBinding(Let.Binding node, C ctx) {
        return defaultVisit(node, ctx);
    }

    public R visitGroupBy(GroupBy node, C ctx) {
        return defaultVisit(node, ctx);
    }

    public R visitGroupByKey(GroupBy.Key node, C ctx) {
        return defaultVisit(node, ctx);
    }

    public R visitOrderBy(OrderBy node, C ctx) {
        return defaultVisit(node, ctx);
    }

    public R visitSort(Sort node, C ctx) {
        return defaultVisit(node, ctx);
    }

    public R visitSetOp(SetOp node, C ctx) {
        return defaultVisit(node, ctx);
    }

    public R visitGraphMatch(GraphMatch node, C ctx) {
        return defaultVisit(node, ctx);
    }

    public R visitGraphPattern(GraphPattern node, C ctx) {
        return defaultVisit(node, ctx);
    }

    public R visitGraphPart(GraphPart node, C ctx) {
        return node.accept(this, ctx);
    }

    public R visitGraphPartNode(GraphPart.Node node, C ctx) {
        return defaultVisit(node, ctx);
    }

    public R visitGraphPartEdge(GraphPart.Edge node, C ctx) {
        return defaultVisit(node, ctx);
    }

    public R visitGraphPartPattern(GraphPart.Pattern node, C ctx) {
        return defaultVisit(node, ctx);
    }

    public R visitGraphQuantifier(GraphQuantifier node, C ctx) {
        return defaultVisit(node, ctx);
    }

    public R visitGraphSelector(GraphSelector node, C ctx) {
        return node.accept(this, ctx);
    }

    public R visitGraphSelectorAnyShortest(GraphSelector.AnyShortest node, C ctx) {
        return defaultVisit(node, ctx);
    }

    public R visitGraphSelectorAllShortest(GraphSelector.AllShortest node, C ctx) {
        return defaultVisit(node, ctx);
    }

    public R visitGraphSelectorAny(GraphSelector.Any node, C ctx) {
        return defaultVisit(node, ctx);
    }

    public R visitGraphSelectorAnyK(GraphSelector.AnyK node, C ctx) {
        return defaultVisit(node, ctx);
    }

    public R visitGraphSelectorShortestK(GraphSelector.ShortestK node, C ctx) {
        return defaultVisit(node, ctx);
    }

    public R visitGraphSelectorShortestKGroup(GraphSelector.ShortestKGroup node, C ctx) {
        return defaultVisit(node, ctx);
    }

    public R visitGraphLabel(GraphLabel node, C ctx) {
        return node.accept(this, ctx);
    }

    public R visitGraphLabelName(GraphLabel.Name node, C ctx) {
        return defaultVisit(node, ctx);
    }

    public R visitGraphLabelWildcard(GraphLabel.Wildcard node, C ctx) {
        return defaultVisit(node, ctx);
    }

    public R visitGraphLabelNegation(GraphLabel.Negation node, C ctx) {
        return defaultVisit(node, ctx);
    }

    public R visitGraphLabelConj(GraphLabel.Conj node, C ctx) {
        return defaultVisit(node, ctx);
    }

    public R visitGraphLabelDisj(GraphLabel.Disj node, C ctx) {
        return defaultVisit(node, ctx);
    }

    public R visitDataType(DataType node, C ctx) {
        return defaultVisit(node, ctx);
    }
}
