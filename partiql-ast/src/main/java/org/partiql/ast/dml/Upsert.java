package org.partiql.ast.dml;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.partiql.ast.AstNode;
import org.partiql.ast.AstVisitor;
import org.partiql.ast.Identifier;
import org.partiql.ast.Statement;

import java.util.ArrayList;
import java.util.List;

/**
 * This is the UPSERT INTO statement.
 *
 * @see InsertSource
 */
@Builder(builderClassName = "Builder")
@EqualsAndHashCode(callSuper = false)
public final class Upsert extends Statement {
    @NotNull
    private final Identifier tableName;

    @Nullable
    private final Identifier.Part asAlias;

    @NotNull
    private final InsertSource source;

    public Upsert(@NotNull Identifier tableName, @Nullable Identifier.Part asAlias, @NotNull InsertSource source) {
        this.tableName = tableName;
        this.asAlias = asAlias;
        this.source = source;
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
        return kids;
    }

    @Override
    public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
        return visitor.visitUpsert(this, ctx);
    }

    @NotNull
    public Identifier getTableName() {
        return this.tableName;
    }

    @Nullable
    public Identifier.Part getAsAlias() {
        return this.asAlias;
    }

    @NotNull
    public InsertSource getSource() {
        return this.source;
    }
}
