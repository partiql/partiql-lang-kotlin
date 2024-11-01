/*
 * Copyright Amazon.com, Inc. or its affiliates.  All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at:
 *
 *      http://aws.amazon.com/apache2.0/
 *
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */

package org.partiql.parser;

import org.jetbrains.annotations.NotNull;
import org.partiql.parser.internal.PartiQLParserDefaultV1;

/**
 * A builder class to instantiate a [PartiQLParserV1]. https://github.com/partiql/partiql-lang-kotlin/issues/1632
 * TODO replace with Lombok builder once [PartiQLParserV1] is migrated to Java.
 */
public class PartiQLParserBuilderV1 {

    @NotNull
    public PartiQLParserV1 build() {
        return new PartiQLParserDefaultV1();
    }
}
