package org.partiql.runner

class ReportAnalyzer(
    private val reportTitle: String,
    private val first: Report,
    private val second: Report
) {

    companion object {
        fun build(first: Report, second: Report): ReportAnalyzer {
            return if (first.engine == second.engine) {
                ReportAnalyzer("CROSS-COMMIT-${first.engine.uppercase()} Conformance Report", first, second)
            } else {
                ReportAnalyzer("CROSS-ENGINE Conformance Report", first, second)
            }
        }

        const val ICON_X = ":x:"
        const val ICON_CHECK = ":white_check_mark:"
        const val ICON_CIRCLE_RED = ":o:"
        const val ICON_DIAMOND_ORANGE = ":large_orange_diamond:"
        const val BASE = "BASE"
        const val TARGET = "TARGET"
    }

    private val passingInBoth = first.passingSet.intersect(second.passingSet)
    private val failingInBoth = first.failingSet.intersect(second.failingSet)
    private val passingFirstFailingSecond = first.passingSet.intersect(second.failingSet)
    private val failureFirstPassingSecond = first.failingSet.intersect(second.passingSet)
    private val firstPassingSize = first.passingSet.size
    private val firstFailingSize = first.failingSet.size
    private val firstIgnoreSize = first.ignoredSet.size
    private val secondPassingSize = second.passingSet.size
    private val secondFailingSize = second.failingSet.size
    private val secondIgnoreSize = second.ignoredSet.size

    private val firstTotalSize = firstPassingSize + firstFailingSize + firstIgnoreSize
    private val secondTotalSize = secondPassingSize + secondFailingSize + secondIgnoreSize

    private val firstPassingPercent = firstPassingSize.toDouble() / firstTotalSize * 100
    private val secondPassingPercent = secondPassingSize.toDouble() / secondTotalSize * 100

    private val firstNameShort = "$BASE (${first.engine.uppercase()}-${first.commitIdShort.uppercase()})"
    private val secondNameShort = "$TARGET (${second.engine.uppercase()}-${second.commitIdShort.uppercase()})"

    fun generateComparisonReport(limit: Int): String {
        return buildString {
            appendTitle(this)
            appendTable(this)
            appendSummary(this)
            appendOptionalNowFailingTests(this, limit)
            appendOptionalNowPassingTests(this, limit)
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
        out.appendMarkdown("## Testing Details")
        out.appendLine("- **Base Commit**: ${first.commitId}")
        out.appendLine("- **Base Engine**: ${first.engine.uppercase()}")
        out.appendLine("- **Target Commit**: ${second.commitId}")
        out.appendLine("- **Target Engine**: ${second.engine.uppercase()}")

        out.appendMarkdown("## Result Details")
        if (passingFirstFailingSecond.isNotEmpty()) {
            out.appendLine("- **$ICON_X REGRESSION DETECTED. See *Now Failing Tests*. $ICON_X**")
        }
        out.appendLine("- **Passing in both**: ${passingInBoth.count()}")
        out.appendLine("- **Failing in both**: ${failingInBoth.count()}")
        out.appendLine("- **PASSING in $BASE but now FAILING in $TARGET**: ${passingFirstFailingSecond.count()}")
        out.appendLine("- **FAILING in $BASE but now PASSING in $TARGET**: ${failureFirstPassingSecond.count()}")
    }

    private fun appendOptionalNowFailingTests(out: Appendable, limit: Int) {
        if (passingFirstFailingSecond.isNotEmpty()) {
            out.appendMarkdown("## Now Failing Tests $ICON_X")
            // character count limitation with comments in GitHub
            // also, not ideal to list out hundreds of test names
            if (passingFirstFailingSecond.size < limit) {
                out.appendMarkdown("The following ${passingFirstFailingSecond.size} test(s) were previously PASSING in $BASE but are now FAILING in $TARGET:")
                out.appendMarkdown("<details><summary>Click here to see</summary>")
                passingFirstFailingSecond.forEachIndexed { index, testName ->
                    out.appendLine("${index + 1}. $testName")
                }
                out.appendMarkdown("</details>")
            } else {
                out.appendMarkdown("The complete list can be found in GitHub CI summary, either from Step Summary or in the Artifact.")
            }
        }
    }

    private fun appendOptionalNowPassingTests(out: Appendable, limit: Int) {
        if (failureFirstPassingSecond.isNotEmpty()) {
            out.appendMarkdown("## Now Passing Tests")
            if (failureFirstPassingSecond.size < limit) {
                out.appendMarkdown("The following ${failureFirstPassingSecond.size} test(s) were previously FAILING in $BASE but are now PASSING in $TARGET. Before merging, confirm they are intended to pass:")
                out.appendMarkdown("<details><summary>Click here to see</summary>")
                failureFirstPassingSecond.forEachIndexed { index, testName ->
                    out.appendLine("${index + 1}. $testName")
                }
                out.appendMarkdown("</details>")
            } else {
                out.appendMarkdown("${failureFirstPassingSecond.size} test(s) were previously failing in $firstNameShort but now pass in $secondNameShort. Before merging, confirm they are intended to pass.")
                out.appendMarkdown("The complete list can be found in GitHub CI summary, either from Step Summary or in the Artifact.")
            }
        }
    }
}
