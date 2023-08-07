package org.partiql.coverage.api.impl

import org.junit.platform.launcher.TestPlan

internal class LcovReportBranchListener : LcovReportListener() {
    private var isLcovEnabled: Boolean = false
    private var reportPath: String? = null

    override fun isLcovEnabled(): Boolean = this.isLcovEnabled

    override fun getReportPath(): String = this.reportPath!!

    override fun getTargetCountKey(): String = ReportKey.BRANCH_COUNT 

    override fun getCoverageTargetType(): ReportKey.CoverageTarget = ReportKey.CoverageTarget.BRANCH

    override fun testPlanExecutionStarted(testPlan: TestPlan?) {
        if (testPlan == null) { return super.testPlanExecutionStarted(testPlan) }

        // Get Configuration Parameters
        val configParams = ConfigurationParameterExtractor.extract(testPlan)
        isLcovEnabled = configParams.lcovBranchConfig != null
        this.reportPath = configParams.lcovBranchConfig?.reportPath
        return super.testPlanExecutionStarted(testPlan)
    }
}
