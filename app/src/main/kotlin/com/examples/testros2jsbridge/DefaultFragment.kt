package com.example.testros2jsbridge

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import com.google.android.material.textfield.TextInputEditText
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/*
This fragment shows up by default when launching the app. It doesn't do too much, just advertises a default topic and sends basic string messages.
*/

class DefaultFragment : Fragment() {
    private lateinit var rosViewModel: RosViewModel
    private var advertiseButton: Button? = null
    private var publishCustomMessageButton: Button? = null
    private var customMessageEditText: TextInputEditText? = null

    /*
        input:    inflater - LayoutInflater, container - ViewGroup?, savedInstanceState - Bundle?
        output:   View?
        remarks:  Inflates the fragment view and sets up default message publishing UI
    */
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        rosViewModel = ViewModelProvider(requireActivity())[RosViewModel::class.java]
        val view = inflater.inflate(R.layout.activity_default, container, false)
        advertiseButton = view.findViewById(R.id.button_advertise)
        publishCustomMessageButton = view.findViewById(R.id.button_publish_custom_message)
        customMessageEditText = view.findViewById(R.id.edittext_custom_message)
        advertiseButton?.setOnClickListener {
            rosViewModel.advertiseTopic("/android_chatter", "std_msgs/msg/String")
        }
        publishCustomMessageButton?.setOnClickListener {
            val customMessage = customMessageEditText?.text.toString()
            if (customMessage.isNotEmpty()) {
                rosViewModel.onCustomMessageChange(customMessage)
                rosViewModel.publishCustomMessageFromTextField("/android_chatter")
                customMessageEditText?.text?.clear()
            } else {
                customMessageEditText?.error = "Message cannot be empty"
            }
        }
        observeViewModel()
        return view
    }

    /*
        input:    None
        output:   None
        remarks:  Observes ViewModel for updates to ROS messages and status
    */
    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            rosViewModel.customMessage.collectLatest { message ->
                if (customMessageEditText?.text.toString() != message) {
                    customMessageEditText?.setText(message)
                }
            }
        }
    }
}