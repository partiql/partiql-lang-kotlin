package org.partiql.runner

class ReportAnalyzer(
    private val reportTitle: String,
    private val first: Report,
    private val second: Report
) {
    private val PARTIQL_TAG = "partiql"
    private val PARTIQL_EXTENDED_TAG = "partiql-extended"

    companion object {
        fun build(title: String, first: Report, second: Report): ReportAnalyzer {
            return ReportAnalyzer(title, first, second)
        }

        const val ICON_X = ":x:"
        const val ICON_CHECK = ":white_check_mark:"
        const val ICON_CIRCLE_RED = ":o:"
        const val ICON_DIAMOND_ORANGE = ":large_orange_diamond:"
        const val BASE = "BASE"
        const val TARGET = "TARGET"
    }

    data class ComparisonResult(val firstReport: Report, val secondReport: Report, val tag: String) {
        private val first = firstReport.testsResults[tag] ?: Report.TestResult()
        private val second = secondReport.testsResults[tag] ?: Report.TestResult()

        val passingInBoth = first.passingSet.intersect(second.passingSet)
        val failingInBoth = first.failingSet.intersect(second.failingSet)
        val ignoredInBoth = first.ignoredSet.intersect(second.ignoredSet)
        val passingFirstFailingSecond = first.passingSet.intersect(second.failingSet)
        val passingFirstIgnoredSecond = first.passingSet.intersect(second.ignoredSet)
        val failureFirstPassingSecond = first.failingSet.intersect(second.passingSet)
        val ignoredFirstPassingSecond = first.ignoredSet.intersect(second.passingSet)
        val firstPassingSize = first.passingSet.size
        val firstFailingSize = first.failingSet.size
        val firstIgnoreSize = first.ignoredSet.size
        val secondPassingSize = second.passingSet.size
        val secondFailingSize = second.failingSet.size
        val secondIgnoreSize = second.ignoredSet.size

        val firstTotalSize = firstPassingSize + firstFailingSize + firstIgnoreSize
        val secondTotalSize = secondPassingSize + secondFailingSize + secondIgnoreSize

        val firstPassingPercent = if (firstTotalSize == 0) 0.0 else firstPassingSize.toDouble() / firstTotalSize * 100
        val secondPassingPercent = if (secondTotalSize == 0) 0.0 else secondPassingSize.toDouble() / secondTotalSize * 100

        val firstNameShort = "$BASE (${firstReport.engine}-${firstReport.commitIdShort.uppercase()})"
        val secondNameShort = "$TARGET (${secondReport.engine}-${secondReport.commitIdShort.uppercase()})"
    }

    fun generateComparisonReport(limit: Int): String {

        val resultList: MutableList<ComparisonResult> = mutableListOf()

        second.testsResults[PARTIQL_TAG]?.let {
            resultList.add(ComparisonResult(first, second, PARTIQL_TAG))
        }

        second.testsResults[PARTIQL_EXTENDED_TAG]?.let {
            resultList.add(ComparisonResult(first, second, PARTIQL_EXTENDED_TAG))
        }

        return buildString {
            appendTitle(this, resultList)
            resultList.forEach { appendTable(this, it) }

            appendSummary(this, resultList)

            appendOptionalNowFailureTests(this, resultList.associate { Pair(it.tag, it.passingFirstFailingSecond) }, limit, TestStatus.FAILING)
            appendOptionalNowFailureTests(this, resultList.associate { Pair(it.tag, it.passingFirstIgnoredSecond) }, limit, TestStatus.IGNORED)
            appendOptionalNowPassingTests(this, resultList.associate { Pair(it.tag, it.failureFirstPassingSecond) }, limit, TestStatus.FAILING)
            appendOptionalNowPassingTests(this, resultList.associate { Pair(it.tag, it.ignoredFirstPassingSecond) }, limit, TestStatus.IGNORED)
        }
    }

    private fun appendTitle(out: Appendable, resultList: MutableList<ComparisonResult>) {
        val icon = if (resultList.all { it.passingFirstFailingSecond.isEmpty() }) ICON_CHECK else ICON_X
        out.appendMarkdown("# $reportTitle $icon")
    }

    private fun appendTable(out: Appendable, result: ComparisonResult?) {
        if (result == null) return
        out.appendLine("| ${result.tag.uppercase()} Data Set| ${result.firstNameShort} | ${result.secondNameShort} | +/- |")
        out.appendLine("| --- | ---: | ---: | ---: |")
        out.appendLine(tableRow("% Passing", result.firstPassingPercent, result.secondPassingPercent))
        out.appendLine(tableRow("Passing", result.firstPassingSize, result.secondPassingSize, true))
        out.appendLine(tableRow("Failing", result.firstFailingSize, result.secondFailingSize, false))
        out.appendLine(tableRow("Ignored", result.firstIgnoreSize, result.secondIgnoreSize, false, badIcon = ICON_DIAMOND_ORANGE))
        out.appendLine(tableRow("Total Tests", result.firstTotalSize, result.secondTotalSize, true))
        out.appendLine()
    }

    private fun tableRow(name: String, first: Double, second: Double): String {
        val firstString = "%.2f".format(first)
        val secondString = "%.2f".format(second)
        val delta = second - first
        val deltaIcon = getIconForComparison(first, second, true, ICON_CHECK, ICON_CIRCLE_RED)
        val deltaString = "%.2f".format(delta)
        return "| $name | $firstString% | $secondString% | $deltaString% $deltaIcon |"
    }

    private fun tableRow(name: String, first: Int, second: Int, positiveDeltaGood: Boolean, goodIcon: String = ICON_CHECK, badIcon: String = ICON_CIRCLE_RED): String {
        val delta = second - first
        val deltaIcon = getIconForComparison(first, second, positiveDeltaGood, goodIcon, badIcon)
        return "| $name | $first | $second | $delta $deltaIcon |"
    }

    private fun <T> getIconForComparison(first: Comparable<T>, second: T, positiveDeltaGood: Boolean, goodIcon: String, badIcon: String): String {
        val comparison = first.compareTo(second)
        return when {
            comparison < 0 -> if (positiveDeltaGood) goodIcon else badIcon
            comparison == 0 -> goodIcon
            else -> if (positiveDeltaGood) badIcon else goodIcon
        }
    }

    /**
     * This appends two lines. Markdown requires this if you actually want a new-line.
     */
    private fun Appendable.appendMarkdown(string: String) {
        this.appendLine(string)
        this.appendLine()
    }

    private fun appendSummary(out: Appendable, resultList: MutableList<ComparisonResult>) {
        out.appendMarkdown("## Testing Details")
        out.appendLine("- **Base Commit**: ${first.commitId}")
        out.appendLine("- **Base Engine**: ${first.engine.uppercase()}")
        out.appendLine("- **Target Commit**: ${second.commitId}")
        out.appendLine("- **Target Engine**: ${second.engine.uppercase()}")

        out.appendMarkdown("## Result Details")

        resultList.forEach {
            out.appendMarkdown("### ${it.tag.uppercase()} Data Set")
            if (it.passingFirstFailingSecond.isNotEmpty()) {
                out.appendLine("- **$ICON_X REGRESSION DETECTED. See *Now Failing/Ignored Tests*. $ICON_X**")
            }

            out.appendLine("- **Passing in both**: **${it.passingInBoth.count()}** ")
            out.appendLine("- **Failing in both**: **${it.failingInBoth.count()}** ")
            out.appendLine("- **Ignored in both**: **${it.ignoredInBoth.count()}** ")
            out.appendLine("- **PASSING in $BASE but now FAILING in $TARGET**: **${it.passingFirstFailingSecond.count()}**")
            out.appendLine("- **PASSING in $BASE but now IGNORED in $TARGET**: **${it.passingFirstIgnoredSecond.count()}**")
            out.appendLine("- **FAILING in $BASE but now PASSING in $TARGET**: **${it.failureFirstPassingSecond.count()}**")
            out.appendLine("- **IGNORED in $BASE but now PASSING in $TARGET**: **${it.ignoredFirstPassingSecond.count()}**")
        }
    }

    private fun appendOptionalNowFailureTests(out: Appendable, resultList: Map<String, Set<String>>, limit: Int, testStatus: TestStatus) {
        if (resultList.values.all { it.isEmpty() }) return

        out.appendMarkdown("## Now $testStatus Tests $ICON_X")
        // character count limitation with comments in GitHub
        // also, not ideal to list out hundreds of test names

        resultList.forEach {

            if (it.value.isEmpty()) return@forEach

            out.appendMarkdown("### ${it.key} data set")
            val set = it.value

            if (set.size < limit) {
                out.appendMarkdown("The following **${set.size}** test(s) were previously PASSING in $BASE but are now $testStatus in $TARGET:")
                out.appendMarkdown("<details><summary>Click here to see</summary>")
                set.forEachIndexed { index, testName ->
                    out.appendLine("${index + 1}. $testName")
                }
                out.appendMarkdown("</details>")
            } else {
                out.appendMarkdown("The complete list can be found in GitHub CI summary, either from Step Summary or in the Artifact.")
            }
        }
    }

    private fun appendOptionalNowPassingTests(out: Appendable, resultList: Map<String, Set<String>>, limit: Int, testStatus: TestStatus) {

        if (resultList.values.all { it.isEmpty() }) return

        out.appendMarkdown("## Now Passing Tests")
        // character count limitation with comments in GitHub
        // also, not ideal to list out hundreds of test names

        resultList.forEach {
            if (it.value.isEmpty()) return@forEach

            out.appendMarkdown("### ${it.key.uppercase()} data set")
            val set = it.value

            if (set.size < limit) {
                out.appendMarkdown("The following **${set.size}** test(s) were previously $testStatus in $BASE but are now PASSING in $TARGET. Before merging, confirm they are intended to pass:")
                out.appendMarkdown("<details><summary>Click here to see</summary>")
                set.forEachIndexed { index, testName ->
                    out.appendLine("${index + 1}. $testName")
                }
                out.appendMarkdown("</details>")
            } else {
                out.appendMarkdown("${set.size} test(s) were previously failing in $BASE but now pass in $TARGET. Before merging, confirm they are intended to pass.")
                out.appendMarkdown("The complete list can be found in GitHub CI summary, either from Step Summary or in the Artifact.")
            }
        }
    }
}

enum class TestStatus {
    PASSING, FAILING, IGNORED
}
