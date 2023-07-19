
import org.partiql.ast.AstNode
import org.partiql.ast.visitor.AstBaseVisitor
import kotlin.reflect.KVisibility
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.jvmErasure

/**
 * Basic printer for debugging during early development lifecycle
 */
internal object AstPrinter {

    fun toString(ast: AstNode): String = buildString { append(this, ast) }

    fun append(out: Appendable, ast: AstNode) {
        val ctx = Args(out)
        Visitor.visit(ast, ctx)
    }

    // args for a visitor invocation
    private class Args(
        val out: Appendable,
        val levels: Array<Boolean> = emptyArray(),
        val last: Boolean = true,
    ) {
        // leading characters of a tree print
        val lead: String = when (levels.size) {
            0 -> "⚬ "
            else -> {
                val prefix = levels.joinToString("") { if (it) "│  " else "   " }
                val suffix = if (last) "└──" else "├──"
                prefix + suffix
            }
        }
    }

    private object Visitor : AstBaseVisitor<Unit, Args>() {

        private val EOL = System.lineSeparator()

        override fun defaultReturn(node: AstNode, ctx: Args) = Unit

        override fun defaultVisit(node: AstNode, ctx: Args): Unit = with(ctx) {
            out.append(lead)
            // print node name
            out.append(node::class.simpleName)
            // print primitive items
            val primitives = node.primitives().filter { it.second != null && it.first != "_id" }
            if (primitives.isNotEmpty()) {
                out.append("[")
                out.append(primitives.joinToString { "${it.first}=${it.second}" })
                out.append("]")
            }
            out.append(EOL)
            // print child nodes
            node.children.forEachIndexed { i, child ->
                val args = Args(out, levels + !last, last = i == node.children.size - 1)
                child.accept(Visitor, args)
            }
        }

        // joins all primitive properties as strings [ (<k_0, v_0>), ... (<k_n>, <v_n>) ]
        private fun AstNode.primitives(): List<Pair<String, Any?>> = javaClass.kotlin.memberProperties
            .filter {
                val t = it.returnType.jvmErasure
                val notChildren = it.name != "children"
                val notNode = !t.isSubclassOf(AstNode::class)
                // not currently correct
                val notCollectionOfNodes = !(t.isSubclassOf(Collection::class))
                notChildren && notNode && notCollectionOfNodes && it.visibility == KVisibility.PUBLIC
            }
            .map { it.name to it.get(this) }
    }
}
