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

package org.partiql.parser

import org.partiql.parser.impl.PartiQLParserDefault

/**
 * A builder class to instantiate a [PartiQLParser].
 */
public class PartiQLParserBuilder {

    companion object {

        @JvmStatic
        fun standard(): PartiQLParserBuilder {
            return PartiQLParserBuilder()
        }
    }

    public fun build(): PartiQLParser {
        return PartiQLParserDefault()
    }
}
