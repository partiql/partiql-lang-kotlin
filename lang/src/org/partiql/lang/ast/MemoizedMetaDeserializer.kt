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

package org.partiql.lang.ast

import com.amazon.ion.IonValue

/**
 * Provides a common way to "deserialize" a memoized meta instance.
 *
 * "Memoized" metas are the same for every time they are used. This could be a "flag meta" (which has no properties
 * and the existence of the meta on a node is used to indicate a boolean condition) or meta of any time whos properties
 * do not vary from instance to instance.
 *
 * [MemoizedMetaDeserializer] is not appropriate for use with metas that have properties which can vary from
 * instance to instance or cannot otherwise be memoized.  Those need their own implementation of [MetaDeserializer].
 */
class MemoizedMetaDeserializer(override val tag: String, val instance: Meta) : MetaDeserializer {
    override fun deserialize(sexp: IonValue): Meta = instance
}