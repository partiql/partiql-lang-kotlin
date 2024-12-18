package org.partiql.ast.dml;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
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
 * This is the REPLACE INTO statement.
 * @see InsertSource
 */
@Builder(builderClassName = "Builder")
@EqualsAndHashCode(callSuper = false)
public final class Replace extends Statement {
    /**
     * TODO
     */
    @NotNull
    @Getter
    private final IdentifierChain tableName;

    /**
     * TODO
     */
    @Nullable
    @Getter
    private final Identifier asAlias;

    /**
     * TODO
     */
    @NotNull
    @Getter
    private final InsertSource source;

    /**
     * TODO
     * @param tableName TODO
     * @param asAlias TODO
     * @param source TODO
     */
    public Replace(@NotNull IdentifierChain tableName, @Nullable Identifier asAlias, @NotNull InsertSource source) {
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
        return visitor.visitReplace(this, ctx);
    }
}
