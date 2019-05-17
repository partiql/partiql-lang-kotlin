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

/**
 * Tests that all files in example_scripts work
 */
class ExamplesTest {

    private fun scriptFilesIn(directoryPath: String) = File(directoryPath)
        .listRecursive(TEST_SCRIPT_FILTER)
        .map { File(it.absolutePath) }

    @Test
    fun passingExamples() {
        val passingPaths = scriptFilesIn("example_scripts/passing")
        passingPaths.forEach {
            val cli = createCli()

            cli.run(listOf(it))

            assertThat(cli.passCount)
                .isGreaterThan(0)
                .withFailMessage("Some tests should have executed")

            assertThat(cli.assertsFailedCount)
                .isEqualTo(0)
                .withFailMessage("No asserts should have failed")

            assertThat(cli.commandSendCount)
                .isGreaterThan(0)
                .withFailMessage("Some commands should have been sent")
        }
    }

    @Test
    fun failingExamples() {
        val failingPaths = scriptFilesIn("example_scripts/failing")
        failingPaths.forEach {
            assertThatThrownBy { createCli().run(listOf(it)) }
                .withFailMessage("$it should have been a failing execution")
                .isInstanceOf(TestSuiteException::class.java)
        }
    }
}