plugins {
    id("java")
}

group = "dev.ptnr"
version = "0.0.1"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    implementation("net.dv8tion:JDA:5.6.1") { // replace $version with the latest version
        // Optionally disable audio natives to reduce jar size by excluding `opus-java` and `tink`
        // Gradle DSL:
        // exclude module: 'opus-java' // required for encoding audio into opus, not needed if audio is already provided in opus encoding
        // exclude module: 'tink' // required for encrypting and decrypting audio
        // Kotlin DSL:
        // exclude(module="opus-java") // required for encoding audio into opus, not needed if audio is already provided in opus encoding
        // exclude(module="tink") // required for encrypting and decrypting audio
    }
    implementation("com.fasterxml.jackson.core:jackson-databind:2.19.0")
    implementation("com.twelvemonkeys.imageio:imageio-webp:3.12.0")
    implementation("io.github.cdimascio:dotenv-java:3.2.0")
}

tasks.jar {
    manifest { attributes["Main-Class"] = "dev.ptnr.AyayaBot" }
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.test {
    useJUnitPlatform()
}