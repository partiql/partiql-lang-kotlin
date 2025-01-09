package org.partiql.plan.rex;

import org.jetbrains.annotations.NotNull;
import org.partiql.plan.Operand;
import org.partiql.plan.OperatorVisitor;
import org.partiql.spi.types.PType;

import java.util.List;

/**
 * Logical struct expression abstract base class.
 */
public abstract class RexStruct extends RexBase {

    /**
     * @return new RexStruct instance
     */
    @NotNull
    public static RexStruct create(@NotNull List<Field> fields) {
        return new Impl(fields);
    }

    /**
     * @return a field constructor instance
     */
    @NotNull
    public static Field field(Rex key, Rex value) {
        return new Field(key, value);
    }

    /**
     * @return list of struct fields (NOT operands)
     */
    @NotNull
    public abstract List<Field> getFields();

    @NotNull
    @Override
    protected final RexType type() {
        return RexType.of(PType.struct());
    }

    @NotNull
    @Override
    protected List<Operand> operands() {
        return List.of();
    }

    @Override
    public <R, C> R accept(OperatorVisitor<R, C> visitor, C ctx) {
        return visitor.visitStruct(this, ctx);
    }

    /**
     * Struct expression field constructor.
     */
    public static class Field {

        private final Rex key;
        private final Rex value;

        private Field(Rex key, Rex value) {
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

    private static class Impl extends RexStruct {

        @NotNull
        private final List<Field> fields;

        private Impl(@NotNull List<Field> fields) {
            this.fields = fields;
        }

        @Override
        @NotNull
        public List<Field> getFields() {
            return fields;
        }
    }
}
