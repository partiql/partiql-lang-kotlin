package org.partiql.testscript.evaluator;

import com.amazon.ion.IonValue;


public interface PtsEquality {
    /**
     * Returns the default implementation of {@link PtsEquality}. This implementation should be used whenever 
     * possible to ensure consistency.
     */
    static PtsEquality getDefault() {
        return DefaultPtsEquality.INSTANCE;
    } 

    boolean isEqual(final IonValue left, final IonValue right); 
}
