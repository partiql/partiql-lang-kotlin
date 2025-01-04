package org.partiql.ast;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a chain of identifiers, such as {@code foo.bar.baz}.
 */
@Builder(builderClassName = "Builder")
@EqualsAndHashCode(callSuper = false)
public final class IdentifierChain extends AstNode {
    @NotNull
    private final Identifier root;

    @Nullable
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

    @NotNull
    public Identifier getRoot() {
        return this.root;
    }

    @Nullable
    public IdentifierChain getNext() {
        return this.next;
    }
}
