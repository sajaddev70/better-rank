package com.marveloushost.betterrank.dialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import com.marveloushost.betterrank.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.marveloushost.betterrank.guestdataextraction.ImageExtractionController


class GuestAddBookingBottomSheet : BottomSheetDialogFragment() {
    private var loadingView: View? = null


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d("Respina", "🧩 Inflating AddBooking bottom sheet layout")
        return inflater.inflate(
            R.layout.guest_add_booking_bottomsheet,
            container,
            false
        )
    }

    private val pickImage =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            Log.d("Respina", "📸 Image picker result → uri=$uri")

            uri?.let {
                showLoading()

                ImageExtractionController.processImage(
                    context = requireContext(),
                    imageUri = it,
                    onFinished = {
                        hideLoading()
                        dismiss()
                    },
                    onError = {
                        hideLoading()
                    }
                )
            }
        }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        Log.d("Respina", "🟢 AddBooking BottomSheet opened")

        view.findViewById<View>(R.id.btnUpload).setOnClickListener {
            Log.d("Respina", "⬆️ Upload Photo clicked")
            pickImage.launch("image/*")
        }

        view.findViewById<View>(R.id.btnClose).setOnClickListener {
            Log.d("Respina", "❌ BottomSheet closed")
            dismiss()
        }
    }
    override fun getTheme() = R.style.CustomBottomSheetDialogTheme
    private fun showLoading() {
        dialog?.setCancelable(false)

        val root = dialog?.window?.decorView as? ViewGroup ?: return

        if (loadingView == null) {
            loadingView = layoutInflater.inflate(
                R.layout.loading_fullscreen,
                root,
                false
            )
            root.addView(loadingView)
        }
    }

    private fun hideLoading() {
        dialog?.setCancelable(true)

        val root = dialog?.window?.decorView as? ViewGroup ?: return
        loadingView?.let {
            root.removeView(it)
            loadingView = null
        }
    }

}
