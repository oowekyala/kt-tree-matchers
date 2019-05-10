package com.github.oowekyala.treeutils.printers

import io.kotlintest.shouldBe
import io.kotlintest.specs.FunSpec

/**
 * @author Clément Fournier
 * @since 1.0
 */
class SimplePrinterTestOtherStrConfig : FunSpec({

    val dumper = SimpleTreePrinter(NodeTreeLikeAdapter, SimpleTreePrinter.UnicodeStrings)


    test("Default dumper should dump the whole tree structure") {

        parseStatement("int i = 0;").let {
            dumper.dumpSubtree(it)
        } shouldBe """
└── LocalVariableDeclaration
    ├── Type
    │   └── PrimitiveType
    └── VariableDeclarator
        ├── VariableDeclaratorId
        └── VariableInitializer
            └── Expression
                └── PrimaryExpression
                    └── PrimaryPrefix
                        └── Literal

        """.trimIndent()
    }

    test("A dumper should ignore children past the max dump depth") {


        parseStatement("int i = 0;").let {
            dumper.dumpSubtree(it, 1)
        } shouldBe """
└── LocalVariableDeclaration
    ├── Type
    │   └── 1 child is not shown
    └── VariableDeclarator
        └── 2 children are not shown

        """.trimIndent()
    }

    test("Dumping with max dump depth 0 should only dump the root") {

        parseStatement("int i = 0;").let {
            dumper.dumpSubtree(it, 0)
        } shouldBe """
      └── LocalVariableDeclaration
          └── 2 children are not shown

        """.trimIndent()
    }

    test("Dumping with max dump depth 1 should dump the root and its children") {

        parseStatement("int i = 0;").let {
            dumper.dumpSubtree(it, 1)
        } shouldBe """
            └── LocalVariableDeclaration
                ├── Type
                │   └── 1 child is not shown
                └── VariableDeclarator
                    └── 2 children are not shown

        """.trimIndent()
    }


})
