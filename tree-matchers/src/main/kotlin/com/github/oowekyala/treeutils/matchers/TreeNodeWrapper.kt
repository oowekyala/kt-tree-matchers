package com.github.oowekyala.treeutils.matchers

import com.github.oowekyala.treeutils.DoublyLinkedTreeLikeAdapter
import com.github.oowekyala.treeutils.TreeLikeAdapter
import com.github.oowekyala.treeutils.printers.DslStructurePrinter
import com.github.oowekyala.treeutils.printers.TreePrinter
import kotlin.test.assertFalse
import kotlin.test.asserter
import kotlin.text.RegexOption.DOT_MATCHES_ALL

/**
 * Sequence of parents of an [TreeNodeWrapper]. Used for
 * error messages.
 *
 * @param H Hierarchy of the node
 */
private typealias MatcherPath<H> = List<TreeNodeWrapper<H, out H>>

/**
 * Wraps a node, providing easy access to [it]. Additional matching
 * methods are provided to match children. This DSL supports objects
 * of any type provided they can be viewed as nodes of a tree. The
 * [TreeLikeAdapter] type class witnesses this property.
 *
 * ### Samples
 *
 * These samples assume you use [kotlintest](https://github.com/kotlintest/kotlintest),
 * and implemented a `matchNode` method like explained on [baseShouldMatchSubtree].
 * The only difference if you don't use kotlintest are the `should` and `shouldBe` calls.
 *
 *    node should matchNode<ASTStatement> {
 *
 *        // nesting matchers allow to specify a whole subtree
 *        child<ASTForStatement> {
 *
 *            // This would fail if the first child of the ForStatement wasn't a ForInit
 *            child<ASTForInit> {
 *                child<ASTLocalVariableDeclaration> {
 *
 *                    // If the parameter ignoreChildren is set to true, the number of children is not asserted
 *                    // Calls to "child" in the block are forbidden
 *                    // The only checks carried out here are the type test and the assertions of the block
 *                    child<ASTType>(ignoreChildren = true) {
 *
 *                        // In a "child" block, the tested node can be referred to as "it"
 *                        // Here, its static type is ASTType, so we can inspect properties
 *                        // of the node and make assertions
 *
 *                        it.typeImage shouldBe "int"
 *                        it.type shouldNotBe null
 *                    }
 *
 *                    // We don't care about that node, we only care that there is "some" node
 *                    unspecifiedChild()
 *                }
 *            }
 *
 *            // The subtree is ignored, but we check a ForUpdate is present at this child position
 *            child<ASTForUpdate>(ignoreChildren = true) {}
 *
 *            // Here, ignoreChildren is not specified and takes its default value of false.
 *            // The lambda has no "child" calls and the node will be asserted to have no children
 *            child<ASTBlock> {}
 *        }
 *    }
 *
 *    // To get good error messages, it's important to define assertions
 *    // on the node that is supposed to verify them, so if it needs some
 *    // value from its children, you can go fetch that value in two ways:
 *    // * if you just need the child node, the child method already returns that
 *    // * if you need some more complex value, or to return some subchild, use fromChild
 *
 *    catchStmt should matchNode<ASTCatchStatement> {
 *       it.isMulticatchStatement shouldBe true
 *
 *       // The fromChild method is a variant of child which can return anything.
 *       // Specify the return type as a type parameter
 *       val types = fromChild<ASTFormalParameter, List<ASTType>> {
 *
 *           // The child method returns the child (strongly typed)
 *           val ioe = child<ASTType>(ignoreChildren = true) {
 *               it.type shouldBe IOException::class.java
 *           }
 *
 *           val aerr = child<ASTType>(ignoreChildren = true) {
 *               it.type shouldBe java.lang.AssertionError::class.java
 *           }
 *
 *           unspecifiedChild()
 *
 *           // The last expression of the lambda is returned from fromChild
 *           listOf(ioe, aerr)
 *       }
 *
 *       // Here you can use the returned value to perform more assertions*
 *
 *       it.caughtExceptionTypeNodes.shouldContainExactly(types)
 *       it.caughtExceptionTypes.shouldContainExactly(types.map { it.type })
 *
 *       it.exceptionName shouldBe "e"
 *
 *       child<ASTBlock> { }
 *    }
 *
 *
 *
 *
 * @property it         The node being matched
 *
 * @constructor
 * @param it            The node being matched
 * @param myMatchingConfig     Instance of the [TreeLikeAdapter] type class for the [H] type hierarchy
 * @param myParentPath  List of the parents of this node, (not including the self), used to reconstruct a path for error messages
 * @param myChildMatchersAreIgnored Ignore calls to [child]
 *
 * @param H Hierarchy of the node
 * @param N Type of the node
 */
class TreeNodeWrapper<H : Any, N : H> private constructor(
        val it: N,
        private val myMatchingConfig: MatchingConfig<H>,
        private val myParentPath: MatcherPath<H>,
        private val myChildMatchersAreIgnored: Boolean
) {

    private val myChildren = myMatchingConfig.adapter.getChildren(it)
    private val pathToMe = myParentPath + this
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
            lazyAssertTrue(childIdx in myChildren.indices) {
                formatErrorMessage(
                        myMatchingConfig,
                        myParentPath,
                        "Node has fewer children than expected, child #$childIdx doesn't exist"
                )
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
    inline fun <reified M : H> child(
            ignoreChildren: Boolean = false,
            noinline nodeSpec: TreeNodeWrapper<H, M>.() -> Unit
    ): M =
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
    inline fun <reified M : H, R> fromChild(
            ignoreChildren: Boolean = false,
            noinline nodeSpec: TreeNodeWrapper<H, M>.() -> R
    ): R =
            childImpl(ignoreChildren, M::class.java, nodeSpec)

    @PublishedApi
    internal fun <M : H, R> childImpl(
            ignoreChildren: Boolean,
            childType: Class<M>,
            nodeSpec: TreeNodeWrapper<H, M>.() -> R
    ): R {
        if (!myChildMatchersAreIgnored)
            return executeWrapper(myMatchingConfig, childType, shiftChild(), pathToMe, ignoreChildren, nodeSpec)
        else
            throw IllegalStateException(
                    formatErrorMessage(
                            myMatchingConfig,
                            pathToMe,
                            "Calling child when ignoreChildren=true is forbidden"
                    )
            )
    }

    /**
     * @suppress
     */
    override fun toString(): String {
        return "NWrapper<${myMatchingConfig.adapter.nodeName(it::class.java)}>"
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
                matchingConfig: MatchingConfig<H>,
                matcherPath: MatcherPath<H>,
                message: String
        ) =
                "At ${formatPath(matchingConfig.adapter, matcherPath)}: $message".plus(
                        if (matchingConfig.errorPrinter != null && matcherPath.isNotEmpty()) {
                            "\n\nThe error occurred in this subtree:\n\n" +
                            matchingConfig.errorPrinter.dumpSubtree(matcherPath.last().it)

                        } else ""
                )

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
                matchingConfig: MatchingConfig<H>,
                childType: Class<M>,
                toWrap: H,
                parentPath: MatcherPath<H>,
                ignoreChildrenMatchers: Boolean,
                spec: TreeNodeWrapper<H, M>.() -> R
        ): R {

            val isRoot = parentPath.isEmpty()

            val nodeNameForMsg = when {
                isRoot -> "root"
                else -> "child #${parentPath.last().myChildren.indexOf(toWrap)}"
            }

            lazyAssertTrue(childType.isInstance(toWrap)) {
                formatErrorMessage(
                        matchingConfig,
                        parentPath,
                        "Expected $nodeNameForMsg to have type ${matchingConfig.adapter.nodeName(childType)}, " +
                                "actual ${matchingConfig.adapter.nodeName(toWrap.javaClass)}"
                )
            }

            if (parentPath.isNotEmpty() && matchingConfig.adapter is DoublyLinkedTreeLikeAdapter) {

                val childParent = matchingConfig.adapter.getParent(toWrap)

                lazyAssertTrue(childParent == parentPath.last().it) {
                    formatErrorMessage(
                            matchingConfig,
                            parentPath,
                            "Expected $nodeNameForMsg to have parent " +
                                    "${matchingConfig.adapter.nodeName(parentPath.last().it)}, actual " +
                                    "${childParent?.let { matchingConfig.adapter.nodeName(childParent.javaClass) }}"
                    )
                }

            }

            @Suppress("UNCHECKED_CAST")
            val m = toWrap as M

            val wrapper = TreeNodeWrapper(m, matchingConfig, parentPath, ignoreChildrenMatchers)

            val ret: R = try {
                val r = wrapper.spec()
                matchingConfig.implicitAssertions(m)
                r
            } catch (e: AssertionError) {
                if (e.message?.matches("At (/.*?|<root>):.*".toRegex(DOT_MATCHES_ALL)) == false) {
                    // the exception has no path, let's add one
                    throw AssertionError(
                            formatErrorMessage(
                                    matchingConfig, wrapper.pathToMe,
                                    e.message ?: "No explanation provided"
                            ), e
                    )
                }
                throw e
            }

            lazyAssertFalse(!ignoreChildrenMatchers && wrapper.nextChildMatcherIdx != wrapper.myNumChildren) {
                formatErrorMessage(
                        matchingConfig,
                        wrapper.pathToMe,
                        "Wrong number of children, expected ${wrapper.nextChildMatcherIdx}, actual ${wrapper.myNumChildren}"
                )
            }
            return ret
        }
    }
}

/**
 * Class containing configuration for [baseShouldMatchSubtree]. This is
 * intended as an extension point for future changes to the API.
 *
 * @param adapter The [TreeLikeAdapter] used to roam the tree under an [H] node.
 *
 * @param errorPrinter  If not null, error messages will use this pretty printer
 * to add a dump of the subtree below the node where the error occurred.
 *
 * @param maxDumpDepth The maximum depth to which the pretty printer will dump nodes.
 * A value of zero only dumps the root node. A negative value dumps the whole subtree.
 * By default the whole subtree is dumped.
 *
 * @param implicitAssertions Assertions that are executed at the end of
 * every node matcher. The default does nothing
 */
data class MatchingConfig<H : Any>(
        val adapter: TreeLikeAdapter<H>,
        val errorPrinter: TreePrinter<H>? = DslStructurePrinter(adapter),
        val maxDumpDepth: Int = -1,
        val implicitAssertions: (H) -> Unit = {}
) {
    constructor(
            adapter: TreeLikeAdapter<H>,
            errorPrinterMaker: (TreeLikeAdapter<H>) -> TreePrinter<H>?,
            maxDumpDepth: Int = -1,
            implicitAssertions: (H) -> Unit = {}
    ) : this(adapter, errorPrinterMaker(adapter), maxDumpDepth)
}

// building the error messages is costly so best avoid it
private fun lazyAssertTrue(condition: Boolean, lazyMessage: () -> String?): Unit =
        asserter.assertTrue(lazyMessage, condition)

private fun lazyAssertFalse(condition: Boolean, lazyMessage: () -> String?): Unit =
        asserter.assertTrue(lazyMessage, !condition)


/**
 * Base method to assert that a node of a hierarchy [H] matches the subtree
 * specified by [nodeSpec]. The [receiver][this] is first asserted not to be null, then
 * is fed to the assertions contained in [nodeSpec].
 *
 * ### Adapting this method to your use case
 *
 * The [H] type parameter and the [matchingConfig] are unnecessary when the domain is known,
 * so concrete DSLs should provide their own method that delegates to this one and provides
 * the config.
 *
 * For example, if your tree type hierarchy is topped by a class named `Node`, and you implemented
 * [TreeLikeAdapter]<Node> on an object `NodeTreeLikeAdapter`, then you could provide the following
 * shorthand to remove the boilerplate parameters:
 *
 *     inline fun <reified N : Node> Node?.shouldMatchNode(ignoreChildren: Boolean = false,
 *                                                         noinline nodeSpec: TreeNodeWrapper<Node, N>.() -> Unit) =
 *         this.baseShouldMatchSubtree(MatchingConfig(adapter = NodeTreeLikeAdapter), ignoreChildren, nodeSpec)
 *
 *
 * Which would allow you to call it like the following:
 *
 *     someNode.shouldMatchNode<SomeNodeType> {
 *          child<SomeOtherNodeType> {
 *              ...
 *          }
 *          ...
 *     }
 *
 * You can also provide a custom pretty printer when building your [MatchingConfig], or set a depth maximum
 * on tree dumps. If you want to be able to configure the pretty printer at call sites, you should add a parameter
 * for it on your `shouldMatchNode`.
 *
 * #### Using [kotlintest](https://github.com/kotlintest/kotlintest)
 *
 * To play well with [kotlintest](https://github.com/kotlintest/kotlintest)'s `should` DSL,
 * you can also implement a function returning another function of type `(Node?) -> Unit` instead.
 * For example:
 *
 *     inline fun <reified N : Node> matchNode(ignoreChildren: Boolean = false,
 *                                             noinline nodeSpec: NWrapper<Node, N>.() -> Unit)
 *                                             : (Node?) -> Unit  = {
 *                                                 it.shouldMatchNode(ignoreChildren, nodeSpec)
 *                                             }
 *
 * Which would allow you to call it like the following:
 *
 *      node should matchNode<ASTExpression> {
 *          ...
 *      }
 *
 * ### Samples
 *
 * See [TreeNodeWrapper].
 *
 * @param H Considered hierarchy
 * @param N Expected type of the node
 *
 * @receiver The node that will be matched against the [nodeSpec]
 *
 * @param matchingConfig Configuration object defining the [TreeLikeAdapter] to use,
 * plus some additional behaviour for error messages, etc.
 *
 * @param ignoreChildren If true, calls to [TreeNodeWrapper.child] in the [nodeSpec] are forbidden.
 * The number of children of the child is not asserted.
 *
 * @param nodeSpec Sequence of assertions to carry out on the node, which can be referred to by [TreeNodeWrapper.it].
 * Assertions may consist of [TreeNodeWrapper.child] calls, which perform the same type of node
 * matching on a child of the tested node, or regular assertions on [TreeNodeWrapper.it].
 *
 * @throws AssertionError If one of the assertions is violated, e.g. a node is not of the expected type,
 * a child doesn't exist, etc.
 *
 */
inline fun <H : Any, reified N : H> H?.baseShouldMatchSubtree(
        matchingConfig: MatchingConfig<H>,
        ignoreChildren: Boolean = false,
        noinline nodeSpec: TreeNodeWrapper<H, N>.() -> Unit
) {
    assertFalse("Expected node of type ${matchingConfig.adapter.nodeName(N::class.java)}, but was null") {
        this == null
    }

    TreeNodeWrapper.executeWrapper(
            matchingConfig,
            N::class.java,
            this!!,
            emptyList(),
            ignoreChildren,
            nodeSpec
    )
}