package org.partiql.plan.rex;

import org.jetbrains.annotations.NotNull;
import org.partiql.plan.Operand;
import org.partiql.plan.OperatorVisitor;
import org.partiql.spi.types.PType;

import java.util.List;

/**
 * Logical map expression abstract base class.
 * @deprecated This feature is experimental and is subject to change.
 */
@Deprecated
public abstract class RexMap extends RexBase {

    /**
     * Creates a new map expression.
     * @param keyType the key type
     * @param valueType the value type
     * @param entries list of map entries (key-value pairs)
     * @return new RexMap instance
     */
    @NotNull
    public static RexMap create(@NotNull PType keyType, @NotNull PType valueType, @NotNull List<Entry> entries) {
        return new Impl(keyType, valueType, entries);
    }

    /**
     * Creates a new map entry.
     * @param key entry key
     * @param value entry value
     * @return an entry instance
     */
    @NotNull
    public static Entry entry(Rex key, Rex value) {
        return new Entry(key, value);
    }

    /**
     * Gets the key type.
     * @return the key type
     */
    @NotNull
    public abstract PType getKeyType();

    /**
     * Gets the value type.
     * @return the value type
     */
    @NotNull
    public abstract PType getValueType();

    /**
     * Gets the map entries.
     * @return list of map entries (NOT operands)
     */
    @NotNull
    public abstract List<Entry> getEntries();

    @NotNull
    @Override
    protected final RexType type() {
        return RexType.of(PType.map(getKeyType(), getValueType()));
    }

    @NotNull
    @Override
    protected List<Operand> operands() {
        return List.of();
    }

    @Override
    public <R, C> R accept(OperatorVisitor<R, C> visitor, C ctx) {
        return visitor.visitMap(this, ctx);
    }

    /**
     * Map expression entry (key-value pair).
     */
    public static class Entry {

        private final Rex key;
        private final Rex value;

        private Entry(Rex key, Rex value) {
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

    private static class Impl extends RexMap {

        @NotNull
        private final PType keyType;

        @NotNull
        private final PType valueType;

        @NotNull
        private final List<Entry> entries;

        private Impl(@NotNull PType keyType, @NotNull PType valueType, @NotNull List<Entry> entries) {
            this.keyType = keyType;
            this.valueType = valueType;
            this.entries = entries;
        }

        @Override
        @NotNull
        public PType getKeyType() {
            return keyType;
        }

        @Override
        @NotNull
        public PType getValueType() {
            return valueType;
        }

        @Override
        @NotNull
        public List<Entry> getEntries() {
            return entries;
        }
    }
}
