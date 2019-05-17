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



fun IonValue.interpolate(variables: IonStruct, ions: IonSystem): IonValue =
    when (this) {
        is IonSymbol -> {
            val symbolText = symbolValue().text
            if (symbolText.startsWith('$')) {
                val variableName = symbolText.substring(1)
                variables[variableName] ?: throw UndefinedVariableInterpolationException(variableName)
            }
            else {
                this
            }.clone()
        }

        is IonString -> {
            ions.newString(stringValue().interpolate(variables, ions))
        }

        is IonList, is IonSexp -> {
            val newList = if (type == IonType.LIST) ions.newEmptyList() else ions.newEmptySexp()
            val oldList = this as IonContainer
            oldList.forEach {
                newList.add(it.interpolate(variables, ions))
            }
            newList
        }

        is IonStruct -> {
            val newStruct = ions.newEmptyStruct()
            forEach {
                newStruct.add(it.fieldName, it.interpolate(variables, ions))
            }
            newStruct
        }

        else -> {
            this.clone()
        }
    }
