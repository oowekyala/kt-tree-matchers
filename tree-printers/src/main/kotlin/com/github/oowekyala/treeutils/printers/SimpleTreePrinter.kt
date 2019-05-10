package com.github.oowekyala.treeutils.printers

import com.github.oowekyala.treeutils.TreeLikeAdapter
import com.github.oowekyala.treeutils.TreeLikeExtensions

/**
 * A simple recursive printer that can output stuff like:
 *
 *    +-- LocalVariableDeclaration
 *        +-- Type
 *        |   +-- PrimitiveType
 *        +-- VariableDeclarator
 *            +-- VariableDeclaratorId
 *            +-- VariableInitializer
 *                +-- 1 child not shown
 *
 * with [AsciiStrings], or
 *
 *           └── LocalVariableDeclaration
 *               ├── Type
 *               │   └── PrimitiveType
 *               └── VariableDeclarator
 *                   ├── VariableDeclaratorId
 *                   └── VariableInitializer
 *                       └── 1 child not shown
 *
 * with [UnicodeStrings].
 *
 * By default just prints the structure, like shown above. You can
 * configure it to render nodes differently by overriding [appendSingleNode].
 *
 * @author Clément Fournier
 */
open class SimpleTreePrinter<H : Any>(
        override val adapter: TreeLikeAdapter<H>,
        protected val str: StringConfig = AsciiStrings
) : TreePrinter<H>, TreeLikeExtensions<H> {

    /**
     * Set of strings used to render the edges of the tree.
     * All strings should have the same length.
     */
    data class StringConfig(
            /** Prefix of a child node that has a following sibling. */
            val fork: String,
            /** Prefix of a child node that has no following sibling. */
            val tailFork: String,
            /** Vertical edge. */
            val verticalEdge: String,
            /** Horizontal blank. */
            val gap: String
    )

    override fun dumpSubtree(node: H, maxDumpDepth: Int): String = StringBuilder().also {
        it.printInnerNode(node, 0, maxDumpDepth, "", true)
    }.toString()

    /**
     * Override to customise how nodes are rendered.
     */
    protected open fun StringBuilder.appendSingleNode(node: H): StringBuilder = append(node.nodeName)

    /**
     * Controls how boundaries are rendered when [dumpSubtree]'s maxDumpDepth
     * is reached. When that is so, the [node] is the node whose children should
     * be ignored. The [prefix] can be used to [appendIndent]. This
     * method is responsible for the whole line, i.e. indent + content + line feed.
     *
     * Overriding with a noop effectively hides boundaries completely.
     */
    protected open fun StringBuilder.appendBoundaryForNode(node: H, prefix: String, isTail: Boolean): StringBuilder {
        appendIndent(childPrefix(prefix, isTail), true)

        return node.numChildren.let {
            if (it == 1) append("1 child is not shown")
            else append("$it children are not shown")
        }.append("\n")
    }

    /**
     * Append the indent string. The [prefix] must be appended first. If [isTail],
     * then this node is the last child of its parent.
     */
    protected fun StringBuilder.appendIndent(prefix: String, isTail: Boolean): StringBuilder =
            append(prefix).append(if (isTail) str.tailFork else str.fork)

    private fun childPrefix(prefix: String, isTail: Boolean) = prefix + if (isTail) str.gap else str.verticalEdge

    private fun StringBuilder.printInnerNode(node: H,
                                             level: Int,
                                             maxLevel: Int,
                                             prefix: String,
                                             isTail: Boolean) {

        append(prefix).append(if (isTail) str.tailFork else str.fork).appendSingleNode(node).append("\n")

        if (level == maxLevel) {
            if (node.numChildren > 0) {
                appendBoundaryForNode(node, prefix, isTail)
            }
        } else {
            val n = node.numChildren - 1
            val childPrefix = childPrefix(prefix, isTail)
            node.children.forEachIndexed { i, child ->
                printInnerNode(child, level + 1, maxLevel, childPrefix, i == n)
            }
        }
    }

    companion object {

        /**
         * Outputs trees like
         *
         *    +-- LocalVariableDeclaration
         *        +-- Type
         *        |   +-- PrimitiveType
         *        +-- VariableDeclarator
         *            +-- VariableDeclaratorId
         *            +-- VariableInitializer
         *                +-- 1 child not shown
         *
         */
        val AsciiStrings = StringConfig(
                fork = "+-- ",
                tailFork = "+-- ",
                verticalEdge = "|   ",
                gap = "    "
        )

        /**
         * Outputs trees like:
         *
         *           └── LocalVariableDeclaration
         *               ├── Type
         *               │   └── PrimitiveType
         *               └── VariableDeclarator
         *                   ├── VariableDeclaratorId
         *                   └── VariableInitializer
         *                       └── 1 child not shown
         *
         */
        val UnicodeStrings = StringConfig(
                fork = "├── ",
                tailFork = "└── ",
                verticalEdge = "│   ",
                gap = "    "
        )
    }
}
