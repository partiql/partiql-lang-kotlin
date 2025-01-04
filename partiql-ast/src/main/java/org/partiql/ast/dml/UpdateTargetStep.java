package org.partiql.ast.dml;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;
import org.partiql.ast.AstNode;
import org.partiql.ast.AstVisitor;
import org.partiql.ast.Identifier;
import org.partiql.ast.Literal;

import java.util.ArrayList;
import java.util.List;

/**
 * This references a column or column's nested properties. In SQL:1999, the EBNF rule is &lt;update target&gt;.
 * This implementation differs from SQL by allowing for references to deeply nested data of varying types.
 *
 * @see SetClause
 */
public abstract class UpdateTargetStep extends AstNode {

    /**
     * This is a reference to a field of an array/struct using the bracket notation.
     *
     * @see UpdateTarget
     * @see UpdateTargetStep
     */
    @Builder(builderClassName = "Builder")
    @EqualsAndHashCode(callSuper = false)
    public static final class Element extends UpdateTargetStep {
        // TODO: Should we explicitly have an ElementInt and ElementString? Especially once PartiQLValue is removed.

        @NotNull
        private final Literal key;

        public Element(@NotNull Literal key) {
            this.key = key;
        }

        public Element(int key) {
            this.key = Literal.intNum(key);
        }

        public Element(@NotNull String key) {
            this.key = Literal.string(key);
        }

        @NotNull
        @Override
        public List<AstNode> getChildren() {
            List<AstNode> kids = new ArrayList<>();
            kids.add(key);
            return kids;
        }

        @Override
        public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
            return visitor.visitUpdateTargetStepElement(this, ctx);
        }

        @NotNull
        public Literal getKey() {
            return this.key;
        }
    }

    /**
     * This is a reference to a field of a struct using the dot notation.
     *
     * @see UpdateTarget
     * @see UpdateTargetStep
     */
    @Builder(builderClassName = "Builder")
    @EqualsAndHashCode(callSuper = false)
    public static final class Field extends UpdateTargetStep {
        @NotNull
        private final Identifier key;

        public Field(@NotNull Identifier key) {
            this.key = key;
        }

        @NotNull
        @Override
        public List<AstNode> getChildren() {
            List<AstNode> kids = new ArrayList<>();
            kids.add(key);
            return kids;
        }

        @Override
        public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
            return visitor.visitUpdateTargetStepField(this, ctx);
        }

        @NotNull
        public Identifier getKey() {
            return this.key;
        }
    }
}