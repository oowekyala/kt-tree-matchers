package com.github.oowekyala.treematchers

/**
 * Type class for something that can behave as a node in a tree.
 * Implement it in an object and share it with e.g. [baseShouldMatchSubtree].
 *
 * @param H Top level type of the hierarchy
 */
interface TreeLikeAdapter<H : Any> {

    /**
     * Returns all the children of the [node] in order.
     */
    fun getChildren(node: H): List<H>

    /**
     * Gets the display name of this [node]. This is used
     * for error messages.
     */
    fun nodeName(node: H): String = nodeName(node::class.java)

    /**
     * Gets the display name of this [type] of node. This is used
     * for error messages. Override if it's different from the simple
     * name of the class.
     */
    fun nodeName(type: @JvmWildcard Class<out H>): String = type.simpleName

}