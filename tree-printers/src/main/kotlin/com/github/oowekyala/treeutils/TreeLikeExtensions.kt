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

    val H.children: List<H>
        get() = adapter.getChildren(this)

    val H.nodeName: String
        get() = adapter.nodeName(this)

    val Class<out H>.nodeName: String
        get() = adapter.nodeName(this)

    val H.numChildren: Int
        get() = adapter.numChildren(this)

    fun H.getChild(index: Int): H? = adapter.getChild(this, index)

    fun H.isLeaf(node: H) = adapter.isLeaf(this)


    // Extra tree navigation methods


    fun H.descendants(): Sequence<H> = children.asSequence().flatMap { it.descendantsOrSelf() }
    fun H.descendantsOrSelf(): Sequence<H> = sequence { yield(this@descendantsOrSelf); yieldAll(descendants()) }


}

interface DlTreeLikeExtensions<H : Any> : TreeLikeExtensions<H> {

    override val adapter: DoublyLinkedTreeLikeAdapter<H>

    val H.parent: H? get() = adapter.getParent(this)

    val H.childIndex: Int get() = adapter.getChildIndex(this)

    // Extra tree navigation methods


    fun H.ancestors(): Sequence<H> = generateSequence(parent) { it.parent }
    fun H.ancestorsOrSelf(): Sequence<H> = sequence { yield(this@ancestorsOrSelf); yieldAll(ancestors()) }

    fun H.followingSiblings(): Sequence<H> =
            parent?.children?.asSequence()?.drop(childIndex + 1)
                    ?: emptySequence()

    val H.nextSibling: H? get() = followingSiblings().firstOrNull()


    fun H.precedingSiblings(): Sequence<H> =
            parent?.children?.asSequence()?.take(childIndex - 1)?.toList()?.asReversed()?.asSequence()
                    ?: emptySequence()

    val H.previousSibling: H? get() = precedingSiblings().firstOrNull()


}


fun <H : Any, R> TreeLikeAdapter<H>.withExt(b: TreeLikeExtensions<H>.() -> R) = with(ext, b)
fun <H : Any, R> DoublyLinkedTreeLikeAdapter<H>.withExt(b: DlTreeLikeExtensions<H>.() -> R) = with(ext, b)

val <H : Any> TreeLikeAdapter<H>.ext: TreeLikeExtensions<H>
    get() = object : TreeLikeExtensions<H> {
        override val adapter: TreeLikeAdapter<H>
            get() = this@ext
    }


val <H : Any> DoublyLinkedTreeLikeAdapter<H>.ext: DlTreeLikeExtensions<H>
    get() = object : DlTreeLikeExtensions<H> {
        override val adapter: DoublyLinkedTreeLikeAdapter<H>
            get() = this@ext
    }
