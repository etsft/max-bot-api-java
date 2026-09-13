plugins {
    `java-library`
}

dependencies {
    api(project(":max-bot-api-core"))
    implementation(libs.slf4j.api)
    testImplementation(libs.wiremock.standalone)
    testImplementation(project(":max-bot-api-jackson"))
    testRuntimeOnly(libs.slf4j.simple)
}
