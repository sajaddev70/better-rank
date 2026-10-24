package com.marveloushost.betterrank.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.IntentCompat
import androidx.lifecycle.lifecycleScope
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore
import com.marveloushost.betterrank.R
import com.marveloushost.betterrank.instructionstranslation.TranslationRepositoryInstructions
import com.marveloushost.betterrank.model.GuestModel
import com.marveloushost.betterrank.model.HomeGuideModel
import com.marveloushost.betterrank.model.MessageHistoryModel
import com.marveloushost.betterrank.model.MessageItem
import com.marveloushost.betterrank.util.DateUtils
import com.marveloushost.betterrank.util.Prefs
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

@AndroidEntryPoint
class MessagingActivity : AppCompatActivity() {

    @Inject
    lateinit var repo: TranslationRepositoryInstructions


    private lateinit var guest: GuestModel
    private lateinit var homeGuide: HomeGuideModel
    private lateinit var messagesContainer: LinearLayout
    private lateinit var scrollMessages: ScrollView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applySystemBarInsets()
        setContentView(R.layout.activity_messaging)

        // Load guest
        guest = IntentCompat.getParcelableExtra(intent, "GUEST_OBJECT", GuestModel::class.java)
            ?: throw IllegalStateException("Guest not passed")


        // Bind UI
        findViewById<TextView>(R.id.tvGuestName).text = guest.name
        messagesContainer = findViewById(R.id.messagesContainer)
        scrollMessages = findViewById(R.id.scrollMessages)


        loadHomeGuide()
        loadMessageHistory()

//        lifecycleScope.launch {
//            repo.restoreState()
//
//            repo.translatedText.collect { translated ->
//                if (translated != null && !repo.isTranslating.value) {
//
//                    openInstructionsDialogWithTranslatedText(translated)
//
//                    repo.consumeTranslatedText()   // <-- prevents double-open
//                }
//            }
//        }




        // ---- CLICK SEND BUBBLE ----
        findViewById<View>(R.id.pillSendInstructions).setOnClickListener {
            if (!::homeGuide.isInitialized) return@setOnClickListener

            if (repo.isTranslating.value &&
                repo.currentGuestId.value != guest.id)
            {
                Toast.makeText(
                    this,
                    "Translating instructions for another guest…",
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }


            // ✔ Allowed → open translation selector
            val intent = Intent(this, InstructionTranslationActivity::class.java)
            intent.putExtra("guest", guest)
            intent.putExtra("guide", homeGuide)
            translationLauncher.launch(intent)
        }






        // Back button
        findViewById<View>(R.id.btnBack).setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }


    }


    private val translationLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data
        if (result.resultCode == RESULT_OK && data != null) {

            val msg = data.getStringExtra("messageText") ?: return@registerForActivityResult
            val titles = data.getStringArrayListExtra("titles") ?: arrayListOf()

            sendWhatsAppMessage(guest.phoneNumber, msg)
            saveMessageHistory(titles)

            titles.forEach {
                addSentMessage("Sent $it", getCurrentTime())
            }
        }
    }








    private fun addSentMessage(text: String, time: String) {
        val view = layoutInflater.inflate(R.layout.item_sent_message, null)
        view.findViewById<TextView>(R.id.tvMessage).text = text
        view.findViewById<TextView>(R.id.tvTime).text = time



        messagesContainer.addView(view)

        scrollMessages.post {
            scrollMessages.fullScroll(View.FOCUS_DOWN)
        }
    }
    fun sendWhatsAppMessage(phone: String, msg: String) {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = Uri.parse("https://wa.me/$phone?text=" + Uri.encode(msg))
        startActivity(intent)
    }

    private fun showSendHouseGuidePopup() {
        // This will be implemented NEXT
    }
    private fun getCurrentTime(): String {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        return sdf.format(Date())
    }
    private fun saveMessageHistory(titles: List<String>) {

        val userId = FirebaseAuth.getInstance().currentUser!!.uid

        val history = MessageHistoryModel(
            id = UUID.randomUUID().toString(),
            guestId = guest.id,
            titles = titles,
            sentAt = System.currentTimeMillis()
        )

//        Log.d("SAVE_HISTORY", "Saving message → user=$userId, guest=${guest.id}, data=$history")

        Firebase.firestore
            .collection("users")
            .document(userId)
            .collection("guests")
            .document(guest.id)
            .collection("messages")
            .document(history.id)
            .set(history)
            .addOnSuccessListener {
//                Log.d("SAVE_HISTORY", "Success!")
            }
            .addOnFailureListener { e ->
//                Log.e("SAVE_HISTORY", "Failed: ", e)
            }
    }

    fun buildUIMessageList(raw: List<MessageHistoryModel>): List<MessageItem> {
        val result = mutableListOf<MessageItem>()
        var lastDate = ""

        for (m in raw.sortedBy { it.sentAt }) {

            val date = DateUtils.formatMessageDate(m.sentAt)

            if (date != lastDate) {
                result += MessageItem.DateSeparator(date)
                lastDate = date
            }

            m.titles.forEach { title ->
                result += MessageItem.MessageBubble(
                    title = title,
                    timestamp = m.sentAt
                )
            }
        }

        return result
    }
    private fun loadHomeGuide() {
        val homeId = Prefs.getHomeId(this)

        Firebase.firestore
            .collection("homes")
            .document(homeId)
            .get()
            .addOnSuccessListener { snap ->
                homeGuide = snap.get("homeGuide", HomeGuideModel::class.java)
                    ?: HomeGuideModel()

//                Log.d("HOME_GUIDE", "Loaded: $homeGuide")

                findViewById<View>(R.id.pillSendInstructions).isEnabled = true
            }
    }
    private fun loadMessageHistory() {
        val userId = FirebaseAuth.getInstance().currentUser!!.uid

        Firebase.firestore
            .collection("users")
            .document(userId)
            .collection("guests")
            .document(guest.id)
            .collection("messages")
            .orderBy("sentAt")
            .get()
            .addOnSuccessListener { snap ->
                val list = snap.toObjects(MessageHistoryModel::class.java)

                val uiItems = buildUIMessageList(list)

                messagesContainer.removeAllViews()

                uiItems.forEach { item ->
                    when (item) {

                        is MessageItem.DateSeparator -> {
                            val sep = layoutInflater.inflate(R.layout.item_date_separator, null)
                            sep.findViewById<TextView>(R.id.tvDate).text = item.date
                            messagesContainer.addView(sep)
                        }

                        is MessageItem.MessageBubble -> {
                            val bubble = layoutInflater.inflate(R.layout.item_sent_message, null)
                            bubble.findViewById<TextView>(R.id.tvMessage).text = item.title
                            bubble.findViewById<TextView>(R.id.tvTime).text =
                                SimpleDateFormat("HH:mm", Locale.getDefault())
                                    .format(Date(item.timestamp))

                            messagesContainer.addView(bubble)
                        }
                    }
                }

                scrollMessages.post {
                    scrollMessages.fullScroll(View.FOCUS_DOWN)
                }
            }
    }





}
