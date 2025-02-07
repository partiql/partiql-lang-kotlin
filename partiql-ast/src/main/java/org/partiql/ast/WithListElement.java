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
    // TODO: Add support for the search/cycle clause.

    @NotNull
    private final Identifier.Simple queryName;

    @NotNull
    private final ExprQuerySet asQuery;

    @Nullable
    private final List<Identifier.Simple> withColumnList;

    /**
     * Creates a new instance of {@link WithListElement}.
     * @param queryName the name to bind
     * @param asQuery the query that defines the with list element
     * @param columnList the list of column names to be output from the query
     */
    public WithListElement(@NotNull Identifier.Simple queryName, @NotNull ExprQuerySet asQuery, @Nullable List<Identifier.Simple> columnList) {
        this.queryName = queryName;
        this.asQuery = asQuery;
        this.withColumnList = columnList;
    }

    /**
     * Creates a new instance of {@link WithListElement}.
     * @param queryName the name to bind
     * @param asQuery the query that defines the with list element
     */
    public WithListElement(@NotNull Identifier.Simple queryName, @NotNull ExprQuerySet asQuery) {
        this(queryName, asQuery, null);
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
     * Returns the query name.
     * @return the query name
     */
    @NotNull
    public Identifier.Simple getQueryName() {
        return this.queryName;
    }

    /**
     * Returns the list of column names to be output from the query.
     * @return the list of column names to be output from the query. This may return null.
     */
    @Nullable
    public List<Identifier.Simple> getColumnList() {
        return this.withColumnList;
    }

    /**
     * Returns the query that defines the with list element.
     * @return the query that defines the with list element
     */
    @NotNull
    public ExprQuerySet getAsQuery() {
        return this.asQuery;
    }
}
