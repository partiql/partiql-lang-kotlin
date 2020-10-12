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

package org.partiql.lang.eval.visitors

import com.amazon.ionelement.api.MetaContainer
import com.amazon.ionelement.api.emptyMetaContainer
import org.partiql.lang.domains.PartiqlAst

class MetaStrippingVisitorTransform : PartiqlAst.VisitorTransform() {
    companion object {
        private val emptyMetas = emptyMetaContainer()
        fun stripMetas(node: PartiqlAst.Expr): PartiqlAst.Expr {
            val visitorTransform = MetaStrippingVisitorTransform()
            return visitorTransform.transformExpr(node)
        }
    }

    override fun transformMetas(metas: MetaContainer): MetaContainer = emptyMetas

}
