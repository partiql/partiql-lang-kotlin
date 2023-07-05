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

package org.partiql.plugins.mockdb

import org.partiql.spi.Plugin
import org.partiql.spi.connector.Connector
import org.partiql.spi.function.PartiQLFunction

/**
 * A mock implementation of [Plugin] to showcase how to retrieve [org.partiql.types.StaticType]'s from
 * a customer-defined JSON schema object. It uses the filesystem and a user-configured "root" to search for JSON files.
 */
class LocalPlugin : Plugin {
    override fun getConnectorFactories(): List<Connector.Factory> = listOf(LocalConnector.Factory())
    override fun getFunctions(): List<PartiQLFunction> {
        return emptyList()
    }
}
