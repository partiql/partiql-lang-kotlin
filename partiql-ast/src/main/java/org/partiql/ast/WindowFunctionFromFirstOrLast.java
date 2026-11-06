/*
 * Copyright Amazon.com, Inc. or its affiliates.  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License").
 *  You may not use this file except in compliance with the License.
 *  A copy of the License is located at:
 *
 *       http://aws.amazon.com/apache2.0/
 *
 *  or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 *  language governing permissions and limitations under the License.
 */

package org.partiql.ast;

import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

/**
 * The {@code <from first or last>} option of the NTH_VALUE window function.
 * @see WindowFunctionType.NthValue#getFromFirstOrLast()
 * @deprecated This feature is experimental and is subject to change.
 */
@EqualsAndHashCode(callSuper = false)
@Deprecated
public final class WindowFunctionFromFirstOrLast extends AstEnum {
    /**
     * The FROM FIRST variant.
     */
    public static final int FROM_FIRST = 0;

    /**
     * The FROM LAST variant.
     */
    public static final int FROM_LAST = 1;

    /**
     * Constructs a new instance with the {@link #FROM_FIRST} code.
     * @return a new instance with the {@link #FROM_FIRST} code
     */
    public static WindowFunctionFromFirstOrLast FROM_FIRST() {
        return new WindowFunctionFromFirstOrLast(FROM_FIRST);
    }

    /**
     * Constructs a new instance with the {@link #FROM_LAST} code.
     * @return a new instance with the {@link #FROM_LAST} code
     */
    public static WindowFunctionFromFirstOrLast FROM_LAST() {
        return new WindowFunctionFromFirstOrLast(FROM_LAST);
    }

    private final int code;

    private WindowFunctionFromFirstOrLast(int code) {
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
            case FROM_FIRST: return "FROM FIRST";
            case FROM_LAST: return "FROM LAST";
            default: throw new IllegalStateException("Invalid code: " + code);
        }
    }

    @NotNull
    private static final int[] codes = {
            FROM_FIRST,
            FROM_LAST
    };

    /**
     * Returns the codes for this enum.
     * @return the codes for this enum
     */
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
