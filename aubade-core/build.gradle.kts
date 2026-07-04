plugins {
    id("java")
    id("com.gradleup.shadow")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.20.1-R0.1-SNAPSHOT")
    compileOnly(files("libs/axs-api.jar"))
    compileOnly("com.github.MilkBowl:VaultAPI:1.7.1")
    compileOnly("me.clip:placeholderapi:2.11.6")
    compileOnly("com.zaxxer:HikariCP:5.1.0")
    compileOnly("redis.clients:jedis:5.2.0")
    compileOnly("com.google.code.gson:gson:2.11.0")
    testCompileOnly("io.papermc.paper:paper-api:1.20.1-R0.1-SNAPSHOT")
    testCompileOnly(files("libs/axs-api.jar"))
    testRuntimeOnly("io.papermc.paper:paper-api:1.20.1-R0.1-SNAPSHOT")
    testRuntimeOnly(files("libs/axs-api.jar"))
    testImplementation(platform("org.junit:junit-bom:5.10.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("com.github.seeseemelk:MockBukkit-v1.20:3.32.1")
    testImplementation(project(":aubade-game-skyblock"))
    runtimeOnly(project(":aubade-game-skyblock"))
}

tasks.shadowJar {
    archiveBaseName.set("Aubade")
    archiveClassifier.set("")
    archiveVersion.set(project.version.toString())
    configurations = listOf(project.configurations.getByName("runtimeClasspath"))
}

tasks.jar {
    enabled = false
}

tasks.build {
    dependsOn(tasks.shadowJar)
}
