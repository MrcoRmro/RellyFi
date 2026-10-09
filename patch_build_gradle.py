import re
build_file = "app/build.gradle"

with open(build_file, "r") as f:
    content = f.read()

# Dependencias necesarias
deps = """
    // MVVM y Corrutinas
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-ktx:1.8.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    
    // Motor yt-dlp local
    implementation("com.github.yausername.youtubedl-android:library:0.17.0")
    implementation("com.github.yausername.youtubedl-android:ffmpeg:0.17.0")
"""

# Limpiamos dependencias mal inyectadas previamente si existen
content = re.sub(r"implementation\s*['\"]androidx\.lifecycle:lifecycle-.*['\"]\n", "", content)
content = re.sub(r"implementation\s*['\"]org\.jetbrains\.kotlinx:kotlinx-coroutines-.*['\"]\n", "", content)
content = re.sub(r"implementation\s*['\"]com\.github\.yausername\.youtubedl-android:.*['\"]\n", "", content)

# Inyectamos las nuevas
content = content.replace("dependencies {", "dependencies {" + deps)

with open(build_file, "w") as f:
    f.write(content)
print("✅ app/build.gradle actualizado.")
