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

package org.partiql.planner.test

import java.io.File
import java.io.InputStream
import java.nio.file.Path
import kotlin.io.path.toPath

/**
 * The PartiQLTestProvider is a simple utility for indexing SQL statements within files for re-use across library tests.
 */
class PartiQLTestProvider {

    /**
     * Backing map for test input lookup.
     */
    private val map: MutableMap<PartiQLTest.Key, PartiQLTest> = mutableMapOf()

    /**
     * Default database of test inputs.
     */
    private val default = this::class.java.getResource("/inputs")!!.toURI().toPath()

    /**
     * Load test groups from a directory.
     */
    public fun load(root: Path? = null) {
        if (root != null) {
            val dir = root.toFile()
            dir.listFiles { f -> f.isDirectory }!!.map {
                for (test in load(it)) {
                    map[test.key] = test
                }
            }
        } else {
            // user default resources
            val inputStream = this::class.java.getResourceAsStream("/resource_path.txt")!!
            inputStream.reader().forEachLine { path ->
                val pathSteps = path.split("/")
                val outMostDir = pathSteps.first()
                if (outMostDir == "inputs") {
                    val group = pathSteps[pathSteps.size - 2]
                    val resource = this::class.java.getResourceAsStream("/$path")!!
                    for (test in load(group, resource)) {
                        map[test.key] = test
                    }
                }
            }
        }
    }

    /**
     * Lookup a test by key
     *
     * @param key
     * @return
     */
    public operator fun get(key: PartiQLTest.Key): PartiQLTest? = map[key]

    /**
     * Lookup a test by key parts
     *
     * @param group
     * @param name
     * @return
     */
    public fun get(group: String, name: String): PartiQLTest? = get(PartiQLTest.Key(group, name))

    // load all tests in a directory
    private fun load(dir: File) = dir.listFiles()!!.flatMap { load(dir.name, it) }

    // load all tests in a file
    private fun load(group: String, file: File): List<PartiQLTest> = load(group, file.inputStream())

    private fun load(group: String, inputStream: InputStream): List<PartiQLTest> {
        val tests = mutableListOf<PartiQLTest>()
        var name = ""
        val statement = StringBuilder()
        for (line in inputStream.reader().readLines()) {

            // start of test
            if (line.startsWith("--#[") and line.endsWith("]")) {
                name = line.substring(4, line.length - 1)
                statement.clear()
            }

            if (name.isNotEmpty() && line.isNotBlank()) {
                // accumulating test statement
                statement.appendLine(line)
            } else {
                // skip these lines
                continue
            }

            // Finish & Reset
            if (line.endsWith(";")) {
                val key = PartiQLTest.Key(group, name)
                tests.add(PartiQLTest(key, statement.toString()))
                name = ""
                statement.clear()
            }
        }
        return tests
    }
}
