package org.partiql.ast.expr;

import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;
import org.partiql.ast.AstEnum;
import org.partiql.ast.AstNode;
import org.partiql.ast.AstVisitor;

import java.util.Collections;
import java.util.List;

/**
 * TODO docs, equals, hashcode
 */
@EqualsAndHashCode(callSuper = false)
public class SessionAttribute extends AstEnum {
    public static final int UNKNOWN = 0;
    public static final int CURRENT_USER = 1;
    public static final int CURRENT_DATE = 2;

    public static SessionAttribute UNKNOWN() {
        return new SessionAttribute(UNKNOWN);
    }

    public static SessionAttribute CURRENT_USER() {
        return new SessionAttribute(CURRENT_USER);
    }

    public static SessionAttribute CURRENT_DATE() {
        return new SessionAttribute(CURRENT_DATE);
    }

    private final int code;

    public SessionAttribute(int code) {
        this.code = code;
    }

    @Override
    public int code() {
        return code;
    }

    @NotNull
    @Override
    public String name() {
        switch (code) {
            case CURRENT_USER: return "CURRENT_USER";
            case CURRENT_DATE: return "CURRENT_DATE";
            default: return "UNKNOWN";
        }
    }

    @NotNull
    private static final int[] codes = {
        CURRENT_USER,
        CURRENT_DATE
    };

    @NotNull
    public static SessionAttribute parse(@NotNull String value) {
        switch (value) {
            case "CURRENT_USER": return CURRENT_USER();
            case "CURRENT_DATE": return CURRENT_DATE();
            default: return UNKNOWN();
        }
    }

    @NotNull
    public static int[] codes() {
        return codes;
    }

    @NotNull
    @Override
    public List<AstNode> getChildren() {
        return Collections.emptyList();
    }

    @Override
    public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
        return null;
    }
}
