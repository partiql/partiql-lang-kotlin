package org.partiql.ast.expr;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.partiql.ast.AstNode;
import org.partiql.ast.AstVisitor;

import java.util.Collections;
import java.util.List;

/**
 * Represents an error expression node in the AST.
 * <p>
 * This node is used to represent expressions that could not be parsed correctly,
 * such as datetime field keywords (YEAR, MONTH, DAY, HOUR, MINUTE, SECOND) used
 * in expression contexts where they are reserved tokens.
 * <p>
 * The ExprError node preserves the original text for target-specific recovery
 * and includes an error code for categorization.
 */
@Builder(builderClassName = "Builder")
@EqualsAndHashCode(callSuper = false)
public final class ExprError extends Expr {

    /**
     * Error code for datetime field keywords used in expression contexts.
     */
    public static final int DATETIME_FIELD_KEYWORD = 1;

    /**
     * The original text that caused the error.
     */
    @NotNull
    private final String message;

    /**
     * The error code categorizing the type of error.
     * Returns the error code categorizing the type of error.
     */
    @Getter
    private final int code;

    /**
     * Creates a new ExprError node.
     *
     * @param message the original text that caused the error
     * @param code the error code categorizing the type of error
     */
    public ExprError(@NotNull String message, int code) {
        this.message = message;
        this.code = code;
    }

    @Override
    @NotNull
    public List<AstNode> getChildren() {
        return Collections.emptyList();
    }

    @Override
    public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
        return visitor.visitExprError(this, ctx);
    }

    /**
     * Returns the original text that caused the error.
     *
     * @return the error message/original text
     */
    @NotNull
    public String getMessage() {
        return this.message;
    }
}
