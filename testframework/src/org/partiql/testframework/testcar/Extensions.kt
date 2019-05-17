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

import com.amazon.ion.*
import org.partiql.lang.errors.*
import java.io.*

fun PropertyValueMap.toStruct(ions: IonSystem): IonStruct {
    val propertiesStruct = ions.newEmptyStruct()
    getProperties().forEach {
        propertiesStruct.put(it.toString(), this[it]?.toIonValue(ions) ?: ions.newNull())
    }
    return propertiesStruct
}

fun PropertyValue.toIonValue(ions: IonSystem): IonValue =
    when(type) {
        PropertyType.LONG_CLASS      -> ions.newInt(longValue())
        PropertyType.STRING_CLASS    -> ions.newString(stringValue())
        PropertyType.INTEGER_CLASS   -> ions.newInt(integerValue())
        PropertyType.TOKEN_CLASS     -> ions.newString(tokenTypeValue().toString())
        PropertyType.ION_VALUE_CLASS -> ionValue()
    }.clone()

fun Throwable.getStackTraceString(): String {
    val sw = StringWriter()
    this.printStackTrace(PrintWriter(sw))
    return sw.toString()
}