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
 * This is the UPSERT INTO statement.
 * @see InsertSource
 */
@Builder(builderClassName = "Builder")
@EqualsAndHashCode(callSuper = false)
public final class Upsert extends Statement {
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
}
