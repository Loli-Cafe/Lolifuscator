import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    kotlin("jvm")

    id("org.jetbrains.compose")
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

group = "cafe.loli"
version =
    "はあぁぁ...くそメスガキが...！！！大人を誘惑しやがって\uD83D\uDCA2\uD83D\uDCA2レイプ矯正が必要なんだよな...\uD83D\uDCA2"

repositories {
    google()
    gradlePluginPortal()
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

kotlin {
    jvmToolchain(17)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

val windows by configurations.creating {
    extendsFrom(project.configurations.implementation.get())
}

val linux by configurations.creating {
    extendsFrom(project.configurations.implementation.get())
}

val garbageos by configurations.creating {
    extendsFrom(project.configurations.implementation.get())
}

val current by configurations.creating {
    extendsFrom(project.configurations.implementation.get())
}

configurations {
    implementation {
        isCanBeResolved = true
    }
}

configurations.all {
    exclude(group = "org.jetbrains.skiko", module = "skiko")
}

dependencies {
    runtimeOnly(compose.desktop.currentOs)

    current(compose.desktop.currentOs)
    windows(compose.desktop.windows_x64)
    linux(compose.desktop.linux_arm64)
    linux(compose.desktop.linux_x64)
    garbageos(compose.desktop.macos_arm64)
    garbageos(compose.desktop.macos_x64)

    implementation("com.darkrockstudios:mpfilepicker-jvm:2.0.2")
    implementation("org.jsoup:jsoup:1.16.1")
    implementation("log4j:log4j:1.2.17")
}

tasks.named<Jar>("jar") {
    this.archiveFileName = "Lolifuscator-base.zip"
}

tasks.named<ShadowJar>("shadowJar") {
    shadowJar(
        this,
        "Lolifuscator-universal.jar",
        listOf(windows, linux, garbageos, project.configurations.implementation.get())
    )
}

tasks {
    create("shadowJarForAllOsWithoutUniversalJar") {
        dependsOn("shadowJarForWindows", "shadowJarForLinux", "shadowJarForGarbageMacOs")
    }

    create("shadowJarForAllOsWithUniversalJar") {
        dependsOn("shadowJarForAllOsWithoutUniversalJar", "shadowJar")
    }

    create<ShadowJar>("shadowJarForCurrentOs") {
        shadowJar(
            this,
            "Lolifuscator-${System.getProperty("os.name")}-${System.getProperty("os.arch")}.jar".replace(' ', '_'),
            listOf(windows, project.configurations.implementation.get())
        )
    }

    create<ShadowJar>("shadowJarForWindows") {
        shadowJar(
            this,
            "Lolifuscator-windows.jar",
            listOf(windows, project.configurations.implementation.get())
        )
    }

    create<ShadowJar>("shadowJarForLinux") {
        shadowJar(
            this,
            "Lolifuscator-linux.jar",
            listOf(linux, project.configurations.implementation.get())
        )
    }

    create<ShadowJar>("shadowJarForGarbageMacOs") {
        shadowJar(
            this,
            "Lolifuscator-garbagnemacos.jar",
            listOf(garbageos, project.configurations.implementation.get())
        )
    }
}

fun shadowJar(task: ShadowJar, fileName: String, configurations: List<Configuration>) {
    task.dependsOn("jar")
    task.archiveFileName.set(fileName)
    task.destinationDirectory = file("$rootDir/build/release")
    task.duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    task.isZip64 = true

    task.manifest {
        attributes(mapOf("Main-Class" to "LoliconAppKt"))
    }

    task.from(project.sourceSets.main.get().output)
    task.configurations = configurations
}

compose.desktop {
    application {
        mainClass = "LoliconAppKt"
    }
}
