package com.marveloushost.betterrank.fragment

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.marveloushost.betterrank.viewModel.BaseViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
abstract class BaseFragment : Fragment()
{
    private lateinit var baseViewModel: BaseViewModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //viewModel
        baseViewModel= ViewModelProvider(this)[BaseViewModel::class.java]
    }

    abstract fun initView()
    abstract fun onClick()

    fun showMessage(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    fun Fragment.hideKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val view = requireActivity().currentFocus ?: View(requireContext())
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }
}