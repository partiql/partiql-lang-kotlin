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
 * The name of an aggregate function used as a window function (SQL:2011 &lt;aggregate function&gt; in
 * &lt;window function type&gt;).
 * @see WindowFunctionType.Aggregate#getFunction()
 * @deprecated This feature is experimental and is subject to change.
 */
@EqualsAndHashCode(callSuper = false)
@Deprecated
public final class WindowFunctionAggregateName extends AstEnum {
    /**
     * The COUNT aggregate. {@code COUNT(*)} is represented by COUNT with a null argument.
     */
    public static final int COUNT = 0;

    /**
     * The SUM aggregate.
     */
    public static final int SUM = 1;

    /**
     * The AVG aggregate.
     */
    public static final int AVG = 2;

    /**
     * The MIN aggregate.
     */
    public static final int MIN = 3;

    /**
     * The MAX aggregate.
     */
    public static final int MAX = 4;

    /**
     * Constructs a new window aggregate name with the {@link #COUNT} code.
     * @return a new window aggregate name with the {@link #COUNT} code
     */
    public static WindowFunctionAggregateName COUNT() {
        return new WindowFunctionAggregateName(COUNT);
    }

    /**
     * Constructs a new window aggregate name with the {@link #SUM} code.
     * @return a new window aggregate name with the {@link #SUM} code
     */
    public static WindowFunctionAggregateName SUM() {
        return new WindowFunctionAggregateName(SUM);
    }

    /**
     * Constructs a new window aggregate name with the {@link #AVG} code.
     * @return a new window aggregate name with the {@link #AVG} code
     */
    public static WindowFunctionAggregateName AVG() {
        return new WindowFunctionAggregateName(AVG);
    }

    /**
     * Constructs a new window aggregate name with the {@link #MIN} code.
     * @return a new window aggregate name with the {@link #MIN} code
     */
    public static WindowFunctionAggregateName MIN() {
        return new WindowFunctionAggregateName(MIN);
    }

    /**
     * Constructs a new window aggregate name with the {@link #MAX} code.
     * @return a new window aggregate name with the {@link #MAX} code
     */
    public static WindowFunctionAggregateName MAX() {
        return new WindowFunctionAggregateName(MAX);
    }

    private final int code;

    private WindowFunctionAggregateName(int code) {
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
            case COUNT: return "COUNT";
            case SUM: return "SUM";
            case AVG: return "AVG";
            case MIN: return "MIN";
            case MAX: return "MAX";
            default: throw new IllegalStateException("Invalid code: " + code);
        }
    }

    @NotNull
    private static final int[] codes = {
            COUNT,
            SUM,
            AVG,
            MIN,
            MAX
    };

    /**
     * Returns the codes for the window aggregate names.
     * @return the codes for the window aggregate names
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
