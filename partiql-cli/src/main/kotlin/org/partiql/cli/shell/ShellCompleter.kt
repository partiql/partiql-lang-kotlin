/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates.  All rights reserved.
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

package org.partiql.cli.shell

import org.jline.reader.Completer
import org.jline.reader.impl.completer.AggregateCompleter
import org.jline.reader.impl.completer.ArgumentCompleter
import org.jline.reader.impl.completer.NullCompleter
import org.jline.reader.impl.completer.StringsCompleter
import org.partiql.cli.shell.ShellCompleter.getAllCommands

internal object ShellCompleter : AggregateCompleter(getAllCommands()) {

    @JvmStatic
    private fun getAllCommands(): List<Completer> = listOf(
        buildCompoundCommand("SELECT"),
        buildCompoundCommand("REMOVE"),
        buildCompoundCommand("UPSERT"),
        buildCompoundCommand("REPLACE"),
        buildCompoundCommand("EXEC"),
        buildCompoundCommand("CREATE", listOf("INDEX", "TABLE")),
        buildCompoundCommand("DROP", listOf("INDEX", "TABLE")),
        buildCompoundCommand("INSERT", listOf("INTO")),
    )

    @JvmStatic
    private fun buildCompoundCommand(primary: String, targets: List<String> = emptyList()) = ArgumentCompleter(
        StringsCompleter(primary),
        StringsCompleter(targets),
        NullCompleter.INSTANCE
    )
}
