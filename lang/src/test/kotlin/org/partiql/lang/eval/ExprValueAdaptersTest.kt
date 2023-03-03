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

import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.partiql.lang.util.newFromIonText

class ExprValueAdaptersTest {
    @Test
    fun asNamed() {
        val value = newFromIonText("5")
        val named = value.asNamed()
        assertSame(value, named.name)
    }

    @Test
    fun unnamedValue() {
        val value = newFromIonText("{a:5}").bindings[BindingName("a", BindingCase.SENSITIVE)]!!
        assertNotNull(value.name)
        assertNull(value.unnamedValue().name)
    }
}
