package com.github.oowekyala.treematchers

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


object NodeTreeLikeAdapter : TreeLikeAdapter<Node> {
    override fun getChildren(node: Node): List<Node> = node.findChildrenOfType(Node::class.java)

    override fun nodeName(type: Class<out Node>): String = type.simpleName.removePrefix("AST")
}


inline fun <reified N : Node> matchNode(
    ignoreChildren: Boolean = false,
    noinline nodeSpec: TreeNodeWrapper<Node, N>.() -> Unit
): (Node?) -> Unit = { it.baseShouldMatchSubtree(NodeTreeLikeAdapter, ignoreChildren, nodeSpec) }


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

