plugins {
    `java-library`
}

dependencies {
    api(project(":max-bot-api-core"))
    api(libs.jackson.databind)
    implementation(libs.slf4j.api)

    testImplementation(libs.json.unit.assertj)
    testImplementation(project(":max-bot-api-test-support"))
    testRuntimeOnly(libs.slf4j.simple)
}
