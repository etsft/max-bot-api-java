dependencies {
    implementation(project(":max-bot-api-core"))
    implementation(project(":max-bot-api-client"))
    implementation("org.slf4j:slf4j-api:2.0.17")

    testImplementation("org.wiremock:wiremock-standalone:3.13.1")
    testImplementation(project(":max-bot-api-jackson"))
    testRuntimeOnly("org.slf4j:slf4j-simple:2.0.17")
}
