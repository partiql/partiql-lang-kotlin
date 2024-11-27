package org.partiql.plan.rex;

import org.jetbrains.annotations.NotNull;
import org.partiql.plan.Operator;
import org.partiql.plan.Visitor;
import org.partiql.types.PType;

import java.util.List;

/**
 * Logical struct expression abstract base class.
 */
public abstract class RexStruct extends RexBase {

    /**
     * @return list of struct fields (NOT children)
     */
    public abstract List<Field> getFields();

    @NotNull
    @Override
    protected final RexType type() {
        return new RexType(PType.struct());
    }

    @Override
    protected List<Operator> children() {
        return List.of();
    }

    @Override
    public <R, C> R accept(Visitor<R, C> visitor, C ctx) {
        return visitor.visitStruct(this, ctx);
    }

    /**
     * Struct expression field constructor.
     */
    public static class Field {

        private final Rex key;
        private final Rex value;

        public Field(Rex key, Rex value) {
            this.key = key;
            this.value = value;
        }

        public Rex getKey() {
            return key;
        }

        public Rex getValue() {
            return value;
        }
    }
}
