package org.javacs.ktda

/**
 * Tests the basic debugging scenarios
 * using a sample Gradle application.
 * In addition to any application specific scenarios
 */
class SampleWorkspaceTest :
    GlobalWorkspaceTest(
        relativeWorkspaceRoot = "sample-workspace",
        mainClass = "sample.workspace.AppKt",
        defaultVmArguments = "-Dtest=testVmArgs",
        buildCommand = Pair("./gradlew", "build")
    )
