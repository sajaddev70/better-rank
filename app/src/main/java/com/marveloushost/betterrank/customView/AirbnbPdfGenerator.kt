package com.marveloushost.betterrank.customView

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.itextpdf.html2pdf.ConverterProperties
import com.itextpdf.html2pdf.HtmlConverter
import com.itextpdf.kernel.pdf.PdfWriter
import com.marveloushost.betterrank.model.CityGuideModel
import com.marveloushost.betterrank.model.HomeGuideModel
import com.marveloushost.betterrank.util.Prefs
import kotlinx.coroutines.tasks.await
import java.io.File
import java.io.FileOutputStream

object AirbnbPdfGenerator {

    private const val TAG = "PDF_PHOTOS"

    // ------------------------------------------------------------
    // FIRESTORE PHOTO LOADER
    // ------------------------------------------------------------
    suspend fun loadAllPhotoSectionsFromFirestore(ctx: Context): Map<String, List<String>> {

        val homeId = Prefs.getHomeId(ctx)
        val db = FirebaseFirestore.getInstance()
        val result = mutableMapOf<String, List<String>>()

        for (section in PhotoName.values()) {

            val sectionName = section.value  // e.g., "cover_photo"
            val path = "homes/$homeId/photos/$sectionName/images"

//            Log.d("PDF_PHOTOS", "Loading → $path")

            try {
                val snap = db.collection(path).get().await()

                val urls = snap.documents.mapNotNull { it.getString("url") }

//                Log.d("PDF_PHOTOS", "Loaded ${urls.size} photos for $sectionName")

                result[sectionName] = urls

            } catch (e: Exception) {
//                Log.e("PDF_PHOTOS", "Error loading $sectionName: ${e.message}")
                result[sectionName] = emptyList()
            }
        }

        return result
    }


    // ------------------------------------------------------------
    // HTML BUILDER
    // ------------------------------------------------------------
    fun generateHtml(
        cityGuide: CityGuideModel,
        homeGuide: HomeGuideModel,
        shortDescription: String?,
        appName: String,
        mainImages: List<String>,                    // << ADD
        sectionImages: Map<String, List<String>>,    // << ADD
        onHtmlReady: (String) -> Unit
    )
    {
        val html = buildHtml(cityGuide, homeGuide, shortDescription, appName, mainImages, sectionImages)
        onHtmlReady(html)
    }


    private fun buildHtml(
        cityGuide: CityGuideModel,
        homeGuide: HomeGuideModel,
        shortDescription: String?,
        appName: String,
        mainImages: List<String>,                  // << ADD
        sectionImages: Map<String, List<String>>   // << RENAME (photos → sectionImages)
    )
            : String {

        val firstPageImages = mainImages.take(8)
        val remainingMainImages = mainImages.drop(8)

        val imagePagesAfterGuides = buildAllImagePages(
            remainingMainImages,
            sectionImages
        )

        val firstPageImageHtml = buildPaginatedImagePages(
            title = "Main Images",
            urls = firstPageImages,
            isFirst = true
        )



        return """
    <!doctype html>
    <html>
    <head>
        <meta charset="utf-8"/>
        <meta name="viewport" content="width=device-width,initial-scale=1"/>
        <title>Airbnb Guide</title>

        <style>
            @page { size: A4; margin: 0; }
            * { margin:0; padding:0; box-sizing:border-box; }
            body { font-family: sans-serif; background:#f5f5f6; color:#030d1c; }

            .wrap { width:100%; padding:18px; }


            /* FIXED TITLE VISIBILITY */
            .hero {
                background:#00a69a;
                color:white;
                border-radius:20px;
                padding:30px 20px;
                margin-bottom:24px;
                text-align:center; /* CENTERED TEXT */
            }
            .hero h1 {
                font-weight:700;
                font-size:26px;
                margin-bottom:12px;
                color:white;
            }
            .hero p {
                font-size:18px;
                color:white;
                opacity:1;
                line-height:1.4;
                white-space: pre-wrap;
            }

            .card {
                background:white;
                border-radius:14px;
                padding:18px;
                margin-bottom:32px;
                box-shadow:0 6px 18px rgba(0,0,0,0.08);
            }
            .card.text {
                page-break-inside: avoid;
            }
            h2 {
                background:#00a69a;
                color:white;
                padding:10px 14px;
                border-radius:10px;
                margin:-18px -18px 20px -18px;
                font-size:16px;
                font-weight:600;
            }
            .line { margin-bottom:16px; }
            .label { font-weight:600; font-size:15px; }
            .small { font-size:14px; color:#333; white-space: pre-wrap; line-height:1.5; }

            /* PHOTO GRID */
            
            .photo-box {
                padding:0;
                border-radius:0;
                display:flex;
                justify-content:center;
                align-items:center;
                width:100%;
                height:100%;
                overflow:hidden;
            }

            .photo-box img {
                max-width:100%;
                max-height:100%;
                object-fit:contain;
            }


            .footer {
                background:#00a69a;
                color:white;
                padding:12px;
                border-radius:10px;
                text-align:center;
                margin-top:24px;
            }
        </style>
    </head>

    <body>
    <div class="wrap">

        <!-- HERO WITH TITLE (FIXED) -->
        <div class="hero">
            <h1>${homeGuide.city ?: ""}</h1>
            <p>${shortDescription ?: ""}</p>
        </div>

        <!-- PHOTO SECTIONS → FIRST -->
        $firstPageImageHtml

        <!-- HOME GUIDE -->
        <div class="card text">
            <h2>Home Guide</h2>
            ${buildHomeGuideLines(homeGuide)}
        </div>
       <div style="page-break-before: always;"></div>
        <!-- CITY GUIDE -->
        <div class="card">
            <h2>City Guide</h2>
            ${buildCityGuideLines(cityGuide)}
        </div>
      

        $imagePagesAfterGuides

        <div class="footer">$appName</div>
    </div>
    </body>
    </html>
    """.trimIndent()
    }

    // ------------------------------------------------------------
    // PHOTO SECTIONS HTML BUILDER
    // ------------------------------------------------------------




    // ------------------------------------------------------------
    // LINE HELPERS
    // ------------------------------------------------------------
    private fun line(label: String, value: String?): String {
        if (value.isNullOrBlank()) return ""
        return """
        <div class="line" style="page-break-inside: avoid;">
            <div class="label">$label</div>
            <div class="small">$value</div>
        </div>
    """
    }

    suspend fun loadMainImages(ctx: Context): List<String> {
        val homeId = Prefs.getHomeId(ctx)
        val db = FirebaseFirestore.getInstance()

        val snap = db.collection("homes").document(homeId).get().await()

        // Log entire document
//        Log.d("PDF_MAIN", "Home snapshot = ${snap.data}")

        // Log the airBnbResponseModel map
        val airbnbMap = snap.get("airBnbResponseModel")
//        Log.d("PDF_MAIN", "airBnbResponseModel = $airbnbMap (${airbnbMap?.javaClass})")

        // Try to extract images with all possible types
        val imagesAny = snap.get("airBnbResponseModel.images")
//        Log.d("PDF_MAIN", "airBnbResponseModel.images = $imagesAny (${imagesAny?.javaClass})")

        // SAFE cast to List<String>
        val images = (imagesAny as? List<*>)?.filterIsInstance<String>()

        if (images == null) {
//            Log.e("PDF_MAIN", "Main images list is NULL or wrong type for home: $homeId")
            return emptyList()
        }

//        Log.d("PDF_MAIN", "Loaded main images count = ${images.size}")
        images.forEachIndexed { index, url ->
//            Log.d("PDF_MAIN", "[$index] → $url")
        }

        return images
    }




    private fun buildPaginatedImagePages(title: String, urls: List<String>, isFirst: Boolean = false): String {
        val sb = StringBuilder()

        sb.append("""
        <div class="card photo" ${if (!isFirst) "style=\"page-break-before:always;\"" else ""}>
            <h2>$title</h2>
            <table style="width:100%; border-spacing:0; page-break-inside:auto;">
    """)


        val rows = urls.chunked(2)
        for (row in rows) {
            sb.append("""
            <tr style="page-break-inside:avoid; page-break-after:auto;">
        """.trimIndent())

            for (url in row) {
                sb.append("""
                <td style="padding:6px; width:50%;">
                    <div class="photo-box" style="height:160px;">
                        ${if (url.isNotBlank())
                    """<img src="$url" />"""
                else
                    """<div></div>"""
                }
                    </div>
                </td>
            """)
            }

            sb.append("</tr>")
        }

        sb.append("""
            </table>
        </div>
    """)

        return sb.toString()
    }


    private fun buildAllImagePages(
        remainingMainImages: List<String>,
        sectionImages: Map<String, List<String>>
    ): String {
        val sb = StringBuilder()

        // Remaining main images (no heading — “More Photos” header is added outside)
        if (remainingMainImages.isNotEmpty()) {
            sb.append(buildPaginatedImagePages("More photos", remainingMainImages))
        }

        // All sections
        for (section in PhotoName.entries) {
            val images = sectionImages[section.value] ?: emptyList()
            if (images.isNotEmpty()) {
                sb.append(buildPaginatedImagePages(section.name, images))
            }
        }

        return sb.toString()
    }




    private fun buildCityGuideLines(guide: CityGuideModel): String = buildString {
        appendLine(line("Public Transport", guide.bestPublicTransportation))
        appendLine(line("Guidance", guide.transportationGuidance))
        appendLine(line("Costs", guide.transportationCosts))
        appendLine(line("Hours", guide.typicalStoreHours))
        appendLine(line("Apps", guide.usefulCityApps))
        appendLine(line("Do's & Don'ts", guide.dosAndDonts))
        appendLine(line("Food", guide.mustTryFoods))
        appendLine(line("Restaurants", guide.recommendedRestaurants))
        appendLine(line("Shopping", guide.popularShopping))
        appendLine(line("Places", guide.topPlacesToVisit))
        appendLine(line("Tips", guide.importantTips))
        appendLine(line("Personal", guide.personalRecommendations))
        appendLine(line("Unique", guide.uniqueExperiences))
        appendLine(line("Safety", guide.safetyEmergencyTips))
    }

    private fun buildHomeGuideLines(guide: HomeGuideModel): String = buildString {
        appendLine(line("City", guide.city))
        appendLine(line("Welcome Note", guide.welcomeNote))
        appendLine(line("Host", guide.hostIntro))
        appendLine(line("Contact Channel", guide.contactChannels.joinToString(", ")))
        appendLine(line("Contact Number", guide.contactNumber))
        appendLine(line("More Info", guide.moreContactInfo))
        appendLine(line("Emergency", guide.emergencyContact))
        appendLine(line("Type", guide.propertyType))
        appendLine(line("Occupancy", guide.occupancyLimit))
        appendLine(line("Description", guide.briefDescription))
        appendLine(line("Address", guide.propertyAddress))
        appendLine(line("Local Address", guide.propertyAddressLocal))
        appendLine(line("Map", guide.googleMapsLink))
        appendLine(line("From Airport", guide.howToReachAirport))
        appendLine(line("From Train", guide.howToReachTrain))
        appendLine(line("By Car", guide.howToReachCar))
        appendLine(line("How to Find", guide.howToFind))
        appendLine(line("Entry", guide.howToGetIn))
        appendLine(line("Code", guide.entranceCode))
        appendLine(line("Check-in", guide.checkInWindow))
        appendLine(line("Extra Access", guide.additionalAccess))
        appendLine(line("Floor", guide.floor))
        appendLine(line("Stairs/Elevator", guide.stairsElevator))
        appendLine(line("Parking", guide.parking))
        appendLine(line("Parking Notes", guide.parkingComments))
        appendLine(line("Wi-Fi", guide.wifiPassword))
        appendLine(line("Rules", guide.propertyRules))
        appendLine(line("Smoking", guide.smokingRules))
        appendLine(line("Pets", guide.petsPolicy))
        appendLine(line("Quiet Hours", guide.quietHours))
        appendLine(line("Lockout", guide.lockoutPolicy))
        appendLine(line("Check-out", guide.checkOutWindow))
        appendLine(line("Checklist", guide.departureChecklist))
        appendLine(line("Keys", guide.whereToLeaveKeys))
        appendLine(line("Facilities", guide.facilities))
        appendLine(line("Amenities", guide.amenities))
        appendLine(line("First Aid", guide.firstAid))
        appendLine(line("Trash", guide.wasteSorting))
        appendLine(line("Linens", guide.extraLinens))
        appendLine(line("Towels", guide.towels))
        appendLine(line("Cleaning", guide.cleaningTools))
        appendLine(line("Hair Dryer", guide.hairDryer))
        appendLine(line("AC/Fans", guide.acFans))
        appendLine(line("Washer", guide.washerDryer))
        appendLine(line("Kettle", guide.kettleCoffee))
        appendLine(line("BBQ", guide.bbqFireplace))
        appendLine(line("Hot Water", guide.hotWater))
        appendLine(line("Fire Escape", guide.fireEscape))
        appendLine(line("Hospital", guide.nearestHospital))
        appendLine(line("Pharmacy", guide.pharmacy))
        appendLine(line("Groceries", guide.groceries))
        appendLine(line("ATM", guide.atms))
        appendLine(line("SIM", guide.simCards))
        appendLine(line("Parks", guide.parks))
        appendLine(line("Metro", guide.metro))
        appendLine(line("Delivery", guide.foodDelivery))
        appendLine(line("Breakfast", guide.breakfast))
        appendLine(line("Lunch/Dinner", guide.lunchDinner))
        appendLine(line("Late Night", guide.lateNight))
        appendLine(line("Attractions", guide.topAttractions))
    }

    // ------------------------------------------------------------
    // PDF CONVERTER
    // ------------------------------------------------------------
    suspend fun generatePdf(
        activity: Activity,
        cityGuide: CityGuideModel,
        homeGuide: HomeGuideModel,
        shortDescription: String?,
        appName: String,
        outputFileName: String,
        sectionImages: Map<String, List<String>>,
        onPdfReady: (File) -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            // Suspend-friendly
            val mainImages = loadMainImages(activity)

            val pdfDir = activity.getExternalFilesDir(null) ?: activity.cacheDir
            val pdfFile = File(pdfDir, outputFileName)
            val writer = PdfWriter(FileOutputStream(pdfFile))

            val html = buildHtml(
                cityGuide,
                homeGuide,
                shortDescription,
                appName,
                mainImages,
                sectionImages
            )

            val props = ConverterProperties()
            props.baseUri = "file:///"

            // Heavy PDF conversion → run in background
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                HtmlConverter.convertToPdf(html, writer, props)
                writer.close()
            }

            activity.runOnUiThread {
                onPdfReady(pdfFile)
            }

        } catch (e: Exception) {
            activity.runOnUiThread { onError(e.message ?: "PDF error") }
        }
    }


    suspend fun cacheAndReturnLocalPaths(
        ctx: Context,
        photos: Map<String, List<String>>
    ): Map<String, List<String>> {

        val cacheDir = File(ctx.cacheDir, "pdf_photos")
        cacheDir.mkdirs()

        val result = mutableMapOf<String, List<String>>()

        for ((section, urls) in photos) {
            val localList = mutableListOf<String>()

            for (url in urls) {
                try {
                    val fileName = url.hashCode().toString() + ".jpg"
                    val localFile = File(cacheDir, fileName)

                    // Already downloaded → reuse
                    if (localFile.exists()) {
                        localList.add(localFile.absolutePath)
                        continue
                    }

                    // Download and save
                    val bytes = java.net.URL(url).readBytes()
                    localFile.writeBytes(bytes)

                    localList.add(localFile.absolutePath)

                } catch (e: Exception) {
//                    Log.e("PDF_CACHE", "Failed caching $url → ${e.message}")
                }
            }

            result[section] = localList
        }

        return result
    }

}
