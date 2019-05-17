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

import org.partiql.testframework.testcar.*
import org.partiql.testframework.testdriver.error.*
import org.partiql.testframework.testdriver.visitors.*
import org.junit.*
import java.io.*
import kotlin.test.*

/*
 * Note that instead of `$` here we are using `#` ([parseScript] will replace all `#` with `$`) because kotlin
 * has no way to escape multi-line strings in a clean fashion. That means to insert a `$` in a multi-line string
 * we have to do this: `${'$'}` which IMHO is unreadable and ugly.
 */
class ForTest : TestBase() {

    @Test
    fun noFailedAssertsBecauseSqlExecutesSuccessfullyWhenExpected() {
        parseScript("""
            for::{
                template: [ test::{ name: 'test', sql: "#{sql}", expected: #expected }],
                variableSets: [ { sql: "1 + 1", expected: result::2 } ],
            }
        """).accept(visitor)

        assertEquals(1, resultStream.passCount)
        assertEquals(1, visitor.commandSendCount)
        assertEquals(0, resultStream.errorCount)
        assertEquals(0, resultStream.assertsFailedCount)
    }

    @Test
    fun noFailedAssertsBecauseSqlFailedExecutionWhenExpected() {
        parseScript("""
            for::{
                template: [
                    test::{
                        name: 'test',
                        sql: "#{sql}",
                        expected: #expected
                    }
                ],
                variableSets: [{
                    sql: "\n\n    ~",
                    expected: error::{
                        code:"LEXER_INVALID_CHAR",
                        properties:{
                            LINE_NUMBER:3,
                            COLUMN_NUMBER: 5,
                            TOKEN_STRING: "'~' [U+7e]"
                        }
                    }
                }]
            }
            """).accept(visitor)

        assertEquals(1, resultStream.passCount)
        assertEquals(1, visitor.commandSendCount)
        assertEquals(0, resultStream.errorCount)
        assertEquals(0, resultStream.assertsFailedCount)
    }

    @Test
    fun noFailedAssertsBecauseMissingValueWasExpected() {
        parseScript("""
            for::{
                template: [
                    test::{
                        name: 'test',
                        compile_options: { undefinedVariable: #undefinedVariable },
                        sql: "#{sql}",
                        expected: #expected
                    }
                ],
                variableSets: [{
                    sql: "a",
                    undefinedVariable: MISSING,
                    expected: missing::null
                }]
            }
        """).accept(visitor)

        assertEquals(1, resultStream.passCount)
        assertEquals(1, visitor.commandSendCount)
        assertEquals(0, resultStream.errorCount)
        assertEquals(0, resultStream.assertsFailedCount)
    }


    @Test
    fun noFailedAssertsBecauseMissingValueWasExpected_whole_struct() {
        parseScript("""
            for::{
                template: [
                    test::{
                        name: 'test',
                        compile_options: #compileOptions,
                        sql: "#{sql}",
                        expected: #expected
                    }
                ],
                variableSets: [{
                    sql: "a",
                    compileOptions: { undefinedVariable: MISSING },
                    expected: missing::null
                }]
            }
        """).accept(visitor)

        assertEquals(1, resultStream.passCount)
        assertEquals(1, visitor.commandSendCount)
        assertEquals(0, resultStream.errorCount)
        assertEquals(0, resultStream.assertsFailedCount)
    }

    @Test
    fun unexpectedResult() {
        val script = parseScript("""
            for::{
                template: [ test::{ name: 'test', sql: #sql, expected: #expected }],
                variableSets: [ { sql: "1 + 1", expected: result::3 } ],
            }
        """)
        script.accept(visitor)

        assertEquals(0, resultStream.passCount)
        assertEquals(1, visitor.commandSendCount)
        assertEquals(0, resultStream.errorCount)
        assertEquals(1, resultStream.assertsFailedCount)
    }
}