import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

fun projectSecret(name: String): String =
    providers.environmentVariable(name).orNull
        ?: localProperties.getProperty(name)
        ?: ""

fun quoted(value: String): String =
    "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""

android {
    namespace = "co.edu.utadeo.tadeoconecta"
    compileSdk = 36

    defaultConfig {
        applicationId = "co.edu.utadeo.tadeoconecta"
        minSdk = 26
        targetSdk = 35
        versionCode = 6
        versionName = "1.5-supabase-api36"

        buildConfigField("String", "SUPABASE_URL", quoted(projectSecret("SUPABASE_URL")))
        buildConfigField("String", "SUPABASE_KEY", quoted(projectSecret("SUPABASE_KEY")))
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.compose.material3:material3")
    // Íconos reales de Material (en vez de glifos de texto tipo "⌂"/"♡"/"⭐",
    // que se ven distinto — a veces mal alineados o casi invisibles — según
    // la fuente de cada teléfono). Mismo motivo que EditPencilIcon más abajo,
    // pero para todos los demás íconos de la app.
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")

    implementation(platform("io.github.jan-tennert.supabase:bom:3.5.0"))
    implementation("io.github.jan-tennert.supabase:auth-kt")
    implementation("io.github.jan-tennert.supabase:postgrest-kt")
    implementation("io.github.jan-tennert.supabase:storage-kt")
    // Puntos 1+2 de la lista de Juan — recomendaciones de mejora con IA: la
    // app nunca llama directo a Gemini (la llave quedaría expuesta en el
    // APK), llama a una Edge Function de Supabase (ver
    // supabase/functions/generate-improvement-recommendations/) que sí
    // tiene la llave como secreto del proyecto.
    implementation("io.github.jan-tennert.supabase:functions-kt")
    implementation("io.ktor:ktor-client-android:3.0.3")

    // Módulo 02 — foto de perfil: carga de la imagen desde Supabase Storage.
    implementation("io.coil-kt:coil-compose:2.7.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
