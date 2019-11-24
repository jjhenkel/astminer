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
import astminer.parse.antlr.decompressTypeLabel

import com.beust.klaxon.Parser as KParser
import com.beust.klaxon.JsonObject
import java.io.File
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;
import java.util.zip.GZIPInputStream;

// Retrieve paths from two Java projects for further usage in python example.
fun processMyExampleData(inputPath: String) {
    val maxPathContexts = 500

    val miner = PathMiner(PathRetrievalSettings(8, 3))

    val greader = BufferedReader(
        InputStreamReader(
            GZIPInputStream(
                FileInputStream(inputPath))));

    val storage1 = Code2VecPathStorage("/tmp/java")
    val storage2 = Code2VecPathStorage("/tmp/python")
    
    greader.forEachLine { content -> 
        val source = KParser.default().parse(StringBuilder(content)) as JsonObject

        if (source.string("language") == "java") {

            val node = JavaParser().parse(ByteArrayInputStream((source.string("source_code") ?: "").toByteArray(Charsets.UTF_8))) ?: return@forEachLine
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

        else if (source.string("language") == "python") {

            val node = PythonParser().parse(ByteArrayInputStream((source.string("source_code") ?: "").toByteArray(Charsets.UTF_8))) ?: return@forEachLine
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
    }

    storage1.save()
    storage2.save()
}

fun main(args: Array<String>) {
    if (args.size > 0) {
        processMyExampleData(args[0])
    }
}
