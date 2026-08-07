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

package org.partiql.spi.function;

import org.junit.jupiter.api.Test;
import org.partiql.spi.types.PType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoutineProviderJavaTest {

    @Test
    void providerMethodsDefaultToEmptyInventories() {
        RoutineProvider provider = new RoutineProvider() {
        };

        assertTrue(provider.getFunctions().isEmpty());
        assertTrue(provider.getAggregations().isEmpty());
    }

    @Test
    void signatureRejectsNullMetadataAtConstruction() {
        assertThrows(
            NullPointerException.class,
            () -> new RoutineOverloadSignature(null, Collections.emptyList())
        );
        assertThrows(
            NullPointerException.class,
            () -> new RoutineOverloadSignature("invalid", null)
        );

        List<PType> parameterTypes = new ArrayList<>();
        parameterTypes.add(null);
        assertThrows(
            NullPointerException.class,
            () -> new RoutineOverloadSignature("invalid", parameterTypes)
        );
    }
}
