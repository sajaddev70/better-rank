plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("org.jetbrains.kotlin.kapt")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
    id("com.google.gms.google-services")
    id ("kotlin-parcelize")
}

android {
    namespace = "com.marveloushost.betterrank"
    compileSdk = 35

    buildFeatures{
        dataBinding = true
        buildConfig = true
    }

    defaultConfig {
        applicationId = "com.marveloushost.betterrank"
        minSdk = 26
        targetSdk = 35
        versionCode = 12
        versionName = "1.1.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        isCoreLibraryDesugaringEnabled
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }

    packaging {
        resources {
            excludes += "META-INF/DEPENDENCIES"
        }
    }
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib:2.0.21")
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.firebase.config.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    //VieWModel
    implementation (libs.androidx.fragment.ktx)
    implementation (libs.androidx.lifecycle.viewmodel.ktx)

    //Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)

    // Retrofit & OkHttp
    implementation(libs.retrofit)
    implementation(libs.converter.gson)

    // Paging 3 Core
    implementation(libs.androidx.paging.runtime)

    //ViewPager2
    implementation(libs.androidx.viewpager2)

    //DotsIndicator
    implementation(libs.dotsindicator)

    //CircleImageView
    implementation (libs.circularimageview)

    //Round ImageView
    implementation(libs.roundedimageview)

    //Glide
    implementation(libs.glide)
    ksp(libs.compiler)

    //CameraX
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.guava)

    //Firebase
    implementation (libs.kotlinx.coroutines.play.services)
    implementation(libs.firebase.firestore.ktx)
    implementation(libs.firebase.storage)
    implementation(libs.firebase.auth.v2101)
    implementation(libs.play.services.auth)



    // Google Identity Services (Google ID)
    implementation(libs.googleid)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)

    //mlkit
    implementation(libs.text.recognition)

    //Jsoup
    implementation(libs.jsoup)

    //OpenAI
    implementation(libs.openai.java)

    //WebViewToPDF
    implementation (libs.convertwebviewtopdfdemo)

    //DateAndTimePicker
    implementation (libs.singledateandtimepicker)
    implementation ("androidx.print:print:1.0.0")
    implementation ("com.itextpdf:itext7-core:8.0.5")
    implementation ("com.itextpdf:html2pdf:5.0.5")
    implementation ("com.itextpdf:font-asian:8.0.5")
    implementation ("de.hdodenhof:circleimageview:3.1.0")
    coreLibraryDesugaring ("com.android.tools:desugar_jdk_libs:2.0.4")
    implementation("com.hbb20:ccp:2.7.2")
    implementation ("io.michaelrocks:libphonenumber-android:8.13.32")
    implementation ("com.tbuonomo:dotsindicator:5.1.0")
    implementation ("com.google.android.flexbox:flexbox:3.0.0")




}