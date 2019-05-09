package com.github.oowekyala.treeutils

/**
 * Type class for something that can behave as a node in a tree.
 * Implement it in an object.
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
    fun nodeName(type: Class<out H>): String = type.simpleName


    // Those are here for convenience and can be overridden to provide more efficient implementations

    /**
     * Returns the number of children of the [node].
     */
    fun numChildren(node: H): Int = getChildren(node).size

    /**
     * Returns the child at the given [index] if it exists.
     */
    fun getChild(node: H, index: Int): H? = if (index in 0..numChildren(node)) getChildren(node)[index] else null

    /**
     * Returns true if this node has no children.
     */
    fun isLeaf(node: H) = numChildren(node) == 0

}

/**
 * A [TreeLikeAdapter] where each node has a reference to its parent.
 */
interface DoublyLinkedTreeLikeAdapter<H : Any> : TreeLikeAdapter<H> {
    /**
     * Returns the parent of the node. This is used to emit
     * an assertion within child calls.
     */
    fun getParent(node: H): H?

    /**
     * Returns the index of this node in the children of its parent.
     * The default implementation is very inefficient, if you have a
     * better one you should probably use it.
     */
    fun getChildIndex(node: H): Int {
        return getChildren(getParent(node) ?: return -1).indexOf(node)
    }

}
