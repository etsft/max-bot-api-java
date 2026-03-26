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
    compileOnly("org.springframework.boot:spring-boot-starter-web:3.5.12")
    compileOnly("org.springframework.boot:spring-boot-autoconfigure:3.5.12")

    api(project(":max-bot-api-core"))
    api(project(":max-bot-api-client"))
    api(project(":max-bot-api-webhook"))
    api(project(":max-bot-api-longpolling"))
    implementation("org.slf4j:slf4j-api:2.0.17")

    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor:3.5.12")

    testImplementation("org.springframework.boot:spring-boot-starter-test:3.5.12")
    testImplementation("org.springframework.boot:spring-boot-starter-web:3.5.12")
    testImplementation(project(":max-bot-api-jackson"))
    testRuntimeOnly("org.slf4j:slf4j-simple:2.0.17")
}
