package org.partiql.ast.v1;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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
import org.partiql.ast.v1.expr.ExprSessionAttribute;
import org.partiql.ast.v1.expr.ExprStruct;
import org.partiql.ast.v1.expr.ExprSubstring;
import org.partiql.ast.v1.expr.ExprTrim;
import org.partiql.ast.v1.expr.ExprValues;
import org.partiql.ast.v1.expr.ExprVarRef;
import org.partiql.ast.v1.expr.ExprVariant;
import org.partiql.ast.v1.expr.ExprWindow;
import org.partiql.ast.v1.expr.PathStep;
import org.partiql.ast.v1.expr.Scope;
import org.partiql.ast.v1.expr.SessionAttribute;
import org.partiql.ast.v1.expr.TrimSpec;
import org.partiql.ast.v1.expr.WindowFunction;
import org.partiql.ast.v1.graph.GraphDirection;
import org.partiql.ast.v1.graph.GraphLabel;
import org.partiql.ast.v1.graph.GraphMatch;
import org.partiql.ast.v1.graph.GraphPart;
import org.partiql.ast.v1.graph.GraphPattern;
import org.partiql.ast.v1.graph.GraphQuantifier;
import org.partiql.ast.v1.graph.GraphRestrictor;
import org.partiql.ast.v1.graph.GraphSelector;
import org.partiql.value.PartiQLValue;

import java.util.List;
import java.util.Map;

// TODO docs for all factory methods
public class Ast {
    // Expr
    @NotNull
    public static ExprAnd exprAnd(Expr lhs, Expr rhs) {
        return new ExprAnd(lhs, rhs);
    }

    @NotNull
    public static ExprArray exprArray(@NotNull List<Expr> values) {
        return new ExprArray(values);
    }

    @NotNull
    public static ExprBag exprBag(@NotNull List<Expr> values) {
        return new ExprBag(values);
    }

    @NotNull
    public static ExprBetween exprBetween(@NotNull Expr value, @NotNull Expr from, @NotNull Expr to, boolean not) {
        return new ExprBetween(value, from, to, not);
    }

    @NotNull
    public static ExprCall exprCall(@NotNull IdentifierChain function, @NotNull List<Expr> args, @Nullable SetQuantifier setq) {
        return new ExprCall(function, args, setq);
    }

    @NotNull
    public static ExprCase exprCase(@Nullable Expr expr, @NotNull List<ExprCase.Branch> branches, @Nullable Expr defaultExpr) {
        return new ExprCase(expr, branches, defaultExpr);
    }

    @NotNull
    public static ExprCase.Branch exprCaseBranch(@NotNull Expr condition, @NotNull Expr expr) {
        return new ExprCase.Branch(condition, expr);
    }

    @NotNull
    public static ExprCast exprCast(@NotNull Expr value, @NotNull DataType asType) {
        return new ExprCast(value, asType);
    }

    @NotNull
    public static ExprCoalesce exprCoalesce(@NotNull List<Expr> args) {
        return new ExprCoalesce(args);
    }

    @NotNull
    public static ExprExtract exprExtract(@NotNull DatetimeField field, @NotNull Expr source) {
        return new ExprExtract(field, source);
    }

    @NotNull
    public static ExprInCollection exprInCollection(@NotNull Expr lhs, @NotNull Expr rhs, boolean not) {
        return new ExprInCollection(lhs, rhs, not);
    }

    @NotNull
    public static ExprIsType exprIsType(@NotNull Expr value, @NotNull DataType type, boolean not) {
        return new ExprIsType(value, type, not);
    }

    @NotNull
    public static ExprLike exprLike(@NotNull Expr value, @NotNull Expr Pattern, @Nullable Expr escape, boolean not) {
        return new ExprLike(value, Pattern, escape, not);
    }

    // This representation will be changed in https://github.com/partiql/partiql-lang-kotlin/issues/1589
    @NotNull
    public static ExprLit exprLit(@NotNull PartiQLValue value) {
        return new ExprLit(value);
    }

    @NotNull
    public static ExprMatch exprMatch(@NotNull Expr expr, @NotNull GraphMatch pattern) {
        return new ExprMatch(expr, pattern);
    }

    @NotNull
    public static ExprNot exprNot(@NotNull Expr value) {
        return new ExprNot(value);
    }

    @NotNull
    public static ExprNullIf exprNullIf(@NotNull Expr v1, @NotNull Expr v2) {
        return new ExprNullIf(v1, v2);
    }

    @NotNull
    public static ExprOperator exprOperator(@NotNull String symbol, @Nullable Expr lhs, @NotNull Expr rhs) {
        return new ExprOperator(symbol, lhs, rhs);
    }

    @NotNull
    public static ExprOr exprOr(@NotNull Expr lhs, @NotNull Expr rhs) {
        return new ExprOr(lhs, rhs);
    }

    @NotNull
    public static ExprOverlay exprOverlay(@NotNull Expr value, @NotNull Expr placing, @NotNull Expr from, @Nullable Expr forLength) {
        return new ExprOverlay(value, placing, from, forLength);
    }

    @NotNull
    public static ExprParameter exprParameter(int index) {
        return new ExprParameter(index);
    }

    @NotNull
    public static ExprPath exprPath(@NotNull Expr root, @Nullable PathStep next) {
        return new ExprPath(root, next);
    }

    @NotNull
    public static ExprPosition exprPosition(@NotNull Expr lhs, @NotNull Expr rhs) {
        return new ExprPosition(lhs, rhs);
    }

    @NotNull
    public static ExprQuerySet exprQuerySet(@NotNull QueryBody body, @Nullable OrderBy orderBy, @Nullable Expr limit, @Nullable Expr offset) {
        return new ExprQuerySet(body, orderBy, limit, offset);
    }

    @NotNull
    public static ExprSessionAttribute exprSessionAttribute(@NotNull SessionAttribute sessionAttribute) {
        return new ExprSessionAttribute(sessionAttribute);
    }

    @NotNull
    public static ExprStruct exprStruct(@NotNull List<ExprStruct.Field> fields) {
        return new ExprStruct(fields);
    }

    @NotNull
    public static ExprStruct.Field exprStructField(@NotNull Expr name, @NotNull Expr value) {
        return new ExprStruct.Field(name, value);
    }

    @NotNull
    public static ExprSubstring exprSubstring(@NotNull Expr value, @Nullable Expr start, @Nullable Expr length) {
        return new ExprSubstring(value, start, length);
    }

    @NotNull
    public static ExprTrim exprTrim(@NotNull Expr Value, @Nullable Expr chars, @Nullable TrimSpec trimSpec) {
        return new ExprTrim(Value, chars, trimSpec);
    }

    @NotNull
    public static ExprValues exprValues(@NotNull List<ExprValues.Row> rows) {
        return new ExprValues(rows);
    }

    @NotNull
    public static ExprValues.Row exprValuesRow(@NotNull List<Expr> values) {
        return new ExprValues.Row(values);
    }

    @NotNull
    public static ExprVariant exprVariant(@NotNull String value, @NotNull String encoding) {
        return new ExprVariant(value, encoding);
    }

    @NotNull
    public static ExprVarRef exprVarRef(@NotNull IdentifierChain identifierChain, @NotNull Scope scope) {
        return new ExprVarRef(identifierChain, scope);
    }

    @NotNull
    public static ExprWindow exprWindow(@NotNull WindowFunction windowFunction, @NotNull Expr exression, @Nullable Expr offset, @Nullable Expr defaultValue, @NotNull ExprWindow.Over over) {
        return new ExprWindow(windowFunction, exression, offset, defaultValue, over);
    }

    @NotNull
    public static ExprWindow.Over exprWindowOver(@Nullable List<Expr> partitions, @Nullable List<Sort> sorts) {
        return new ExprWindow.Over(partitions, sorts);
    }

    @NotNull
    public static PathStep.Field exprPathStepField(@NotNull Identifier value, @Nullable PathStep next) {
        return new PathStep.Field(value, next);
    }

    @NotNull
    public static PathStep.Element exprPathStepElement(@NotNull Expr element, @Nullable PathStep next) {
        return new PathStep.Element(element, next);
    }

    @NotNull
    public static PathStep.AllElements exprPathStepAllElements(@Nullable PathStep next) {
        return new PathStep.AllElements(next);
    }

    @NotNull
    public static PathStep.AllFields exprPathStepAllFields(@Nullable PathStep next) {
        return new PathStep.AllFields(next);
    }

    // Graph
    @NotNull
    public static GraphLabel.Name graphLabelName(@NotNull String name) {
        return new GraphLabel.Name(name);
    }

    @NotNull
    public static GraphLabel.Wildcard graphLabelWildcard() {
        return new GraphLabel.Wildcard();
    }

    @NotNull
    public static GraphLabel.Negation graphLabelNegation(@NotNull GraphLabel arg) {
        return new GraphLabel.Negation(arg);
    }

    @NotNull
    public static GraphLabel.Conj graphLabelConj(@NotNull GraphLabel lhs, @NotNull GraphLabel rhs) {
        return new GraphLabel.Conj(lhs, rhs);
    }

    @NotNull
    public static GraphLabel.Disj graphLabelDisj(@NotNull GraphLabel lhs, @NotNull GraphLabel rhs) {
        return new GraphLabel.Disj(lhs, rhs);
    }

    @NotNull
    public static GraphMatch graphMatch(@NotNull List<GraphPattern> patterns, @Nullable GraphSelector selector) {
        return new GraphMatch(patterns, selector);
    }

    @NotNull
    public static GraphPart.Node graphMatchNode(@Nullable Expr prefilter, @Nullable String variable, @Nullable GraphLabel label) {
        return new GraphPart.Node(prefilter, variable, label);
    }

    @NotNull
    public static GraphPart.Edge graphMatchEdge(@NotNull GraphDirection direction, @Nullable GraphQuantifier quantifier, @Nullable Expr prefilter, @Nullable String variable, @Nullable GraphLabel label) {
        return new GraphPart.Edge(direction, quantifier, prefilter, variable, label);
    }

    @NotNull
    public static GraphPart.Pattern graphMatchPattern(@NotNull GraphPattern pattern) {
        return new GraphPart.Pattern(pattern);
    }

    @NotNull
    public static GraphPattern graphPattern(@Nullable GraphRestrictor restrictor, @Nullable Expr prefilter, @Nullable String variable, @Nullable GraphQuantifier quantifier, @NotNull List<GraphPart> parts) {
        return new GraphPattern(restrictor, prefilter, variable, quantifier, parts);
    }

    @NotNull
    public static GraphQuantifier graphQuantifier(long lower, @Nullable Long upper) {
        return new GraphQuantifier(lower, upper);
    }

    @NotNull
    public static GraphSelector.AnyShortest graphSelectorAnyShortest() {
        return new GraphSelector.AnyShortest();
    }

    @NotNull
    public static GraphSelector.AllShortest graphSelectorAllShortest() {
        return new GraphSelector.AllShortest();
    }

    @NotNull
    public static GraphSelector.Any graphSelectorAny() {
        return new GraphSelector.Any();
    }

    @NotNull
    public static GraphSelector.AnyK graphSelectorAnyK(long k) {
        return new GraphSelector.AnyK(k);
    }

    @NotNull
    public static GraphSelector.ShortestK graphSelectorShortestK(long k) {
        return new GraphSelector.ShortestK(k);
    }

    @NotNull
    public static GraphSelector.ShortestKGroup graphSelectorShortestKGroup(long k) {
        return new GraphSelector.ShortestKGroup(k);
    }

    // Other
    @NotNull
    public static Exclude exclude(@NotNull List<ExcludePath> excludePaths) {
        return new Exclude(excludePaths);
    }

    @NotNull
    public static ExcludePath excludePath(@NotNull ExprVarRef varRef, @NotNull List<ExcludeStep> excludeSteps) {
        return new ExcludePath(varRef, excludeSteps);
    }

    @NotNull
    public static ExcludeStep.StructField excludeStepStructField(@NotNull Identifier symbol) {
        return new ExcludeStep.StructField(symbol);
    }

    @NotNull
    public static ExcludeStep.CollIndex excludeStepCollIndex(int index) {
        return new ExcludeStep.CollIndex(index);
    }

    @NotNull
    public static ExcludeStep.StructWildcard excludeStepStructWildcard() {
        return new ExcludeStep.StructWildcard();
    }

    @NotNull
    public static ExcludeStep.CollWildcard excludeStepCollWildcard() {
        return new ExcludeStep.CollWildcard();
    }

    // This representation will be changed in https://github.com/partiql/partiql-lang-kotlin/issues/1589
    @NotNull
    public static Explain explain(@NotNull Map<String, PartiQLValue> options, @NotNull Statement statement) {
        return new Explain(options, statement);
    }

    @NotNull
    public static From from(@NotNull List<FromTableRef> tableRefs) {
        return new From(tableRefs);
    }

    @NotNull
    public static FromExpr fromExpr(@NotNull Expr expr, @NotNull FromType fromType, @Nullable Identifier asAlias, @Nullable Identifier atAlias) {
        return new FromExpr(expr, fromType, asAlias, atAlias);
    }

    @NotNull
    public static FromJoin fromJoin(@NotNull From lhs, @NotNull From rhs, @Nullable JoinType joinType, @Nullable Expr condition) {
        return new FromJoin(lhs, rhs, joinType, condition);
    }

    @NotNull
    public static GroupBy groupBy(@NotNull GroupByStrategy strategy, @NotNull List<GroupBy.Key> keys, @Nullable Identifier asAlias) {
        return new GroupBy(strategy, keys, asAlias);
    }

    @NotNull
    public static GroupBy.Key groupByKey(@NotNull Expr expr, @Nullable Identifier asAlias) {
        return new GroupBy.Key(expr, asAlias);
    }

    @NotNull
    public static Identifier identifier(@NotNull String symbol, boolean isDelimited) {
        return new Identifier(symbol, isDelimited);
    }

    @NotNull
    public static IdentifierChain identifierChain(@NotNull Identifier root, @Nullable IdentifierChain next) {
        return new IdentifierChain(root, next);
    }

    @NotNull
    public static Let let(@NotNull List<Let.Binding> bindings) {
        return new Let(bindings);
    }

    @NotNull
    public static Let.Binding letBinding(@NotNull Expr expr, @NotNull Identifier asAlias) {
        return new Let.Binding(expr, asAlias);
    }

    @NotNull
    public static OrderBy orderBy(@NotNull List<Sort> sorts) {
        return new OrderBy(sorts);
    }

    @NotNull
    public static Query query(@NotNull Expr expr) {
        return new Query(expr);
    }

    @NotNull
    public static QueryBody.SFW queryBodySFW(@NotNull Select select, @Nullable Exclude exclude, @NotNull From from, @Nullable Let let, @Nullable Expr where, @Nullable GroupBy groupBy, @Nullable Expr having) {
        return new QueryBody.SFW(select, exclude, from, let, where, groupBy, having);
    }

    @NotNull
    public static QueryBody.SetOp queryBodySetOp(@NotNull org.partiql.ast.v1.SetOp type, boolean isOuter, @NotNull Expr lhs, @NotNull Expr rhs) {
        return new QueryBody.SetOp(type, isOuter, lhs, rhs);
    }

    @NotNull
    public static SelectItem.Star selectItemStar(@NotNull Expr expr) {
        return new SelectItem.Star(expr);
    }

    @NotNull
    public static SelectItem.Expr selectItemExpr(@NotNull Expr expr, @Nullable Identifier asAlias) {
        return new SelectItem.Expr(expr, asAlias);
    }

    @NotNull
    public static SelectList selectList(@NotNull List<SelectItem> items, @Nullable SetQuantifier setq) {
        return new SelectList(items, setq);
    }

    @NotNull
    public static SelectPivot selectPivot(@NotNull Expr key, @NotNull Expr value) {
        return new SelectPivot(key, value);
    }

    @NotNull
    public static SelectStar selectStar(@Nullable SetQuantifier setq) {
        return new SelectStar(setq);
    }

    @NotNull
    public static SelectValue selectValue(@NotNull Expr constructor, @Nullable SetQuantifier setq) {
        return new SelectValue(constructor, setq);
    }

    @NotNull
    public static SetOp setOp(@NotNull SetOpType setOpType, @Nullable SetQuantifier setq) {
        return new SetOp(setOpType, setq);
    }

    @NotNull
    public static Sort sort(@NotNull Expr expr, @Nullable Order order, @Nullable Nulls nulls) {
        return new Sort(expr, order, nulls);
    }
}
