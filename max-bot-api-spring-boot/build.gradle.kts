plugins {
    `java-library`
}

tasks.withType<JavaCompile> {
    // Spring Boot's configuration-processor does not claim Spring annotations,
    // causing "unclaimed annotation" warnings. Suppress only that lint category
    // while keeping -Werror active for all other warnings.
    options.compilerArgs.add("-Xlint:-processing")
}

dependencies {
    compileOnly(libs.spring.boot.starter.web)
    compileOnly(libs.spring.boot.autoconfigure)

    api(project(":max-bot-api-core"))
    api(project(":max-bot-api-client"))
    api(project(":max-bot-api-webhook"))
    api(project(":max-bot-api-longpolling"))
    implementation(libs.slf4j.api)

    annotationProcessor(libs.spring.boot.configuration.processor)

    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.spring.boot.starter.web)
    testImplementation(project(":max-bot-api-jackson"))
    testRuntimeOnly(libs.slf4j.simple)
}
