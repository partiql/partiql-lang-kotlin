package org.partiql.ast;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * TODO docs, equals, hashcode
 */
@Builder(builderClassName = "Builder")
@EqualsAndHashCode(callSuper = false)
public final class IdentifierChain extends AstNode {
    @NotNull
    @Getter
    private final Identifier root;

    @Nullable
    @Getter
    private final IdentifierChain next;

    public IdentifierChain(@NotNull Identifier root, @Nullable IdentifierChain next) {
        this.root = root;
        this.next = next;
    }

    @NotNull
    @Override
    public List<AstNode> getChildren() {
        List<AstNode> kids = new ArrayList<>();
        kids.add(root);
        if (next != null) {
            kids.add(next);
        }
        return kids;
    }

    @Override
    public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
        return visitor.visitIdentifierChain(this, ctx);
    }
}
