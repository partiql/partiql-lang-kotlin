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

package org.partiql.lang.util

import java.time.ZoneOffset
import kotlin.math.absoluteValue

/**
 * Returns the string representation of the [ZoneOffset] in HH:mm format.
 */
fun ZoneOffset.getOffsetHHmm(): String {
    return "${if(totalSeconds >= 0) "+" else "-"}${(totalSeconds / 3600).absoluteValue.toString().padStart(2, '0')}:${((totalSeconds / 60) % 60).absoluteValue.toString().padStart(2, '0')}"
}
