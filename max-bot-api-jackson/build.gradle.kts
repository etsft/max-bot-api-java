plugins {
    `java-library`
}

dependencies {
    api(project(":max-bot-api-core"))
    api("com.fasterxml.jackson.core:jackson-databind:2.21.1")
    implementation("org.slf4j:slf4j-api:2.0.17")

    testImplementation("net.javacrumbs.json-unit:json-unit-assertj:4.1.1")
    testImplementation(project(":max-bot-api-test-support"))
    testRuntimeOnly("org.slf4j:slf4j-simple:2.0.17")
}
