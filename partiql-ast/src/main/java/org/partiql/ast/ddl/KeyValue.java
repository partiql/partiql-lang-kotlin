package org.partiql.ast.ddl;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;
import org.partiql.ast.AstNode;
import org.partiql.ast.AstVisitor;

import java.util.Collections;
import java.util.List;

/**
 * Any option that consists of a key value pair where the key is a string and value is a string.
 */
@Builder(builderClassName = "Builder")
@EqualsAndHashCode(callSuper = false)
public final class KeyValue extends AstNode {
    @NotNull
    private final String key;

    @NotNull
    private final String value;

    public KeyValue(@NotNull String key, @NotNull String value) {
        this.key = key;
        this.value = value;
    }

    @NotNull
    @Override
    public List<AstNode> getChildren() {
        return Collections.emptyList();
    }

    @Override
    public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
        return visitor.visitKeyValue(this, ctx);
    }

    @NotNull
    public String getKey() {
        return this.key;
    }

    @NotNull
    public String getValue() {
        return this.value;
    }
}
