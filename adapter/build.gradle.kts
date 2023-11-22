plugins {
    kotlin("jvm")
    id("maven-publish")
    id("application")
    id("com.jaredsburrows.license")
}

val debugPort = 8000
val debugArgs = "-agentlib:jdwp=transport=dt_socket,server=y,address=8000,suspend=n,quiet=y"

val adapterMainClassName = "org.javacs.ktda.KDAMainKt"

application {
    mainClass.set(adapterMainClassName)
    description = "Debug Adapter for Kotlin"
    applicationDefaultJvmArgs = listOf("-DkotlinLanguageServer.version=$version")
    applicationDistribution.into("bin") {
        fileMode = 755
    }
}

dependencies {
	// The JSON-RPC and Debug Adapter Protocol implementations
	implementation("org.eclipse.lsp4j:org.eclipse.lsp4j.debug:0.15.0")
	implementation("org.jetbrains.kotlin:kotlin-stdlib")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("kotlin-language-server:shared:gradle_dsl_pre_platform")
	testImplementation("junit:junit:4.12")
	testImplementation("org.hamcrest:hamcrest-all:1.3")
}

tasks.startScripts {
    applicationName = "kotlin-debug-adapter"
}

tasks.register<Exec>("fixFilePermissions") {
    // When running on macOS or Linux the start script
    // needs executable permissions to run.

    onlyIf { !System.getProperty("os.name").lowercase().contains("windows") }
    commandLine("chmod", "+x", "${tasks.installDist.get().destinationDir}/bin/kotlin-debug-adapter")
}

tasks.register<JavaExec>("debugRun") {
    mainClass.set(adapterMainClassName)
    classpath(sourceSets.main.get().runtimeClasspath)
    standardInput = System.`in`

    jvmArgs(debugArgs)
    doLast {
        println("Using debug port $debugPort")
    }
}

tasks.register<CreateStartScripts>("debugStartScripts") {
    applicationName = "kotlin-debug-adapter"
    mainClass.set(adapterMainClassName)
    outputDir = tasks.installDist.get().destinationDir.toPath().resolve("bin").toFile()
    classpath = tasks.startScripts.get().classpath
    defaultJvmOpts = listOf(debugArgs)
}

tasks.register<Sync>("installDebugDist") {
    dependsOn("installDist")
    finalizedBy("debugStartScripts")
}

tasks.withType<Test>() {
    testLogging {
        events("failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.SHORT
    }
}

tasks.installDist {
    finalizedBy("fixFilePermissions")
}

tasks.build {
    finalizedBy("installDist")
}
