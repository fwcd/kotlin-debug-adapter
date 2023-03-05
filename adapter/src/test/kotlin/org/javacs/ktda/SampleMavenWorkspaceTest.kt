package org.javacs.ktda

/**
 * Tests the basic debugging scenarios
 * using a sample maven application.
 * In addition to any application specific scenarios
 */
class SampleMavenWorkspaceTest : GlobalWorkspaceTest(
    relativeWorkspaceRoot = "sample-workspace-maven",
    mainClass = "sample.workspace.AppKt",
    defaultVmArguments = "-Dtest=testVmArgs",
    buildCommand = Pair("./mvnw", "test-compile")
)