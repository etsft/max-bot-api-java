plugins {
    `java-library`
}

dependencies {
    implementation(project(":max-bot-api-core"))
    implementation("org.wiremock:wiremock-standalone:3.13.1")
}
