package com.github.oowekyala.treematchers

/** Type class for something that can behave as a node in a tree. */
interface TreeLikeAdapter<H : Any> {

    /** Returns all children of the given node. */
    fun getChildren(node: H): List<H>

    /** Gets the display name of this type of node. */
    fun nodeName(type: H): String = nodeName(type::class.java)

    /** Gets the display name of this type of node. */
    fun nodeName(type: Class<out H>): String

}