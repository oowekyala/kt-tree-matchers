package com.github.oowekyala.treeutils

/**
 * Trait allowing using the methods of the adapter as extensions.
 * Implement it in an object.
 *
 * @param H Top level type of the hierarchy
 */
interface TreeLikeAdapterUser<H : Any> {


    val adapter: TreeLikeAdapter<H>

    val H.children: List<H>
        get() = adapter.getChildren(this)

    val H.nodeName: String
        get() = adapter.nodeName(this)

    val Class<out H>.nodeName: String
        get() = adapter.nodeName(this)

    val H.numChildren: Int
        get() = adapter.numChildren(this)

}
