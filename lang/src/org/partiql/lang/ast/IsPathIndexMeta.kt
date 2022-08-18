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
 * This is used by the PartiQLVisitor to determine whether the path step is of type index. This is because it is
 * used to evaluate path expressions in FROM clauses in convertPathToProjectionItem().
 */
class IsPathIndexMeta private constructor() : Meta {
    override val tag = TAG

    companion object {
        const val TAG = "\$is_path_index"

        val instance = IsPathIndexMeta()
        val deserializer = MemoizedMetaDeserializer(TAG, instance)
    }
}
