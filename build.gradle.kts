import com.ncorti.ktfmt.gradle.tasks.KtfmtBaseTask
import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinJvm
import com.vanniktech.maven.publish.SourcesJar
import dev.detekt.gradle.Detekt
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

// The Qualithm Platform API client, published to Maven Central. The version
// comes from gradle.properties.
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kover)
    alias(libs.plugins.detekt)
    alias(libs.plugins.ktfmt)
    alias(libs.plugins.dokka)
    alias(libs.plugins.maven.publish)
}

group = "com.qualithm"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    explicitApi()
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
        allWarningsAsErrors = true
    }
}

// The generated client: src/main/kotlin/com/qualithm/platform/generated/,
// written by scripts/generate and checked by CI's Generated Drift job. It
// compiles as its own source set, so the generator's warnings don't fail the
// build, and it sits outside ktfmt, detekt and the Kover gate below. Its classes and sources ship in the one jar.
val generatedSources = "com/qualithm/platform/generated/**"

val generated = sourceSets.create("generated")

kotlin.sourceSets.named("generated") {
    kotlin.setSrcDirs(listOf("src/main/kotlin"))
    kotlin.include(generatedSources)
}

kotlin.sourceSets.named("main") { kotlin.exclude(generatedSources) }

configurations.named("generatedImplementation") {
    extendsFrom(configurations.api.get(), configurations.implementation.get())
}

tasks.named<KotlinCompile>("compileGeneratedKotlin") { compilerOptions.allWarningsAsErrors = false }

sourceSets.main {
    compileClasspath += generated.output
    runtimeClasspath += generated.output
}

sourceSets.test {
    compileClasspath += generated.output
    runtimeClasspath += generated.output
}

tasks.jar { from(generated.output) }

// Runnable examples under examples/<name>/Main.kt, compiled against the
// library's public API and run with ./gradlew runExample -Pexample=<name>.
val examples =
    sourceSets.create("examples") {
        compileClasspath += sourceSets.main.get().output + generated.output
        runtimeClasspath += sourceSets.main.get().output + generated.output
    }

kotlin.sourceSets.named("examples") { kotlin.setSrcDirs(listOf("examples")) }

configurations.named("examplesImplementation") {
    extendsFrom(configurations.api.get(), configurations.implementation.get())
}

// CI's Build job runs assemble, so the examples compile there too.
tasks.assemble { dependsOn(examples.classesTaskName) }

tasks.register<JavaExec>("runExample") {
    description = "Runs examples/<name>/Main.kt, chosen with -Pexample=<name>."
    group = "application"
    classpath = examples.runtimeClasspath
    mainClass = providers.gradleProperty("example").orElse("listdevices").map { "$it.MainKt" }
}

ktfmt { kotlinLangStyle() }

tasks.withType<KtfmtBaseTask>().configureEach { exclude(generatedSources) }

detekt {
    buildUponDefaultConfig = true
    config.setFrom(file("config/detekt/detekt.yml"))
    source.from("examples")
}

tasks.withType<Detekt>().configureEach { exclude(generatedSources) }

// The gate measures the hand-written layer; the examples are compiled but not
// measured, and the generated client is checked by the drift job instead.
kover {
    currentProject { sources { excludedSourceSets.addAll("examples", "generated") } }
    reports {
        filters { excludes { packages("com.qualithm.platform.generated") } }
        verify { rule { minBound(80) } }
    }
}

// API docs: an undocumented public declaration is a warning, and a warning
// fails the build. The generated client is documented as far as the spec
// describes it, so it isn't held to that.
dokka {
    dokkaPublications.html { failOnWarning = true }
    dokkaSourceSets.configureEach {
        reportUndocumented = true
        perPackageOption {
            matchingRegex = """com\.qualithm\.platform\.generated(\..*)?"""
            reportUndocumented = false
        }
    }
    // Dokka documents only main by default; the generated client is public API.
    dokkaSourceSets.named("generated") { suppress = false }
}

// What ships is locked (gradle.lockfile).
configurations.named("runtimeClasspath") { resolutionStrategy.activateDependencyLocking() }

dependencies {
    // Both appear in the public API: PlatformClient takes an OkHttpClient, and
    // the generated models are Moshi-annotated.
    api(libs.okhttp)
    api(libs.moshi.kotlin)
    implementation(libs.moshi.adapters)
    implementation(libs.kotlin.reflect)
    testImplementation(libs.junit)
}

mavenPublishing {
    configure(
        KotlinJvm(
            javadocJar = JavadocJar.Dokka("dokkaGeneratePublicationHtml"),
            sourcesJar = SourcesJar.Sources(),
        )
    )
    publishToMavenCentral()
    // The release workflow passes the signing key, which Maven Central
    // requires; without one, publishToMavenLocal still works for local testing.
    if (providers.gradleProperty("signingInMemoryKey").isPresent) signAllPublications()
    coordinates("com.qualithm", "platform", version.toString())
    pom {
        name = "platform"
        description =
            "Kotlin client for the Qualithm Platform API, generated from its OpenAPI spec."
        url = "https://github.com/qualithm/platform-sdk-kt"
        licenses {
            license {
                name = "Apache-2.0"
                url = "https://www.apache.org/licenses/LICENSE-2.0"
            }
        }
        developers {
            developer {
                id = "qualithm"
                name = "Qualithm"
                url = "https://github.com/qualithm"
            }
        }
        scm {
            url = "https://github.com/qualithm/platform-sdk-kt"
            connection = "scm:git:https://github.com/qualithm/platform-sdk-kt.git"
        }
    }
}
