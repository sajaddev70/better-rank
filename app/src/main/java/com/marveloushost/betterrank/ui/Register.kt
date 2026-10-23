package com.marveloushost.betterrank.ui

import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.databinding.DataBindingUtil
import com.marveloushost.betterrank.R
import com.marveloushost.betterrank.customView.TakePhoto
import com.marveloushost.betterrank.databinding.RegisterBinding
import com.marveloushost.betterrank.dialog.ChangeProfileDialog
import com.marveloushost.betterrank.fileManager.FilePickerHelper
import com.marveloushost.betterrank.fileManager.ImageHelper
import com.marveloushost.betterrank.ui.Login.Companion.EMAIL
import com.marveloushost.betterrank.ui.Login.Companion.NAME
import com.marveloushost.betterrank.ui.Login.Companion.PHOTO
import com.marveloushost.betterrank.ui.Login.Companion.UID
import com.marveloushost.betterrank.viewModel.RegisterViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class Register:BaseActivity(), ChangeProfileDialog.OnChangeProfileDialogListener {

    val viewModel: RegisterViewModel by viewModels()
    val binding: RegisterBinding by lazy { DataBindingUtil.setContentView(this, R.layout.register) }
    private lateinit var filePickerHelper: FilePickerHelper
    private lateinit var imageHelper: ImageHelper
    private var imagePath:String? = null
    private val uid: String? by lazy { intent.getStringExtra(UID) }
    private val email: String? by lazy { intent.getStringExtra(EMAIL) }
    private val name: String? by lazy { intent.getStringExtra(NAME) }
    private val photo: String? by lazy { intent.getStringExtra(PHOTO) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initView()
        onClickView()
    }

    override fun initView() {

        //ViewModel
        binding.viewModel=viewModel
        binding.lifecycleOwner=this

        //ActionBar
        binding.baseActionBar.imgStart.visibility=View.VISIBLE

        //EditText
        binding.etFullName.setText(name?:"")

        //ImageView
        DataBindingAdapter.imageLoadByUrl(binding.imgProfile,photo)

        //ImageHelper
        imageHelper=ImageHelper(this)

        //FilePicker
        filePickerHelper = FilePickerHelper(
            context = this,
            activityResultRegistry = this.activityResultRegistry,
            onFilePicked = { uri, name, stream ,realPath->
                setImagePath(ImageHelper(this).compressImageFromPath(inputPath = realPath))
            },
            onError = {
                showMessage(it)
            }
        )
        filePickerHelper.register()

    }

    override fun onClickView() {

        viewModel.showMessage.observe(this) { value ->
            showMessage(value)
        }

        viewModel.userModel.observe(this) {
            val intent=Intent(this@Register,HomeRegister::class.java)
            intent.putExtra(UID,it!!.uid)
            startActivity(intent)
            finish()
        }

        binding.baseActionBar.imgStart.setOnClickListener {
            startActivity(Intent(this@Register,Login::class.java))
            finish()
        }

        binding.clProfile.setOnClickListener {
            val changeProfileDialog=ChangeProfileDialog()
            changeProfileDialog.show(supportFragmentManager,ChangeProfileDialog::class.simpleName)
            changeProfileDialog.onChangeProfileDialogListener=this
        }

        binding.register.setOnClickListener {

            if (imagePath==null)
            {
                viewModel.validationAndCreateUser(uid!!,binding.etFullName.text.toString(),email!!,photo,null)
            }
            else
            {
                viewModel.validationAndCreateUser(uid!!,binding.etFullName.text.toString(),email!!,photo,
                    imageHelper.getUriFromFilePath(imagePath!!))
            }
        }
    }

    override fun onCamera() {
        launcher.launch(Intent(this@Register, TakePhoto::class.java))
    }

    override fun onGallery() {
        filePickerHelper.pickFile("image/*")
    }

    override fun onAvatar(image: Int) {
        binding.imgProfile.setImageResource(image)
        setImagePath(imageHelper.getFileFromDrawable(image).absolutePath)
    }


    private var launcher: ActivityResultLauncher<Intent> = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val photoPath = result.data?.getStringExtra("photoPath")
            if (photoPath != null) {
                setImagePath(photoPath)
            }
        }
    }

    private fun setImagePath(path:String?){
        imagePath=path
        binding.imgProfile.setImageBitmap(BitmapFactory.decodeFile(imagePath))
    }


}