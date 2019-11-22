@file:JvmName("MyExample")

package astminer.examples.myExample

import astminer.common.model.LabeledPathContexts
import astminer.common.getNormalizedToken
import astminer.common.model.*
import astminer.common.preOrder
import astminer.common.setNormalizedToken
import astminer.common.splitToSubtokens
import astminer.parse.antlr.java.JavaMethodSplitter
import astminer.parse.antlr.java.JavaParser
import astminer.parse.antlr.python.PythonMethodSplitter
import astminer.parse.antlr.python.PythonParser
import astminer.parse.cpp.FuzzyCppParser
import astminer.parse.cpp.FuzzyMethodSplitter
import astminer.paths.Code2VecPathStorage
import astminer.paths.PathMiner
import astminer.paths.PathRetrievalSettings
import astminer.paths.toPathContext
import java.io.File
import astminer.parse.antlr.decompressTypeLabel

// Retrieve paths from two Java projects for further usage in python example.
fun processMyExampleData() {
    val maxPathContexts = 500

    val inputDir = "/analysis/inputs/public/source-code"

    val miner = PathMiner(PathRetrievalSettings(8, 3))

    val storage1 = Code2VecPathStorage("/analysis/output/fs/ast-paths/java")
    File(inputDir).walkTopDown().filter { it.isFile() && it.path.endsWith(".java") }.forEach { file ->
        val node = JavaParser().parse(file.inputStream()) ?: return@forEach


        val methods = JavaMethodSplitter().splitIntoMethods(node)

        methods.forEach { methodInfo ->
            val methodNameNode = methodInfo.method.nameNode ?: return@forEach
            val methodRoot = methodInfo.method.root
            val label = splitToSubtokens(methodNameNode.getToken()).joinToString("|")
            methodRoot.preOrder().forEach { it.setNormalizedToken() }
            methodNameNode.setNormalizedToken("METHOD_NAME")

            // Retrieve paths from every node individually
            val paths = miner.retrievePaths(methodRoot).take(maxPathContexts)
            storage1.store(LabeledPathContexts(label, paths.map {
                toPathContext(it) { node ->
                    node.getNormalizedToken()
                }
            }))
        }
    }

    storage1.save()

    val storage2 = Code2VecPathStorage("/analysis/output/fs/ast-paths/python")
    File(inputDir).walkTopDown().filter { it.isFile() && it.path.endsWith(".py") }.forEach { file ->
        val node = PythonParser().parse(file.inputStream()) ?: return@forEach
        val methods = PythonMethodSplitter().splitIntoMethods(node)

        methods.forEach { methodInfo ->
            val methodNameNode = methodInfo.method.nameNode ?: return@forEach
            val methodRoot = methodInfo.method.root
            val label = splitToSubtokens(methodNameNode.getToken()).joinToString("|")
            methodRoot.preOrder().forEach { it.setNormalizedToken() }
            methodNameNode.setNormalizedToken("METHOD_NAME")

            // Retrieve paths from every node individually
            val paths = miner.retrievePaths(methodRoot).take(maxPathContexts)
            storage2.store(LabeledPathContexts(label, paths.map {
                toPathContext(it) { node ->
                    node.getNormalizedToken()
                }
            }))
        }
    }

    storage2.save()

    val storage3 = Code2VecPathStorage("/analysis/output/fs/ast-paths/c-and-cpp")
    File(inputDir).walkTopDown().filter { 
        it.isFile() && (
            it.path.endsWith(".cpp") || 
            it.path.endsWith(".c") || 
            it.path.endsWith(".cc") || 
            it.path.endsWith(".cxx") || 
            it.path.endsWith(".h") || 
            it.path.endsWith(".hpp")
        ) 
    }.forEach { file ->
        val node = FuzzyCppParser().parse(file.inputStream()) ?: return@forEach
        val methods = FuzzyMethodSplitter().splitIntoMethods(node)

        methods.forEach { methodInfo ->
            val methodNameNode = methodInfo.method.nameNode ?: return@forEach
            val methodRoot = methodInfo.method.root
            val label = splitToSubtokens(methodNameNode.getToken()).joinToString("|")
            methodRoot.preOrder().forEach { it.setNormalizedToken() }
            methodNameNode.setNormalizedToken("METHOD_NAME")

            // Retrieve paths from every node individually
            val paths = miner.retrievePaths(methodRoot).take(maxPathContexts)
            storage3.store(LabeledPathContexts(label, paths.map {
                toPathContext(it) { node ->
                    node.getNormalizedToken()
                }
            }))
        }
    }

    storage3.save()
}

fun main(args: Array<String>) {
    processMyExampleData()
}
