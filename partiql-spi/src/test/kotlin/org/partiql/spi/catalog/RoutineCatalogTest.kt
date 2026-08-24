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

package org.partiql.spi.catalog

import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.partiql.spi.function.AggOverload
import org.partiql.spi.function.FnOverload
import org.partiql.spi.function.MemRoutineProvider

class RoutineCatalogTest {

    @Test
    fun hostCatalogCanDelegateExactLookupToProvider() {
        val function = FnOverload.Builder("pow").build()
        val provider = MemRoutineProvider.builder()
            .register(function, Namespace.of("math"))
            .build()
        val catalog = object : RoutineCatalog {
            override fun getName(): String = "andes"

            override fun resolveFunctions(
                session: Session,
                identifier: Identifier,
            ): Collection<RoutineBinding<FnOverload>> = provider.getFunctions(identifier)

            override fun resolveAggregations(
                session: Session,
                identifier: Identifier,
            ): Collection<RoutineBinding<AggOverload>> = provider.getAggregations(identifier)
        }

        val binding = catalog.resolveFunctions(
            Session.empty(),
            Identifier.regular("math", "pow"),
        ).single()

        assertSame(function, binding.overloads.single())
    }
}
