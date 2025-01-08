package org.partiql.ast;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Represents a SQL identifier, which may be qualified, such as {@code foo.bar.baz}.
 */
@Builder(builderClassName = "Builder")
@EqualsAndHashCode(callSuper = false)
public final class Identifier extends AstNode {
    @NotNull
    private final List<Simple> qualifier;

    @NotNull
    private final Simple identifier;

    public Identifier(@NotNull List<Simple> qualifier, @NotNull Simple identifier) {
        this.qualifier = qualifier;
        this.identifier = identifier;
    }

    @NotNull
    @Override
    public List<AstNode> getChildren() {
        List<AstNode> kids = new ArrayList<>(qualifier);
        kids.add(identifier);
        return kids;
    }

    @Override
    public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
        return visitor.visitIdentifier(this, ctx);
    }

    @NotNull
    public List<Simple> getQualifier() {
        return this.qualifier;
    }

    @NotNull
    public Simple getIdentifier() {
        return this.identifier;
    }

    /**
     * Returns true if there is no qualifier for the identifier.
     */
    public boolean hasQualifier() {
        return !this.qualifier.isEmpty();
    }

    /**
     * Represents a part of a SQL identifier, such as {@code c} in {@code a.b.c}.
     */
    @lombok.Builder(builderClassName = "Builder")
    @EqualsAndHashCode(callSuper = false)
    public static final class Simple extends AstNode {
        @NotNull
        private final String text;

        private final boolean regular;

        public Simple(@NotNull String text, boolean regular) {
            this.text = text;
            this.regular = regular;
        }

        @NotNull
        @Override
        public List<AstNode> getChildren() {
            return new ArrayList<>();
        }

        @Override
        public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
            return visitor.visitIdentifierPart(this, ctx);
        }

        @NotNull
        public String getText() {
            return this.text;
        }

        /**
         * @return true iff this is a regular identifier.
         */
        public boolean isRegular() {
            return this.regular;
        }

        @NotNull
        public static Simple regular(@NotNull String text) {
            return new Simple(text, true);
        }

        @NotNull
        public static Simple delimited(@NotNull String text) {
            return new Simple(text, false);
        }
    }

    ///// Static constructor methods
    @NotNull
    public static Identifier regular(@NotNull String text) {
        return new Identifier(Collections.emptyList(), Simple.regular(text));
    }

    @NotNull
    public static Identifier regular(@NotNull String... parts) {
        return regular(Arrays.asList(parts));
    }

    @NotNull
    public static Identifier regular(@NotNull List<String> parts) {
        if (parts.isEmpty()) {
            throw new IllegalStateException("Cannot create an identifier with no parts");
        }
        List<Simple> qualifier = parts.subList(0, parts.size() - 1)
            .stream()
            .map(Simple::regular)
            .collect(Collectors.toList());
        Simple identifier = Simple.regular(parts.get(parts.size() - 1));
        return new Identifier(qualifier, identifier);
    }

    @NotNull
    public static Identifier delimited(@NotNull String text) {
        return new Identifier(Collections.emptyList(), Simple.delimited(text));
    }

    @NotNull
    public static Identifier delimited(@NotNull String... parts) {
        return delimited(Arrays.asList(parts));
    }

    @NotNull
    public static Identifier delimited(@NotNull List<String> parts) {
        if (parts.isEmpty()) {
            throw new IllegalStateException("Cannot create an identifier with no parts");
        }
        List<Simple> qualifier = parts.subList(0, parts.size() - 1)
            .stream()
            .map(Simple::delimited)
            .collect(Collectors.toList());
        Simple identifier = Simple.delimited(parts.get(parts.size() - 1));
        return new Identifier(qualifier, identifier);
    }

    @NotNull
    public static Identifier of(@NotNull Simple... parts) {
        return of(Arrays.asList(parts));
    }

    @NotNull
    public static Identifier of(@NotNull List<Simple> parts) {
        if (parts.isEmpty()) {
            throw new IllegalStateException("Cannot create an identifier with no parts");
        }
        List<Simple> qualifier = parts.subList(0, parts.size() - 1);
        Simple identifier = parts.get(parts.size() - 1);
        return new Identifier(qualifier, identifier);
    }
}
