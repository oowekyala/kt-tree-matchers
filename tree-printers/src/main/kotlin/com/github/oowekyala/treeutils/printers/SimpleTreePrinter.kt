package com.github.oowekyala.treeutils.printers

import com.github.oowekyala.treeutils.TreeLikeAdapter
import java.util.*

/**
 * @author Clément Fournier
 */
open class SimpleTreePrinter<H : Any>(protected val adapter: TreeLikeAdapter<H>) : TreePrinter<H> {


    /**
     * Dumps the given subtree to a string.
     *
     * @param node Subtree to dump
     * @param maxDumpDepth Maximum depth on which to recurse.
     * A value of zero only dumps the root node. A negative value
     * dumps the whole subtree.
     */
    override fun dumpSubtree(node: H, maxDumpDepth: Int): String = StringBuilder().also {
        it.printInnerNode(node, 0, maxDumpDepth, Stack<Boolean>().also { it += false })
    }.toString()

    protected open fun StringBuilder.appendSingleNode(node: H): StringBuilder = append(adapter.nodeName(node))
    protected open fun StringBuilder.appendBoundaryForNode(node: H, level: Int, hasFollower: List<Boolean>): StringBuilder {

        appendIndent(level + 1, hasFollower)

        return adapter.getChildren(node).size.let {
            if (it == 1) append("1 child is not shown")
            else append("$it children are not shown")
        }.append("\n")
    }

    protected open fun StringBuilder.appendIndent(indent: Int, hasFollower: List<Boolean>): StringBuilder {
        for (i in 0 until indent) {
            if (hasFollower[i]) append("|  ") else append("   ")

        }
        return append("+--")
    }


    private fun StringBuilder.printInnerNode(node: H,
                                             level: Int,
                                             maxLevel: Int,
                                             hasFollower: Stack<Boolean>) {
        appendIndent(level, hasFollower).appendSingleNode(node).append("\n")

        if (level == maxLevel && adapter.numChildren(node) > 0) {
            appendBoundaryForNode(node, level, hasFollower)
        } else {
            val n = adapter.numChildren(node)
            adapter.getChildren(node).forEachIndexed { i, child ->
                hasFollower.push(i < n - 1)
                printInnerNode(child, level + 1, maxLevel, hasFollower)
                hasFollower.pop()
            }
        }
    }

}
