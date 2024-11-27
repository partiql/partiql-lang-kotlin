package org.partiql.plan;

import org.jetbrains.annotations.NotNull;
import org.partiql.plan.rel.*;
import org.partiql.plan.rex.*;

/**
 * A visitor for a logical [Operator] tree.
 *
 * @param <R> Visit return type
 * @param <C> Context parameter type
 */
public interface Visitor<R, C> {

    default R defaultVisit(Operator operator, C ctx) {
        for (Operator child : operator.getChildren()) {
            child.accept(this, ctx);
        }
        return defaultReturn(operator, ctx);
    }

    R defaultReturn(@NotNull Operator operator, C ctx);

    default R visit(@NotNull Operator operator, C ctx) {
        return operator.accept(this, ctx);
    }

    // --[Rel]-----------------------------------------------------------------------------------------------------------

    default R visitAggregate(@NotNull RelAggregate rel, C ctx) {
        return defaultVisit(rel, ctx);
    }

    default R visitDistinct(@NotNull RelDistinct rel, C ctx) {
        return defaultVisit(rel, ctx);
    }

    default R visitExcept(@NotNull RelExcept rel, C ctx) {
        return defaultVisit(rel, ctx);
    }

    default R visitExclude(@NotNull RelExclude rel, C ctx) {
        return defaultVisit(rel, ctx);
    }

    default R visitFilter(@NotNull RelFilter rel, C ctx) {
        return defaultVisit(rel, ctx);
    }

    default R visitIntersect(@NotNull RelIntersect rel, C ctx) {
        return defaultVisit(rel, ctx);
    }

    default R visitIterate(@NotNull RelIterate rel, C ctx) {
        return defaultVisit(rel, ctx);
    }

    default R visitJoin(@NotNull RelJoin rel, C ctx) {
        return defaultVisit(rel, ctx);
    }

    default R visitCorrelate(@NotNull RelCorrelate rel, C ctx) {
        return defaultVisit(rel, ctx);
    }

    default R visitLimit(@NotNull RelLimit rel, C ctx) {
        return defaultVisit(rel, ctx);
    }

    default R visitOffset(@NotNull RelOffset rel, C ctx) {
        return defaultVisit(rel, ctx);
    }

    default R visitProject(@NotNull RelProject rel, C ctx) {
        return defaultVisit(rel, ctx);
    }

    default R visitScan(@NotNull RelScan rel, C ctx) {
        return defaultVisit(rel, ctx);
    }

    default R visitSort(@NotNull RelSort rel, C ctx) {
        return defaultVisit(rel, ctx);
    }

    default R visitUnion(@NotNull RelUnion rel, C ctx) {
        return defaultVisit(rel, ctx);
    }

    default R visitUnpivot(@NotNull RelUnpivot rel, C ctx) {
        return defaultVisit(rel, ctx);
    }
    // --[Rex]-----------------------------------------------------------------------------------------------------------

    default R visitArray(@NotNull RexArray rex, C ctx) {
        return defaultVisit(rex, ctx);
    }

    default R visitBag(@NotNull RexBag rex, C ctx) {
        return defaultVisit(rex, ctx);
    }

    default R visitCall(@NotNull RexCall rex, C ctx) {
        return defaultVisit(rex, ctx);
    }

    default R visitCallDynamic(@NotNull RexDispatch rex, C ctx) {
        return defaultVisit(rex, ctx);
    }

    default R visitCase(@NotNull RexCase rex, C ctx) {
        return defaultVisit(rex, ctx);
    }

    default R visitCast(@NotNull RexCast rex, C ctx) {
        return defaultVisit(rex, ctx);
    }

    default R visitCoalesce(@NotNull RexCoalesce rex, C ctx) {
        return defaultVisit(rex, ctx);
    }

    default R visitError(@NotNull RexError rex, C ctx) {
        return defaultVisit(rex, ctx);
    }

    default R visitLit(@NotNull RexLit rex, C ctx) {
        return defaultVisit(rex, ctx);
    }

    default R visitNullIf(@NotNull RexNullIf rex, C ctx) {
        return defaultVisit(rex, ctx);
    }

    default R visitPathIndex(@NotNull RexPathIndex rex, C ctx) {
        return defaultVisit(rex, ctx);
    }

    default R visitPathKey(@NotNull RexPathKey rex, C ctx) {
        return defaultVisit(rex, ctx);
    }

    default R visitPathSymbol(@NotNull RexPathSymbol rex, C ctx) {
        return defaultVisit(rex, ctx);
    }

    default R visitPivot(@NotNull RexPivot rex, C ctx) {
        return defaultVisit(rex, ctx);
    }

    default R visitSelect(@NotNull RexSelect rex, C ctx) {
        return defaultVisit(rex, ctx);
    }

    default R visitStruct(@NotNull RexStruct rex, C ctx) {
        return defaultVisit(rex, ctx);
    }

    default R visitSubquery(@NotNull RexSubquery rex, C ctx) {
        return defaultVisit(rex, ctx);
    }

    default R visitSubqueryComp(@NotNull RexSubqueryComp rex, C ctx) {
        return defaultVisit(rex, ctx);
    }

    default R visitSubqueryIn(@NotNull RexSubqueryIn rex, C ctx) {
        return defaultVisit(rex, ctx);
    }

    default R visitSubqueryTest(@NotNull RexSubqueryTest rex, C ctx) {
        return defaultVisit(rex, ctx);
    }

    default R visitSpread(@NotNull RexSpread rex, C ctx) {
        return defaultVisit(rex, ctx);
    }

    default R visitTable(@NotNull RexTable rex, C ctx) {
        return defaultVisit(rex, ctx);
    }

    default R visitVar(@NotNull RexVar rex, C ctx) {
        return defaultVisit(rex, ctx);
    }
}
