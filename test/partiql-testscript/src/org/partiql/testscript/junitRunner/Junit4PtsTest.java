package org.partiql.testscript.junitRunner;

import org.junit.runner.RunWith;
import org.partiql.testscript.evaluator.Evaluator;

import java.util.List;

@RunWith(PtsRunner.class)
public abstract class Junit4PtsTest {
    public abstract Evaluator getEvaluator();

    public abstract List<String> getPtsFilePaths();
}
