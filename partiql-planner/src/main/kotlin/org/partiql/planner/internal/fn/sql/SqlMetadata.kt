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

package org.partiql.planner.internal.fn.sql

import org.partiql.spi.BindingPath
import org.partiql.spi.connector.ConnectorHandle
import org.partiql.spi.connector.ConnectorMetadata
import org.partiql.spi.connector.ConnectorPath
import org.partiql.spi.connector.ConnectorSession
import org.partiql.spi.connector.sql.info.InfoSchema
import org.partiql.spi.fn.FnExperimental

/**
 * An instance of [SqlMetadata]
 *
 * @property session
 * @property info
 */
public open class SqlMetadata(
    private val session: ConnectorSession,
    private val info: InfoSchema,
) : ConnectorMetadata {

    /**
     * TODO provide schemas from `info`.
     *
     * @param path
     * @return
     */
    override fun getObject(path: BindingPath): ConnectorHandle.Obj? = null

    @FnExperimental
    override fun getFunction(path: BindingPath): ConnectorHandle.Fn? {
        val cnf = path.steps.map { it.name.uppercase() }
        val name = cnf.last()
        val variants = info.functions.get(cnf).map { it.signature }
        if (variants.isEmpty()) {
            return null
        }
        return ConnectorHandle.Fn(ConnectorPath(cnf), org.partiql.planner.internal.fn.sql.SqlFn(name, variants))
    }

    @FnExperimental
    override fun getAggregation(path: BindingPath): ConnectorHandle.Agg? {
        val cnf = path.steps.map { it.name.uppercase() }
        val name = cnf.last()
        val variants = info.aggregations.get(cnf).map { it.signature }
        if (variants.isEmpty()) {
            return null
        }
        return ConnectorHandle.Agg(ConnectorPath(cnf), org.partiql.planner.internal.fn.sql.SqlAgg(name, variants))
    }
}
