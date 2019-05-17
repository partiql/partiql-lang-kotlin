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

package org.partiql.cli

import com.amazon.ion.*
import org.partiql.lang.eval.*

internal abstract class SqlCommand {
    abstract fun run()


    protected fun writeResult(result: ExprValue, writer: IonWriter): Int {
        var itemCount = 0
        result.rangeOver().forEach {
            it.ionValue.writeTo(writer)
            itemCount++
        }
        writer.flush()
        return itemCount
    }
}
