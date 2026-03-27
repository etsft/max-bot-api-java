plugins {
    java
    checkstyle
    id("jacoco-report-aggregation")
    id("com.github.spotbugs") version "6.4.8" apply false
}

allprojects {
    group = property("group")!!
    version = property("version")!!

    repositories {
        mavenCentral()
    }
}

// Modules published to Maven Central (excludes examples)
val publishedModules = setOf(
    "max-bot-api-core",
    "max-bot-api-client",
    "max-bot-api-jackson",
    "max-bot-api-gson",
    "max-bot-api-webhook",
    "max-bot-api-longpolling",
    "max-bot-api-spring-boot"
)

subprojects {
    apply(plugin = "java")
    apply(plugin = "jacoco")
    apply(plugin = "checkstyle")
    apply(plugin = "com.github.spotbugs")

    java {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.compilerArgs.addAll(listOf("-Xlint:all", "-Werror"))
    }

    dependencies {
        testImplementation(platform("org.junit:junit-bom:5.14.3"))
        testImplementation("org.junit.jupiter:junit-jupiter")
        testImplementation("org.assertj:assertj-core:3.27.7")
        testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    }

    tasks.test {
        useJUnitPlatform()
    }

    jacoco {
        toolVersion = "0.8.12"
    }

    tasks.jacocoTestReport {
        dependsOn(tasks.test)
        reports {
            xml.required.set(true)
            html.required.set(true)
        }
    }

    tasks.jacocoTestCoverageVerification {
        violationRules {
            rule {
                limit {
                    minimum = "0.85".toBigDecimal()
                }
                limit {
                    counter = "BRANCH"
                    minimum = "0.80".toBigDecimal()
                }
            }
        }
    }

    tasks.check {
        dependsOn(tasks.jacocoTestCoverageVerification)
    }

    checkstyle {
        toolVersion = "10.20.1"
        configFile = rootProject.file("config/checkstyle/checkstyle.xml")
        isIgnoreFailures = false
        maxWarnings = 0
    }

    configure<com.github.spotbugs.snom.SpotBugsExtension> {
        effort.set(com.github.spotbugs.snom.Effort.MAX)
        reportLevel.set(com.github.spotbugs.snom.Confidence.HIGH)
    }

    tasks.withType<com.github.spotbugs.snom.SpotBugsTask> {
        reports.create("html") {
            required.set(true)
        }
        reports.create("xml") {
            required.set(true)
        }
    }

    // Disable SpotBugs on test sources — only analyze production code
    tasks.matching { it.name == "spotbugsTest" }.configureEach {
        enabled = false
    }

    // --- Maven Central publishing setup for published modules ---
    if (name in publishedModules) {
        apply(plugin = "maven-publish")
        apply(plugin = "signing")

        java {
            withSourcesJar()
            withJavadocJar()
        }

        configure<PublishingExtension> {
            publications {
                create<MavenPublication>("mavenJava") {
                    from(components["java"])

                    pom {
                        name.set(provider {
                            "${rootProject.property("POM_NAME")} — ${project.name}"
                        })
                        description.set(
                            rootProject.property("POM_DESCRIPTION") as String
                        )
                        url.set(rootProject.property("POM_URL") as String)
                        inceptionYear.set(
                            rootProject.property("POM_INCEPTION_YEAR") as String
                        )

                        licenses {
                            license {
                                name.set(
                                    rootProject.property("POM_LICENSE_NAME")
                                            as String
                                )
                                url.set(
                                    rootProject.property("POM_LICENSE_URL")
                                            as String
                                )
                                distribution.set("repo")
                            }
                        }

                        developers {
                            developer {
                                id.set(
                                    rootProject.property("POM_DEVELOPER_ID")
                                            as String
                                )
                                name.set(
                                    rootProject.property("POM_DEVELOPER_NAME")
                                            as String
                                )
                                email.set(
                                    rootProject.property("POM_DEVELOPER_EMAIL")
                                            as String
                                )
                            }
                        }

                        scm {
                            url.set(
                                rootProject.property("POM_SCM_URL") as String
                            )
                            connection.set(
                                rootProject.property("POM_SCM_CONNECTION")
                                        as String
                            )
                            developerConnection.set(
                                rootProject.property("POM_SCM_DEV_CONNECTION")
                                        as String
                            )
                        }
                    }
                }
            }

            repositories {
                maven {
                    name = "staging"
                    url = uri(
                        rootProject.layout.buildDirectory.dir("staging-deploy")
                    )
                }
            }
        }

        configure<SigningExtension> {
            // Only sign when "signing.keyId" is provided (release builds)
            isRequired = project.hasProperty("signing.keyId")

            // Support in-memory keys for CI (set via environment variables)
            val signingKey: String? =
                project.findProperty("signingInMemoryKey") as? String
            val signingKeyId: String? =
                project.findProperty("signingInMemoryKeyId") as? String
            val signingPassword: String? =
                project.findProperty("signingInMemoryKeyPassword") as? String

            if (signingKey != null) {
                useInMemoryPgpKeys(signingKeyId, signingKey, signingPassword)
            }

            sign(
                the<PublishingExtension>().publications["mavenJava"]
            )
        }

        // Disable signing tasks when key is not available (development)
        tasks.withType<Sign>().configureEach {
            onlyIf {
                project.hasProperty("signing.keyId")
                        || project.hasProperty("signingInMemoryKey")
            }
        }
    }
}

dependencies {
    jacocoAggregation(project(":max-bot-api-core"))
    jacocoAggregation(project(":max-bot-api-client"))
    jacocoAggregation(project(":max-bot-api-jackson"))
    jacocoAggregation(project(":max-bot-api-gson"))
    jacocoAggregation(project(":max-bot-api-webhook"))
    jacocoAggregation(project(":max-bot-api-longpolling"))
    jacocoAggregation(project(":max-bot-api-spring-boot"))
}

tasks.named<JacocoReport>("testCodeCoverageReport") {
    reports {
        xml.required.set(true)
    }
}
