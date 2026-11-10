plugins {
    `java-library`
}

dependencies {
    api(project(":max-bot-api-core"))
    api(libs.gson)
    implementation(libs.slf4j.api)

    testImplementation(libs.json.unit.assertj)
    testImplementation(project(":max-bot-api-test-support"))
    // Parity tests pin the Gson adapter to the wire format of the Jackson adapter
    testImplementation(project(":max-bot-api-jackson"))
    testRuntimeOnly(libs.slf4j.simple)
}
