package com.team6.travel_app.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import com.team6.travel_app.R
import com.team6.travel_app.data.CartDatabase
import com.team6.travel_app.data.CustomerDatabase
import com.team6.travel_app.databinding.FragmentLogInBinding
import com.team6.travel_app.databinding.FragmentProfileBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**

A simple [Fragment] subclass.

Use the [ProfileFragment.newInstance] factory method to

create an instance of this fragment.
 */
class ProfileFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null
    private lateinit var binding : FragmentProfileBinding
    private lateinit var customerDatabase: CustomerDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
// Inflate the layout for this fragment
        binding = FragmentProfileBinding.inflate(layoutInflater)
        return binding.root
    }

    companion object {
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            ProfileFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        customerDatabase = CustomerDatabase.invoke(requireContext())
        CoroutineScope(Dispatchers.IO).launch {
            val name = customerDatabase.CustomerDao().getName()
            binding.textViewProfileName.text = name.toString()
            binding.textViewPhoneNumber.text = customerDatabase.CustomerDao().getPhoneNumber().toString()
            binding.textViewEmail.text = customerDatabase.CustomerDao().getEmail().toString()
            binding.textViewAddress.text = customerDatabase.CustomerDao().getAddress().toString()
        }

    }
}