package org.partiql.ast.v1;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * TODO docs, equals, hashcode
 */
public abstract class Target extends AstNode {
    @Override
    public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
        if (this instanceof Domain) {
            return visitor.visitTargetDomain((Domain) this, ctx);
        } else {
            throw new IllegalStateException("Unexpected value: " + this);
        }
    }

    /**
     * TODO docs, equals, hashcode
     */
    public static class Domain extends Target {
        @NotNull
        public Statement statement;

        @Nullable
        public String type;

        @Nullable
        public String format;

        public Domain(@NotNull Statement statement, @Nullable String type, @Nullable String format) {
            this.statement = statement;
            this.type = type;
            this.format = format;
        }

        @NotNull
        @Override
        public Collection<AstNode> children() {
            List<AstNode> kids = new ArrayList<>();
            kids.add(statement);
            return kids;
        }

        @Override
        public <R, C > R accept(@NotNull AstVisitor < R, C > visitor, C ctx) {
            return visitor.visitTargetDomain(this, ctx);
        }
    }
}
