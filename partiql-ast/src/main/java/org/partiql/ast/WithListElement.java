package org.partiql.ast;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.partiql.ast.expr.ExprQuerySet;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * Represents a {@code <with list element>}.
 * </p>
 * <p>
 * {@code
 * <with list element>    ::=
 *          <query name>
 *          [ <left paren> <with column list> <right paren> ]
 *          AS <left paren> <query expression> <right paren>
 *          [ <search or cycle clause> ] 
 * }
 * </p>
 * <p>{@code <with column list>    ::=   <column name list>}</p>
 * <p>{@code <column name list>    ::=   <column name> [ { <comma> <column name> }... ]}</p>
 * <p>{@code <column name>    ::=   <identifier>}</p>
 * @see With
 * @see ExprQuerySet
 */
@Builder(builderClassName = "Builder")
@EqualsAndHashCode(callSuper = false)
public final class WithListElement extends AstNode {

    @NotNull
    private final Identifier.Simple queryName;

    @NotNull
    private final ExprQuerySet asQuery;

    @Nullable
    private final List<Identifier.Simple> withColumnList;

    /**
     * TODO
     * @param queryName TODO
     * @param asQuery TODO
     * @param columnList TODO
     */
    public WithListElement(@NotNull Identifier.Simple queryName, @NotNull ExprQuerySet asQuery, @Nullable List<Identifier.Simple> columnList) {
        this.queryName = queryName;
        this.asQuery = asQuery;
        this.withColumnList = columnList;
    }

    @NotNull
    @Override
    public List<AstNode> getChildren() {
        List<AstNode> children = new ArrayList<>();
        children.add(queryName);
        if (withColumnList != null) {
            children.addAll(withColumnList);
        }
        children.add(asQuery);
        return children;
    }

    @Override
    public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
        return visitor.visitWithListElement(this, ctx);
    }

    /**
     * TODO
     * @return TODO
     */
    @NotNull
    public Identifier.Simple getQueryName() {
        return this.queryName;
    }

    /**
     * TODO
     * @return TODO
     */
    @Nullable
    public List<Identifier.Simple> getColumnList() {
        return this.withColumnList;
    }

    /**
     * TODO
     * @return TODO
     */
    @NotNull
    public ExprQuerySet getAsQuery() {
        return this.asQuery;
    }
}
