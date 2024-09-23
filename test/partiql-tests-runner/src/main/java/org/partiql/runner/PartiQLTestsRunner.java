package org.partiql.runner;

import picocli.CommandLine;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;

@CommandLine.Command(
        name = "partiql-tests-runner",
        mixinStandardHelpOptions = true,
        description = {"%nRuns a report comparison between two conformance reports and outputs a markdown summary.%n"},
        showDefaultValues = true
)
public class PartiQLTestsRunner implements Runnable {

    @CommandLine.Option(
            required = false,
            description = {"Title of comparison report. Example: 'CROSS-COMMIT', 'CROSS-ENGINE', etc"},
            paramLabel = "<title>",
            defaultValue = "CONFORMANCE REPORT",
            names = {"-t", "--title"}
    )
    private String title;

    @CommandLine.Option(
            required = true,
            description = {"Path to base conformance report file."},
            paramLabel = "<base_report_file_path>",
            names = {"-bf", "--base-report-file"}
    )
    private File baseReportFile;

    @CommandLine.Option(
            required = true,
            description = {"Path to conformance report to compare against the base report."},
            paramLabel = "<target_report_file_path>",
            names = {"-tf", "--target-report-file"}
    )
    private File targetReportFile;

    @CommandLine.Option(
            required = true,
            description = {"The label for the base conformance report. Example: 'LEGACY', 'EVAL', etc."},
            paramLabel = "<base_label>",
            names = {"-bl", "--base-label"}
    )
    private String baseLabel;

    @CommandLine.Option(
            required = true,
            description = {"The tag for the base conformance report. Example: a commit id."},
            paramLabel = "<target_label>",
            names = {"-bt", "--base-tag"}
    )
    private String baseTag;

    @CommandLine.Option(
            required = true,
            description = {"The label for the target conformance report. Example: 'LEGACY', 'EVAL', etc."},
            paramLabel = "<target_label>",
            names = {"-tl", "--target-label"}
    )
    private String targetLabel;

    @CommandLine.Option(
            required = true,
            description = {"The tag for the target conformance report. Example: a commit id."},
            paramLabel = "<target_tag>",
            names = {"-tt", "--target-tag"}
    )
    private String targetTag;

    @CommandLine.Option(
            required = true,
            description = {"Path to the conformance comparison markdown file."},
            paramLabel = "<output_path>",
            names = {"-o", "--output"}
    )
    private File outputFile;

    @CommandLine.Option(
            required = false,
            defaultValue = "2147483647",
            description = {"Limit for the markdown generated."},
            paramLabel = "<limit>",
            names = {"-l", "--limit"}
    )
    private Integer limit;

    @Override
    public void run() {
        Report baseReport = ConformanceComparisonKt.loadReportFile(baseReportFile, baseLabel, baseTag);
        Report newReports = ConformanceComparisonKt.loadReportFile(targetReportFile, targetLabel, targetTag);
        try {
            outputFile.createNewFile();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        ConformanceComparisonKt.analyze(outputFile, Arrays.asList(baseReport, newReports), limit, title);
    }

    public static void main(String... args) {
        int result = new CommandLine(new PartiQLTestsRunner()).execute(args);
        System.exit(result);
    }
}
