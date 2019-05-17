
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

package org.partiql.testframework.testcar

import com.amazon.ion.system.*
import org.partiql.testframework.contracts.*
import org.assertj.core.api.Assertions.*
import org.junit.*
import kotlin.test.*

class TestCarTest {
    private val ions = IonSystemBuilder.standard().build()!!
    private var car = ReferenceSqlCar(ions)

    @Before
    fun beforeTest() {
        car = ReferenceSqlCar(ions)
    }

    @Test
    fun mergeStructs_duplicateField() {
        val structs = listOf(ions.newEmptyStruct(), ions.newEmptyStruct())
            .apply { forEach { it.put("duplicate", ions.newString("field")) } }

        val thrown = catchThrowable { car.mergeStructs(structs) }
        assertThat(thrown)
            .isInstanceOf(ReferenceSqlCar.CarErrorException::class.java)
            .hasMessage("The field name 'duplicate' was used more than once in the set of structs to be merged.")
    }

    @Test
    fun execute_happy() {
        val response = car.executeCmd(ExecuteTestCommand("name", "1", null, null, null))
        val successResponse = response as? ExecuteSuccess
        assertNotNull(successResponse)
        assertEquals(ions.newInt(1), successResponse?.value)
    }

    //TODO:  execute command with query and environment
    //TODO:  execute command with query nad compile options
    //TODO:  execute command with query and session

    //TODO:  execute command with failing query

    //TODO:  set environment with populated struct
    //TODO:  set environment with empty struct

    //TODO:  set compile options with populated struct
    //TODO:  set compile options with empty struct

    //TODO:  set session with populated struct
    //TODO:  set session with empty struct

}
