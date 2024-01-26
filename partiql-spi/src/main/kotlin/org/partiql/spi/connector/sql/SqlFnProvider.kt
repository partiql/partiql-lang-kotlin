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

package org.partiql.spi.connector.sql

import org.partiql.spi.connector.ConnectorFnProvider
import org.partiql.spi.connector.ConnectorHandle
import org.partiql.spi.fn.FnAggregation
import org.partiql.spi.fn.FnExperimental
import org.partiql.spi.fn.FnIndex
import org.partiql.spi.fn.FnScalar

/**
 * A basic [ConnectorFnProvider] over an [FnIndex].
 */
@OptIn(FnExperimental::class)
public class SqlFnProvider(private val index: FnIndex) : ConnectorFnProvider {

    override fun getFnScalar(handle: ConnectorHandle.Fn, specific: String): FnScalar? {
        val path = handle.path
        val fn = index.get(path, specific)
        return if (fn is FnScalar) fn else null
    }

    override fun getFnAggregation(handle: ConnectorHandle.Fn, specific: String): FnAggregation? {
        val path = handle.path
        val fn = index.get(path, specific)
        return if (fn is FnAggregation) fn else null
    }
}
