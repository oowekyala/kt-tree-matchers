package com.github.oowekyala.treeutils.matchers

import com.github.oowekyala.treeutils.DoublyLinkedTreeLikeAdapter
import com.github.oowekyala.treeutils.printers.DslStructurePrinter
import com.github.oowekyala.treeutils.printers.TreePrinter
import net.sourceforge.pmd.lang.LanguageRegistry
import net.sourceforge.pmd.lang.ast.Node
import net.sourceforge.pmd.lang.java.JavaLanguageModule
import net.sourceforge.pmd.lang.java.ast.ASTBlockStatement
import net.sourceforge.pmd.lang.java.ast.ASTCompilationUnit
import java.io.StringReader

fun parseStatement(statement: String): Node {

    // place the param in a statement parsing context
    val source = """
            class Foo {
               {
                 $statement
               }
            }
        """.trimIndent()

    val root = parseCompilationUnit(source)

    return root.getFirstDescendantOfType(ASTBlockStatement::class.java).jjtGetChild(0)
}

object NodeTreeLikeAdapter : DoublyLinkedTreeLikeAdapter<Node> {
    override fun getChildren(node: Node): List<Node> = node.findChildrenOfType(Node::class.java)

    override fun nodeName(type: Class<out Node>): String = type.simpleName.removePrefix("AST")

    override fun getParent(node: Node): Node? = node.jjtGetParent()
}

inline fun <reified N : Node> matchNode(
        errorPrinter: TreePrinter<Node>? = DslStructurePrinter(NodeTreeLikeAdapter),
        ignoreChildren: Boolean = false,
        noinline implicitAssertions: (Node) -> Unit = {},
        noinline nodeSpec: TreeNodeWrapper<Node, N>.() -> Unit
): (Node?) -> Unit = {
    val myConfig = MatchingConfig(
            adapter = NodeTreeLikeAdapter,
            errorPrinter = errorPrinter,
            implicitAssertions = implicitAssertions
    )
    it.baseShouldMatchSubtree(myConfig, ignoreChildren, nodeSpec)
}

fun parseCompilationUnit(sourceCode: String): ASTCompilationUnit {

    val languageVersionHandler =
            LanguageRegistry.getLanguage(JavaLanguageModule.NAME).defaultVersion.languageVersionHandler
    val rootNode = languageVersionHandler.getParser(languageVersionHandler.defaultParserOptions)
            .parse(":test:", StringReader(sourceCode))
    languageVersionHandler.getQualifiedNameResolutionFacade(ClassLoader.getSystemClassLoader()).start(rootNode)
    languageVersionHandler.symbolFacade.start(rootNode)
    languageVersionHandler.dataFlowFacade.start(rootNode)
    languageVersionHandler.getTypeResolutionFacade(ClassLoader.getSystemClassLoader()).start(rootNode)
    languageVersionHandler.multifileFacade.start(rootNode)
    return rootNode as ASTCompilationUnit
}

