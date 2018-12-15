package com.github.oowekyala.treematchers

import arrow.core.Either
import io.kotlintest.Matcher
import io.kotlintest.Result
import io.kotlintest.shouldNotBe

/**
 * Base method to produce a kotlintest [Matcher] for nodes of a hierarchy [H]. The [H] type
 * parameter and the [adapter] are unnecessary when the domain is known, so concrete DSLs
 * should provide their own method that delegates to this one and provides the adapter.
 *
 * For example, if your tree type hierarchy is topped by a class named `Node`, and you implemented
 * [TreeLikeAdapter]<Node> on an object `NodeTreeLikeAdapter`, then you could provide the following
 * shorthand to remove the boilerplate parameter:
 *
 *     inline fun <reified N : Node> matchNode(ignoreChildren: Boolean = false,
 *                                                   noinline nodeSpec: NWrapper<Node, N>.() -> Unit) =
 *         baseMatchSubtree(NodeTreeLikeAdapter, ignoreChildren, nodeSpec)
 *
 *
 * Use it with [io.kotlintest.should], e.g. `node should matchNode<ASTExpression> { /* Tree spec */}`.
 * See also samples below.
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
 * @return A matcher for AST nodes, suitable for use by [io.kotlintest.should].
 *
 * ### Samples
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
 *    // * if you need some more complex value, or to return some subchild, use childRet
 *
 *    catchStmt should matchStmt<ASTCatchStatement> {
 *       it.isMulticatchStatement shouldBe true
 *
 *       // The childRet method is a variant of child which can return anything.
 *       // Specify the return type as a type parameter
 *       val types = childRet<ASTFormalParameter, List<ASTType>> {
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
 *           // You have to use the annotated return type syntax
 *           return@childRet listOf(ioe, aerr)
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
 */
inline fun <H : Any, reified N : H> baseMatchSubtree(
    adapter: TreeLikeAdapter<H>,
    ignoreChildren: Boolean = false,
    noinline nodeSpec: NWrapper<H, N>.() -> Unit
): Matcher<H?> =

    verify {
        it shouldNotBe null
        it!!.executeNodeSpec(adapter, ignoreChildren, nodeSpec)
    }

/**
 * Turns a block of assertions into a [Matcher] that succeeds if executing the [assertions]
 * doesn't throw any [AssertionError]s.
 */
inline fun <T> verify(crossinline assertions: (T) -> Unit): Matcher<T> = object : Matcher<T> {
    override fun test(value: T): Result {

        val matchRes = try {
            Either.Right(assertions(value))
        } catch (e: AssertionError) {
            Either.Left(e)
        }

        val didMatch = matchRes.isRight()

        val failureMessage: String = matchRes.fold({
            // Here the node failed
            it.message ?: "Did not match the assertions (no cause specified)"
        }, {
            // The node matched, which was expected
            "SHOULD NOT BE OUTPUT"
        })

        val negatedMessage = matchRes.fold({
            // the node didn't match, which was expected
            "SHOULD NOT BE OUTPUT"
        }, {
            "Should have matched the assertions"
        })

        return Result(didMatch, failureMessage, negatedMessage)
    }
}
