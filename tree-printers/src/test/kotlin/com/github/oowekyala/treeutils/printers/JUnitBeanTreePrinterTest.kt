package com.github.oowekyala.treeutils.printers

import io.kotlintest.shouldBe
import io.kotlintest.specs.FunSpec
import net.sourceforge.pmd.lang.ast.Node
import java.beans.PropertyDescriptor

/**
 * @author Clément Fournier
 * @since 1.0
 */
class JUnitBeanTreePrinterTest : FunSpec({

    test("The filter should control which properties are dumped") {
        val dumper = object : JUnitBeanTreePrinter<Node>(NodeTreeLikeAdapter) {

            override fun takePropertyDescriptorIf(node: Node, prop: PropertyDescriptor): Boolean = true
        }

        parseStatement("int i = 0;").let {
            dumper.dumpSubtree(it, 1)
        } shouldBe """
            node<ASTLocalVariableDeclaration> {
                assertEquals("LocalVariableDeclaration", it.XPathNodeName)
                assertFalse(it.isAbstract)
                assertFalse(it.isArray)
                assertEquals(0, it.arrayDepth)
                assertEquals(6, it.beginColumn)
                assertEquals(3, it.beginLine)
                assertEquals(net.sourceforge.pmd.lang.java.ast.ASTLocalVariableDeclaration::class.java, it.class)
                assertNull(it.dataFlowNode)
                assertFalse(it.isDefault)
                assertEquals(14, it.endColumn)
                assertEquals(3, it.endLine)
                assertFalse(it.isFinal)
                assertFalse(it.isFindBoundary)
                assertNull(it.image)
                assertEquals(0, it.modifiers)
                assertFalse(it.isNative)
                assertTrue(it.isPackagePrivate)
                assertFalse(it.isPrivate)
                assertFalse(it.isProtected)
                assertFalse(it.isPublic)
                assertTrue(it.isSingleLine)
                assertFalse(it.isStatic)
                assertFalse(it.isStrictfp)
                assertFalse(it.isSynchronized)
                assertFalse(it.isTransient)
                assertFalse(it.isTypeInferred)
                assertNull(it.userData)
                assertEquals("i", it.variableName)
                assertFalse(it.isVolatile)

                val child0 = child<ASTType>(ignoreChildren = true) {
                    assertEquals("Type", it.XPathNodeName)
                    assertFalse(it.isArray)
                    assertEquals(0, it.arrayDepth)
                    assertEquals(6, it.beginColumn)
                    assertEquals(3, it.beginLine)
                    assertEquals(net.sourceforge.pmd.lang.java.ast.ASTType::class.java, it.class)
                    assertNull(it.dataFlowNode)
                    assertEquals(8, it.endColumn)
                    assertEquals(3, it.endLine)
                    assertFalse(it.isFindBoundary)
                    assertNull(it.image)
                    assertTrue(it.isSingleLine)
                    assertEquals(int::class.java, it.type)
                    assertEquals("int", it.typeImage)
                    assertNull(it.userData)
                }
                assertEquals(child0, it.typeNode)
                child<ASTVariableDeclarator>(ignoreChildren = true) {
                    assertEquals("VariableDeclarator", it.XPathNodeName)
                    assertEquals(10, it.beginColumn)
                    assertEquals(3, it.beginLine)
                    assertEquals(net.sourceforge.pmd.lang.java.ast.ASTVariableDeclarator::class.java, it.class)
                    assertNull(it.dataFlowNode)
                    assertEquals(14, it.endColumn)
                    assertEquals(3, it.endLine)
                    assertFalse(it.isFindBoundary)
                    assertNull(it.image)
                    assertTrue(it.isSingleLine)
                    assertEquals(int::class.java, it.type)
                    assertNull(it.userData)
                }
            }
        """.trimIndent()
    }


    test("The default printer should print only properties declared on the class") {
        val dumper = JUnitBeanTreePrinter(NodeTreeLikeAdapter)

        parseStatement("int i = 0;").let {
            dumper.dumpSubtree(it, 1)
        } shouldBe """
            node<ASTLocalVariableDeclaration> {
                assertFalse(it.isArray)
                assertEquals(0, it.arrayDepth)
                assertFalse(it.isTypeInferred)
                assertEquals("i", it.variableName)

                val child0 = child<ASTType>(ignoreChildren = true) {
                    assertFalse(it.isArray)
                    assertEquals(0, it.arrayDepth)
                    assertEquals("int", it.typeImage)
                }
                assertEquals(child0, it.typeNode)
                child<ASTVariableDeclarator>(ignoreChildren = true) {}
            }
        """.trimIndent()
    }

})