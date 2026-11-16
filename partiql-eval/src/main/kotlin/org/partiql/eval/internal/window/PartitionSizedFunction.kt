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

package org.partiql.eval.internal.window

import org.partiql.eval.WindowFunction
import org.partiql.eval.WindowPartition

/**
 * Base class for window functions whose value depends on the number of rows in the current partition.
 */
internal abstract class PartitionSizedFunction : WindowFunction {

    /**
     * The number of rows in the current partition, captured on [reset].
     */
    protected var partitionSize: Long = 0
        private set

    override fun reset(partition: WindowPartition) {
        partitionSize = partition.size()
    }
}
