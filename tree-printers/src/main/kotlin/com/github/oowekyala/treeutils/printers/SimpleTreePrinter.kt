package com.github.oowekyala.treeutils.printers

import com.github.oowekyala.treeutils.TreeLikeAdapter

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
        printInnerNode(node, 0, maxDumpDepth, it)
    }.toString()

    protected open fun StringBuilder.appendSingleNode(node: H): StringBuilder = append(adapter.nodeName(node))
    protected open fun StringBuilder.appendBoundaryForNode(node: H, level: Int): StringBuilder {

        appendIndent(level + 1)

        return adapter.getChildren(node).size.let {
            if (it == 1) append("1 child is not shown")
            else append("$it children are not shown")
        }
    }

    protected open fun StringBuilder.appendIndent(indent: Int): StringBuilder {
        for (i in 0 until indent) {
            append("|  ")
        }
        return append("+--")
    }

    private fun printInnerNode(node: H,
                               level: Int,
                               maxLevel: Int,
                               sb: StringBuilder) {
        sb.appendIndent(level).appendSingleNode(node).append("\n")

        if (level == maxLevel && adapter.numChildren(node) > 0) {
            sb.appendBoundaryForNode(node, level).append("\n")
        } else {
            for (child in adapter.getChildren(node)) {
                printInnerNode(child, level + 1, maxLevel, sb)
            }
        }
    }

}
