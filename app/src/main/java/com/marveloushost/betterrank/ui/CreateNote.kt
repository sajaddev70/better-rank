package com.marveloushost.betterrank.ui


import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.EditText
import androidx.activity.viewModels
import androidx.databinding.DataBindingUtil
import com.marveloushost.betterrank.mediaManager.SpeechToTextManager
import com.marveloushost.betterrank.R
import com.marveloushost.betterrank.databinding.CreateNoteBinding
import com.marveloushost.betterrank.dialog.ReminderManagerDialog
import com.marveloushost.betterrank.model.NoteModel
import com.marveloushost.betterrank.permissionManager.PermissionManager
import com.marveloushost.betterrank.viewModel.CreateNoteViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CreateNote:BaseActivity(), SpeechToTextManager.OnSpeechResultListener {

    val viewModel: CreateNoteViewModel by viewModels()
    val binding: CreateNoteBinding by lazy { DataBindingUtil.setContentView(this, R.layout.create_note) }
    private val noteModel: NoteModel? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("note", NoteModel::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra<NoteModel>("note")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initView()
        onClickView()

        //initData
        viewModel.initNote(noteModel?:NoteModel())
        checkAndRequestNotificationPermission()
    }

    override fun initView() {

        //ViewModel
        binding.viewModel=viewModel
        binding.lifecycleOwner=this

        //ActionBar
        binding.baseActionBar.txtTitle.text=getString(R.string.create_note)
        binding.baseActionBar.imgStart.visibility=View.VISIBLE

    }

    override fun onClickView() {

        viewModel.showMessage.observe(this) { value ->
            showMessage(value)
        }

        viewModel.createNoteResult.observe(this){
            val intent=Intent().apply {
                putExtra("action","create")
                putExtra("note",it)
            }
            setResult(RESULT_OK,intent)
            showMessage("Your Note Created")
            finish()
        }

        viewModel.updateNoteResult.observe(this){
            val intent=Intent().apply {
                putExtra("action","update")
                putExtra("note",it)
            }
            setResult(RESULT_OK,intent)
            showMessage("Your Note Updated")
            finish()
        }



        binding.baseActionBar.imgStart.setOnClickListener {
            finish()
        }

        binding.switchReminded.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked)
                checkAndRequestNotificationPermission()
            viewModel.reminded.value=isChecked
        }

        binding.liStartAt.setOnClickListener {
            val reminderManagerDialog=ReminderManagerDialog(onAddReminder = {
                viewModel.startAt.value= it
            })
            reminderManagerDialog.show(supportFragmentManager,ReminderManagerDialog::class.java.name)
        }

        binding.imgMic.setOnClickListener {
            if (PermissionManager.isPermissionGranted(this, Manifest.permission.RECORD_AUDIO)) {
                SpeechToTextManager.startListening(applicationContext, this)
            }
            else {
                PermissionManager.requestPermission(this, Manifest.permission.RECORD_AUDIO, PermissionManager.PERMISSION_REQUEST_CODE)
            }
        }
    }

    private fun checkAndRequestNotificationPermission(){
        if(!PermissionManager.isPermissionGranted(this, Manifest.permission.POST_NOTIFICATIONS))
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                PermissionManager.requestPermission(this, Manifest.permission.POST_NOTIFICATIONS, PermissionManager.PERMISSION_REQUEST_CODE)
            }
    }

    override fun onSpeechReady() {

        binding.etMessage.setText("")
        binding.voiceWaveView.setBarColor(getColor(R.color.colorSecondary))
        Handler(Looper.getMainLooper()).postDelayed({
            binding.voiceWaveView.reset()
            binding.voiceWaveView.visibility=View.VISIBLE
        }, 1000)

    }

    override fun onSpeechBeginning() {
    }

    override fun onSpeechRmsChanged(p0: Float) {
        binding.voiceWaveView.updateRms(p0)
    }

    override fun onSpeechResult(result: String) {
        binding.voiceWaveView.visibility=View.GONE
        animateTyping(binding.etMessage,result)

    }

    override fun onSpeechError(errorMessage: String) {
        binding.voiceWaveView.visibility= View.GONE
    }

    private fun animateTyping(editText: EditText, text: String, delay: Long = 30L) {
        val handler = Handler(Looper.getMainLooper())
        editText.setText("")
        var index = 0
        handler.post(object : Runnable {
            override fun run() {
                if (index <= text.length) {
                    editText.setText(text.substring(0, index))
                    index++
                    handler.postDelayed(this, delay)
                }
            }
        })
    }
}