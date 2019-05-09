package com.github.oowekyala.treeutils

/**
 * Traits allowing using the methods of the adapter as extensions,
 * also providing some extra navigation methods.
 *
 * Use with
 *
 *     with (adapter.ext) {
 *       node.descendantsOrSelf()
 *       node.prevSibling
 *       // etc
 *     }
 *
 * Or implement it in a subclass if you don't care about the inheritance.
 *
 * @param H Top level type of the hierarchy
 */
interface TreeLikeExtensions<H : Any> {

    val adapter: TreeLikeAdapter<H>

    /** See [TreeLikeAdapter.getChildren] */
    val H.children: List<H>
        get() = adapter.getChildren(this)

    /** See [TreeLikeAdapter.nodeName] */
    val H.nodeName: String
        get() = adapter.nodeName(this)

    /** See [TreeLikeAdapter.nodeName] */
    val Class<out H>.nodeName: String
        get() = adapter.nodeName(this)

    /** See [TreeLikeAdapter.numChildren] */
    val H.numChildren: Int
        get() = adapter.numChildren(this)

    /** See [TreeLikeAdapter.getChild] */
    fun H.getChild(index: Int): H? = adapter.getChild(this, index)

    /** See [TreeLikeAdapter.isLeaf] */
    fun H.isLeaf(node: H) = adapter.isLeaf(this)


    // Extra tree navigation methods

    /**
     * Returns a sequence of the strict descendants of this node (excluding this node).
     */
    fun H.descendants(): Sequence<H> = children.asSequence().flatMap { it.descendantsOrSelf() }

    /**
     * Returns a sequence of nodes starting with this node, and followed by its [descendants].
     */
    fun H.descendantsOrSelf(): Sequence<H> = sequence { yield(this@descendantsOrSelf); yieldAll(descendants()) }


}

/**
 * Extensions for [DoublyLinkedTreeLikeAdapter].
 */
interface DlTreeLikeExtensions<H : Any> : TreeLikeExtensions<H> {

    override val adapter: DoublyLinkedTreeLikeAdapter<H>

    /** See [DoublyLinkedTreeLikeAdapter.getParent] */
    val H.parent: H? get() = adapter.getParent(this)

    /** See [DoublyLinkedTreeLikeAdapter.getChildIndex] */
    val H.childIndex: Int get() = adapter.getChildIndex(this)

    // Extra tree navigation methods

    /**
     * Returns a sequence of the strict ancestors of this node (not containing this node),
     * ending with the root.
     */
    fun H.ancestors(): Sequence<H> = generateSequence(parent) { it.parent }

    /** Returns the root node of the tree in which this node lives. */
    val H.root: H
        get() = ancestorsOrSelf().last()

    /** Returns true if this is the [root] node of the tree. */
    val H.isRoot: Boolean
        get() = parent == null

    /** Returns a sequence of nodes starting with this node, and followed by its [ancestors].  */
    fun H.ancestorsOrSelf(): Sequence<H> = sequence { yield(this@ancestorsOrSelf); yieldAll(ancestors()) }

    /**
     * Returns a sequence of the siblings following this node, iterated from left to right.
     */
    fun H.followingSiblings(): Sequence<H> =
            parent?.children?.asSequence()?.drop(childIndex + 1)
                    ?: emptySequence()

    /**
     * Returns the next sibling of this node if it exists.
     */
    val H.nextSibling: H? get() = parent?.getChild(childIndex + 1)

    /**
     * Returns a sequence of the siblings preceding this node, iterated from right to left.
     */
    fun H.precedingSiblings(): Sequence<H> =
            parent?.children?.asSequence()?.take(childIndex - 1)?.toList()?.asReversed()?.asSequence()
                    ?: emptySequence()

    /**
     * Returns the previous sibling of this node if it exists.
     */
    val H.previousSibling: H? get() = parent?.getChild(childIndex - 1)


}


fun <H : Any, R> TreeLikeAdapter<H>.withExt(b: TreeLikeExtensions<H>.() -> R) = with(ext, b)
fun <H : Any, R> DoublyLinkedTreeLikeAdapter<H>.withExt(b: DlTreeLikeExtensions<H>.() -> R) = with(ext, b)

/**
 * Creates a [TreeLikeExtensions] instance for this adapter.
 */
val <H : Any> TreeLikeAdapter<H>.ext: TreeLikeExtensions<H>
    get() = object : TreeLikeExtensions<H> {
        override val adapter: TreeLikeAdapter<H>
            get() = this@ext
    }


/**
 * Creates a [DlTreeLikeExtensions] instance for this adapter.
 */
val <H : Any> DoublyLinkedTreeLikeAdapter<H>.ext: DlTreeLikeExtensions<H>
    get() = object : DlTreeLikeExtensions<H> {
        override val adapter: DoublyLinkedTreeLikeAdapter<H>
            get() = this@ext
    }
