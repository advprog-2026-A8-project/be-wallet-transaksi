plugins {
    java
    jacoco
    checkstyle
    id("org.springframework.boot") version "3.5.10"
    id("io.spring.dependency-management") version "1.1.7"
    id("org.sonarqube") version "4.4.1.3373"
    id("com.google.protobuf") version "0.9.4"
}

group = "id.ac.ui.cs.advprog"
version = "0.0.1-SNAPSHOT"
description = "be-wallet-transaksi"

val grpcVersion = "1.68.1"
val jjwtVersion = "0.12.6"
val springdocVersion = "2.8.8"
val protobufVersion = "3.25.5"
val grpcPluginVersion = "1.68.1"
val tomcatAnnotationsVersion = "6.0.53"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:$springdocVersion")
    implementation("io.grpc:grpc-netty-shaded:$grpcVersion")
    implementation("io.grpc:grpc-protobuf:$grpcVersion")
    implementation("io.grpc:grpc-stub:$grpcVersion")
    implementation("io.jsonwebtoken:jjwt-api:$jjwtVersion")

    compileOnly("org.apache.tomcat:annotations-api:$tomcatAnnotationsVersion")
    compileOnly("org.projectlombok:lombok")

    annotationProcessor("org.projectlombok:lombok")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    developmentOnly("org.springframework.boot:spring-boot-devtools")

    runtimeOnly("io.jsonwebtoken:jjwt-impl:$jjwtVersion")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:$jjwtVersion")
    runtimeOnly("org.postgresql:postgresql")

    testImplementation("io.grpc:grpc-testing:$grpcVersion")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("com.h2database:h2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:$protobufVersion"
    }
    plugins {
        create("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:$grpcPluginVersion"
        }
    }
    generateProtoTasks {
        all().configureEach {
            plugins {
                create("grpc")
            }
        }
    }
}

dependencyLocking {
    lockAllConfigurations()
}

sonar {
    properties {
        property("sonar.projectKey", "advprog-2026-A8-project_be-wallet-transaksi")
        property("sonar.organization", "advprog-2026-a8-project")
        property("sonar.host.url", "https://sonarcloud.io")
        property("sonar.coverage.jacoco.xmlReportPaths", "build/reports/jacoco/test/jacocoTestReport.xml")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
    finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

checkstyle {
    toolVersion = "10.20.0"
    configFile = file("config/checkstyle/checkstyle.xml")
}

tasks.withType<Checkstyle> {
    reports {
        xml.required.set(false)
        html.required.set(true)
    }
}

tasks.named<Checkstyle>("checkstyleMain") {
    source = fileTree("src/main/java")
}

tasks.named<Checkstyle>("checkstyleTest") {
    source = fileTree("src/test/java")
}
