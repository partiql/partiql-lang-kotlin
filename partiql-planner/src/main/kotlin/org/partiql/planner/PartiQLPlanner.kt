package org.partiql.planner

import com.amazon.ionelement.api.StructElement
import org.partiql.ast.Statement
import org.partiql.plan.PartiQLPlan
import java.time.Instant

/**
 * PartiQLPlanner is responsible for transforming an AST into a logical query plan.
 */
public interface PartiQLPlanner {

    public fun plan(session: Session, statement: Statement): Result

    public class Result(
        val plan: PartiQLPlan,
    )

    /**
     * From [org.partiql.lang.planner.transforms]
     *
     * @property queryId
     * @property userId
     * @property currentCatalog
     * @property currentDirectory
     * @property catalogConfig
     * @property instant
     */
    public class Session(
        public val queryId: String,
        public val userId: String,
        public val currentCatalog: String? = null,
        public val currentDirectory: List<String> = emptyList(),
        public val catalogConfig: Map<String, StructElement> = emptyMap(),
        public val instant: Instant = Instant.now()
    )
}
