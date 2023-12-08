package org.partiql.runner.test

import org.partiql.runner.ION
import org.partiql.runner.schema.Namespace
import org.partiql.runner.schema.TestCase
import org.partiql.runner.schema.parseNamespace
import java.io.File

/**
 * Checks all the PartiQL conformance test data in [PARTIQL_EVAL_TEST_DATA_DIR] conforms to the test data schema.
 */
object TestLoader {

    fun load(path: String): List<TestCase> {
        val allFiles = File(path).walk()
            .filter { it.isFile }
            .filter { it.path.endsWith(".ion") }
            .toList()
        val filesAsNamespaces = allFiles.map { file ->
            parseTestFile(file)
        }

        val allTestCases = filesAsNamespaces.flatMap { ns ->
            allTestsFromNamespace(ns)
        }
        return allTestCases
    }

    private fun parseTestFile(file: File): Namespace {
        val loadedData = file.readText()
        val dataInIon = ION.loader.load(loadedData)
        val emptyNamespace = Namespace(
            env = ION.newEmptyStruct(),
            namespaces = mutableListOf(),
            testCases = mutableListOf(),
            equivClasses = mutableMapOf()
        )
        dataInIon.forEach { d ->
            parseNamespace(emptyNamespace, d)
        }
        return emptyNamespace
    }

    private fun allTestsFromNamespace(ns: Namespace): List<TestCase> {
        return ns.testCases + ns.namespaces.fold(listOf()) { acc, subns ->
            acc + allTestsFromNamespace(subns)
        }
    }
}
