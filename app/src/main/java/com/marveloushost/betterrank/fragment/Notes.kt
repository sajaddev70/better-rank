package com.marveloushost.betterrank.fragment

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity.RESULT_OK
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.viewModels
import com.marveloushost.betterrank.R
import com.marveloushost.betterrank.adapter.NoteAdapter
import com.marveloushost.betterrank.databinding.NotesBinding
import com.marveloushost.betterrank.model.NoteModel
import com.marveloushost.betterrank.permissionManager.PermissionManager
import com.marveloushost.betterrank.reminder.ReminderScheduler
import com.marveloushost.betterrank.ui.CreateNote
import com.marveloushost.betterrank.viewModel.NotesViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class Notes:BaseFragment() {

    lateinit var binding: NotesBinding
    val viewModel:NotesViewModel by viewModels()
    private var notes=mutableListOf<NoteModel>()
    private lateinit var noteAdapter:NoteAdapter
    private var selectPosition=-1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {

        binding= DataBindingUtil.inflate(layoutInflater, R.layout.notes,container,false)

        initView()
        onClick()

        return binding.root
    }

    override fun initView() {

        //ViewModel
        binding.viewModel=viewModel
        binding.lifecycleOwner=this
    }

    override fun onClick() {

        viewModel.showMessage.observe(viewLifecycleOwner){
            showMessage(it)
        }

        viewModel.resultGetNotes.observe(viewLifecycleOwner){
            notes=it
            noteAdapter= NoteAdapter(
                notes,
                viewModel,
                onEdit = { note, position->
                    selectPosition=position
                    launcher.launch(Intent(requireContext(),CreateNote::class.java).apply {
                        putExtra("action","create")
                        putExtra("note",note)
                    })
                },
                onDelete = {id,position->
                    selectPosition=position
                    viewModel.deleteNote(id)
                }
            )
            binding.recyclerNotes.apply {
                adapter=noteAdapter
            }
            updateReminderScheduler()
        }

        viewModel.resultDeleteNote.observe(viewLifecycleOwner){
            if (it)
            {
                ReminderScheduler.cancelReminder(requireContext(),notes[selectPosition].id)
                notes.removeAt(selectPosition)
                viewModel.resultGetNotes.value=notes
                noteAdapter.notifyItemRemoved(selectPosition)
                showMessage("Your Note Deleted!")
            }
        }

        binding.imgAdd.setOnClickListener {
            launcher.launch(Intent(requireContext(),CreateNote::class.java).apply {
                putExtra("action","create")
                putExtra("note",NoteModel())
            })
        }

        binding.btnApply.setOnClickListener {
            launcher.launch(Intent(requireContext(),CreateNote::class.java).apply {
                putExtra("action","create")
                putExtra("note",NoteModel())
            })
        }
    }

    fun getData(){

        //get Permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            PermissionManager.requestPermission(requireActivity(), Manifest.permission.POST_NOTIFICATIONS, PermissionManager.PERMISSION_REQUEST_CODE)
        }

        if (viewModel.resultGetNotes.value==null)
            viewModel.getNotes()
    }

    private val launcher= registerForActivityResult(ActivityResultContracts.StartActivityForResult()){ result->

        if (result.resultCode== RESULT_OK){
            val action=result.data?.getStringExtra("action")
            val note : NoteModel? by lazy {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    result.data?.getParcelableExtra("note", NoteModel::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    result.data?.getParcelableExtra<NoteModel>("note")
                }
            }
            action?.let {
                when(it){
                    "create"->{
                        note?.let {item->
                            notes.add(0,item)
                            viewModel.resultGetNotes.value=notes
                            noteAdapter.notifyItemInserted(0)
                        }
                    }
                    "update"->{
                        note?.let { item->
                            notes[selectPosition] = item
                            viewModel.resultGetNotes.value=notes
                            noteAdapter.notifyItemChanged(selectPosition)
                        }
                    }
                    else -> {}
                }
                updateReminderScheduler()
            }
        }
    }

    private fun updateReminderScheduler()
    {
        viewModel.resultGetNotes.value?.let {
            ReminderScheduler.syncReminders(requireContext(), it)
        }
    }
}