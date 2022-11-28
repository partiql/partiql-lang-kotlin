/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates.  All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 *  You may not use this file except in compliance with the License.
 * A copy of the License is located at:
 *
 *      http://aws.amazon.com/apache2.0/
 *
 *  or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 *  language governing permissions and limitations under the License.
 */

package org.partiql.lang.ast

/**
 * The old AST has nodes: `not_like`, `not_between` and `is_not` which are respectively paired with its
 * `like`, `between`, and `is` nodes.
 *
 * The new AST lacks the `not` version of these, instead wrapping the non-`not` versions of these notes in a `not`
 * n-ary expression to achieve the same semantics.
 *
 * For example:
 *  - Legacy:  (not_like <expr>)
 *  - New AST (will be something like):
 *              (nary
 *                  (op not)
 *                  (args (nary like <expr>))
 *                  (metas ((name LegacyLogicalNotMeta))))
 *
 * [LegacyLogicalNotMeta] is added to `(nary (op not) ...` node so that the [ToLegacyAstPass] knows to emit the
 * `not_like`, `not_between` or `is_not` s-expression nodes.
 */
class LegacyLogicalNotMeta private constructor() : Meta {
    override val tag = TAG
    companion object {
        const val TAG = "\$legacy_logical_not"

        val instance = LegacyLogicalNotMeta()
    }
}
