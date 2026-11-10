plugins {
    `java-library`
}

dependencies {
    // Part of this module's public API (MaxBotAPI, UpdateHandler, MaxSerializer), so consumers
    // must get them on their compile classpath.
    api(project(":max-bot-api-core"))
    api(project(":max-bot-api-client"))
    implementation(libs.slf4j.api)

    testImplementation(libs.wiremock.standalone)
    testImplementation(project(":max-bot-api-jackson"))
    testImplementation(project(":max-bot-api-gson"))
    testImplementation(project(":max-bot-api-test-support"))
    testRuntimeOnly(libs.slf4j.simple)
}
