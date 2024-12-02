package org.partiql.plan;

import org.jetbrains.annotations.NotNull;
import org.partiql.plan.rel.*;
import org.partiql.plan.rel.RelAggregate.Measure;
import org.partiql.plan.rex.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Operator transform is an abstract base visitor which recursively rewrites an operator tree.
 * <br>
 * Developer Note
 * -
 * - use the visitAll for rewriting collections as it's more efficient and won't trigger rewrites.
 */
public abstract class OperatorTransform<C> implements OperatorVisitor<Operator, C> {

    /**
     * Operator factory to use for rewrites.
     */
    private final Operators operators;

    /**
     * Base operator transform with standard operators factory.
     */
    public OperatorTransform() {
        this.operators = Operators.STANDARD;
    }

    /**
     * Base operator transform with custom operators factory.
     */
    public OperatorTransform(Operators operators) {
        this.operators = operators;
    }

    @NotNull
    @Override
    public Operator defaultReturn(@NotNull Operator operator, C ctx) {
        return operator;
    }

    @NotNull
    @Override
    public Operator defaultVisit(Operator operator, C ctx) {
        return operator;
    }

    @NotNull
    @Override
    public Operator visitAggregate(@NotNull RelAggregate rel, C ctx) {
        // rewrite input
        Rel input = rel.getInput();
        Rel input_new = visit(input, ctx, Rel.class);
        // rewrite measures
        List<Measure> measures = rel.getMeasures();
        List<Measure> measures_new = visitAll(measures, ctx, this::visitAggregateMeasure);
        // rewrite groups
        List<Rex> groups = rel.getGroups();
        List<Rex> groups_new = visitAll(groups, ctx, this::visitAggregateGroup);
        // rewrite aggregate
        if (input != input_new || measures != measures_new || groups != groups_new) {
            return operators.aggregate(input_new, measures_new, groups_new);
        }
        return rel;
    }

    @NotNull
    public Measure visitAggregateMeasure(@NotNull Measure measure, C ctx) {
        // rewrite args
        List<Rex> args = measure.getArgs();
        List<Rex> args_new = visitAll(args, ctx, this::visitAggregateGroup);
        // rewrite aggregate measure
        if (args != args_new) {
            return measure.copy(args_new);
        }
        return measure;
    }

    @NotNull
    public Rex visitAggregateGroup(@NotNull Rex rex, C ctx) {
        // rewrite aggregate group
        return visit(rex, ctx, Rex.class);
    }

    @Override
    public Operator visitCorrelate(@NotNull RelCorrelate rel, C ctx) {
        // rewrite left
        Rel left = rel.getLeft();
        Rel left_new = visit(left, ctx, Rel.class);
        // rewrite right
        Rel right = rel.getRight();
        Rel right_new = visit(right, ctx, Rel.class);
        // rewrite correlate
        if (left != left_new || right != right_new) {
            return operators.correlate(left_new, right_new, rel.getJoinType());
        }
        return rel;
    }

    @NotNull
    @Override
    public Operator visitDistinct(@NotNull RelDistinct rel, C ctx) {
        // rewrite input
        Rel input = rel.getInput();
        Rel input_new = visit(input, ctx, Rel.class);
        // rewrite distinct
        if (input != input_new) {
            return operators.distinct(input_new);
        }
        return rel;
    }

    @NotNull
    @Override
    public Operator visitExcept(@NotNull RelExcept rel, C ctx) {
        // rewrite left
        Rel left = rel.getLeft();
        Rel left_new = visit(left, ctx, Rel.class);
        // rewrite right
        Rel right = rel.getRight();
        Rel right_new = visit(right, ctx, Rel.class);
        // rewrite except
        if (left != left_new || right != right_new) {
            return operators.except(left_new, right_new, rel.isAll());
        }
        return rel;
    }

    @NotNull
    @Override
    public Operator visitExclude(@NotNull RelExclude rel, C ctx) {
        // rewrite input
        Rel input = rel.getInput();
        Rel input_new = visit(input, ctx, Rel.class);
        // rewrite exclusions
        List<Exclusion> exclusions = rel.getExclusions();
        List<Exclusion> exclusions_new = visitAll(exclusions, ctx, this::visitExclusions);
        // rewrite exclude
        if (input != input_new) {
            return operators.exclude(input_new, exclusions_new);
        }
        return rel;
    }

    @NotNull
    public Exclusion visitExclusions(@NotNull Exclusion exclusion, C ctx) {
        // rewrite exclusion
        return exclusion;
    }

    @Override
    public Operator visitFilter(@NotNull RelFilter rel, C ctx) {
        // rewrite input
        Rel input = rel.getInput();
        Rel input_new = visit(input, ctx, Rel.class);
        // rewrite predicate
        Rex predicate = rel.getPredicate();
        Rex predicate_new = visit(predicate, ctx, Rex.class);
        // rewrite filter
        if (input != input_new || predicate != predicate_new) {
            return operators.filter(input_new, predicate_new);
        }
        return rel;
    }

    @Override
    public Operator visitIntersect(@NotNull RelIntersect rel, C ctx) {
        // rewrite left
        Rel left = rel.getLeft();
        Rel left_new = visit(left, ctx, Rel.class);
        // rewrite right
        Rel right = rel.getRight();
        Rel right_new = visit(right, ctx, Rel.class);
        // rewrite intersect
        if (left != left_new || right != right_new) {
            return operators.intersect(left_new, right_new, rel.isAll());
        }
        return rel;
    }

    @Override
    public Operator visitIterate(@NotNull RelIterate rel, C ctx) {
        // rewrite rex
        Rex rex = rel.getRex();
        Rex rex_new = visit(rex, ctx, Rex.class);
        // rewrite iterate
        if (rex != rex_new) {
            return operators.iterate(rex_new);
        }
        return rel;
    }

    @Override
    public Operator visitJoin(@NotNull RelJoin rel, C ctx) {
        // rewrite left
        Rel left = rel.getLeft();
        Rel left_new = visit(left, ctx, Rel.class);
        // rewrite right
        Rel right = rel.getRight();
        Rel right_new = visit(right, ctx, Rel.class);
        // rewrite condition
        Rex condition = rel.getCondition();
        Rex condition_new = visit(condition, ctx, Rex.class);
        // rewrite join
        if (left != left_new || right != right_new || condition != condition_new) {
            return operators.join(left_new, right_new, condition_new, rel.getJoinType());
        }
        return rel;
    }

    @Override
    public Operator visitLimit(@NotNull RelLimit rel, C ctx) {
        // rewrite input
        Rel input = rel.getInput();
        Rel input_new = visit(input, ctx, Rel.class);
        // rewrite limit
        Rex limit = rel.getLimit();
        Rex limit_new = visit(limit, ctx, Rex.class);
        // rewrite limit
        if (input != input_new || limit != limit_new) {
            return operators.limit(input_new, limit_new);
        }
        return rel;
    }

    @Override
    public Operator visitOffset(@NotNull RelOffset rel, C ctx) {
        // rewrite input
        Rel input = rel.getInput();
        Rel input_new = visit(input, ctx, Rel.class);
        // rewrite offset
        Rex offset = rel.getOffset();
        Rex offset_new = visit(offset, ctx, Rex.class);
        // rewrite offset
        if (input != input_new || offset != offset_new) {
            return operators.offset(input_new, offset_new);
        }
        return rel;
    }

    @Override
    public Operator visitProject(@NotNull RelProject rel, C ctx) {
        // rewrite input
        Rel input = rel.getInput();
        Rel input_new = visit(input, ctx, Rel.class);
        // rewrite projections
        List<Rex> projections = rel.getProjections();
        List<Rex> projects_new = visitAll(projections, ctx, this::visitProjection);
        // rewrite projection
        if (input != input_new || projections != projects_new) {
            return operators.project(input_new, projects_new);
        }
        return rel;
    }

    @NotNull
    public Rex visitProjection(@NotNull Rex rex, C ctx) {
        // rewrite projection
        return visit(rex, ctx, Rex.class);
    }

    @Override
    public Operator visitScan(@NotNull RelScan rel, C ctx) {
        // rewrite rex
        Rex rex = rel.getRex();
        Rex rex_new = visit(rex, ctx, Rex.class);
        // rewrite scan
        if (rex != rex_new) {
            return operators.scan(rex_new);
        }
        return rel;
    }

    @Override
    public Operator visitSort(@NotNull RelSort rel, C ctx) {
        // rewrite input
        Rel input = rel.getInput();
        Rel input_new = visit(input, ctx, Rel.class);
        // rewrite collations
        List<Collation> collations = rel.getCollations();
        List<Collation> collations_new = visitAll(collations, ctx, this::visitCollation);
        // rewrite sort
        if (input != input_new || collations != collations_new) {
            return operators.sort(input_new, collations_new);
        }
        return rel;
    }

    @NotNull
    public Collation visitCollation(@NotNull Collation collation, C ctx) {
        // rewrite collation
        return collation;
    }

    @Override
    public Operator visitUnion(@NotNull RelUnion rel, C ctx) {
        // rewrite left
        Rel left = rel.getLeft();
        Rel left_new = visit(left, ctx, Rel.class);
        // rewrite right
        Rel right = rel.getRight();
        Rel right_new = visit(right, ctx, Rel.class);
        // rewrite union
        if (left != left_new || right != right_new) {
            return operators.union(left_new, right_new, rel.isAll());
        }
        return rel;
    }

    @Override
    public Operator visitUnpivot(@NotNull RelUnpivot rel, C ctx) {
        Rex rex = rel.getRex();
        Rex rex_new = visit(rex, ctx, Rex.class);
        if (rex != rex_new) {
            return operators.unpivot(rex);
        }
        return rel;
    }

    @Override
    public Operator visitArray(@NotNull RexArray rex, C ctx) {
        return OperatorVisitor.super.visitArray(rex, ctx);
    }

    @Override
    public Operator visitBag(@NotNull RexBag rex, C ctx) {
        return OperatorVisitor.super.visitBag(rex, ctx);
    }

    @Override
    public Operator visitCall(@NotNull RexCall rex, C ctx) {
        return OperatorVisitor.super.visitCall(rex, ctx);
    }

    @Override
    public Operator visitCallDynamic(@NotNull RexDispatch rex, C ctx) {
        return OperatorVisitor.super.visitCallDynamic(rex, ctx);
    }

    @Override
    public Operator visitCase(@NotNull RexCase rex, C ctx) {
        return OperatorVisitor.super.visitCase(rex, ctx);
    }

    @Override
    public Operator visitCast(@NotNull RexCast rex, C ctx) {
        return OperatorVisitor.super.visitCast(rex, ctx);
    }

    @Override
    public Operator visitCoalesce(@NotNull RexCoalesce rex, C ctx) {
        return OperatorVisitor.super.visitCoalesce(rex, ctx);
    }

    @Override
    public Operator visitError(@NotNull RexError rex, C ctx) {
        return OperatorVisitor.super.visitError(rex, ctx);
    }

    @Override
    public Operator visitLit(@NotNull RexLit rex, C ctx) {
        return OperatorVisitor.super.visitLit(rex, ctx);
    }

    @Override
    public Operator visitNullIf(@NotNull RexNullIf rex, C ctx) {
        return OperatorVisitor.super.visitNullIf(rex, ctx);
    }

    @Override
    public Operator visitPathIndex(@NotNull RexPathIndex rex, C ctx) {
        return OperatorVisitor.super.visitPathIndex(rex, ctx);
    }

    @Override
    public Operator visitPathKey(@NotNull RexPathKey rex, C ctx) {
        return OperatorVisitor.super.visitPathKey(rex, ctx);
    }

    @Override
    public Operator visitPathSymbol(@NotNull RexPathSymbol rex, C ctx) {
        return OperatorVisitor.super.visitPathSymbol(rex, ctx);
    }

    @Override
    public Operator visitPivot(@NotNull RexPivot rex, C ctx) {
        return OperatorVisitor.super.visitPivot(rex, ctx);
    }

    @Override
    public Operator visitSelect(@NotNull RexSelect rex, C ctx) {
        return OperatorVisitor.super.visitSelect(rex, ctx);
    }

    @Override
    public Operator visitStruct(@NotNull RexStruct rex, C ctx) {
        return OperatorVisitor.super.visitStruct(rex, ctx);
    }

    @Override
    public Operator visitSubquery(@NotNull RexSubquery rex, C ctx) {
        return OperatorVisitor.super.visitSubquery(rex, ctx);
    }

    @Override
    public Operator visitSubqueryComp(@NotNull RexSubqueryComp rex, C ctx) {
        return OperatorVisitor.super.visitSubqueryComp(rex, ctx);
    }

    @Override
    public Operator visitSubqueryIn(@NotNull RexSubqueryIn rex, C ctx) {
        return OperatorVisitor.super.visitSubqueryIn(rex, ctx);
    }

    @Override
    public Operator visitSubqueryTest(@NotNull RexSubqueryTest rex, C ctx) {
        return OperatorVisitor.super.visitSubqueryTest(rex, ctx);
    }

    @Override
    public Operator visitSpread(@NotNull RexSpread rex, C ctx) {
        return OperatorVisitor.super.visitSpread(rex, ctx);
    }

    @Override
    public Operator visitTable(@NotNull RexTable rex, C ctx) {
        return OperatorVisitor.super.visitTable(rex, ctx);
    }

    @Override
    public Operator visitVar(@NotNull RexVar rex, C ctx) {
        return OperatorVisitor.super.visitVar(rex, ctx);
    }

    /**
     * Helper method to visit an operator and cast to the expected type.
     */
    public final <T extends Operator> T visit(@NotNull Operator operator, C ctx, Class<T> clazz) {
        Operator o = visit(operator, ctx);
        if (clazz.isInstance(o)) {
            return clazz.cast(o);
        }
        return onError(o, clazz);
    }

    /**
     * Helper method to visit a list of operators and return a new list if any operator was rewritten.
     * Using this will drastically reduce rebuilds since a new list is created ONLY IF necessary.
     * Doing .stream().map().collect() will always create a new list, even if no operators were rewritten.
     */
    public final <T> List<T> visitAll(List<T> objects, C ctx, Mapper<T, C> mapper) {
        if (objects.isEmpty()) {
            return objects;
        }
        boolean diff = false;
        List<T> mapped = new ArrayList<>(objects.size());
        for (T o : objects) {
            T t = mapper.apply(o, ctx);
            mapped.add(t);
            diff |= o != t;
        }
        return diff ? mapped : objects;
    }

    /**
     * Default error handling throws a ClassCastException.
     */
    public <T extends Operator> T onError(Operator o, Class<T> clazz) {
        throw new ClassCastException("OperatorTransform expected " + clazz.getName() + ", found: " + o.getClass().getName());
    }

    /**
     * @param <T>
     * @param <C>
     */
    public interface Mapper<T, C> {
        T apply(T op, C ctx);
    }
}
