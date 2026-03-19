plugins {
    `java-library`
}

dependencies {
    api(project(":max-bot-api-core"))
    implementation("org.slf4j:slf4j-api:2.0.16")
}
