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

package org.partiql.cli

import com.amazon.ion.system.*
import org.partiql.lang.eval.*
import org.partiql.lang.syntax.*
import org.junit.*
import org.junit.Assert.*
import org.partiql.lang.*
import java.io.*
import java.util.concurrent.*
import kotlin.concurrent.*

const val SLEEP_TIME = 5L

class RequiredFlushOutputStream : OutputStream() {
    private val backingOS = ByteArrayOutputStream()
    private var availableSize = 0

    override fun write(b: Int) {
        backingOS.write(b)
    }

    fun size() = availableSize

    fun reset() {
        availableSize = 0
        backingOS.reset()
    }

    override fun flush() {
        availableSize = backingOS.size()
    }

    override fun write(b: ByteArray?) {
        backingOS.write(b)
    }

    override fun write(b: ByteArray?, off: Int, len: Int) {
        backingOS.write(b, off, len)
    }

    override fun close() {
        backingOS.close()
    }

    override fun toString() = backingOS.toString("UTF-8")
}

private class ReplTester(bindings: Bindings<ExprValue> = Bindings.empty()) {
    val ion = IonSystemBuilder.standard().build()
    val parser: Parser = SqlParser(ion)
    val compiler = CompilerPipeline.build(ion) { sqlParser(parser) }
    val valueFactory = ExprValueFactory.standard(ion)

    private val input = PipedInputStream()
    private val inputPipe = PipedOutputStream(input)
    private val output = RequiredFlushOutputStream()

    private val zeroTimer = object : Timer {
        override fun <T> timeIt(block: () -> T): Long {
            block()
            return 0
        }
    }

    private val repl = Repl(valueFactory, input, output, parser, compiler, bindings, zeroTimer)

    val partiqlVersionAndHash = repl.retrievePartiQLVersionAndHash()

    private val actualReplPrompt = StringBuilder()

    private val outputPhaser = Phaser()

    private val replThread = thread(start = false) { repl.run() }
    private val outputCollectorThread = thread(start = false) {
        outputPhaser.register()

        while (replThread.isAlive || output.size() > 0) {
            Thread.sleep(SLEEP_TIME)
            if (output.size() > 0) {
                actualReplPrompt.append(output.toString())
                output.reset()
                outputPhaser.arrive()
            }
        }

        outputPhaser.arriveAndDeregister()
    }

    fun assertReplPrompt(expectedPromptText: String) {
        outputPhaser.register()


        replThread.start()
        outputCollectorThread.start()

        // wait for the output thread to register
        while (outputPhaser.registeredParties != 2) {
            Thread.sleep(SLEEP_TIME)
        }

        // wait for the REPL to print the initial message and prompt
        outputPhaser.arriveAndAwaitAdvance()

        val inputLines = extractInputLines(expectedPromptText)
        inputLines.forEach { line ->
            actualReplPrompt.append(line)

            inputPipe.write(line.toByteArray(Charsets.UTF_8))
            // flush to repl input
            inputPipe.flush()

            // wait for output to be written before inputting more
            outputPhaser.arriveAndAwaitAdvance()
        }

        // nothing more to write
        inputPipe.close()

        // make sure output was written
        outputPhaser.arriveAndAwaitAdvance()

        assertEquals(expectedPromptText, actualReplPrompt.toString())
    }

    private fun extractInputLines(expectedPromptText: String): List<String> =
        expectedPromptText.split("\n")
            .filter { line ->
                line.startsWith(PROMPT_2) || (line.startsWith(PROMPT_1) && line != PROMPT_1)
            }
            .map { it.removePrefix(PROMPT_1) }
            .map { it.removePrefix(PROMPT_2) }
            .map { line -> "$line\n" } // add back the \n removed in the split

}

@Ignore("https://github.com/partiql/partiql-lang-kotlin/issues/266")
class ReplTest {
    private val partiqlVersionAndHash = ReplTester().partiqlVersionAndHash

    @Test
    fun singleQuery() {
        ReplTester().assertReplPrompt("""
            #Welcome to the PartiQL REPL!
            #Using version: $partiqlVersionAndHash
            #PartiQL> 1+1
            #   | 
            #===' 
            #2
            #--- 
            #OK!
            #PartiQL> 
        """.trimMargin("#"))
    }

    @Test
    fun querySemiColon() {
        ReplTester().assertReplPrompt("""
            #Welcome to the PartiQL REPL!
            #Using version: $partiqlVersionAndHash
            #PartiQL> 1+1;
            #===' 
            #2
            #--- 
            #OK!
            #PartiQL> 
        """.trimMargin("#"))
    }

    @Test
    fun multipleQuery() {
        ReplTester().assertReplPrompt("""
            #Welcome to the PartiQL REPL!
            #Using version: $partiqlVersionAndHash
            #PartiQL> 1 + 1
            #   | 
            #===' 
            #2
            #--- 
            #OK!
            #PartiQL> 2 + 2
            #   | 
            #===' 
            #4
            #--- 
            #OK!
            #PartiQL> 
        """.trimMargin("#"))

    }

    @Test
    fun astWithoutMetas() {
        ReplTester().assertReplPrompt("""
            #Welcome to the PartiQL REPL!
            #Using version: $partiqlVersionAndHash
            #PartiQL> 1 + 1
            #   | !!
            #===' 
            #
            #(
            #  query
            #  (
            #    plus
            #    (
            #      lit
            #      1
            #    )
            #    (
            #      lit
            #      1
            #    )
            #  )
            #)
            #--- 
            #OK!
            #PartiQL> 
        """.trimMargin("#"))
    }

    @Test
    fun addToGlobalEnvAndQuery() {
        ReplTester().assertReplPrompt("""
            #Welcome to the PartiQL REPL!
            #Using version: $partiqlVersionAndHash
            #PartiQL> !add_to_global_env {'myTable': <<{'a':1}, {'a': 2}>>}
            #   | 
            #===' 
            #{
            #  'myTable': <<
            #    {
            #      'a': 1
            #    },
            #    {
            #      'a': 2
            #    }
            #  >>
            #}
            #--- 
            #OK!
            #PartiQL> SELECT * FROM myTable
            #   | 
            #===' 
            #<<
            #  {
            #    'a': 1
            #  },
            #  {
            #    'a': 2
            #  }
            #>>
            #--- 
            #OK!
            #PartiQL> 
        """.trimMargin("#"))
    }

    @Test
    fun dumpInitialEnv() {
        val replTester = ReplTester()
        val initialBindings = replTester.compiler
            .compile("{'foo': [1,2,3]}")
            .eval(EvaluationSession.standard())
            .bindings

        ReplTester(initialBindings).assertReplPrompt("""
            #Welcome to the PartiQL REPL!
            #Using version: $partiqlVersionAndHash
            #PartiQL> !global_env
            #   | 
            #===' 
            #{
            #  'foo': [
            #    1,
            #    2,
            #    3
            #  ]
            #}
            #--- 
            #OK!
            #PartiQL> 
        """.trimMargin("#"))
    }

    @Test
    fun dumpEmptyInitialEnv() {
        ReplTester().assertReplPrompt("""
            #Welcome to the PartiQL REPL!
            #Using version: $partiqlVersionAndHash
            #PartiQL> !global_env
            #   | 
            #===' 
            #{}
            #--- 
            #OK!
            #PartiQL> 
        """.trimMargin("#"))
    }

    @Test
    fun dumpEnvAfterAltering() {
        ReplTester().assertReplPrompt("""
            #Welcome to the PartiQL REPL!
            #Using version: $partiqlVersionAndHash
            #PartiQL> !add_to_global_env {'myTable': <<{'a':1}, {'a': 2}>>}
            #   | 
            #===' 
            #{
            #  'myTable': <<
            #    {
            #      'a': 1
            #    },
            #    {
            #      'a': 2
            #    }
            #  >>
            #}
            #--- 
            #OK!
            #PartiQL> !global_env
            #   | 
            #===' 
            #{
            #  'myTable': <<
            #    {
            #      'a': 1
            #    },
            #    {
            #      'a': 2
            #    }
            #  >>
            #}
            #--- 
            #OK!
            #PartiQL> 
        """.trimMargin("#"))
    }

    @Test
    fun listCommands() {
        ReplTester().assertReplPrompt("""
            #Welcome to the PartiQL REPL!
            #Using version: $partiqlVersionAndHash
            #PartiQL> !list_commands
            #   | 
            #
            #!add_to_global_env: adds a value to the global environment
            #!global_env: displays the current global environment
            #!list_commands: print this message
            #OK!
            #PartiQL> 
        """.trimMargin("#"))
    }
}
