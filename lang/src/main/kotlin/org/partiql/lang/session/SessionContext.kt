package org.partiql.lang.session

public class SessionContext(
    public val catalog: String?,
    public val schema: String?
) {
    init {
        if (catalog == null && schema != null) {
            throw RuntimeException("Cannot specify schema without a Catalog")
        }
    }
}
