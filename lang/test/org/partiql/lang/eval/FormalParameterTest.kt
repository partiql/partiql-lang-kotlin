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

package org.partiql.lang.eval

import org.partiql.lang.types.SingleFormalParameter
import org.partiql.lang.types.StaticType
import org.partiql.lang.types.VarargFormalParameter
import org.junit.Assert.*
import org.junit.Test

class FormalParameterTest {
    @Test
    fun paramToString() = assertEquals("BOOL", SingleFormalParameter(StaticType.BOOL).toString())

    @Test
    fun varargToString() = assertEquals("BOOL...", VarargFormalParameter(StaticType.BOOL).toString())
}