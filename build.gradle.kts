import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "2.6.7"
    id("io.spring.dependency-management") version "1.0.11.RELEASE"
    kotlin("jvm") version "1.6.10"
    kotlin("plugin.spring") version "1.6.10"
    kotlin("plugin.jpa") version "1.6.10"
}

group = "com.example"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_11

// =====================================================================
// 脆弱なバージョンを強制指定（CVE解消テスト用）
// =====================================================================
// CVE-2021-44228 (Log4Shell)  : log4j-core 2.17.1 → 2.17.1以上で修正
// CVE-2022-1471, CVE-2022-25857 : snakeyaml 1.30 → 2.0以上で修正
// CVE-2022-45868               : h2 2.1.210 → 2.1.214以上で修正
extra["log4j2.version"]   = "2.17.1"
extra["snakeyaml.version"] = "1.30"
extra["h2.version"]        = "2.1.210"

repositories {
    mavenCentral()
}

dependencies {
    // Spring Boot Web — Spring Framework 5.3.19 を使用
    // CVE-2022-22965 (Spring4Shell): 5.3.18未満が対象
    // CVE-2022-22978 (Spring Security): Spring Security 5.6.2 (5.6.4未満)
    implementation("org.springframework.boot:spring-boot-starter-web") {
        exclude(group = "org.springframework.boot", module = "spring-boot-starter-logging")
    }
    implementation("org.springframework.boot:spring-boot-starter-security")

    // Log4j2 starter — CVE-2021-44228 (Log4Shell, CVSS 10.0)
    // 検索クエリ等のユーザー入力をそのままログ出力するとJNDIインジェクション
    implementation("org.springframework.boot:spring-boot-starter-log4j2")

    // Spring Data JPA + H2 console
    // CVE-2022-45868 (CVSS 9.8): H2 console の web-allow-others=true で
    // リモートから任意コマンド実行が可能
    implementation("org.springframework.boot:spring-boot-starter-data-jpa") {
        exclude(group = "org.springframework.boot", module = "spring-boot-starter-logging")
    }
    runtimeOnly("com.h2database:h2")

    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    // CVE-2021-20190 (Jackson Databind 2.12.3, CVSS 8.1)
    // enableDefaultTyping + gadgetクラスで任意コード実行
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.12.3")

    // CVE-2022-42889 (Text4Shell, CVSS 9.8)
    // StringSubstitutor.createInterpolator() で script:/dns: ルックアップ経由の RCE
    implementation("org.apache.commons:commons-text:1.9")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "11"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
