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
@file:JvmName("Main")

package org.partiql.lib.tpc

import io.trino.tpcds.Results.constructResults
import io.trino.tpcds.Session
import io.trino.tpcds.Table
import org.partiql.lib.tpc.formats.ResultSetWriterFactory
import picocli.CommandLine
import picocli.CommandLine.Option
import java.nio.file.Path
import kotlin.io.path.createDirectory
import kotlin.io.path.notExists
import kotlin.system.exitProcess

/**
 * Runs the PartiQL CLI.
 */
fun main(args: Array<String>) {
    val command = CommandLine(MainCommand())
    val exitCode = command.execute(*args)
    exitProcess(exitCode)
}

@CommandLine.Command(
    name = "partiql-tpc",
    mixinStandardHelpOptions = true,
    version = ["0"],
    description = ["Writes a TPC dataset"],
)
class MainCommand : Runnable {

    @Option(names = ["--output"], required = true)
    lateinit var output: Path

    @Option(
        names = ["--scale"],
        description = ["Scale factor is 1GB"],
    )
    var scale: Double = 1.0

    @Option(names = ["--partitions"])
    var partitions: Int = 1

    @Option(names = ["--part"])
    var part: Int = 1

    @Option(
        names = ["--table"],
        description = ["Table to generate; if not specified, all tables are generated. https://www.tpc.org/tpc_documents_current_versions/pdf/tpc-ds_v2.6.0.pdf"],
    )
    var table: String? = null

    @Option(
        names = ["-d", "-dataset"],
        required = true,
        description = ["Dataset type; valid values: \${COMPLETION-CANDIDATES}"],
    )
    lateinit var benchmark: Benchmark

    @Option(
        names = ["--format"],
        required = true,
        description = ["Output format; valid values: \${COMPLETION-CANDIDATES}"],
    )
    lateinit var format: Format

    override fun run() {
        val sNano = System.nanoTime()
        // Prepare
        if (output.notExists()) {
            output.createDirectory()
        }
        // Generate
        when (table) {
            null -> genAll()
            else -> genOne(table!!)
        }
        val eNano = System.nanoTime()
        println("Generated ${benchmark.display} data in ${(eNano - sNano) / 1e9}s")
    }

    private fun genOne(t: String) {
        try {
            val table = Table.valueOf(t.uppercase())
            write(table)
        } catch (ex: IllegalArgumentException) {
            throw IllegalArgumentException("Table $t does not exist for benchmark $benchmark")
        }
    }

    private fun genAll() {
        for (table in Table.getBaseTables()) {
            write(table)
        }
    }

    private fun write(table: Table) {
        val session = Session.getDefaultSession()
            .withScale(scale)
            .withParallelism(partitions)
            .withChunkNumber(part)
            .withTable(table)
            .withNoSexism(true)
        val writer = ResultSetWriterFactory.create(table, format, output)
        val results = constructResults(table, session)
        writer.use {
            it.open(table)
            it.write(results)
            // auto-close
        }
    }
}
