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
import java.nio.file.Path

/**
 * Provides a set of tests to be run by the test runner.
 *
 * The tests are loaded from a directory or a resource path.
 *
 * @param factory The test builder factory to use.
 * @param root The root directory to load tests from. If null, the tests will be loaded from resources.
 * @see TestBuilderFactory
 * @see Test
 * @see PartiQLTestProvider
 */
class TestProvider(
    private val factory: TestBuilderFactory,
    private val root: Path?
) : Iterable<Test> {

    private val inputProvider: PartiQLTestProvider = PartiQLTestProvider()

    init {
        inputProvider.load()
    }

    /**
     * Load test groups from a directory.
     */
    override fun iterator(): Iterator<Test> {
        val testIterators: MutableList<Iterator<Test>> = mutableListOf()
        if (root != null) {
            val dir = root.toFile()
            val tests = load(dir, emptyList())
            testIterators.add(tests)
        } else {
            // user default resources
            val inputStream = this::class.java.getResourceAsStream("/resource_path.txt")!!
            inputStream.reader().forEachLine { path ->
                val pathSteps = path.split("/")
                val outMostDir = pathSteps.first()
                // Open tests directory
                if (outMostDir == "tests") {
                    val resource = this::class.java.getResourceAsStream("/$path")!!
                    val dirs = pathSteps.drop(1).dropLast(1)
                    val tests = TestFileIterator(dirs, resource, factory, inputProvider)
                    testIterators.add(tests)
                }
            }
        }
        return IteratorChain(testIterators)
    }

    // load all tests in a directory
    private fun load(file: File, parent: List<String>): Iterator<Test> {
        if (file.isDirectory) {
            val iterators = file.listFiles()!!.map {
                load(it, parent + listOf(it.nameWithoutExtension))
            }
            return IteratorChain(iterators)
        } else {
            return TestFileIterator(parent, file.inputStream(), factory, inputProvider)
        }
    }
}
