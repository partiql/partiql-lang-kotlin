/*
 * Copyright Amazon.com, Inc. or its affiliates.  All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at:
 *
 *      http://aws.amazon.com/apache2.0/
 *
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */

package org.partiql.planner.transforms

import org.partiql.lang.domains.PartiqlAst
import org.partiql.planner.impl.interruptibleFold

/**
 * A simple visitor transformer that provides a pipeline of transformers to be executed in sequential order.
 *
 * @param transformers visitor transforms to be executed
 */
public class PipelinedVisitorTransform(vararg transformers: PartiqlAst.VisitorTransform) : PartiqlAst.VisitorTransform() {
    private val transformerList = transformers.toList()

    override fun transformStatement(node: PartiqlAst.Statement): PartiqlAst.Statement =
        transformerList.interruptibleFold(node) {
            intermediateNode, transformer ->
            transformer.transformStatement(intermediateNode)
        }

    public fun appendVisitorTransforms(vararg newVisitorTransforms: PartiqlAst.VisitorTransform): PipelinedVisitorTransform =
        PipelinedVisitorTransform(*(transformerList + newVisitorTransforms.toList()).toTypedArray())
}
