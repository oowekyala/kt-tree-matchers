package com.github.oowekyala.treeutils.printers

import io.kotlintest.shouldBe
import io.kotlintest.specs.FunSpec
import net.sourceforge.pmd.lang.ast.Node
import net.sourceforge.pmd.lang.java.ast.ASTPrimitiveType
import net.sourceforge.pmd.lang.java.ast.ASTType
import net.sourceforge.pmd.lang.java.ast.ASTVariableDeclarator
import net.sourceforge.pmd.lang.java.ast.ASTVariableDeclaratorId

/**
 * @author Clément Fournier
 * @since 1.0
 */
class DslPrinterTest : FunSpec({

    test("Default dumper should dump the whole tree structure") {
        val dumper = DslStructurePrinter(NodeTreeLikeAdapter)

        parseStatement("int i = 0;").let {
            dumper.dumpSubtree(it)
        } shouldBe """
            node<ASTLocalVariableDeclaration> {
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
        val dumper = DslStructurePrinter(NodeTreeLikeAdapter)

        parseStatement("int i = 0;").let {
            dumper.dumpSubtree(it, 3)
        } shouldBe """
            node<ASTLocalVariableDeclaration> {
                child<ASTType> {
                    child<ASTPrimitiveType> {}
                }
                child<ASTVariableDeclarator> {
                    child<ASTVariableDeclaratorId> {}
                    child<ASTVariableInitializer> {
                        child<ASTExpression>(ignoreChildren = true) {}
                    }
                }
            }
        """.trimIndent()
    }

    test("Dumping with max dump depth 0 should only dump the root") {
        val dumper = DslStructurePrinter(NodeTreeLikeAdapter)

        parseStatement("int i = 0;").let {
            dumper.dumpSubtree(it, 0)
        } shouldBe """
            node<ASTLocalVariableDeclaration>(ignoreChildren = true) {}
        """.trimIndent()
    }

    test("Dumping with max dump depth 1 should dump the root and its children") {
        val dumper = DslStructurePrinter(NodeTreeLikeAdapter)

        parseStatement("int i = 0;").let {
            dumper.dumpSubtree(it, 1)
        } shouldBe """
            node<ASTLocalVariableDeclaration> {
                child<ASTType>(ignoreChildren = true) {}
                child<ASTVariableDeclarator>(ignoreChildren = true) {}
            }
        """.trimIndent()
    }



    test("Child assertions should surround their corresponding child call") {

        val dumper = object : DslStructurePrinter<Node>(NodeTreeLikeAdapter) {

            override fun getChildCallContexts(parent: Node): Map<Int, Pair<String, String>> =
                    when (parent) {
                        is ASTVariableDeclarator -> mapOf(
                                0 to Pair("it.id shouldBe ", ""),
                                1 to Pair("it.initializer shouldBe ", "")
                        )
                        else                     -> emptyMap()
                    }
        }

        parseStatement("int i = 0;").let {
            dumper.dumpSubtree(it)
        } shouldBe """
            node<ASTLocalVariableDeclaration> {
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

        val dumper = object : DslStructurePrinter<Node>(NodeTreeLikeAdapter) {

            override fun getChildCallContexts(parent: Node): Map<Int, Pair<String, String>> =
                    when (parent) {
                        is ASTVariableDeclarator -> mapOf(
                                0 to Pair("it.id shouldBe ", ""),
                                1 to Pair("it.initializer shouldBe ", "")
                        )
                        else                     -> emptyMap()
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
            node<ASTLocalVariableDeclaration> {
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


    test("Custom child assertion should override child call context and replace child call") {

        val dumper = object : DslStructurePrinter<Node>(NodeTreeLikeAdapter) {

            override fun getCustomChildAssertion(parent: Node, child: Node): String? {
                return when (child) {
                    is ASTPrimitiveType -> "primitive(${child.image})"
                    else                -> null
                }
            }

            override fun getChildCallContexts(parent: Node): Map<Int, Pair<String, String>> =
                    when (parent) {
                        is ASTType -> mapOf(
                                0 to Pair("it.id shouldBe ", "")
                        )
                        else                     -> emptyMap()
                    }

            override fun getAdditionalAssertions(node: Node): List<String> =
                    when (node) {
                        is ASTPrimitiveType -> listOf("it.image shouldBe \"${node.image}\"")
                        is ASTType -> listOf("it.myFu shouldBe \"${node.image}\"")
                        else                       -> emptyList()
                    }
        }

        parseStatement("int i = 0;").let {
            dumper.dumpSubtree(it)
        } shouldBe """
            node<ASTLocalVariableDeclaration> {
                child<ASTType> {
                    it.myFu shouldBe "null"

                    primitive(int)
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

})