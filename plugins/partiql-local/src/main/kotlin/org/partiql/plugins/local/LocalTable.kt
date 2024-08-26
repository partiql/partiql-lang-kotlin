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

package org.partiql.plugins.local

import com.amazon.ion.system.IonReaderBuilder
import com.amazon.ionelement.api.loadSingleElement
import org.partiql.eval.value.Datum
import org.partiql.planner.catalog.Name
import org.partiql.planner.catalog.Table
import org.partiql.types.PType
import org.partiql.types.StaticType
import java.nio.file.Path
import kotlin.io.path.isDirectory
import kotlin.io.path.reader

/**
 * Associate a resolved path with a [StaticType]
 */
internal class LocalTable(
    private val name: Name,
    private val path: Path,
) : Table {

    init {
        assert(!path.isDirectory()) { "LocalTable path must be a file." }
    }

    override fun getName(): Name = name

    override fun getSchema(): PType {
        val reader = IonReaderBuilder.standard().build(path.reader())
        val element = loadSingleElement(reader)
        val staticType = element.toStaticType()
        return PType.fromStaticType(staticType)
    }

    // TODO for now files are `type` only.
    override fun getDatum(): Datum = Datum.nullValue()
}
