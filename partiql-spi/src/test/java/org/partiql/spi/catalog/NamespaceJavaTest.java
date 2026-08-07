/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package org.partiql.spi.catalog;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertThrows;

class NamespaceJavaTest {

    @Test
    void rejectsNullNamespaceLevels() {
        assertThrows(
            NullPointerException.class,
            () -> Namespace.of("example", null)
        );
        assertThrows(
            NullPointerException.class,
            () -> Namespace.of(Arrays.asList("example", null))
        );
    }

    @Test
    void nameRejectsNullParts() {
        assertThrows(
            NullPointerException.class,
            () -> Name.of(Arrays.asList(null, "function"))
        );
        assertThrows(
            NullPointerException.class,
            () -> Name.of(Arrays.asList("namespace", null))
        );
    }
}
