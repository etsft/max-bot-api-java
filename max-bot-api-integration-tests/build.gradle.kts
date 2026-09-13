dependencies {
    testImplementation(project(":max-bot-api-core"))
    testImplementation(project(":max-bot-api-client"))
    testImplementation(project(":max-bot-api-jackson"))
    testImplementation(project(":max-bot-api-longpolling"))
    testImplementation(project(":max-bot-api-webhook"))

    // Launcher for the `liveTest` task below; version comes from the JUnit BOM in the root build.
    testRuntimeOnly(libs.junit.platform.console)
    testRuntimeOnly(libs.slf4j.simple)
}

// The suite talks to the real MAX API with a real bot token: never run it from `build` or CI.
// Test sources are still compiled and linted, which is what catches drift at review time.
tasks.test {
    enabled = false
}

// Deliberately a JavaExec rather than a Test task: Gradle's Test task gives its workers an empty
// stdin, so the interactive prompts would hang. The JUnit console launcher runs in a process we
// can hand the real console to.
tasks.register<JavaExec>("liveTest") {
    group = "verification"
    description = "Runs the live suite against the real MAX API (requires MAX_BOT_TOKEN)."

    dependsOn(tasks.testClasses)
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("org.junit.platform.console.ConsoleLauncher")

    args(
        "execute",
        "--select-package=ru.max.botapi.it",
        "--details=tree",
        "--disable-ansi-colors",
        "--reports-dir=${layout.buildDirectory.get().asFile}/test-results/liveTest"
    )

    standardInput = System.`in`
    outputs.upToDateWhen { false }

    // How long an interactive prompt waits, without editing the environment:
    //   ./gradlew :max-bot-api-integration-tests:liveTest -Pit.promptTimeout=60
    // An explicit property wins over MAX_IT_PROMPT_TIMEOUT_SECONDS from the shell.
    (project.findProperty("it.promptTimeout") as String?)?.let { seconds ->
        require(seconds.toLongOrNull()?.let { it > 0 } == true) {
            "it.promptTimeout must be a positive number of seconds, got: $seconds"
        }
        environment("MAX_IT_PROMPT_TIMEOUT_SECONDS", seconds)
    }
}

// No production sources here, and `test` is disabled, so neither gate has anything to measure.
tasks.named("jacocoTestCoverageVerification") {
    enabled = false
}

tasks.named("spotbugsMain") {
    enabled = false
}
