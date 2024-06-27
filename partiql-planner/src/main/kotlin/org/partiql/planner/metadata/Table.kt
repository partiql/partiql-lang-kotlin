package org.partiql.planner.metadata

import org.partiql.types.PType

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
     * The table's schema.
     */
    public fun getSchema(): PType = PType.typeDynamic()

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