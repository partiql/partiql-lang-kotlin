package org.partiql.ast;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;
import org.partiql.value.StringValue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * TODO docs, equals, hashcode
 */
public abstract class Options extends AstNode {

    /**
     * Any option that consists of a key value pair where the key is a string and value is an StringValue.
     *
     * TODO: equals, hashcode
     */
    @Builder(builderClassName = "Builder")
    @EqualsAndHashCode(callSuper = false)
    public static class KeyValue extends Options{
        @NotNull
        public final String key;
        @NotNull
        public final StringValue value;

        public KeyValue(@NotNull String key, @NotNull StringValue value) {
            this.key = key;
            this.value = value;
        }

        @NotNull
        @Override
        public Collection<AstNode> children() {
            return Collections.emptyList();
        }

        @Override
        public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
            return visitor.visitKeyValue(this, ctx);
        }
    }

    @Builder(builderClassName = "Builder")
    @EqualsAndHashCode(callSuper = false)
    public static class PartitionBy extends Options{
        @NotNull
        public final List<Identifier> columns;

        public PartitionBy(@NotNull List<Identifier> columns) {
            this.columns = columns;
        }

        @NotNull
        @Override
        public Collection<AstNode> children() {
            return new ArrayList<>(columns);
        }

        @Override
        public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
            return visitor.visitPartitionBy(this, ctx);
        }
    }
}
