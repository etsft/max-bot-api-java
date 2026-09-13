plugins {
    `java-library`
}

dependencies {
    implementation(project(":max-bot-api-core"))
    implementation(libs.wiremock.standalone)
}
