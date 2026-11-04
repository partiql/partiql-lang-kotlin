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

package org.partiql.extensions.functions;

import org.junit.jupiter.api.Test;
import org.partiql.extensions.functions.compat.datetime.DateAdd;
import org.partiql.extensions.functions.compat.datetime.UtcNow;
import org.partiql.extensions.functions.custom.collection.Contains;
import org.partiql.extensions.functions.custom.conversion.HexToBigInt;
import org.partiql.extensions.functions.custom.datetime.DateTrunc;
import org.partiql.extensions.functions.custom.datetime.ToUnixTime;
import org.partiql.extensions.functions.custom.math.Pow;
import org.partiql.spi.function.Fn;
import org.partiql.spi.function.FnOverload;
import org.partiql.spi.types.PType;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FunctionExportsTest {

    @Test
    public void exportsExactSignatures() {
        assertEquals(6, Pow.overloads().size());
        for (PType type : Arrays.asList(
            PType.tinyint(),
            PType.smallint(),
            PType.integer(),
            PType.bigint(),
            PType.real(),
            PType.doublePrecision()
        )) {
            assertSignature(
                FunctionTestSupport.overloadFor(Pow.overloads(), type, type),
                "pow",
                Arrays.asList(type, type),
                PType.doublePrecision()
            );
        }

        assertSignature(
            Contains.overloads().get(0),
            "contains",
            Arrays.asList(PType.array(), PType.string()),
            PType.bool()
        );
        assertSignature(
            ToUnixTime.overloads().get(0),
            "to_unixtime",
            Collections.singletonList(PType.timestamp()),
            PType.bigint()
        );
        assertSignature(
            HexToBigInt.overloads().get(0),
            "hex_to_bigint",
            Collections.singletonList(PType.string()),
            PType.bigint()
        );
        assertSignature(
            DateAdd.overloads().get(0),
            "date_add",
            Arrays.asList(PType.string(), PType.integer(), PType.timestamp()),
            PType.timestamp()
        );
        assertEquals(2, DateTrunc.overloads().size());
        for (PType type : Arrays.asList(PType.timestamp(), PType.date())) {
            assertSignature(
                FunctionTestSupport.overloadFor(DateTrunc.overloads(), PType.string(), type),
                "date_trunc",
                Arrays.asList(PType.string(), type),
                PType.timestamp()
            );
        }
        assertSignature(
            UtcNow.overloads().get(0),
            "utcnow",
            Collections.emptyList(),
            PType.timestamp()
        );
    }

    @Test
    public void exportsStableImmutableCollections() {
        assertStableAndImmutable(Pow.overloads(), Pow.overloads());
        assertStableAndImmutable(Contains.overloads(), Contains.overloads());
        assertStableAndImmutable(HexToBigInt.overloads(), HexToBigInt.overloads());
        assertStableAndImmutable(ToUnixTime.overloads(), ToUnixTime.overloads());
        assertStableAndImmutable(DateAdd.overloads(), DateAdd.overloads());
        assertStableAndImmutable(UtcNow.overloads(), UtcNow.overloads());
        assertStableAndImmutable(DateTrunc.overloads(), DateTrunc.overloads());
    }

    private static void assertSignature(
        FnOverload overload,
        String name,
        List<PType> parameterTypes,
        PType returnType
    ) {
        assertEquals(name, overload.getSignature().getName());
        assertEquals(parameterTypes, overload.getSignature().getParameterTypes());

        Fn function = FunctionTestSupport.function(
            overload,
            parameterTypes.toArray(new PType[0])
        );
        assertEquals(returnType, function.getSignature().getReturns());
        assertTrue(function.getSignature().isNullCall());
        assertTrue(function.getSignature().isMissingCall());
    }

    private static void assertStableAndImmutable(
        List<FnOverload> first,
        List<FnOverload> second
    ) {
        assertSame(first, second);
        assertThrows(UnsupportedOperationException.class, first::clear);
    }
}
