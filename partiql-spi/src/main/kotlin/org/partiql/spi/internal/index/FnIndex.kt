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

package org.partiql.spi.internal.fn

import org.partiql.types.function.FunctionSignature

/**
 *
 *
 * @param T
 */
internal interface FnIndex<T : FunctionSignature> {

    fun search(path: List<String>): List<T>

    fun insert(path: List<String>, variants: List<T>)
}
