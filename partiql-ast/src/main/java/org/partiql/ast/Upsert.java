package org.partiql.ast;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * This is the UPSERT INTO statement.
 * @see InsertSource
 */
@Builder(builderClassName = "Builder")
@EqualsAndHashCode(callSuper = false)
public final class Upsert extends Statement {
    // TODO: Equals and hashcode

    /**
     * TODO
     */
    @NotNull
    public final IdentifierChain tableName;

    /**
     * TODO
     */
    @Nullable
    public final Identifier asAlias;

    /**
     * TODO
     */
    @NotNull
    public final InsertSource source;

    /**
     * TODO
     * @param tableName TODO
     * @param asAlias TODO
     * @param source TODO
     */
    public Upsert(@NotNull IdentifierChain tableName, @Nullable Identifier asAlias, @NotNull InsertSource source) {
        this.tableName = tableName;
        this.asAlias = asAlias;
        this.source = source;
    }

    @NotNull
    @Override
    public Collection<AstNode> children() {
        List<AstNode> kids = new ArrayList<>();
        kids.add(tableName);
        return kids;
    }

    @Override
    public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
        return visitor.visitUpsert(this, ctx);
    }
}
