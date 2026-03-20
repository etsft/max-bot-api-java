plugins {
    `java-library`
}

dependencies {
    api(project(":max-bot-api-core"))
    api("com.fasterxml.jackson.core:jackson-databind:2.17.2")

    testImplementation("net.javacrumbs.json-unit:json-unit-assertj:3.4.1")
    testImplementation(project(":max-bot-api-test-support"))
}
