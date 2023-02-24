package org.partiql.plan.impl

import org.partiql.lang.eval.BindingName

internal class QualifiedObjectName(
    public val catalogName: BindingName?,
    public val schemaName: BindingName?,
    public val objectName: BindingName?
)
