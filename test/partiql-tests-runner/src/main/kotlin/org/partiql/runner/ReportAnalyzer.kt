package org.partiql.runner

class ReportAnalyzer(
    private val first: Report,
    private val second: Report
) {
    companion object {
        private const val TITLE_FORMAT = "CROSS-COMMIT-REPORT-(%s DATA SET)"

        fun build(first: Report, second: Report): ReportAnalyzer {
            if (first.dataSet != second.dataSet) throw IllegalArgumentException("Data sets do not match")
            return ReportAnalyzer(first, second)
        }

        const val ICON_X = ":x:"
        const val ICON_CHECK = ":white_check_mark:"
        const val ICON_CIRCLE_RED = ":o:"
        const val ICON_DIAMOND_ORANGE = ":large_orange_diamond:"
        const val BASE = "BASE"
        const val TARGET = "TARGET"
    }

    private val passingInBoth = first.testResult.passingSet.intersect(second.testResult.passingSet)
    private val failingInBoth = first.testResult.failingSet.intersect(second.testResult.failingSet)
    private val ignoredInBoth = first.testResult.ignoredSet.intersect(second.testResult.ignoredSet)
    private val passingFirstFailingSecond = first.testResult.passingSet.intersect(second.testResult.failingSet)
    private val passingFirstIgnoredSecond = first.testResult.passingSet.intersect(second.testResult.ignoredSet)
    private val failureFirstPassingSecond = first.testResult.failingSet.intersect(second.testResult.passingSet)
    private val ignoredFirstPassingSecond = first.testResult.ignoredSet.intersect(second.testResult.passingSet)
    private val firstPassingSize = first.testResult.passingSet.size
    private val firstFailingSize = first.testResult.failingSet.size
    private val firstIgnoreSize = first.testResult.ignoredSet.size
    private val secondPassingSize = second.testResult.passingSet.size
    private val secondFailingSize = second.testResult.failingSet.size
    private val secondIgnoreSize = second.testResult.ignoredSet.size

    private val firstTotalSize = firstPassingSize + firstFailingSize + firstIgnoreSize
    private val secondTotalSize = secondPassingSize + secondFailingSize + secondIgnoreSize

    private val firstPassingPercent = firstPassingSize.toDouble() / firstTotalSize * 100
    private val secondPassingPercent = secondPassingSize.toDouble() / secondTotalSize * 100

    private val firstNameShort = "$BASE (${first.commitId.uppercase()})"
    private val secondNameShort = "$TARGET (${second.commitId.uppercase()})"

    private val reportTitle = TITLE_FORMAT.format(first.dataSet.dataSetName.uppercase())

    fun generateComparisonReport(limit: Int): String {
        return buildString {
            appendTitle(this)
            appendTable(this)
            appendSummary(this)
            appendOptionalNowFailingTests(this, limit, passingFirstFailingSecond, "FAILING")
            appendOptionalNowFailingTests(this, limit, passingFirstIgnoredSecond, "IGNORED")
            appendOptionalNowPassingTests(this, limit, failureFirstPassingSecond, "FAILING")
            appendOptionalNowPassingTests(this, limit, ignoredFirstPassingSecond, "IGNORED")
        }
    }

    private fun appendTitle(out: Appendable) {
        val icon = if (passingFirstFailingSecond.isEmpty()) ICON_CHECK else ICON_X
        out.appendMarkdown("# $reportTitle $icon")
    }

    private fun appendTable(out: Appendable) {
        out.appendLine("| | $firstNameShort | $secondNameShort | +/- |")
        out.appendLine("| --- | ---: | ---: | ---: |")
        out.appendLine(tableRow("% Passing", firstPassingPercent, secondPassingPercent))
        out.appendLine(tableRow("Passing", firstPassingSize, secondPassingSize, true))
        out.appendLine(tableRow("Failing", firstFailingSize, secondFailingSize, false))
        out.appendLine(tableRow("Ignored", firstIgnoreSize, secondIgnoreSize, false, badIcon = ICON_DIAMOND_ORANGE))
        out.appendLine(tableRow("Total Tests", firstTotalSize, secondTotalSize, true))
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

    private fun appendSummary(out: Appendable) {
        out.appendMarkdown("## Result Details")
        if (passingFirstFailingSecond.isNotEmpty() || passingFirstIgnoredSecond.isNotEmpty()) {
            out.appendLine("- **$ICON_X REGRESSION DETECTED. See *Now Failing/Ignored Tests*. $ICON_X**")
        }
        out.appendLine("- **Passing in both**: ${passingInBoth.count()}")
        out.appendLine("- **Failing in both**: ${failingInBoth.count()}")
        out.appendLine("- **Ignored in both**: ${ignoredInBoth.count()}")
        out.appendLine("- **PASSING in $BASE but now FAILING in $TARGET**: ${passingFirstFailingSecond.count()}")
        out.appendLine("- **PASSING in $BASE but now IGNORED in $TARGET**: ${passingFirstIgnoredSecond.count()}")
        out.appendLine("- **FAILING in $BASE but now PASSING in $TARGET**: ${failureFirstPassingSecond.count()}")
        out.appendLine("- **IGNORED in $BASE but now PASSING in $TARGET**: ${ignoredFirstPassingSecond.count()}")
    }

    private fun appendOptionalNowFailingTests(out: Appendable, limit: Int, set: Set<String>, description: String) {
        if (set.isNotEmpty()) {
            out.appendMarkdown("## Now $description Tests $ICON_X")
            // character count limitation with comments in GitHub
            // also, not ideal to list out hundreds of test names
            if (set.size < limit) {
                out.appendMarkdown("The following ${set.size} test(s) were previously PASSING in $BASE but are now $description in $TARGET:")
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

    private fun appendOptionalNowPassingTests(out: Appendable, limit: Int, set: Set<String>, description: String) {
        if (set.isNotEmpty()) {
            out.appendMarkdown("## Now Passing Tests")
            if (set.size < limit) {
                out.appendMarkdown("The following ${set.size} test(s) were previously $description in $BASE but are now PASSING in $TARGET. Before merging, confirm they are intended to pass:")
                out.appendMarkdown("<details><summary>Click here to see</summary>")
                set.forEachIndexed { index, testName ->
                    out.appendLine("${index + 1}. $testName")
                }
                out.appendMarkdown("</details>")
            } else {
                out.appendMarkdown("${set.size} test(s) were previously failing in ${first.commitId} but now pass in ${second.commitId}. Before merging, confirm they are intended to pass.")
                out.appendMarkdown("The complete list can be found in GitHub CI summary, either from Step Summary or in the Artifact.")
            }
        }
    }
}
