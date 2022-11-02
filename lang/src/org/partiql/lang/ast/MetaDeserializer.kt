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

// Don't need warnings about ExprNode deprecation.
@file:Suppress("DEPRECATION")

package org.partiql.lang.ast

import com.amazon.ion.IonValue

/**
 * Deserializes an instance of a node meta from its s-expression representation.
 */
interface MetaDeserializer {
    /**
     * The tag of the meta which this [MetaDeserializer] can deserialize.
     */
    val tag: String

    /**
     * Perform deserialization of the given s-exp into an instance of [Meta].
     */
    fun deserialize(sexp: IonValue): Meta
}
