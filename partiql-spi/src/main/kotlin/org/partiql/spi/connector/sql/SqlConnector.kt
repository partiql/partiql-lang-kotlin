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

import org.partiql.spi.connector.Connector
import org.partiql.spi.connector.ConnectorAggProvider
import org.partiql.spi.connector.ConnectorBindings
import org.partiql.spi.connector.ConnectorFnProvider
import org.partiql.spi.connector.ConnectorMetadata
import org.partiql.spi.connector.ConnectorSession
import org.partiql.spi.connector.sql.info.InfoSchema
import org.partiql.spi.fn.FnExperimental

/**
 * An SQL-99 based [Connector] implementation.
 */
public abstract class SqlConnector : Connector {

    /**
     * Default SQL-99 INFORMATION_SCHEMA for use in function resolution.
     */
    public open val info: InfoSchema = InfoSchema.default()

    /**
     * Returns an implementation of [ConnectorMetadata] which provides the INFORMATION_SCHEMA and delegates
     * to a user-provided [ConnectorMetadata] implementation.
     *
     * @param session
     * @return
     */
    abstract override fun getMetadata(session: ConnectorSession): SqlMetadata

    override fun getBindings(): ConnectorBindings = SqlBindings(info)

    @FnExperimental
    override fun getFunctions(): ConnectorFnProvider = SqlFnProvider(info.functions)

    @FnExperimental
    override fun getAggregations(): ConnectorAggProvider = SqlAggProvider(info.aggregations)
}
