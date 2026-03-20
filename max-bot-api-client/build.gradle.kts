plugins {
    `java-library`
}

dependencies {
    api(project(":max-bot-api-core"))
    implementation("org.slf4j:slf4j-api:2.0.16")
    testImplementation("org.wiremock:wiremock-standalone:3.6.0")
    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core:3.27.3")
    testImplementation(project(":max-bot-api-jackson"))
    testRuntimeOnly("org.slf4j:slf4j-simple:2.0.16")
}
