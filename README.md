# Kotlin Tree Matchers

This is a lightweight testing DSL to assert that a tree conforms to an
expected structure. Its focus is to maximise readability and provide
detailed error messages. It supports any tree-like structure, you just have
to plug in an adapter.

Jump to the [Setup](#setup) section or take a look at the samples below.


## Samples


```kotlin

node should matchNode<ASTStatement> {

    // nesting matchers allow to specify a whole subtree
    child<ASTForStatement> {

        // This would fail if the first child of the ForStatement wasn't a ForInit
        child<ASTForInit> {
            child<ASTLocalVariableDeclaration> {

                // If the parameter ignoreChildren is set to true, the number of children is not asserted
                // Calls to "child" in the block are forbidden
                // The only checks carried out here are the type test and the assertions of the block
                child<ASTType>(ignoreChildren = true) {

                    // In a "child" block, the tested node can be referred to as "it"
                    // Here, its static type is ASTType, so we can inspect properties
                    // of the node and make assertions

                    it.typeImage shouldBe "int"
                    it.type shouldNotBe null
                }

                // We don't care about that node, we only care that there is "some" node
                unspecifiedChild()
            }
        }

        // The subtree is ignored, but we check a ForUpdate is present at this child position
        child<ASTForUpdate>(ignoreChildren = true) {}

        // Here, ignoreChildren is not specified and takes its default value of false.
        // The lambda has no "child" calls and the node will be asserted to have no children
        child<ASTBlock> {}
    }
}

```


See also the docs.

## Setup



### Maven

TODO

### Gradle

TODO

### Provide an adapter

For example, if your tree type hierarchy is topped by a class named `Node`,
you should:

* Implement `TreeLikeAdapter<Node>` on some object
    * Define some shorthand methods, to avoid providing the adapter every time

Here it is in code:

```kotlin

object NodeTreeLikeAdapter : TreeLikeAdapter<Node> {
    override fun getChildren(node: Node): List<Node> = /* implementation */
}


typealias NodeSpec<N> = TreeNodeWrapper<Node, N>.() -> Unit

// This can be used with plain kotlin.test : someNode.shouldMatchNode<Foo> { ... }
inline fun <reified N : Node> Node?.shouldMatchNode(
                                        ignoreChildren: Boolean = false,
                                        noinline nodeSpec: NodeSpec<N>
                                    ) = this.baseShouldMatchSubtree(MatchingConfig(adapter = NodeTreeLikeAdapter), ignoreChildren, nodeSpec = nodeSpec)

// This can be used with kotlintest's "someNode should matchNode<Foo> { ... }"
inline fun <reified N : Node> matchNode(
                                    ignoreChildren: Boolean = false,
                                    noinline nodeSpec: NodeSpec<N>
                              ) : (Node?) -> Unit = { it.shouldMatchNode(ignoreChildren, nodeSpec) }

```


Happy testing!