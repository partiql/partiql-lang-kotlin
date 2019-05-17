/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates.  All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 *  You may not use this file except in compliance with the License.
 * A copy of the License is located at:
 *
 *      http://aws.amazon.com/apache2.0/
 *
 *  or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 *  language governing permissions and limitations under the License.
 */

package org.partiql.testframework.testdriver.ast

import com.amazon.ion.*
import org.partiql.testframework.testdriver.error.*


/**
 * Creates a deep clone of an AST, inheritors may override the `rewrite*(...)` methods to effect AST rewriting.
 *
 * Each abstract type in the AST has a public method which (indirectly) dispatches to methods that handle nodes of
 * types that directly inherit from the abstract type.
 *
 * The methods do the actual deep cloning.  Inheritors of [AstRewriter] may override those methods
 * to affect rewrites for specific AST nodes.
 *
 * All of the rewrite* methods are public extension methods.  To use them from outside the
 */
abstract class AstRewriter(val resultStream: ResultStream) {

    open fun rewriteScriptCommand(node: ScriptCommand): ScriptCommand? =
        when (node) {
            is TestCommand              -> rewriteTestCommand(node)
            is BenchmarkCommand         -> rewriteBenchmark(node)
            is Include                  -> {
                resultStream.pushContext("In script included from ${node.location}")
                val clone = rewriteInclude(node)
                resultStream.popContext()
                clone
            }
            is SetDefaultEnvironment    -> rewriteSetDefaultEnvironment(node)
            is SetDefaultCompileOptions -> rewriteSetDefaultCompileOptions(node)
            is SetDefaultSession        -> rewriteSetDefaultSession(node)
            is For                      -> rewriteFor(node)
            is ScriptCommandList        -> rewriteScriptCommandList(node)
        }

    /**
     * This method dispatches to other rewrite methods that handle instances of types
     * that are direct descendants of [Expectation], since it is an abstract class and
     * doesn't otherwise have a rewrite method of its own.
     */
    fun rewriteExpectation(node: Expectation?) =
        node?.let {
            when (it) {
                is SuccessExpectation     -> rewriteSuccessExpectation(it)
                is ErrorExpectation       -> rewriteErrorExpectation(it)
                is VariableRefExpectation -> rewriteVariableRefExpectation(it)
                is CountExpectation       -> rewriteCountExpectation(it)
            }
        }

    /**
     * This method dispatches to other rewrite methods that handle instances of types
     * that are direct descendants of [EnvironmentSpec].
     */
    open fun rewriteEnvironmentSpec(node: EnvironmentSpec): EnvironmentSpec = when (node) {
        is EnvironmentSpecFiles       -> rewriteEnvironmentSpecFiles(node)
        is EnvironmentSpecStruct      -> rewriteEnvironmentSpecStruct(node)
        is EnvironmentSpecVariableRef -> rewriteEnvironmentSpecVariableRef(node)
    }


    /*
     * open rewrite methods for non-abstract AST node types go below this line.
     */

    /** The default implementation simply returns a deep copy of its argument. */
    open fun rewriteTestScript(node: TestScript): TestScript =
        TestScript(node.scriptPath, node.commands.mapNotNull { rewriteScriptCommand(it) })

    /** The default implementation simply returns a deep copy of its argument. */
    open fun rewriteScriptCommandList(node: ScriptCommandList): ScriptCommandList? =
        ScriptCommandList(node.location, node.commands.mapNotNull { rewriteScriptCommand(it) })

    /** The default implementation simply returns a deep copy of its argument. */
    open fun rewriteTestCommand(node: TestCommand): ScriptCommand = TestCommand(
        //NOTE:  location, name, and description shouldn't ever be transformed
        node.location,
        node.name,
        node.description,
        rewriteIonText(node.sql),
        rewriteExpectation(node.expectation),
        node.environmentSpec?.let { rewriteEnvironmentSpec(it) },
        node.alternateCompileOptions?.let { rewriteIonValue(it) },
        node.alternateSession?.let { rewriteIonValue(it) as IonStruct? })

    open fun rewriteBenchmark(node: BenchmarkCommand): ScriptCommand = BenchmarkCommand(
        //NOTE:  location, name, and description shouldn't ever be transformed
        node.location,
        node.name,
        node.description,
        rewriteIonText(node.sql),
        rewriteExpectation(node.expectation),
        node.environmentSpec?.let { rewriteEnvironmentSpec(it) },
        node.alternateCompileOptions?.let { rewriteIonValue(it) },
        node.alternateSession?.let { rewriteIonValue(it) as IonStruct? })


    /** The default implementation simply returns a deep copy of its argument. */
    open fun rewriteFor(node: For): ScriptCommand =
        For(
            node.location,
            node.variableSets.map { VariableSet(it.location, rewriteIonValue(it.struct) as IonStruct) }.toList(),
            node.template?.let { rewriteScriptCommandList(it) }
        )

    /** The default implementation simply returns a deep copy of its argument. */
    open fun rewriteInclude(node: Include): ScriptCommand =
        Include(node.location, rewriteTestScript(node.script))

    /** The default implementation simply returns a deep copy of its argument. */
    open fun rewriteSetDefaultEnvironment(node: SetDefaultEnvironment): ScriptCommand =
        SetDefaultEnvironment(
            node.location,
            rewriteEnvironmentSpec(node.envSpec)
        )

    /** The default implementation simply returns a deep copy of its argument. */
    open fun rewriteSetDefaultCompileOptions(node: SetDefaultCompileOptions): ScriptCommand =
        SetDefaultCompileOptions(node.location, rewriteIonValue(node.options))

    /** The default implementation simply returns a deep copy of its argument. */
    open fun rewriteSetDefaultSession(node: SetDefaultSession): ScriptCommand =
        SetDefaultSession(node.location, rewriteIonValue(node.session) as IonStruct)

    /** The default implementation simply returns a deep copy of its argument. */
    open fun rewriteEnvironmentSpecFiles(node: EnvironmentSpecFiles): EnvironmentSpec =
        EnvironmentSpecFiles(node.location, node.paths.map { rewriteString(it) })

    /** The default implementation simply returns a deep copy of its argument. */
    open fun rewriteEnvironmentSpecStruct(node: EnvironmentSpecStruct): EnvironmentSpec =
        EnvironmentSpecStruct(node.location, rewriteIonValue(node.struct) as IonStruct)

    /** The default implementation simply returns a deep copy of its argument. */
    open fun rewriteEnvironmentSpecVariableRef(node: EnvironmentSpecVariableRef): EnvironmentSpec =
        EnvironmentSpecVariableRef(node.location, node.name)

    /** The default implementation simply returns a deep copy of its argument. */
    open fun rewriteCountExpectation(node: CountExpectation): Expectation =
        CountExpectation(node.location, node.expectedResult)

    /** The default implementation simply returns a deep copy of its argument. */
    open fun rewriteSuccessExpectation(node: SuccessExpectation): Expectation =
        SuccessExpectation(node.location, rewriteIonValue(node.expectedResult))

    /** The default implementation simply returns a deep copy of its argument. */
    open fun rewriteErrorExpectation(node: ErrorExpectation): Expectation =
        ErrorExpectation(
            node.location,
            node.expectedErrorCode?.let { rewriteIonText(it) },
            node.expectedProperties?.let { rewriteIonValue(it) as IonStruct? }
        )

    /** The default implementation simply returns a deep copy of its argument. */
    open fun rewriteVariableRefExpectation(node: VariableRefExpectation): Expectation =
        VariableRefExpectation(node.location, node.variableName, node.type)

    /** The default implementation simply returns a deep copy of its argument. */
    open fun rewriteIonValue(ionValue: IonValue) = ionValue.clone()

    /** The default implementation simply returns a deep copy of its argument. */
    open fun rewriteIonText(ionText: IonText) =
        ionText.clone()

    /** The default implementation simply returns its argument. */
    open fun rewriteString(s: String) = s
}