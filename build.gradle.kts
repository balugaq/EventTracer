import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    java
    alias(libs.plugins.shadow)
    alias(libs.plugins.run.paper)
}

group = "io.github.balugaq"
version = "0.1.0"

// 不用 toolchain：options.release=17 已保证 API 目标为 Java 17，
// 且不要求开发机/CI 之外额外安装 JDK 17（Gradle 9.8.1 可在 JDK 17+ 上运行）。
// 若误用 18+ API，javac --release 17 会直接报错拦截。
java {
    // toolchain 不设置，见上
}

tasks.compileJava {
    options.encoding = "UTF-8"
    // 插件产物锁定到 Java 17 字节码（最低运行环境 1.17 的服务器多为 Java 17+）
    options.release.set(17)
}

tasks.compileTestJava {
    options.encoding = "UTF-8"
    // 测试期依赖 MockBukkit 4.x（要求 JVM 21+），测试代码仅开发期/CI 运行，放宽到 21
    options.release.set(21)
}

tasks.withType<JavaExec>().configureEach {
    systemProperty("file.encoding", "UTF-8")
    systemProperty("sun.stdout.encoding", "UTF-8")
    systemProperty("sun.stderr.encoding", "UTF-8")
}

tasks.withType<Javadoc>().configureEach {
    // 不让 javadoc 警告阻断构建
    isFailOnError = false
    (options as StandardJavadocDocletOptions).apply {
        encoding = "UTF-8"
        charSet = "UTF-8"
        addStringOption("Xdoclint", "none")
    }
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    // ACF 仅发布在 aikar 自有仓库（SNAPSHOT）
    maven("https://repo.aikar.co/content/groups/aikar/")
}

dependencies {
    // Paper 编译期依赖：最低运行 1.17（见 gradle.properties / plugin.yml api-version）
    compileOnly(libs.paper.api)
    // 测试编译/运行环境：MockBukkit 4.x 照 paper-api 1.21.1 编写，测试须用同版 Bukkit 类
    testCompileOnly(libs.paper.api.test)
    testRuntimeOnly(libs.paper.api.test)
    compileOnly(libs.jetbrains.annotations)

    // 命令框架 ACF：注解驱动，shade 进 fat jar（co.aikar 命名空间独立，不 relocate）
    implementation(libs.acf.bukkit)

    // 测试：JUnit 5 + MockBukkit。
    // MockBukkit 无 v1.17 线，最低为 v1.21；其自带 paper-api 1.21.1 运行时，
    // 测试因此跑在 1.21 mock 上（主代码编译期锁定 1.17.1，API 子集已核对跨版本稳定）。
    // 排除 MockBukkit 传递的 junit-jupiter，由我们显式锁定 5.14.x。
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.mockbukkit) {
        exclude(group = "org.junit.jupiter")
    }
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}

tasks.jar {
    enabled = false
}

tasks.named<ProcessResources>("processResources") {
    // 仅对 plugin.yml 做版本占位符替换；语言/配置文件内含 {0} 占位符，不能走 expand。
    filesMatching("plugin.yml") {
        expand(
            mapOf(
                "version" to project.version,
                "name" to project.name
            )
        )
    }
}

tasks.named<ShadowJar>("shadowJar") {
    archiveVersion.set(project.version.toString())
    archiveClassifier.set("")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

val sourcesJar = tasks.register<Jar>("sourcesJar") {
    archiveClassifier.set("sources")
    from(sourceSets.main.get().allSource)
}

tasks.build {
    dependsOn(tasks.named("shadowJar"))
}

tasks.runServer {
    dependsOn(tasks.named("shadowJar"))
    val run = file(providers.gradleProperty("server.run.dir").orElse("run"))
    runDirectory.set(run)

    doFirst {
        run.resolve("eula.txt").writeText("eula=true")

        val pl = run.resolve("plugins")
        pl.mkdirs()
        copy {
            from(projectDir.resolve("build/libs")) {
                include("${name}-${version}.jar")
            }
            into(pl)
        }
    }

    jvmArgs(
        "-Dfile.encoding=UTF-8",
        "-Dsun.jnu.encoding=UTF-8",
        "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005",
        "-Dnet.kyori.adventure.text.warn_when_legacy_formatting_detected=false"
    )
    maxHeapSize = "4G"
    // 开发服：用于本地调试的 MC 版本（与最低运行版本 1.17 解耦）
    minecraftVersion("26.2")
}

defaultTasks("clean", "build")
