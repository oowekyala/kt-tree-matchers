package com.github.oowekyala.treeutils.printers

import io.kotlintest.shouldBe
import io.kotlintest.specs.FunSpec
import net.sourceforge.pmd.lang.ast.Node
import java.beans.PropertyDescriptor

/**
 * @author Clément Fournier
 * @since 1.0
 */
class KotlintestBeanTreePrinterTest : FunSpec({

    test("The filter should control which properties are dumped") {
        val dumper = object : KotlintestBeanTreePrinter<Node>(NodeTreeLikeAdapter) {
            override fun takePropertyDescriptorIf(node: Node, prop: PropertyDescriptor): Boolean = true
        }

        parseStatement("int i = 0;").let {
            dumper.dumpSubtree(it, 1)
        } shouldBe """
            node<ASTLocalVariableDeclaration> {
                it.XPathNodeName shouldBe "LocalVariableDeclaration"
                it.isAbstract shouldBe false
                it.isArray shouldBe false
                it.arrayDepth shouldBe 0
                it.beginColumn shouldBe 6
                it.beginLine shouldBe 3
                it.class shouldBe net.sourceforge.pmd.lang.java.ast.ASTLocalVariableDeclaration::class.java
                it.dataFlowNode shouldBe null
                it.isDefault shouldBe false
                it.endColumn shouldBe 14
                it.endLine shouldBe 3
                it.isFinal shouldBe false
                it.isFindBoundary shouldBe false
                it.image shouldBe null
                it.modifiers shouldBe 0
                it.isNative shouldBe false
                it.isPackagePrivate shouldBe true
                it.isPrivate shouldBe false
                it.isProtected shouldBe false
                it.isPublic shouldBe false
                it.isSingleLine shouldBe true
                it.isStatic shouldBe false
                it.isStrictfp shouldBe false
                it.isSynchronized shouldBe false
                it.isTransient shouldBe false
                it.isTypeInferred shouldBe false
                it.userData shouldBe null
                it.variableName shouldBe "i"
                it.isVolatile shouldBe false

                it.typeNode shouldBe child<ASTType>(ignoreChildren = true) {
                    it.XPathNodeName shouldBe "Type"
                    it.isArray shouldBe false
                    it.arrayDepth shouldBe 0
                    it.beginColumn shouldBe 6
                    it.beginLine shouldBe 3
                    it.class shouldBe net.sourceforge.pmd.lang.java.ast.ASTType::class.java
                    it.dataFlowNode shouldBe null
                    it.endColumn shouldBe 8
                    it.endLine shouldBe 3
                    it.isFindBoundary shouldBe false
                    it.image shouldBe null
                    it.isSingleLine shouldBe true
                    it.type shouldBe int::class.java
                    it.typeImage shouldBe "int"
                    it.userData shouldBe null
                }
                child<ASTVariableDeclarator>(ignoreChildren = true) {
                    it.XPathNodeName shouldBe "VariableDeclarator"
                    it.beginColumn shouldBe 10
                    it.beginLine shouldBe 3
                    it.class shouldBe net.sourceforge.pmd.lang.java.ast.ASTVariableDeclarator::class.java
                    it.dataFlowNode shouldBe null
                    it.endColumn shouldBe 14
                    it.endLine shouldBe 3
                    it.isFindBoundary shouldBe false
                    it.image shouldBe null
                    it.isSingleLine shouldBe true
                    it.type shouldBe int::class.java
                    it.userData shouldBe null
                }
            }
        """.trimIndent()
    }


    test("The default printer should print only properties declared on the class") {
        val dumper = KotlintestBeanTreePrinter(NodeTreeLikeAdapter)

        parseStatement("int i = 0;").let {
            dumper.dumpSubtree(it, 1)
        } shouldBe """
            node<ASTLocalVariableDeclaration> {
                it.isArray shouldBe false
                it.arrayDepth shouldBe 0
                it.isTypeInferred shouldBe false
                it.variableName shouldBe "i"

                it.typeNode shouldBe child<ASTType>(ignoreChildren = true) {
                    it.isArray shouldBe false
                    it.arrayDepth shouldBe 0
                    it.typeImage shouldBe "int"
                }
                child<ASTVariableDeclarator>(ignoreChildren = true) {}
            }
        """.trimIndent()
    }

})