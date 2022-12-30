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

import org.jline.reader.Expander
import org.jline.reader.History

/**
 * Unfortunately we cannot use the default expander because the existing REPL commands are actually
 * Bash Event Designators which trigger history expansions.
 * See [Event Designators](https://www.gnu.org/software/bash/manual/html_node/Event-Designators.html)
 *
 * This means the command `!list_commands` which will reference `list_commands` in history. So the `list_commands`
 * will be substituted, but this isn't a valid command. Effectively, every `!` gets dropped and commands don't work.
 *
 * To avoid this, I would prefer to have our PartiQL shell align with bash, which means getting rid of command prefix `!`.
 * This is the workaround.
 */
object ShellExpander : Expander {

    override fun expandHistory(history: History?, line: String?): String = line ?: ""

    override fun expandVar(word: String?): String = word ?: ""
}
