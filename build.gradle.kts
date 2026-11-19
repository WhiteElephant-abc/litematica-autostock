import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
	id("net.fabricmc.fabric-loom")
	id("org.jetbrains.kotlin.jvm") version "2.4.21"
	id("org.jetbrains.kotlin.plugin.serialization") version "2.4.21"
}

repositories {
	// Modrinth maven for Litematica, MaLiLib and MiniHUD
	exclusiveContent {
		forRepository {
			maven {
				name = "Modrinth"
				url = uri("https://api.modrinth.com/maven")
			}
		}
		filter {
			includeGroup("maven.modrinth")
		}
	}
	mavenCentral()
}

loom {
	splitEnvironmentSourceSets()

	mods {
		register("litematica-autostock") {
			sourceSet(sourceSets.main.get())
			sourceSet(sourceSets.getByName("client"))
		}
	}
}

dependencies {
	minecraft("com.mojang:minecraft:${providers.gradleProperty("minecraft_version").get()}")
	implementation("net.fabricmc:fabric-loader:${providers.gradleProperty("loader_version").get()}")
	implementation("net.fabricmc.fabric-api:fabric-api:${providers.gradleProperty("fabric_api_version").get()}")
	implementation("net.fabricmc:fabric-language-kotlin:${providers.gradleProperty("fabric_kotlin_version").get()}")

	// 26.3 jars are not obfuscated, so no remap configurations are used.
	implementation("maven.modrinth:litematica:0.29.1")
	implementation("maven.modrinth:malilib:0.30.2")
	compileOnly("maven.modrinth:minihud:0.41.2")

	// Baritone 26.3 (Fabric). Drop the built jar into libs/.
	compileOnly(fileTree("libs") { include("baritone*.jar") })
	runtimeOnly(fileTree("libs") { include("baritone*.jar") })
}

tasks.processResources {
	val version = version
	inputs.property("version", version)

	filesMatching("fabric.mod.json") {
		expand("version" to version)
	}
}

tasks.withType<JavaCompile>().configureEach {
	options.release = 25
}

kotlin {
	compilerOptions {
		jvmTarget = JvmTarget.JVM_25
	}
}

java {
	withSourcesJar()

	sourceCompatibility = JavaVersion.VERSION_25
	targetCompatibility = JavaVersion.VERSION_25
}

tasks.jar {
	val projectName = project.name
	inputs.property("projectName", projectName)

	from("LICENSE") {
		rename { "${it}_$projectName" }
	}
}
