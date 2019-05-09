package com.github.oowekyala.treeutils.printers

import com.github.oowekyala.treeutils.TreeLikeAdapter
import com.github.oowekyala.treeutils.TreeLikeExtensions
import java.util.*

/**
 * A simple recursive printer that can output stuff like:
 *
 *   +--LocalVariableDeclaration
 *      +--Type
 *      |  +--PrimitiveType
 *      +--VariableDeclarator
 *         +--VariableDeclaratorId
 *         +--VariableInitializer
 *            +--1 child not shown
 *
 * By default just prints the structure, like shown above. You can
 * configure it to render nodes differently by overriding [appendSingleNode].
 *
 * @author Clément Fournier
 */
open class SimpleTreePrinter<H : Any>(override val adapter: TreeLikeAdapter<H>) : TreePrinter<H>, TreeLikeExtensions<H> {


    override fun dumpSubtree(node: H, maxDumpDepth: Int): String = StringBuilder().also {
        it.printInnerNode(node, 0, maxDumpDepth, Stack<Boolean>().also { it += false })
    }.toString()

    /**
     * Override to customise how nodes are rendered.
     */
    protected open fun StringBuilder.appendSingleNode(node: H): StringBuilder = append(node.nodeName)

    /**
     * Controls how boundaries are rendered when [dumpSubtree]'s maxDumpDepth
     * is reached. When that is so, the [node] is the node whose children should
     * be ignored. [level] and [hasFollower] can be used to [appendIndent]. This
     * method is responsible for the whole line, i.e. indent + content + line feed.
     *
     * Overriding with a noop effectively hides boundaries completely.
     */
    protected open fun StringBuilder.appendBoundaryForNode(node: H, level: Int, hasFollower: List<Boolean>): StringBuilder {

        appendIndent(level + 1, hasFollower)

        return adapter.getChildren(node).size.let {
            if (it == 1) append("1 child is not shown")
            else append("$it children are not shown")
        }.append("\n")
    }

    /**
     * Append the indent string. The [level] is the depth of the node. A [level]
     * of zero means this is the root node. A position *i* of [hasFollower] is
     * true if the *i*'s node on the path from the root to the current node has
     * a next sibling. Simply put, if a position *i* is true, then the *i*'th
     * repetition of the indent string should have a vertical bar to join a node
     * to its siblings.
     */
    protected open fun StringBuilder.appendIndent(level: Int, hasFollower: List<Boolean>): StringBuilder {
        for (i in 0 until level) {
            if (hasFollower[i]) append("|  ") else append("   ")

        }
        return append("+--")
    }


    private fun StringBuilder.printInnerNode(node: H,
                                             level: Int,
                                             maxLevel: Int,
                                             hasFollower: Stack<Boolean>) {
        appendIndent(level, hasFollower).appendSingleNode(node).append("\n")

        if (level == maxLevel && node.numChildren > 0) {
            appendBoundaryForNode(node, level, hasFollower)
        } else {
            val n = node.numChildren
            node.children.forEachIndexed { i, child ->
                hasFollower.push(i < n - 1)
                printInnerNode(child, level + 1, maxLevel, hasFollower)
                hasFollower.pop()
            }
        }
    }

}
