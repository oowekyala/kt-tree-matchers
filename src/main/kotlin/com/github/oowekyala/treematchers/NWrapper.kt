package com.github.oowekyala.treematchers

import kotlin.test.assertFalse
import kotlin.test.assertTrue

typealias MatcherPath<H> = List<NWrapper<H, out H>>

/**
 * Wraps a node, providing easy access to [it]. Additional matching
 * methods are provided to match children. This DSL supports objects
 * of any type provided they can be viewed as nodes of a tree. The
 * [TreeLikeAdapter] type class witnesses this property.
 *
 *
 * @property it         Wrapped node
 * @param adapter       Instance of the [TreeLikeAdapter] type class for the [H] type hierarchy
 * @param parentPath    List of the parents of this node, (not including the self), used to reconstruct a path for error messages
 * @param childMatchersAreIgnored Ignore calls to [child]
 *
 * @param H Hierarchy of the node
 * @param N Type of the node
 */
class NWrapper<H : Any, N : H> private constructor(
    val it: N,
    private val adapter: TreeLikeAdapter<H>,
    private val parentPath: MatcherPath<H>,
    private val childMatchersAreIgnored: Boolean
) {

    private val myChildren = adapter.getChildren(it)
    private val pathToMe = parentPath + this
    private val myNumChildren = myChildren.size

    /** Index to which the next child matcher will apply. */
    private var nextChildMatcherIdx = 0

    private fun shiftChild(num: Int = 1): H {

        checkChildExists(nextChildMatcherIdx)

        val ret = myChildren[nextChildMatcherIdx]

        nextChildMatcherIdx += num
        return ret
    }

    private fun checkChildExists(childIdx: Int) =
        assertTrue(
            formatErrorMessage(
                adapter,
                parentPath,
                "Node has fewer children than expected, child #$childIdx doesn't exist"
            )
        ) {
            childIdx in 0..myChildren.size
        }

    /**
     * Specify that the next [num] children will only be tested for existence,
     * but not for type, or anything else.
     */
    fun unspecifiedChildren(num: Int) {
        shiftChild(num)
        // Checks that the last child mentioned exists
        checkChildExists(nextChildMatcherIdx - 1)
    }

    /**
     * Specify that the next child will only be tested for existence,
     * but not for type, or anything else.
     */
    fun unspecifiedChild() = unspecifiedChildren(1)

    /**
     * Specify that the next child will be tested against the assertions
     * defined by the lambda.
     *
     * This method asserts that the child exists, and that it is of the
     * required type [M]. The lambda is then executed on it. Subsequent
     * calls to this method at the same tree level will test the next
     * children.
     *
     * @param ignoreChildren If true, the number of children of the child is not asserted.
     *                       Calls to [child] in the [nodeSpec] throw an exception.
     * @param nodeSpec Sequence of assertions to carry out on the child node
     *
     * @param M Expected type of the child
     *
     * @throws AssertionError If the child is not of type [M], or fails the assertions of the [nodeSpec]
     * @return The child, if it passes all assertions, otherwise throws an exception
     */
    inline fun <reified M : H> child(ignoreChildren: Boolean = false, noinline nodeSpec: NWrapper<H, M>.() -> Unit): M =
        childImpl(ignoreChildren, M::class.java) { nodeSpec(); it }

    /**
     * Specify that the next child will be tested against the assertions
     * defined by the lambda, and returns the return value of the lambda.
     *
     * This method asserts that the child exists, and that it is of the
     * required type [M]. The lambda is then executed on it. Subsequent
     * calls to this method at the same tree level will test the next
     * children.
     *
     * @param ignoreChildren If true, the number of children of the child is not asserted.
     *                       Calls to [child] in the [nodeSpec] throw an exception.
     * @param nodeSpec Sequence of assertions to carry out on the child node
     *
     * @param M Expected type of the child
     * @param R Return type of the call
     *
     * @throws AssertionError If the child is not of type [M], or fails the assertions of the [nodeSpec]
     * @return The return value of the lambda
     */
    inline fun <reified M : H, R> childRet(
        ignoreChildren: Boolean = false,
        noinline nodeSpec: NWrapper<H, M>.() -> R
    ): R =
        childImpl(ignoreChildren, M::class.java, nodeSpec)

    @PublishedApi
    internal fun <M : H, R> childImpl(
        ignoreChildren: Boolean,
        childType: Class<M>,
        nodeSpec: NWrapper<H, M>.() -> R
    ): R {
        if (!childMatchersAreIgnored)
            return executeWrapper(adapter, childType, shiftChild(), pathToMe, ignoreChildren, nodeSpec)
        else
            throw IllegalStateException(
                formatErrorMessage(
                    adapter,
                    pathToMe,
                    "Calling child when ignoreChildren=true is forbidden"
                )
            )
    }

    override fun toString(): String {
        return "NWrapper<${adapter.nodeName(it::class.java)}>"
    }

    @PublishedApi
    internal companion object {

        private fun <H : Any> formatPath(adapter: TreeLikeAdapter<H>, matcherPath: MatcherPath<H>) =
            when {
                matcherPath.isEmpty() -> "<root>"
                else -> matcherPath.joinToString(separator = "/", prefix = "/") {
                    adapter.nodeName(it.it)
                }
            }

        private fun <H : Any> formatErrorMessage(
            adapter: TreeLikeAdapter<H>,
            matcherPath: MatcherPath<H>,
            message: String
        ) =
            "At ${formatPath(adapter, matcherPath)}: $message"

        /**
         * Execute wrapper assertions on a node.
         *
         * @param childType Expected type of [toWrap]
         * @param toWrap Node on which to execute the assertions
         * @param parentPath List of types of the parents of this node, used to reconstruct a path for error messages
         * @param ignoreChildrenMatchers Ignore the children matchers in [spec]
         * @param spec Assertions to carry out on [toWrap]
         *
         * @param H Hierarchy of the node
         * @param M Expected type of [toWrap]
         * @param R Return type
         *
         * @throws AssertionError If some assertions fail
         * @return [toWrap], if it passes all assertions, otherwise throws an exception
         */
        @PublishedApi
        internal fun <H : Any, M : H, R> executeWrapper(
            adapter: TreeLikeAdapter<H>,
            childType: Class<M>,
            toWrap: H,
            parentPath: MatcherPath<H>,
            ignoreChildrenMatchers: Boolean,
            spec: NWrapper<H, M>.() -> R
        ): R {

            val nodeNameForMsg = when {
                parentPath.isEmpty() -> "root"
                else -> "child #${parentPath.last().myChildren.indexOf(toWrap)}"
            }

            assertTrue(
                formatErrorMessage(
                    adapter,
                    parentPath,
                    "Expected $nodeNameForMsg to have type ${adapter.nodeName(childType)}, actual ${adapter.nodeName(
                        toWrap.javaClass
                    )}"
                )
            ) {
                childType.isInstance(toWrap)
            }

            @Suppress("UNCHECKED_CAST")
            val m = toWrap as M

            val wrapper = NWrapper(m, adapter, parentPath, ignoreChildrenMatchers)

            val ret: R = try {
                wrapper.spec()
            } catch (e: AssertionError) {
                if (e.message?.matches("At (/.*?|<root>):.*".toRegex()) == false) {
                    // the exception has no path, let's add one
                    throw AssertionError(
                        formatErrorMessage(
                            adapter, wrapper.pathToMe, e.message
                                ?: "No explanation provided"
                        ), e
                    )
                }
                throw e
            }

            assertFalse(
                formatErrorMessage(
                    adapter,
                    wrapper.pathToMe,
                    "Wrong number of children, expected ${wrapper.nextChildMatcherIdx}, actual ${wrapper.myNumChildren}"
                )
            ) {
                !ignoreChildrenMatchers && wrapper.nextChildMatcherIdx != wrapper.myNumChildren
            }
            return ret
        }
    }
}


/**
 * Executes the given node spec on this node. The [adapter] and [H] type parameter
 * should be hidden away behind a concrete method adapted to your use case.
 *
 * For example, if your tree type hierarchy is topped by a class named `Node`, and you implemented
 * [TreeLikeAdapter]<Node> on an object `NodeTreeLikeAdapter`, then you could provide the following
 * shorthand to remove the boilerplate parameter:
 *
 *     inline fun <reified N : Node> executeNodeSpec(ignoreChildren: Boolean = false,
 *                                                   noinline nodeSpec: NWrapper<Node, N>.() -> Unit) =
 *         executeNodeSpec(NodeTreeLikeAdapter, ignoreChildren, nodeSpec)
 *
 * Which would allow you to call it like the following:
 *
 *     someNode.executeNodeSpec<SomeNodeType> {
 *          child<SomeOtherNodeType> {
 *              ...
 *          }
 *          ...
 *     }
 *
 *
 *
 * @param H Considered hierarchy
 * @param N Expected type of the node
 *
 * @param adapter Instance of the [TreeLikeAdapter] type class for the [H] hierarchy
 *
 * @param ignoreChildren If true, calls to [NWrapper.child] in the [nodeSpec] are forbidden.
 *                       The number of children of the child is not asserted.
 *
 * @param nodeSpec Sequence of assertions to carry out on the node, which can be referred to by [NWrapper.it].
 *                 Assertions may consist of [NWrapper.child] calls, which perform the same type of node
 *                 matching on a child of the tested node.
 *
 * @throws AssertionError If one of the assertions is violated, e.g. a node is not of the expected type,
 *                        a child doesn't exist, etc.
 *
 */
inline fun <H : Any, reified N : H> H.executeNodeSpec(
    adapter: TreeLikeAdapter<H>,
    ignoreChildren: Boolean = false,
    noinline nodeSpec: NWrapper<H, N>.() -> Unit
) {
    NWrapper.executeWrapper(
        adapter,
        N::class.java,
        this,
        emptyList(),
        ignoreChildren,
        nodeSpec
    )
}
