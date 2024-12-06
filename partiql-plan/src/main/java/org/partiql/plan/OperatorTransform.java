package org.partiql.plan;

import org.jetbrains.annotations.NotNull;
import org.partiql.plan.rel.*;
import org.partiql.plan.rel.RelAggregate.Measure;
import org.partiql.plan.rex.*;
import org.partiql.plan.rex.RexCase.Branch;
import org.partiql.plan.rex.RexStruct.Field;

import java.util.ArrayList;
import java.util.List;

/**
 * Operator transform is an abstract base visitor which recursively rewrites an operator tree.
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
        // rewrite values
        List<Rex> values = rex.getValues();
        List<Rex> values_new = visitAll(values, ctx, this::visitRex);
        // rewrite array
        if (values != values_new) {
            return operators.array(values_new);
        }
        return rex;
    }

    @Override
    public Operator visitBag(@NotNull RexBag rex, C ctx) {
        // rewrite values (necessarily ascribes order)
        List<Rex> values = List.copyOf(rex.getValues());
        List<Rex> values_new = visitAll(values, ctx, this::visitRex);
        // rewrite bag
        if (values != values_new) {
            return operators.bag(values_new);
        }
        return rex;
    }

    @Override
    public Operator visitCall(@NotNull RexCall rex, C ctx) {
        // rewrite args
        List<Rex> args = rex.getArgs();
        List<Rex> args_new = visitAll(args, ctx, this::visitRex);
        // rewrite call
        if (args != args_new) {
            return operators.call(rex.getFunction(), args_new);
        }
        return rex;
    }

    @Override
    public Operator visitCase(@NotNull RexCase rex, C ctx) {
        // rewrite match
        Rex match = rex.getMatch();
        Rex match_new = (match != null) ? visit(match, ctx, Rex.class) : null;
        // rewrite branches
        List<Branch> branches = rex.getBranches();
        List<Branch> branches_new = visitAll(branches, ctx, this::visitCaseBranch);
        // rewrite default
        Rex default_ = rex.getDefault();
        Rex default_new = (default_ != null) ? visit(default_, ctx, Rex.class) : null;
        // rewrite case
        if (match != match_new || branches != branches_new || default_ != default_new) {
            return operators.caseWhen(match_new, branches_new, default_new);
        }
        return rex;
    }

    @NotNull
    public Branch visitCaseBranch(@NotNull Branch branch, C ctx) {
        // rewrite condition
        Rex condition = branch.getCondition();
        Rex condition_new = visit(condition, ctx, Rex.class);
        // rewrite result
        Rex result = branch.getResult();
        Rex result_new = visit(result, ctx, Rex.class);
        // rewrite branch
        if (condition != condition_new || result != result_new) {
            return RexCase.branch(condition_new, result_new);
        }
        return branch;
    }

    @Override
    public Operator visitCast(@NotNull RexCast rex, C ctx) {
        // rewrite operand
        Rex operand = rex.getOperand();
        Rex operand_new = visit(operand, ctx, Rex.class);
        // rewrite cast
        if (operand != operand_new) {
            return operators.cast(operand_new, rex.getTarget());
        }
        return rex;
    }

    @Override
    public Operator visitCoalesce(@NotNull RexCoalesce rex, C ctx) {
        // rewrite args
        List<Rex> args = rex.getArgs();
        List<Rex> args_new = visitAll(args, ctx, this::visitRex);
        // rewrite coalesce
        if (args != args_new) {
            return operators.coalesce(args_new);
        }
        return rex;
    }

    @Override
    public Operator visitDispatch(@NotNull RexDispatch rex, C ctx) {
        // rewrite args
        List<Rex> args = rex.getArgs();
        List<Rex> args_new = visitAll(args, ctx, this::visitRex);
        // rewrite dispatch
        if (args != args_new) {
            return operators.dispatch(rex.getName(), rex.getFunctions(), args_new);
        }
        return rex;
    }

    @Override
    public Operator visitError(@NotNull RexError rex, C ctx) {
        return rex;
    }

    @Override
    public Operator visitLit(@NotNull RexLit rex, C ctx) {
        return rex;
    }

    @Override
    public Operator visitNullIf(@NotNull RexNullIf rex, C ctx) {
        // rewrite v1
        Rex v1 = rex.getV1();
        Rex v1_new = visit(v1, ctx, Rex.class);
        // rewrite v2
        Rex v2 = rex.getV2();
        Rex v2_new = visit(v2, ctx, Rex.class);
        // rewrite nullif
        if (v1 != v1_new || v2 != v2_new) {
            return operators.nullIf(v1_new, v2_new);
        }
        return rex;
    }

    @Override
    public Operator visitPathIndex(@NotNull RexPathIndex rex, C ctx) {
        // rewrite operand
        Rex operand = rex.getOperand();
        Rex operand_new = visit(operand, ctx, Rex.class);
        // rewrite index
        Rex index = rex.getIndex();
        Rex index_new = visit(index, ctx, Rex.class);
        // rewrite path index
        if (operand != operand_new || index != index_new) {
            return operators.pathIndex(operand_new, index_new);
        }
        return rex;
    }

    @Override
    public Operator visitPathKey(@NotNull RexPathKey rex, C ctx) {
        // rewrite operand
        Rex operand = rex.getOperand();
        Rex operand_new = visit(operand, ctx, Rex.class);
        // rewrite key
        Rex key = rex.getKey();
        Rex key_new = visit(key, ctx, Rex.class);
        // rewrite path key
        if (operand != operand_new || key != key_new) {
            return operators.pathKey(operand_new, key_new);
        }
        return rex;
    }

    @Override
    public Operator visitPathSymbol(@NotNull RexPathSymbol rex, C ctx) {
        // rewrite operand
        Rex operand = rex.getOperand();
        Rex operand_new = visit(operand, ctx, Rex.class);
        // rewrite path symbol
        if (operand != operand_new) {
            return operators.pathSymbol(operand_new, rex.getSymbol());
        }
        return rex;
    }

    @Override
    public Operator visitPivot(@NotNull RexPivot rex, C ctx) {
        // rewrite input
        Rel input = rex.getInput();
        Rel input_new = visit(input, ctx, Rel.class);
        // rewrite key
        Rex key = rex.getKey();
        Rex key_new = visit(key, ctx, Rex.class);
        // rewrite value
        Rex value = rex.getValue();
        Rex value_new = visit(value, ctx, Rex.class);
        // rewrite pivot
        if (input != input_new || key != key_new || value != value_new) {
            return operators.pivot(input_new, key_new, value_new);
        }
        return rex;
    }

    @Override
    public Operator visitSelect(@NotNull RexSelect rex, C ctx) {
        // rewrite input
        Rel input = rex.getInput();
        Rel input_new = visit(input, ctx, Rel.class);
        // rewrite constructor
        Rex constructor = rex.getConstructor();
        Rex constructor_new = visit(constructor, ctx, Rex.class);
        // rewrite select
        if (input != input_new || constructor != constructor_new) {
            return operators.select(input_new, constructor_new);
        }
        return rex;
    }

    @Override
    public Operator visitStruct(@NotNull RexStruct rex, C ctx) {
        // rewrite fields
        List<Field> fields = rex.getFields();
        List<Field> fields_new = visitAll(fields, ctx, this::visitStructField);
        // rewrite struct
        if (fields != fields_new) {
            return operators.struct(fields_new);
        }
        return rex;
    }

    @NotNull
    public Field visitStructField(@NotNull Field field, C ctx) {
        // rewrite key
        Rex key = field.getKey();
        Rex key_new = visit(key, ctx, Rex.class);
        // rewrite value
        Rex value = field.getValue();
        Rex value_new = visit(value, ctx, Rex.class);
        // rewrite field
        if (key != key_new || value != value_new) {
            return RexStruct.field(key_new, value_new);
        }
        return field;
    }

    @Override
    public Operator visitSubquery(@NotNull RexSubquery rex, C ctx) {
        // rewrite input
        Rel input = rex.getInput();
        Rel input_new = visit(input, ctx, Rel.class);
        // rewrite constructor
        Rex constructor = rex.getConstructor();
        Rex constructor_new = visit(constructor, ctx, Rex.class);
        // rewrite subquery
        if (input != input_new || constructor != constructor_new) {
            return operators.subquery(input_new, constructor_new, rex.isScalar());
        }
        return rex;
    }

    @Override
    public Operator visitSubqueryComp(@NotNull RexSubqueryComp rex, C ctx) {
        // rewrite input
        Rel input = rex.getInput();
        Rel input_new = visit(input, ctx, Rel.class);
        // rewrite args
        List<Rex> args = rex.getArgs();
        List<Rex> args_new = visitAll(args, ctx, this::visitRex);
        // rewrite subquery comp
        if (input != input_new) {
            return operators.subqueryComp(input_new, args_new, rex.getComparison(), rex.getQuantifier());
        }
        return rex;
    }

    @Override
    public Operator visitSubqueryIn(@NotNull RexSubqueryIn rex, C ctx) {
        // rewrite input
        Rel input = rex.getInput();
        Rel input_new = visit(input, ctx, Rel.class);
        // rewrite args
        List<Rex> args = rex.getArgs();
        List<Rex> args_new = visitAll(args, ctx, this::visitRex);
        // rewrite subquery in
        if (input != input_new) {
            return operators.subqueryIn(input_new, args_new);
        }
        return rex;
    }

    @Override
    public Operator visitSubqueryTest(@NotNull RexSubqueryTest rex, C ctx) {
        // rewrite input
        Rel input = rex.getInput();
        Rel input_new = visit(input, ctx, Rel.class);
        // rewrite subquery test
        if (input != input_new) {
            return operators.subqueryTest(input_new, rex.getTest());
        }
        return rex;
    }

    @Override
    public Operator visitSpread(@NotNull RexSpread rex, C ctx) {
        // rewrite args
        List<Rex> args = rex.getArgs();
        List<Rex> args_new = visitAll(args, ctx, this::visitRex);
        // rewrite spread
        if (args != args_new) {
            return operators.spread(args_new);
        }
        return rex;
    }

    @Override
    public Operator visitTable(@NotNull RexTable rex, C ctx) {
        return rex;
    }

    @Override
    public Operator visitVar(@NotNull RexVar rex, C ctx) {
        return rex;
    }

    /**
     * Helper method to visit a rel and cast as rel.
     */
    @NotNull
    public final Rel visitRel(@NotNull Rel rel, C ctx) {
        return visit(rel, ctx, Rel.class);
    }

    /**
     * Helper method to visit a rex and cast as rex.
     */
    @NotNull
    public final Rex visitRex(@NotNull Rex rex, C ctx) {
        return visit(rex, ctx, Rex.class);
    }

    /**
     * Helper method to visit an operator and cast to the expected type.
     */
    @NotNull
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
    @NotNull
    public final <T> List<T> visitAll(@NotNull List<T> objects, C ctx, @NotNull Mapper<T, C> mapper) {
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
    @NotNull
    public <T extends Operator> T onError(@NotNull Operator o, @NotNull Class<T> clazz) {
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
