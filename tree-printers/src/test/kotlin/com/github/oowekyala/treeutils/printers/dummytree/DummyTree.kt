package com.github.oowekyala.treeutils.printers.dummytree

import com.github.oowekyala.treeutils.TreeLikeAdapter

/**
 * Dummy tree hierarchy.
 *
 * @author Clément Fournier
 * @since 1.0
 */
sealed class DummyTree(open val children: List<DummyTree>) {

    data class StringLeaf(val myText: String) : DummyTree(emptyList())
    data class PrefixNode(val myPrefix: String, val child: DummyTree) : DummyTree(listOf(child))
    data class MultiChildNode(override val children: List<DummyTree>) : DummyTree(children) {
        constructor(vararg children: DummyTree) : this(children.toList())
    }
}

object DummyAdapter : TreeLikeAdapter<DummyTree> {
    override fun getChildren(node: DummyTree): List<DummyTree> = node.children
}

