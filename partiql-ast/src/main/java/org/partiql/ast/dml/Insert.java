package org.partiql.ast.dml;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.partiql.ast.AstNode;
import org.partiql.ast.AstVisitor;
import org.partiql.ast.Identifier;
import org.partiql.ast.IdentifierChain;
import org.partiql.ast.Statement;

import java.util.ArrayList;
import java.util.List;

/**
 * This is the insert statement.
 *
 * @see InsertSource
 */
@Builder(builderClassName = "Builder")
@EqualsAndHashCode(callSuper = false)
public final class Insert extends Statement {
    /**
     * TODO
     */
    @NotNull
    private final IdentifierChain tableName;

    /**
     * TODO
     */
    @Nullable
    private final Identifier asAlias;

    /**
     * TODO
     */
    @NotNull
    private final InsertSource source;

    /**
     * TODO
     */
    @Nullable
    private final OnConflict onConflict;

    /**
     * TODO
     *
     * @param tableName  TODO
     * @param asAlias    TODO
     * @param source     TODO
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
    public List<AstNode> getChildren() {
        List<AstNode> kids = new ArrayList<>();
        kids.add(tableName);
        if (asAlias != null) {
            kids.add(asAlias);
        }
        kids.add(source);
        if (onConflict != null) {
            kids.add(onConflict);
        }
        return kids;
    }

    @Override
    public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
        return visitor.visitInsert(this, ctx);
    }

    @NotNull
    public IdentifierChain getTableName() {
        return this.tableName;
    }

    @Nullable
    public Identifier getAsAlias() {
        return this.asAlias;
    }

    @NotNull
    public InsertSource getSource() {
        return this.source;
    }

    @Nullable
    public OnConflict getOnConflict() {
        return this.onConflict;
    }
}
