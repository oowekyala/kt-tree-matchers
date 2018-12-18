package com.github.oowekyala.treematchers

import io.kotlintest.shouldBe
import io.kotlintest.specs.FunSpec
import net.sourceforge.pmd.lang.ast.Node
import net.sourceforge.pmd.lang.java.ast.ASTVariableDeclarator
import net.sourceforge.pmd.lang.java.ast.ASTVariableDeclaratorId

/**
 * @author Clément Fournier
 * @since 1.0
 */
class DumperTest : FunSpec({

    test("Default dumper should dump the whole tree structure") {
        val dumper = TreeDumper(NodeTreeLikeAdapter)

        parseStatement("int i = 0;").let {
            dumper.dumpSubtree(it)
        } shouldBe """
            matchNode<ASTLocalVariableDeclaration> {
                child<ASTType> {
                    child<ASTPrimitiveType> {}
                }
                child<ASTVariableDeclarator> {
                    child<ASTVariableDeclaratorId> {}
                    child<ASTVariableInitializer> {
                        child<ASTExpression> {
                            child<ASTPrimaryExpression> {
                                child<ASTPrimaryPrefix> {
                                    child<ASTLiteral> {}
                                }
                            }
                        }
                    }
                }
            }
        """.trimIndent()
    }

    test("A dumper should ignore children past the max dump depth") {
        val dumper = TreeDumper(NodeTreeLikeAdapter)

        parseStatement("int i = 0;").let {
            dumper.dumpSubtree(it, 3)
        } shouldBe """
            matchNode<ASTLocalVariableDeclaration> {
                child<ASTType> {
                    child<ASTPrimitiveType>(ignoreChildren = true) {}
                }
                child<ASTVariableDeclarator> {
                    child<ASTVariableDeclaratorId>(ignoreChildren = true) {}
                    child<ASTVariableInitializer>(ignoreChildren = true) {}
                }
            }
        """.trimIndent()
    }

    test("Dumping with max dump depth 0 should only dump the root") {
        val dumper = TreeDumper(NodeTreeLikeAdapter)

        parseStatement("int i = 0;").let {
            dumper.dumpSubtree(it, 0)
        } shouldBe """
            matchNode<ASTLocalVariableDeclaration>(ignoreChildren = true) {
            }
        """.trimIndent()
    }


    test("Child assertions should be prepended to their corresponding child call") {

        val dumper = object : TreeDumper<Node>(NodeTreeLikeAdapter) {

            override fun getChildAssertions(node: Node): Map<Int, String> =
                    if (node is ASTVariableDeclarator) {
                        mapOf(0 to "it.id shouldBe ",
                                1 to "it.initializer shouldBe ")
                    } else emptyMap()
        }

        parseStatement("int i = 0;").let {
            dumper.dumpSubtree(it)
        } shouldBe """
            matchNode<ASTLocalVariableDeclaration> {
                child<ASTType> {
                    child<ASTPrimitiveType> {}
                }
                child<ASTVariableDeclarator> {

                    it.id shouldBe child<ASTVariableDeclaratorId> {}

                    it.initializer shouldBe child<ASTVariableInitializer> {
                        child<ASTExpression> {
                            child<ASTPrimaryExpression> {
                                child<ASTPrimaryPrefix> {
                                    child<ASTLiteral> {}
                                }
                            }
                        }
                    }
                }
            }
        """.trimIndent()
    }



    test("Additional assertions should be added before child calls") {

        val dumper = object : TreeDumper<Node>(NodeTreeLikeAdapter) {

            override fun getChildAssertions(node: Node): Map<Int, String> =
                    when (node) {
                        is ASTVariableDeclarator -> mapOf(0 to "it.id shouldBe ",
                                1 to "it.initializer shouldBe ")
                        else -> emptyMap()
                    }

            override fun getAdditionalAssertions(node: Node): List<String> =
                    when (node) {
                        is ASTVariableDeclaratorId -> listOf("it.image shouldBe \"${node.image}\"")
                        else -> emptyList()
                    }
        }

        parseStatement("int i = 0;").let {
            dumper.dumpSubtree(it)
        } shouldBe """
            matchNode<ASTLocalVariableDeclaration> {
                child<ASTType> {
                    child<ASTPrimitiveType> {}
                }
                child<ASTVariableDeclarator> {

                    it.id shouldBe child<ASTVariableDeclaratorId> {
                        it.image shouldBe "i"
                    }

                    it.initializer shouldBe child<ASTVariableInitializer> {
                        child<ASTExpression> {
                            child<ASTPrimaryExpression> {
                                child<ASTPrimaryPrefix> {
                                    child<ASTLiteral> {}
                                }
                            }
                        }
                    }
                }
            }
        """.trimIndent()
    }

})