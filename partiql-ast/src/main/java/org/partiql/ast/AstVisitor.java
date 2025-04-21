package org.partiql.ast;

import org.partiql.ast.ddl.AttributeConstraint;
import org.partiql.ast.ddl.ColumnDefinition;
import org.partiql.ast.ddl.CreateTable;
import org.partiql.ast.ddl.KeyValue;
import org.partiql.ast.ddl.PartitionBy;
import org.partiql.ast.ddl.TableConstraint;
import org.partiql.ast.dml.ConflictAction;
import org.partiql.ast.dml.ConflictTarget;
import org.partiql.ast.dml.Delete;
import org.partiql.ast.dml.DoReplaceAction;
import org.partiql.ast.dml.DoUpdateAction;
import org.partiql.ast.dml.Insert;
import org.partiql.ast.dml.InsertSource;
import org.partiql.ast.dml.OnConflict;
import org.partiql.ast.dml.Replace;
import org.partiql.ast.dml.SetClause;
import org.partiql.ast.dml.Update;
import org.partiql.ast.dml.UpdateTarget;
import org.partiql.ast.dml.UpdateTargetStep;
import org.partiql.ast.dml.Upsert;
import org.partiql.ast.expr.Expr;
import org.partiql.ast.expr.ExprAnd;
import org.partiql.ast.expr.ExprArray;
import org.partiql.ast.expr.ExprBag;
import org.partiql.ast.expr.ExprBetween;
import org.partiql.ast.expr.ExprCall;
import org.partiql.ast.expr.ExprCase;
import org.partiql.ast.expr.ExprCast;
import org.partiql.ast.expr.ExprCoalesce;
import org.partiql.ast.expr.ExprExtract;
import org.partiql.ast.expr.ExprInCollection;
import org.partiql.ast.expr.ExprMissingPredicate;
import org.partiql.ast.expr.ExprNullPredicate;
import org.partiql.ast.expr.ExprBoolTest;
import org.partiql.ast.expr.ExprIsType;
import org.partiql.ast.expr.ExprLike;
import org.partiql.ast.expr.ExprLit;
import org.partiql.ast.expr.ExprMatch;
import org.partiql.ast.expr.ExprNot;
import org.partiql.ast.expr.ExprNullIf;
import org.partiql.ast.expr.ExprOperator;
import org.partiql.ast.expr.ExprOr;
import org.partiql.ast.expr.ExprOverlay;
import org.partiql.ast.expr.ExprParameter;
import org.partiql.ast.expr.ExprPath;
import org.partiql.ast.expr.ExprPosition;
import org.partiql.ast.expr.ExprQuerySet;
import org.partiql.ast.expr.ExprRowValue;
import org.partiql.ast.expr.ExprSessionAttribute;
import org.partiql.ast.expr.ExprStruct;
import org.partiql.ast.expr.ExprSubstring;
import org.partiql.ast.expr.ExprTrim;
import org.partiql.ast.expr.ExprValues;
import org.partiql.ast.expr.ExprVarRef;
import org.partiql.ast.expr.ExprVariant;
import org.partiql.ast.expr.ExprWindow;
import org.partiql.ast.expr.ExprWindowFunction;
import org.partiql.ast.expr.PathStep;
import org.partiql.ast.graph.GraphLabel;
import org.partiql.ast.graph.GraphMatch;
import org.partiql.ast.graph.GraphPart;
import org.partiql.ast.graph.GraphPattern;
import org.partiql.ast.graph.GraphQuantifier;
import org.partiql.ast.graph.GraphSelector;

/**
 * Visitor interface for AST nodes.
 * @param <R> return type of the visitor functions.
 * @param <C> context type of the visitor functions.
 */
public abstract class AstVisitor<R, C> {
    public R defaultVisit(AstNode node, C ctx) {
        for (AstNode child : node.getChildren()) {
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

    //
    // DDL
    //
    public R visitCreateTable(CreateTable node, C ctx) {
        return defaultVisit(node, ctx);
    }

    public R visitColumnDefinition(ColumnDefinition node, C ctx) {
        return defaultVisit(node, ctx);
    }

    public R visitTableConstraintUnique(TableConstraint.Unique node, C ctx) {
        return defaultVisit(node, ctx);
    }

    public R visitAttributeConstraintNull(AttributeConstraint.Null node, C ctx) {
        return defaultVisit(node, ctx);
    }

    public R visitKeyValue(KeyValue node, C ctx) {
        return defaultVisit(node, ctx);
    }

    public R visitPartitionBy(PartitionBy node, C ctx) {
        return defaultVisit(node, ctx);
    }

    public R visitAttributeConstraintUnique(AttributeConstraint.Unique node, C ctx) {
        return defaultVisit(node, ctx);
    }

    public R visitAttributeConstraintCheck(AttributeConstraint.Check node, C ctx) {
        return defaultVisit(node, ctx);
    }


    //
    // END OF DDL
    //

    public R visitQuery(Query node, C ctx) {
        return defaultVisit(node, ctx);
    }

    public R visitInsert(Insert node, C ctx) {
        return defaultVisit(node, ctx);
    }

    public R visitUpsert(Upsert node, C ctx) {
        return defaultVisit(node, ctx);
    }

    public R visitReplace(Replace node, C ctx) {
        return defaultVisit(node, ctx);
    }

    public R visitUpdate(Update node, C ctx) {
        return defaultVisit(node, ctx);
    }

    public R visitUpdateTarget(UpdateTarget node, C ctx) {
        return defaultVisit(node, ctx);
    }

    public R visitUpdateTargetStep(UpdateTargetStep node, C ctx) {
        return node.accept(this, ctx);
    }

    public R visitUpdateTargetStepElement(UpdateTargetStep.Element node, C ctx) {
        return defaultVisit(node, ctx);
    }

    public R visitUpdateTargetStepField(UpdateTargetStep.Field node, C ctx) {
        return defaultVisit(node, ctx);
    }

    public R visitDelete(Delete node, C ctx) {
        return defaultVisit(node, ctx);
    }

    public R visitSetClause(SetClause node, C ctx) {
        return defaultVisit(node, ctx);
    }

    public R visitInsertSource(InsertSource node, C ctx) {
        return node.accept(this, ctx);
    }

    public R visitInsertSourceFromExpr(InsertSource.FromExpr node, C ctx) {
        return defaultVisit(node, ctx);
    }

    public R visitInsertSourceFromDefault(InsertSource.FromDefault node, C ctx) {
        return defaultVisit(node, ctx);
    }

    public R visitOnConflict(OnConflict node, C ctx) {
        return defaultVisit(node, ctx);
    }

    public R visitConflictTarget(ConflictTarget node, C ctx) {
        return node.accept(this, ctx);
    }

    public R visitConflictTargetIndex(ConflictTarget.Index node, C ctx) {
        return defaultVisit(node, ctx);
    }

    public R visitConflictTargetConstraint(ConflictTarget.Constraint node, C ctx) {
        return defaultVisit(node, ctx);
    }

    public R visitConflictAction(ConflictAction node, C ctx) {
        return defaultVisit(node, ctx);
    }

    public R visitConflictActionDoNothing(ConflictAction.DoNothing node, C ctx) {
        return defaultVisit(node, ctx);
    }

    public R visitConflictActionDoReplace(ConflictAction.DoReplace node, C ctx) {
        return defaultVisit(node, ctx);
    }

    public R visitConflictActionDoUpdate(ConflictAction.DoUpdate node, C ctx) {
        return defaultVisit(node, ctx);
    }

    public R visitDoReplaceAction(DoReplaceAction node, C ctx) {
        return node.accept(this, ctx);
    }

    public R visitDoReplaceActionExcluded(DoReplaceAction.Excluded node, C ctx) {
        return defaultVisit(node, ctx);
    }

    public R visitDoUpdateAction(DoUpdateAction node, C ctx) {
        return node.accept(this, ctx);
    }

    public R visitDoUpdateActionExcluded(DoUpdateAction.Excluded node, C ctx) {
        return defaultVisit(node, ctx);
    }

    public R visitExplain(Explain node, C ctx) {
        return defaultVisit(node, ctx);
    }

    public R visitIdentifierSimple(Identifier.Simple node, C ctx) {
        return defaultVisit(node, ctx);
    }

    public R visitIdentifier(Identifier node, C ctx) {
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

    /**
     * TODO
     * @param node TODO
     * @param ctx TODO
     * @return TODO
     */
    public R visitExprWindowFunction(ExprWindowFunction node, C ctx) {
        return defaultVisit(node, ctx);
    }

    /**
     * TODO
     * @param node TODO
     * @param ctx TODO
     * @return TODO
     */
    public R visitWindowFunctionType(WindowFunctionType node, C ctx) {
        return node.accept(this, ctx);
    }

    /**
     * TODO
     * @param node TODO
     * @param ctx TODO
     * @return TODO
     */
    public R visitWindowFunctionTypeNoArg(WindowFunctionType.NoArg node, C ctx) {
        return defaultVisit(node, ctx);
    }

    /**
     * TODO
     * @param node TODO
     * @param ctx TODO
     * @return TODO
     */
    public R visitWindowReference(WindowReference node, C ctx) {
        return node.accept(this, ctx);
    }

    /**
     * TODO
     * @param node TODO
     * @param ctx TODO
     * @return TODO
     */
    public R visitWindowPartitionClause(WindowPartitionClause node, C ctx) {
        return node.accept(this, ctx);
    }

    /**
     * TODO
     * @param node TODO
     * @param ctx TODO
     * @return TODO
     */
    public R visitWindowOrderClause(WindowOrderClause node, C ctx) {
        return node.accept(this, ctx);
    }

    /**
     * TODO
     * @param node TODO
     * @param ctx TODO
     * @return TODO
     */
    public R visitWindowPartition(WindowPartition node, C ctx) {
        return node.accept(this, ctx);
    }

    /**
     * TODO
     * @param node TODO
     * @param ctx TODO
     * @return TODO
     */
    public R visitWindowPartitionName(WindowPartition.Name node, C ctx) {
        return defaultVisit(node, ctx);
    }

    /**
     * TODO
     * @param node TODO
     * @param ctx TODO
     * @return TODO
     */
    public R visitWindowReferenceName(WindowReference.Name node, C ctx) {
        return defaultVisit(node, ctx);
    }

    /**
     * TODO
     * @param node TODO
     * @param ctx TODO
     * @return TODO
     */
    public R visitWindowReferenceInLineSpecification(WindowReference.InLineSpecification node, C ctx) {
        return defaultVisit(node, ctx);
    }

    /**
     * TODO
     * @param node TODO
     * @param ctx TODO
     * @return TODO
     */
    public R visitWindowFunctionTypeLagOrLead(WindowFunctionType.LeadOrLag node, C ctx) {
        return defaultVisit(node, ctx);
    }

    /**
     * TODO
     * @param node TODO
     * @param ctx TODO
     * @return TODO
     */
    public R visitWindowSpecification(WindowSpecification node, C ctx) {
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

    public R visitExprNullPredicate(ExprNullPredicate node, C ctx) {
        return defaultVisit(node, ctx);
    }

    public R visitExprMissingPredicate(ExprMissingPredicate node, C ctx) {
        return defaultVisit(node, ctx);
    }

    public R visitExprBoolTest(ExprBoolTest node, C ctx) {
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

    public R visitLiteral(Literal node, C ctx) {
        return defaultVisit(node, ctx);
    }

    public R visitIntervalQualifier(IntervalQualifier node, C ctx) {
        return node.accept(this, ctx);
    }

    public R visitIntervalQualifierRange(IntervalQualifier.Range node, C ctx) {
        return defaultVisit(node, ctx);
    }

    public R visitIntervalQualifierSingle(IntervalQualifier.Single node, C ctx) {
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

    public R visitWindowClause(WindowClause node, C ctx) {
        return defaultVisit(node, ctx);
    }

    public R visitWindowDefinition(WindowClause.WindowDefinition node, C ctx) {
        return defaultVisit(node, ctx);
    }

    public R visitWith(With node, C ctx) {
        return defaultVisit(node, ctx);
    }

    public R visitWithListElement(WithListElement node, C ctx) {
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

    public R visitDataTypeStructField(DataType.StructField node, C ctx) {
        return defaultVisit(node, ctx);
    }
}
