plugins {
    `java-library`
}

dependencies {
    api(project(":max-bot-api-core"))
    api("com.fasterxml.jackson.core:jackson-databind:2.21.1")

    testImplementation("net.javacrumbs.json-unit:json-unit-assertj:4.1.1")
    testImplementation(project(":max-bot-api-test-support"))
}
