plugins {
    java
    checkstyle
    id("jacoco-report-aggregation")
    id("com.github.spotbugs") version "6.4.8" apply false
}

allprojects {
    group = "ru.etsft.max.botapi"
    version = "0.1.0-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}

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
}

dependencies {
    jacocoAggregation(project(":max-bot-api-core"))
    jacocoAggregation(project(":max-bot-api-client"))
    jacocoAggregation(project(":max-bot-api-jackson"))
    jacocoAggregation(project(":max-bot-api-gson"))
    jacocoAggregation(project(":max-bot-api-webhook"))
    jacocoAggregation(project(":max-bot-api-longpolling"))
}

tasks.named<JacocoReport>("testCodeCoverageReport") {
    reports {
        xml.required.set(true)
    }
}
