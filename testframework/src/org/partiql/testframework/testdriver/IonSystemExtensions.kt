
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

package org.partiql.testframework.testdriver

import com.amazon.ion.*

fun IonSystem.newList(s: kotlin.collections.Iterable<String>): IonList {
    val l = newEmptyList()
    for(i in s) {
        l.add(newString(i))
    }
    return l
}

fun <V : IonValue> IonSystem.structOf(vararg pairs: Pair<String, V>): IonStruct {
    val s = this.newEmptyStruct()
    for(p in pairs) {
        s.put(p.first, p.second)
    }
    return s
}

fun IonList.addIf(c: Boolean, v: IonValue) {
    if (c) {
        this.add(v)
    }
}

fun IonStruct.putIfValueNotNull(name:  String, v: IonValue?) {
    if (v != null && v.isNullValue) {
        this.put(name, v)
    }
}
