package com.github.oowekyala.treeutils.matchers

import io.kotlintest.should
import io.kotlintest.shouldBe
import io.kotlintest.shouldThrow
import io.kotlintest.specs.FunSpec
import net.sourceforge.pmd.lang.ast.Node
import net.sourceforge.pmd.lang.java.ast.*

class DslTest : FunSpec({

    failureTest("Empty matcher spec should check the number of children",
            messageContains = setOf("Wrong", "number", "children", "expected 0", "actual 2")) {

        parseStatement("int i = 0;") should matchNode<ASTLocalVariableDeclaration> {}
    }

    test("Matcher with ignoreChildren should not check the number of children") {

        parseStatement("int i = 0;") should matchNode<ASTLocalVariableDeclaration>(ignoreChildren = true) {}
    }

    failureTest("Incorrect node type should cause failure",
            messageContains = setOf("Expression", "actual LocalVariableDeclaration")) {
        parseStatement("int i = 0;") should matchNode<ASTExpression>(ignoreChildren = true) {}
    }

    failureTest("Specifying any child in a pattern should cause the number of children to be checked",
            messageContains = setOf("number", "children", "expected 1", "actual 2")) {

        parseStatement("int i = 0;") should matchNode<ASTLocalVariableDeclaration> {
            child<ASTType>(ignoreChildren = true) {}
            // There's a VarDeclarator
        }
    }


    test("Unspecified children should shift the next child matchers") {
        parseStatement("int i = 0;") should matchNode<ASTLocalVariableDeclaration> {
            unspecifiedChild()
            child<ASTVariableDeclarator>(ignoreChildren = true) {}
        }
    }

    test("Unspecified children should count in total number of children") {
        parseStatement("int i = 0;") should matchNode<ASTLocalVariableDeclaration> {
            unspecifiedChildren(2)
        }
    }

    test("fromChild should return correctly") {
        parseStatement("int i = 0;") should matchNode<ASTLocalVariableDeclaration> {

            unspecifiedChild()

            val name = fromChild<ASTVariableDeclarator, String> {
                val myName = fromChild<ASTVariableDeclaratorId, String> {

                    it.image
                }

                unspecifiedChild()

                myName
            }

            name shouldBe "i"

        }
    }

    failureTest("Unspecified children should be counted in the number of expected children",
            messageContains = setOf("number of children", "expected 3", "actual 2")) {

        parseStatement("int i = 0;") should matchNode<ASTLocalVariableDeclaration> {
            unspecifiedChildren(3)
        }
    }

    failureTest("Assertions are always executed in order",
            messageContains = setOf("PrimitiveType")) {

        parseStatement("int[] i = 0;") should matchNode<ASTLocalVariableDeclaration> {

            child<ASTType> {

                // Here we check that the child type check fails before the assertion
                child<ASTPrimitiveType> {}

                it.typeImage shouldBe "bratwurst"

            }

            unspecifiedChild()
        }
    }

    failureTest("Assertions are always executed in order #2",
            messageContains = setOf("bratwurst")) {

        parseStatement("int[] i = 0;") should matchNode<ASTLocalVariableDeclaration> {

            child<ASTType> {

                it.typeImage shouldBe "bratwurst"

                child<ASTPrimitiveType> {}

            }

            unspecifiedChild()
        }
    }

    failureTest("All assertions should have a node path",
            messageContains = setOf("At /LocalVariableDeclaration/Type:", "expected: \"bratwurst\"")) {

        parseStatement("int[] i = 0;") should matchNode<ASTLocalVariableDeclaration> {

            child<ASTType> {

                // this fails
                it.typeImage shouldBe "bratwurst"

            }

            unspecifiedChild()
        }
    }

    failureTest("An assertion should be emitted for the parent",
            messageContains = setOf("expected", "to have parent")) {

        parseStatement("int[] i = 0;") should matchNode<ASTLocalVariableDeclaration> {

            it.jjtGetChild(0).jjtSetParent(null) // break the parent

            child<ASTType> {
                // this fails
                unspecifiedChild()
            }

            unspecifiedChild()
        }
    }

    test("Implicit assertions should be executed for each node") {

        var invocCount = 0

        val implicitAssertions: (Node) -> Unit = {
            invocCount++
        }

        parseStatement("int[] i = 0;") should matchNode<ASTLocalVariableDeclaration>(implicitAssertions = implicitAssertions) {
            // here

            child<ASTType> {
                // and here
                unspecifiedChild()
            }

            unspecifiedChild()
        }

        invocCount shouldBe 2
    }

    failureTest("Child assertions should have a node path",
            messageContains = setOf("At /LocalVariableDeclaration/Type:", "expected", "type", "LambdaExpression")) {

        parseStatement("int[] i = 0;") should matchNode<ASTLocalVariableDeclaration> {

            child<ASTType> {

                // this fails
                child<ASTLambdaExpression> { }
            }

            unspecifiedChild()
        }
    }

    failureTest("Leaf nodes should assert that they have no children",
            messageContains = setOf("number", "children", "expected 0")) {

        parseStatement("int[] i = 0;") should matchNode<ASTLocalVariableDeclaration> {

            child<ASTType> {} // This should fail
            unspecifiedChild()
        }
    }

    test("Error messages should contain a dump of the subtree where the error occurred") {

        val exception = shouldThrow<AssertionError> {
            parseStatement("int[] i = 0;") should matchNode<ASTLocalVariableDeclaration> {

                child<ASTType> {

                    // this fails
                    it.typeImage shouldBe "bratwurst"

                }

                unspecifiedChild()
            }
        }

        exception.message shouldBe """
            At /LocalVariableDeclaration/Type: expected: "bratwurst" but was: "int"

            The error occurred in this subtree:

            node<ASTType> {
                child<ASTReferenceType> {
                    child<ASTPrimitiveType> {}
                }
            }
        """.trimIndent()
    }


    test("Error messages should not contain a subtree dump if the pretty printer is null") {

        val exception = shouldThrow<AssertionError> {
            parseStatement("int[] i = 0;") should matchNode<ASTLocalVariableDeclaration>(errorPrinter = null) {

                child<ASTType> {

                    // this fails
                    it.typeImage shouldBe "bratwurst"

                }

                unspecifiedChild()
            }
        }

        exception.message shouldBe "At /LocalVariableDeclaration/Type: expected: \"bratwurst\" but was: \"int\""
    }

})



