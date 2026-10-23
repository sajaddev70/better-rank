package com.marveloushost.betterrank.adapter

import android.view.LayoutInflater
import android.view.View
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import android.view.ViewGroup
import com.marveloushost.betterrank.R
import com.marveloushost.betterrank.databinding.NoteAdapterBinding
import com.marveloushost.betterrank.model.NoteModel
import com.marveloushost.betterrank.viewModel.BaseViewModel

class NoteAdapter (
    private var list: List<NoteModel>,
    private val viewModel:BaseViewModel,
    val onEdit:(noteMode:NoteModel, position:Int)->Unit,
    val onDelete:(id:String, position:Int)->Unit,
) : RecyclerView.Adapter<NoteAdapter.NoteHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteHolder {

        val inflater= LayoutInflater.from(parent.context)
        val binding= DataBindingUtil.inflate<NoteAdapterBinding>(inflater, R.layout.note_adapter,parent,false)
        return NoteHolder(binding)
    }

    override fun onBindViewHolder(holder: NoteHolder, position: Int) {
        val note = list[position]
        holder.bindingData(note)

        val v = holder.binding.txtMessage

        if (note.isExpanded && v.visibility == View.VISIBLE && v.alpha == 0f) {
            v.animate().alpha(1f).setDuration(200).start()
        }
    }


    override fun getItemCount(): Int {
        return list.size
    }

    inner class NoteHolder(val binding: NoteAdapterBinding) : RecyclerView.ViewHolder(binding.root)
    {

        fun bindingData(note: NoteModel) {

            binding.note = note   // ← REQUIRED

            if (note.reminded) {
                binding.liReminded.visibility = View.VISIBLE
                binding.txtStartAt.text = viewModel.timestampToDateTime(note.startAt)
            } else binding.liReminded.visibility = View.GONE

            val v = binding.txtMessage

            if (note.isExpanded) {
                v.visibility = View.VISIBLE
                v.alpha = 1f
                v.text = note.message
            } else {
                v.visibility = View.GONE
                v.alpha = 0f
                v.text = note.message
            }

            binding.root.setOnClickListener {
                note.isExpanded = !note.isExpanded
                notifyItemChanged(bindingAdapterPosition)
            }

            binding.imgEdit.setOnClickListener {
                onEdit(note, bindingAdapterPosition)
            }

            binding.imgDelete.setOnClickListener {
                onDelete(note.id, bindingAdapterPosition)
            }

            binding.executePendingBindings()
        }



    }
}