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

package org.partiql.lang.planner.transforms

import com.amazon.ionelement.api.StructElement
import org.partiql.spi.connector.ConnectorSession
import java.time.Instant

/**
 * Contains session information for the purposes of planning.
 *
 * @param queryId a unique identifier for a query. This will be passed to [ConnectorSession] during planning.
 * @param userId a unique identifier for a user. This will be passed to [ConnectorSession] during planning.
 * @param currentCatalog the current catalog of the session
 * @param currentDirectory the current "namespace" within the Catalog. This will aid in building
 * [org.partiql.spi.connector.ConnectorObjectPath]'s for unresolved variables.
 * @param instant the instant evaluation begins
 */
public class PlannerSession(
    public val queryId: String,
    public val userId: String,
    public val currentCatalog: String? = null,
    public val currentDirectory: List<String> = emptyList(),
    public val catalogConfig: Map<String, StructElement> = emptyMap(),
    public val instant: Instant = Instant.now()
) {
    internal fun toConnectorSession(): ConnectorSession = object : ConnectorSession {
        override fun getQueryId(): String = queryId
        override fun getUserId(): String = userId
    }
}
