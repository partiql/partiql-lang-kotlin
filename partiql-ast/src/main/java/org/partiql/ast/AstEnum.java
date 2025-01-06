package org.partiql.ast;

import org.jetbrains.annotations.NotNull;

/**
 * Abstract base class representing enums in the AST.
 */
public abstract class AstEnum extends AstNode {
    /**
     * The code of the enum.
     * @return the code of the enum.
     */
    public abstract int code();

    /**
     * String representation for the enum variant.
     * @return string representation for the enum variant.
     */
    @NotNull
    public abstract String name();
}
