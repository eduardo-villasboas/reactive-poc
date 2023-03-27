plugins {
    kotlin("jvm") version "1.8.10"
    application
    idea
}

group = "org.reactive"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {

//    implementation("io.reactivex:rxjava:3.0.4")
    implementation("io.reactivex.rxjava3:rxjava:3.1.6")
    // https://mvnrepository.com/artifact/com.google.guava/guava
    implementation("com.google.guava:guava:31.1-jre")

    //implementation("io.reactivex:rxjava:1.3.8")
    //// https://mvnrepository.com/artifact/io.reactivex/rxjava-reactive-streams
    //implementation("io.reactivex:rxjava-reactive-streams:1.2.1")


    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.2")

}

kotlin {
    jvmToolchain(17)
}


tasks.getByName<Test>("test") {
    useJUnitPlatform()
}

tasks.withType<JavaExec> {
    mainClass.set("org.reactive.MainKt")
}
