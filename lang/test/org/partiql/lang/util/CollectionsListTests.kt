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

import org.junit.Test
import org.partiql.lang.TestBase

/*
 * Copyright 2017 Amazon.com, Inc. or its affiliates.  All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at:
 *
 *     http://aws.amazon.com/apache2.0/
 *
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */

class CollectionsListTests : TestBase() {

    val isEven = { x: Int -> (x % 2) == 0}
    var empty :List<Int> = listOf()
    @Test fun forAllEmptyList() = assertTrue(empty.forAll(isEven))
    @Test fun forAllTrue() = assertTrue(listOf(2, 4, 6).forAll(isEven))
    @Test fun forAllFalse() = assertFalse(listOf(2, 3, 6).forAll(isEven))
}