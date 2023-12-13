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

package org.partiql.spi

import org.partiql.spi.connector.Connector
import org.partiql.spi.function.PartiQLFunction

/**
 * A singular unit of external logic.
 */
public interface Plugin {

    /**
     * A [Connector.Factory] is used to instantiate a connector.
     */
    public val factory: Connector.Factory

    /**
     * Functions defined by this plugin.
     */
    public val functions: List<PartiQLFunction>
}
