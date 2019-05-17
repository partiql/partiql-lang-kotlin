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

package org.partiql.testframework.testdriver

import org.partiql.testframework.*
import org.assertj.core.api.Assertions.*
import org.junit.*
import java.io.*
import kotlin.test.*

class CliTest  {

    private val cli = createCli()

    @Test
    fun runNoArgs() {
        assertThatThrownBy { cli.run(listOf()) }
            .isExactlyInstanceOf(ValidationException::class.java)
            .withFailMessage(
                "Incorrect number of command-line arguments, expected 1, got: 0.\n" +
                "usage: testdriver <path to scripts>")
    }

    @Test
    fun runNonExistingFile() {
        assertThatThrownBy { cli.run(listOf(File("I don't exist"))) }
            .isExactlyInstanceOf(FatalException::class.java)
            .hasMessageContaining("'I don't exist' not found")
    }

    @Test
    fun runSingleFile() {
        cli.run(listOf(File("test-resources/single_working.sqlts")))
        assertCounts(cli, passCount = 1, assertsFailedCount = 0, commandSentCount = 1)
    }

    @Test
    fun runSingleFileWithErrors() {
        assertThatThrownBy { cli.run(listOf(File("test-resources/single_not_working.sqlts"))) }
            .isExactlyInstanceOf(ExecutionException::class.java)

        assertCounts(cli, passCount = 0, assertsFailedCount = 1, commandSentCount = 1)
    }

    @Test
    fun runDirectoryWithSubdirectories() {
        assertThatThrownBy { cli.run(listOf(File("test-resources/"))) }
            .isExactlyInstanceOf(ExecutionException::class.java)

        assertCounts(cli, passCount = 2, assertsFailedCount = 2, commandSentCount = 4)
    }

    private fun assertCounts(cli: Cli, passCount: Int, assertsFailedCount: Int, commandSentCount: Int) {
        assertEquals(cli.passCount, passCount)
        assertEquals(cli.assertsFailedCount, assertsFailedCount)
        assertEquals(cli.commandSendCount, commandSentCount)
    }
}