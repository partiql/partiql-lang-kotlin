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
import org.partiql.spi.catalog.Identifier;
import org.partiql.spi.catalog.Namespace;
import org.partiql.spi.catalog.RoutineBinding;
import org.partiql.spi.types.PType;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoutineProviderJavaTest {

    @Test
    void providerMethodsDefaultToEmptyExactLookups() {
        RoutineProvider provider = new RoutineProvider() {
        };
        Identifier missing = Identifier.regular("missing");

        assertTrue(provider.getFunctions(missing).isEmpty());
        assertTrue(provider.getAggregations(missing).isEmpty());
        assertThrows(NullPointerException.class, () -> provider.getFunctions(null));
        assertThrows(NullPointerException.class, () -> provider.getAggregations(null));
    }

    @Test
    void memoryProviderBuilderIsUsableFromJava() {
        FnOverload function = new FnOverload.Builder("pow")
            .addParameters(PType.integer(), PType.integer())
            .build();
        RoutineProvider provider = MemRoutineProvider.builder()
            .register(function, Namespace.of("math"))
            .build();

        Collection<RoutineBinding<FnOverload>> matches =
            provider.getFunctions(Identifier.regular("math", "pow"));

        assertSame(function, matches.iterator().next().getOverloads().get(0));
        assertThrows(UnsupportedOperationException.class, matches::clear);
    }
}
