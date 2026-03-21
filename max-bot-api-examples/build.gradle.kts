dependencies {
    implementation(project(":max-bot-api-core"))
    implementation(project(":max-bot-api-client"))
    implementation(project(":max-bot-api-jackson"))
    implementation(project(":max-bot-api-longpolling"))
    implementation("org.slf4j:slf4j-simple:2.0.17")
}

// Examples module has no tests — disable JaCoCo coverage verification and SpotBugs
tasks.named("jacocoTestCoverageVerification") {
    enabled = false
}

tasks.named("spotbugsMain") {
    enabled = false
}
