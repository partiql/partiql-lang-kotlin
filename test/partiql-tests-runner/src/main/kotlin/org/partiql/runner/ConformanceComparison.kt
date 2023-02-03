package org.partiql.runner

import com.amazon.ionelement.api.loadSingleElement
import java.io.File

fun main(args: Array<String>) {
    if (args.size != 5) {
        error(
            "Expected 5 args: pathToFirstConformanceTestResults, pathToSecondConformanceTestResults" +
                "firstCommitId, secondCommitId, pathToComparisonReport"
        )
    }

    val origInput = File(args[0]).readText()
    val newInput = File(args[1]).readText()

    val origInputStruct = loadSingleElement(origInput).asStruct()
    val newInputStruct = loadSingleElement(newInput).asStruct()

    val origPassingSet = origInputStruct["passing"].listValues.map { it.stringValue }
    val origFailingSet = origInputStruct["failing"].listValues.map { it.stringValue }
    val origIgnoredSet = origInputStruct["ignored"].listValues.map { it.stringValue }

    val newPassingSet = newInputStruct["passing"].listValues.map { it.stringValue }
    val newFailingSet = newInputStruct["failing"].listValues.map { it.stringValue }
    val newIgnoredSet = newInputStruct["ignored"].listValues.map { it.stringValue }

    val origCommitId = args[2]
    val newCommitId = args[3]

    val passingInBoth = origPassingSet.intersect(newPassingSet)
    val failingInBoth = origFailingSet.intersect(newFailingSet)
    val passingOrigFailingNew = origPassingSet.intersect(newFailingSet)
    val failureOrigPassingNew = origFailingSet.intersect(newPassingSet)

    val comparisonReportFile = File(args[4])
    comparisonReportFile.createNewFile()

    val numOrigPassing = origPassingSet.size
    val numNewPassing = newPassingSet.size

    val numOrigFailing = origFailingSet.size
    val numNewFailing = newFailingSet.size

    val numOrigIgnored = origIgnoredSet.size
    val numNewIgnored = newIgnoredSet.size

    val totalOrig = numOrigPassing + numOrigFailing + numOrigIgnored
    val totalNew = numNewPassing + numNewFailing + numNewIgnored

    val origPassingPercent = numOrigPassing.toDouble() / totalOrig * 100
    val newPassingPercent = numNewPassing.toDouble() / totalNew * 100

    comparisonReportFile.writeText(
        """### Conformance comparison report
| | Base ($origCommitId) | $newCommitId | +/- |
| --- | ---: | ---: | ---: |
| % Passing | ${"%.2f".format(origPassingPercent)}% | ${"%.2f".format(newPassingPercent)}% | ${"%.2f".format(newPassingPercent - origPassingPercent)}% |
| :white_check_mark: Passing | $numOrigPassing | $numOrigPassing | ${numNewPassing - numOrigFailing} |
| :x: Failing | $numOrigFailing | $numNewFailing | ${numNewFailing - numOrigFailing} |
| :large_orange_diamond: Ignored | $numOrigIgnored | $numNewIgnored | ${numNewIgnored - numOrigIgnored} |
| Total Tests | $totalOrig | $totalNew | ${totalNew - totalOrig} |
""",
    )

    comparisonReportFile.appendText(
"""
Number passing in both: ${passingInBoth.count()}

Number failing in both: ${failingInBoth.count()}

Number passing in Base ($origCommitId) but now fail: ${passingOrigFailingNew.count()}

Number failing in Base ($origCommitId) but now pass: ${failureOrigPassingNew.count()}
"""
    )

    if (!passingOrigFailingNew.isEmpty()) {
        comparisonReportFile.appendText(
            "\n:interrobang: CONFORMANCE REPORT REGRESSION DETECTED :interrobang:. The following test(s) were previously passing but now fail:\n<details><summary>Click here to see</summary>\n\n"
        )
        passingOrigFailingNew.forEach { testName ->
            comparisonReportFile
                .appendText("- ${testName}\n")
        }
        comparisonReportFile
            .appendText("\n</details>")
    }

    if (!failureOrigPassingNew.isEmpty()) {
        comparisonReportFile.appendText(
            "\nThe following test(s) were previously failing but now pass. Before merging, confirm they are intended to pass: \n<details><summary>Click here to see</summary>\n\n"
        )
        failureOrigPassingNew.forEach { testName ->
            comparisonReportFile
                .appendText("- ${testName}\n")
        }
        comparisonReportFile
            .appendText("\n</details>")
    }
}
