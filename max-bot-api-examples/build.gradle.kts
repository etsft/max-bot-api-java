plugins {
    application
}

dependencies {
    implementation(project(":max-bot-api-core"))
    implementation(project(":max-bot-api-client"))
    implementation(project(":max-bot-api-jackson"))
    implementation(project(":max-bot-api-longpolling"))
    implementation(project(":max-bot-api-webhook"))
    implementation(libs.slf4j.simple)
}

// Pick the example to run: ./gradlew :max-bot-api-examples:run -PmainClass=ru.max.botapi.examples.KeyboardBot
application {
    mainClass = providers.gradleProperty("mainClass").orElse("ru.max.botapi.examples.EchoBot")
}

// Examples module has no tests — disable JaCoCo coverage verification and SpotBugs
tasks.named("jacocoTestCoverageVerification") {
    enabled = false
}

tasks.named("spotbugsMain") {
    enabled = false
}
