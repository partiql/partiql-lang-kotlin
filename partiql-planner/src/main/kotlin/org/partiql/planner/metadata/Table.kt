package org.partiql.planner.metadata

/**
 * In PartiQL, a [Table] can take on any shape and is not necessarily rows+columns.
 *
 *   From Calcite,
 *
 *   '''
 *      Note that a table does not know its name. It is in fact possible for
 *      a table to be used more than once, perhaps under multiple names or under
 *      multiple schemas. (Compare with the <a href="http://en.wikipedia.org/wiki/Inode">i-node</a> concept
 *      in the UNIX filesystem.)
 *   '''
 *
 * See, https://github.com/apache/calcite/blob/main/core/src/main/java/org/apache/calcite/schema/Table.java
 */
public interface Table {

    /**
     * The table's kind.
     */
    public fun getKind(): Kind = Kind.TABLE

    /**
     * The table's shape; in SQL this is called 'table schema'.
     */
    public fun getShape(): TempShape = TempShape

    /**
     * A [Statistics] object for this table.
     */
    public fun getStatistics(): Statistics = Statistics.None

    /**
     * A [Table] can be one of several [Kind]s.
     */
    public enum class Kind {
        TABLE,
        VIEW,
        INDEX,
        OTHER,
    }
}
