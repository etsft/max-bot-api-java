plugins {
    `java-library`
}

dependencies {
    api(project(":max-bot-api-core"))
    implementation(libs.slf4j.api)
    testImplementation(libs.wiremock.standalone)
    testImplementation(project(":max-bot-api-jackson"))
    testImplementation(project(":max-bot-api-test-support"))
    testImplementation(project(":max-bot-api-gson"))
    testRuntimeOnly(libs.slf4j.simple)
}
