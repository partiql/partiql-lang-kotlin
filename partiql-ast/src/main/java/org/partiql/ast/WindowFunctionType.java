package org.partiql.ast;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.partiql.ast.expr.Expr;

import java.util.ArrayList;
import java.util.List;

/**
 * TODO
 */
public abstract class WindowFunctionType extends AstNode {
    /**
     * TODO
     */
    @Builder(builderClassName = "Builder")
    @EqualsAndHashCode(callSuper = false)
    public static final class NoArg extends WindowFunctionType {
        private final WindowFunctionSimpleName name;

        /**
         * TODO
         * @param name TODO
         */
        public NoArg(@NotNull WindowFunctionSimpleName name) {
            this.name = name;
        }

        /**
         * TODO
         * @return TODO
         */
        @NotNull
        public WindowFunctionSimpleName getName() {
            return this.name;
        }

        @NotNull
        @Override
        public List<AstNode> getChildren() {
            List<AstNode> kids = new ArrayList<>();
            kids.add(name);
            return kids;
        }

        @Override
        public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
            return visitor.visitWindowFunctionTypeNoArg(this, ctx);
        }
    }

    /**
     * TODO
     */
    @Builder(builderClassName = "Builder")
    @EqualsAndHashCode(callSuper = false)
    public static final class LeadOrLag extends WindowFunctionType {
        private final boolean isLead;
        private final Expr extent;
        private final Long offset;
        private final Expr defaultValue;
        private final WindowFunctionNullTreatment nullTreatment;

        /**
         * TODO
         * @param isLead TODO
         * @param extent TODO
         * @param offset TODO
         * @param defaultValue TODO
         */
        public LeadOrLag(
                boolean isLead,
                @NotNull Expr extent,
                @Nullable Long offset,
                @Nullable Expr defaultValue,
                @Nullable WindowFunctionNullTreatment nullTreatment
        ) {
            this.isLead = isLead;
            this.extent = extent;
            this.offset = offset;
            this.defaultValue = defaultValue;
            this.nullTreatment = nullTreatment;
        }

        /**
         * TODO
         * @return TODO
         */
        public boolean isLead() {
            return this.isLead;
        }

        /**
         * TODO
         * @return TODO
         */
        public boolean isLag() {
            return !this.isLead;
        }

        /**
         * TODO
         * @return TODO
         */
        @NotNull
        public Expr getExtent() {
            return this.extent;
        }

        /**
         * TODO
         * @return TODO
         */
        @Nullable
        public Long getOffset() {
            return this.offset;
        }

        /**
         * TODO
         * @return TODO
         */
        @Nullable
        public Expr getDefaultValue() {
            return this.defaultValue;
        }

        /**
         * TODO
         * @return TODO
         */
        @Nullable
        public WindowFunctionNullTreatment getNullTreatment() {
            return this.nullTreatment;
        }

        @NotNull
        @Override
        public List<AstNode> getChildren() {
            List<AstNode> kids = new ArrayList<>();
            kids.add(extent);
            kids.add(defaultValue);
            return kids;
        }

        @Override
        public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
            return visitor.visitWindowFunctionTypeLagOrLead(this, ctx);
        }
    }
}
