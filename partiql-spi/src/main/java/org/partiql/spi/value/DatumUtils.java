package org.partiql.spi.value;

import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

/**
 * Internal utility methods for Datum implementations.
 * This class shall always be package-private (internal).
 */
class DatumUtils {
    
    /**
     * Formats a map of field names to lists of Datum values into a string representation
     * used by structured Datum types (DatumStruct and DatumRow).
     * 
     * @param fields Map of field names to lists of Datum values
     * @return Formatted string representation of the fields
     */
    static String formatFieldsToString(Map<String, List<Datum>> fields) {
        StringJoiner outerJoiner = new StringJoiner(", ", "{", "}");

        fields.forEach((key, value) -> {
            String sb = key +  ": " + DatumUtils.formatListToString(value);
            outerJoiner.add(sb);
        });
        
        return outerJoiner.toString();
    }
    
    /**
     * Formats an iterable of Datum values into a string representation
     * used by collection Datum types (DatumCollection for arrays and bags).
     * 
     * @param values Iterable of Datum values
     * @return Formatted string representation of the array
     */
    static String formatListToString(Iterable<Datum> values) {
        StringJoiner joiner = new StringJoiner(", ", "[", "]");
        values.forEach(d -> joiner.add(d.toString()));
        return joiner.toString();
    }
}