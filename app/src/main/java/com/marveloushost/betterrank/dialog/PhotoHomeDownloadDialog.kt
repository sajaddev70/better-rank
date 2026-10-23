package com.marveloushost.betterrank.dialog

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.marveloushost.betterrank.R
import com.marveloushost.betterrank.adapter.PhotoHomeAdapter
import com.marveloushost.betterrank.databinding.PhotoHomeDownloadDialogBinding
import com.marveloushost.betterrank.model.PhotoModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class PhotoHomeDownloadDialog(
    private val approvedPhotos: List<PhotoModel>,
    private val onDownload: (PhotoModel) -> Unit
) : BottomSheetDialogFragment() {

    private lateinit var binding: PhotoHomeDownloadDialogBinding
    private val selectedPhotos = mutableSetOf<PhotoModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.CustomBottomSheetDialogTheme)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.photo_home_download_dialog, container, false)
        binding.dialog = this
        binding.selectedCount = 0
        binding.totalCount = approvedPhotos.size
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.post {
            val bottomSheet = dialog?.findViewById<View>(
                com.google.android.material.R.id.design_bottom_sheet
            )
            bottomSheet?.let {
                val behavior = BottomSheetBehavior.from(it)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.skipCollapsed = true
                behavior.isDraggable = true
            }
        }
        setupAdapter()
    }

    private fun setupAdapter() {
        val adapter = PhotoHomeAdapter(
            list = approvedPhotos,
            selectedItems = selectedPhotos,
            onItemToggled = { photo, selected ->
                if (selected) selectedPhotos.add(photo) else selectedPhotos.remove(photo)
                updateSelectionUI()

                // FIX:
                val index = approvedPhotos.indexOf(photo)
                if (index != -1) binding.recyclerPhotos.adapter?.notifyItemChanged(index)
            },
            onDownload = { } // not used here
        )
        binding.recyclerPhotos.adapter = adapter
        updateSelectionUI()
    }

    private fun updateSelectionUI() {
        val count = selectedPhotos.size
        binding.selectedCount = count
        binding.executePendingBindings() // important!
    }

    fun onSelectAllClicked(checked: Boolean) {
        if (checked) selectedPhotos.addAll(approvedPhotos) else selectedPhotos.clear()
        updateSelectionUI()
        binding.recyclerPhotos.adapter?.notifyDataSetChanged()
    }

    fun onDownloadSelected() {
        selectedPhotos.forEach { onDownload(it) }
        dismiss()
    }

    fun onShareSelected() {
        if (selectedPhotos.isEmpty()) return

        lifecycleScope.launch(Dispatchers.IO) {
            val uris = selectedPhotos.mapNotNull { photo ->
                photo.url?.let { url ->
                    downloadToCache(url)?.let { file ->
                        FileProvider.getUriForFile(
                            requireContext(),
                            "${requireContext().packageName}.fileprovider",
                            file
                        )
                    }
                }
            }

            withContext(Dispatchers.Main) {
                if (uris.isNotEmpty()) {
                    val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                        type = "image/*"
                        putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    startActivity(Intent.createChooser(intent, "Share ${uris.size} photos"))
                }
            }
        }
    }

    private fun downloadToCache(url: String): File? = try {
        val file = File.createTempFile("share_", ".jpg", requireContext().cacheDir)
        java.net.URL(url).openStream().use { input ->
            file.outputStream().use { output -> input.copyTo(output) }
        }
        file
    } catch (e: Exception) {
        null
    }
    fun setEnabledAlphaColor(button: Button, isEnabled: Boolean) {
        button.isEnabled = isEnabled

        // Alpha
        button.alpha = if (isEnabled) 1f else 0.3f

        // Text color
        val colorRes = if (isEnabled) R.color.white else R.color.black
        button.setTextColor(ContextCompat.getColor(button.context, colorRes))
    }
}