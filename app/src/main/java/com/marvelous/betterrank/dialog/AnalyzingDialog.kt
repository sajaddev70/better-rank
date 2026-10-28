//package com.marvelous.betterrank.dialog
//
//import android.animation.ValueAnimator
//import android.os.Bundle
//import android.os.CountDownTimer
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import android.widget.ProgressBar
//import android.widget.TextView
//import androidx.fragment.app.DialogFragment
//import com.marvelous.betterrank.R
//
//class AnalyzingDialog : DialogFragment() {
//
//    private lateinit var tvMessage: TextView
//    private lateinit var tvCountdown: TextView
//    private var timer: CountDownTimer? = null
//
//    override fun onCreateView(
//        inflater: LayoutInflater,
//        container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View? {
//        return inflater.inflate(R.layout.dialog_analyzing, container, false)
//    }
//
//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//
//        // Find views manually
//        tvMessage = view.findViewById(R.id.tv_message)
//        tvCountdown = view.findViewById(R.id.tv_countdown)
//        val progressBar = view.findViewById<ProgressBar>(R.id.progress_bar)
//
//        isCancelable = false
//        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
//
//        startEllipsisAnimation()
//        startCountdown()
//    }
//
//    private fun startEllipsisAnimation() {
//        val baseText = "Analyzing"
//        val animator = ValueAnimator.ofInt(0, 3).apply {
//            duration = 1200
//            repeatMode = ValueAnimator.RESTART
//            repeatCount = ValueAnimator.INFINITE
//            addUpdateListener { anim ->
//                val dots = ".".repeat((anim.animatedValue as Int) + 1)
//                tvMessage.text = "$baseText$dots"
//            }
//        }
//        animator.start()
//    }
//
//    private fun startCountdown() {
//        timer = object : CountDownTimer(30_000, 1_000) {
//            override fun onTick(millisUntilFinished: Long) {
//                val seconds = millisUntilFinished / 1000
//                tvCountdown.text = seconds.toString()
//            }
//
//            override fun onFinish() {
//                tvCountdown.text = "0"
//                dismissAllowingStateLoss()
//            }
//        }.start()
//    }
//
//    override fun onDestroyView() {
//        timer?.cancel()
//        timer = null
//        super.onDestroyView()
//    }
//}