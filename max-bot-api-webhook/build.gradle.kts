dependencies {
    implementation(project(":max-bot-api-core"))
    implementation(project(":max-bot-api-client"))
    implementation(libs.slf4j.api)

    testImplementation(libs.wiremock.standalone)
    testImplementation(project(":max-bot-api-jackson"))
    testRuntimeOnly(libs.slf4j.simple)
}
