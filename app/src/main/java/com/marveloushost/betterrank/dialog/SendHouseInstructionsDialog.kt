//package com.marveloushost.betterrank.dialog
//
//import android.app.Dialog
//import android.content.Context
//import android.content.Intent
//import android.os.Bundle
//import android.view.Gravity
//import android.view.View
//import android.view.ViewGroup
//import androidx.core.view.isVisible
//import androidx.fragment.app.DialogFragment
//import androidx.fragment.app.viewModels
//import androidx.lifecycle.LifecycleOwner
//import androidx.lifecycle.lifecycleScope
//import com.marveloushost.betterrank.R
//import com.marveloushost.betterrank.databinding.DialogSendInstructionsBinding
//import com.marveloushost.betterrank.instructionstranslation.InstructionsTranslationService
//import com.marveloushost.betterrank.instructionstranslation.TranslationRepositoryInstructions
//import com.marveloushost.betterrank.model.GuestModel
//import com.marveloushost.betterrank.model.HomeGuideModel
//import com.marveloushost.betterrank.viewModel.SendInstructionsViewModel
//import dagger.hilt.android.AndroidEntryPoint
//import kotlinx.coroutines.launch
//import javax.inject.Inject
//
//@AndroidEntryPoint
//class SendHouseInstructionsDialog : DialogFragment() {
//
//    private val vm: SendInstructionsViewModel by viewModels()
//    private lateinit var binding: DialogSendInstructionsBinding
//
//    @Inject
////    lateinit var repo: TranslationRepositoryInstructions
//
//    private var onSend: ((String, List<String>) -> Unit)? = null
//
//    private val guest by lazy {
//        requireArguments().getParcelable<GuestModel>("guest")!!
//    }
//    private val homeGuide by lazy {
//        requireArguments().getParcelable<HomeGuideModel>("guide")!!
//    }
//
//    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
//        val dialog = Dialog(requireContext())
//        binding = DialogSendInstructionsBinding.inflate(layoutInflater)
//        dialog.setContentView(binding.root)
//
//        dialog.window?.setBackgroundDrawableResource(R.drawable.bg_roundedwhite)
//
//        binding.vm = vm
//        binding.guestName = guest.name
//        binding.guestLanguage = guest.language ?: "Guest Language"
//
//        setupLanguage()
//        setupButtons()
//
//
//        // Restore previous translation session (if any)
//        repo.restoreState()
//        restoreIfTranslating()
//        if (preTranslatedText != null) {
//            binding.translationOverlay.isVisible = false
//            disableUiDuringTranslation()
//            binding.btnSend.isEnabled = true
//            binding.btnSend.alpha = 1f
//            binding.btnSend.setTextColor(0xFF000000.toInt())
//        }
//        repo.translatedText.value?.let { translated ->
//            enterReadyMode(translated)
//        }
//
//
//        return dialog
//    }
//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//        setupObservers()   // ✅ SAFE HERE
//    }
//
//
//    // -------------------------------------------------------------------------
//    // LANGUAGE LOGIC (English vs Guest Language)
//    // -------------------------------------------------------------------------
//    private fun setupLanguage() {
//        val guestLang = guest.language ?: "English"
//
//        binding.rbEnglish.isChecked = true
//
//        if (guestLang.equals("english", ignoreCase = true)) {
//            binding.rgLanguage.visibility = View.GONE
//        } else {
//            binding.rgLanguage.visibility = View.VISIBLE
//            binding.rbGuestLang.text = guestLang
//        }
//    }
//
//    // -------------------------------------------------------------------------
//    // BUTTON STATE HANDLING
//    // -------------------------------------------------------------------------
//    private fun setupButtons() {
//        binding.btnSend.setOnClickListener { handleSendClick() }
//        binding.btnTranslate.setOnClickListener { startTranslationFlow() }
//        setupEnableLogic()
//    }
//
//    private fun handleSendClick() {
//        val titles = getSelectedTitles()
//
//        val textToSend = repo.translatedText.value ?: vm.buildMessage(homeGuide, binding)
//        repo.stopTranslation()
//
//        dismiss()
//        onSend?.invoke(textToSend, titles)
//    }
//
//    private fun setupEnableLogic() {
//
//        fun updateStates() {
//            val anySelected =
//                listOf(
//                    binding.cb1, binding.cb2, binding.cb3, binding.cb4, binding.cb5,
//                    binding.cb6, binding.cb7, binding.cb8, binding.cb9
//                ).any { it.isChecked }
//
//            val isGuestLang = binding.rbGuestLang.isChecked && binding.rgLanguage.isVisible
//            val isEnglish = binding.rbEnglish.isChecked
//
//            val hasTranslation = repo.translatedText.value != null
//
//            // --- SEND BUTTON LOGIC ---
//            binding.btnSend.isEnabled =
//                anySelected && (
//                        isEnglish || (isGuestLang && hasTranslation)
//                        )
//
//            binding.btnSend.alpha = if (binding.btnSend.isEnabled) 1f else 0.3f
//            binding.btnSend.setTextColor(
//                if (binding.btnSend.isEnabled) 0xFFFFFFFF.toInt() else 0xFF000000.toInt()
//            )
//
//            // --- TRANSLATE BUTTON LOGIC ---
//            val translateEnabled = anySelected && isGuestLang && !hasTranslation
//            binding.btnTranslate.isEnabled = translateEnabled
//            binding.btnTranslate.alpha = if (translateEnabled) 1f else 0.3f
//            binding.btnTranslate.setTextColor(
//                if (translateEnabled) 0xFFFFFFFF.toInt() else 0xFF000000.toInt()
//            )
//        }
//
//
//        val checkboxes = listOf(
//            binding.cb1, binding.cb2, binding.cb3, binding.cb4, binding.cb5,
//            binding.cb6, binding.cb7, binding.cb8, binding.cb9
//        )
//
//        checkboxes.forEach { cb ->
//            cb.setOnCheckedChangeListener { _, _ -> updateStates() }
//        }
//
//        binding.rgLanguage.setOnCheckedChangeListener { _, _ -> updateStates() }
//
//        updateStates()
//    }
//
//    // -------------------------------------------------------------------------
//    // TRANSLATION START
//    // -------------------------------------------------------------------------
//    private fun startTranslationFlow() {
//
//        val rawText = vm.buildMessage(homeGuide, binding)
//        val lang = binding.rbGuestLang.text.toString()
//
//        repo.startTranslation(rawText, lang)
//
//        Intent(requireContext(), InstructionsTranslationService::class.java).apply {
//            putExtra("rawText", rawText)
//            putExtra("lang", lang)
//            requireContext().startService(this)
//        }
//
//        showTranslationBox()
//        disableUiDuringTranslation()
//    }
//
//    // -------------------------------------------------------------------------
//    // OBSERVERS FOR COUNTDOWN & RESULT
//    // -------------------------------------------------------------------------
//    private fun setupObservers() {
//        observe(repo.countdown) { value ->
//            if (repo.isTranslating.value) {
//                binding.tvCounter.text = value.toString()
//
//            }
//        }
//
//        observe(repo.isTranslating) { active ->
//            binding.translationOverlay.isVisible = active
//        }
//
//        observe(repo.translatedText) { translated ->
//            if (translated != null) {
//                enterReadyMode(translated)
//            }
//        }
//    }
//
//    private fun <T> observe(flow: kotlinx.coroutines.flow.StateFlow<T>, block: (T) -> Unit) {
//        viewLifecycleOwner.lifecycleScope.launch {
//            flow.collect { block(it) }
//        }
//    }
//
//    // -------------------------------------------------------------------------
//    // RESTORE IF A TRANSLATION IS ALREADY RUNNING
//    // -------------------------------------------------------------------------
//    private fun restoreIfTranslating() {
//        if (repo.isTranslating.value) {
//            showTranslationBox()
//            disableUiDuringTranslation()
//        }
//    }
//
//    private fun showTranslationBox() {
//        binding.translationOverlay.isVisible = true
//        binding.translationBoxRoot.isVisible = true
//    }
//
//    private fun disableUiDuringTranslation() {
//        binding.cb1.isEnabled = false
//        binding.cb2.isEnabled = false
//        binding.cb3.isEnabled = false
//        binding.cb4.isEnabled = false
//        binding.cb5.isEnabled = false
//        binding.cb6.isEnabled = false
//        binding.cb7.isEnabled = false
//        binding.cb8.isEnabled = false
//        binding.cb9.isEnabled = false
//
//        binding.rbEnglish.isEnabled = false
//        binding.rbGuestLang.isEnabled = false
//    }
//
//    // -------------------------------------------------------------------------
//    // SELECTED TITLES
//    // -------------------------------------------------------------------------
//    private fun getSelectedTitles(): List<String> {
//        val titles = listOf(
//            "Host & Welcome",
//            "Contact Info",
//            "Property Basics",
//            "Address & Access",
//            "Check-in & Access",
//            "House Rules",
//            "Check-out",
//            "Amenities",
//            "Neighborhood Info"
//        )
//
//        return titles.filterIndexed { index, _ ->
//            listOf(
//                binding.cb1, binding.cb2, binding.cb3, binding.cb4, binding.cb5,
//                binding.cb6, binding.cb7, binding.cb8, binding.cb9
//            )[index].isChecked
//        }
//    }
//
//    // -------------------------------------------------------------------------
//    // IF BACK BUTTON IS PRESSED → GO TO FRAGMENT, NOT CLOSE DIALOG
//    // -------------------------------------------------------------------------
//    override fun onStart() {
//        super.onStart()
//        dialog?.window?.setLayout(
//            ViewGroup.LayoutParams.MATCH_PARENT,
//            ViewGroup.LayoutParams.WRAP_CONTENT
//        )
//        dialog?.window?.setGravity(Gravity.CENTER)
//    }
//
//    override fun onResume() {
//        super.onResume()
//
//        dialog?.setOnKeyListener { _, keyCode, event ->
//            if (repo.isTranslating.value &&
//                keyCode == android.view.KeyEvent.KEYCODE_BACK &&
//                event.action == android.view.KeyEvent.ACTION_UP) {
//
//                // Don't dismiss dialog — let activity handle the back navigation
//                requireActivity().onBackPressedDispatcher.onBackPressed()
//                true
//            } else false
//        }
//    }
//
//
//    private var preTranslatedText: String? = null
//
//    fun setPreTranslatedText(txt: String) {
//        preTranslatedText = txt
//    }
//
//    fun setCallback(cb: (String, List<String>) -> Unit): SendHouseInstructionsDialog {
//        this.onSend = cb
//        return this
//    }
//    private fun enterReadyMode(translated: String) {
//
//        // Hide translation overlay (countdown box)
//        binding.translationOverlay.isVisible = false
//        binding.translationBoxRoot.isVisible = false
//
//        // Disable all checkboxes
//        listOf(
//            binding.cb1, binding.cb2, binding.cb3, binding.cb4, binding.cb5,
//            binding.cb6, binding.cb7, binding.cb8, binding.cb9
//        ).forEach { it.isEnabled = false }
//
//        // Disable language selection
//        binding.rbEnglish.isEnabled = false
//        binding.rbGuestLang.isEnabled = false
//        binding.rgLanguage.isEnabled = false
//
//        // Hide the translate button
//        binding.btnTranslate.visibility = View.GONE
//
//        // Enable send button
//        binding.btnSend.isEnabled = true
//        binding.btnSend.alpha = 1f
//        binding.btnSend.setTextColor(0xFFFFFFFF.toInt())
//    }
//
//
//    companion object {
//        fun newInstance(
//            guest: GuestModel,
//            guide: HomeGuideModel
//        ): SendHouseInstructionsDialog {
//            val f = SendHouseInstructionsDialog()
//            val b = Bundle()
//            b.putParcelable("guest", guest)
//            b.putParcelable("guide", guide)
//            f.arguments = b
//            return f
//        }
//    }
//
//
//
//}
