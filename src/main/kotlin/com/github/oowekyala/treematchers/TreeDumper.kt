package com.github.oowekyala.treematchers

/**
 * Dumps a node of the hierarchy [H] to a string,
 * e.g. for error messages, or to generate unit tests.
 *
 * @author Clément Fournier
 * @since 1.0
 */
interface TreeDumper<in H : Any> {

    /**
     * Dumps the given subtree to a string.
     *
     * @param node Subtree to dump
     * @param maxDumpDepth Maximum depth on which to recurse.
     * A value of zero only dumps the root node. A negative value
     * dumps the whole subtree.
     */
    fun dumpSubtree(node: H, maxDumpDepth: Int = -1): String
}