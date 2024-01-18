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

import org.partiql.spi.connector.ConnectorBindings
import org.partiql.spi.connector.ConnectorHandle
import org.partiql.spi.connector.sql.info.InfoSchema
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental

/**
 * An implementation of [ConnectorBindings] including the INFORMATION_SCHEMA.
 */
public class SqlBindings(private val info: InfoSchema) : ConnectorBindings {

    @OptIn(PartiQLValueExperimental::class)
    public override fun getValue(handle: ConnectorHandle.Obj): PartiQLValue {
        TODO("Not yet implemented")
    }
}
