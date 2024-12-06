package org.partiql.ast;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * This is the insert statement.
 * @see InsertColumnList
 * @see InsertSource
 */
@Builder(builderClassName = "Builder")
@EqualsAndHashCode(callSuper = false)
public final class Insert extends Statement {
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
     */
    @Nullable
    public final OnConflict onConflict;

    /**
     * TODO
     * @param tableName TODO
     * @param asAlias TODO
     * @param source TODO
     * @param onConflict TODO
     */
    public Insert(@NotNull IdentifierChain tableName, @Nullable Identifier asAlias, @NotNull InsertSource source, @Nullable OnConflict onConflict) {
        this.tableName = tableName;
        this.asAlias = asAlias;
        this.source = source;
        this.onConflict = onConflict;
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
        return visitor.visitInsert(this, ctx);
    }
}
