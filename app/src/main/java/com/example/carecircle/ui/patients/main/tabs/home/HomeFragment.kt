package com.example.carecircle.ui.patients.main.tabs.home

import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.UnderlineSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.carecircle.R
import com.example.carecircle.databinding.FragmentHomeBinding
import com.example.carecircle.model.CategoryData
import com.example.carecircle.model.Doctor
import com.example.carecircle.ui.patients.main.tabs.categories.CategoriesAdapter
import com.example.carecircle.ui.patients.main.tabs.categories.CategoriesFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener


class HomeFragment : Fragment() {
    private lateinit var binding: FragmentHomeBinding
    private var categories: ArrayList<CategoryData> = ArrayList()
    lateinit var adapter: CategoriesAdapter
    lateinit var doctorsAadapter: TopDoctorsAdapter
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        underLineText()
        initRecyclerView()
        fetchDataFromDatabase()

        binding.seeAllTextView.setOnClickListener {
            navigateToCategoryFragment()
        }
        bindUserName()

    }


    private fun initRecyclerView() {
        val layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.categoriesList.layoutManager = layoutManager
        categories.add(CategoryData("Cardiologist", R.drawable.cardiologist_img))
        categories.add(CategoryData("Dermatologist", R.drawable.dermatologist_img))
        categories.add(CategoryData("Orthopedic", R.drawable.orthopedic_surgeon_img))
        categories.add(CategoryData("Gynecologist", R.drawable.gynecologist_img))
        categories.add(CategoryData("Pediatrician", R.drawable.pediatrician_img))
        categories.add(CategoryData("Neurologist", R.drawable.neurologist_img))
        categories.add(CategoryData("Psychiatrist", R.drawable.psychiatrist_img))
        categories.add(CategoryData("Ophthalmologist", R.drawable.ophthalmologist_img))
        categories.add(CategoryData("Oncologist", R.drawable.oncologist_img))
        categories.add(CategoryData("Dentist", R.drawable.dentist_img))
        adapter = CategoriesAdapter(categories)
        binding.categoriesList.adapter = adapter


    }

    private fun navigateToCategoryFragment() {
        val anotherFragment = CategoriesFragment()

        // Get the FragmentManager
        val fragmentManager: FragmentManager = requireActivity().supportFragmentManager

        // Begin the fragment transaction
        val transaction: FragmentTransaction = fragmentManager.beginTransaction()

        // Replace the current Fragment with the new Fragment
        transaction.replace(R.id.fragment_container, anotherFragment)

        transaction.addToBackStack(null)
        // Commit the transaction
        transaction.commit()
    }

    private fun underLineText() {
        // Your text
        val yourText = binding.seeAllTextView.text.toString()

        // Create a SpannableString with UnderlineSpan
        val spannableString = SpannableString(yourText)
        spannableString.setSpan(
            UnderlineSpan(),
            0,
            yourText.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        // Set the SpannableString to the TextView
        binding.seeAllTextView.text = spannableString
    }

    private fun fetchDataFromDatabase() {
        val databaseReference: DatabaseReference =
            FirebaseDatabase.getInstance().getReference("users")

        databaseReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val doctorList: MutableList<Doctor> = mutableListOf()

                for (userSnapshot in dataSnapshot.children) {
                    val userType = userSnapshot.child("userType").getValue(String::class.java)

                    // Check if the user type is "Doctor"
                    if (userType == "Doctor") {
                        // Retrieve other relevant data for doctors
                        val name = userSnapshot.child("userName").getValue(String::class.java)
                        val id = userSnapshot.child("userId").getValue(String::class.java)
                        val speciality = userSnapshot.child("category").getValue(String::class.java)
                        val rating = userSnapshot.child("rating").getValue(Float::class.java)

                        // Create a Doctor object and add it to the list
                        val doctor = Doctor(name, id, speciality, rating!!)
                        doctorList.add(doctor)
                    }
                }
                doctorsAadapter = TopDoctorsAdapter(doctorList)

                binding.doctorsRecycler.adapter = doctorsAadapter

            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Handle errors
            }
        })
    }

    private fun bindUserName() {
        val currentUser = FirebaseAuth.getInstance().currentUser

        if (currentUser != null) {
            val userName = currentUser.displayName

            if (userName != null) {
                // Now, userName contains the username of the currently authenticated user
                // You can use it as needed
                binding.userName.text = userName

            } else {
                Log.d("YourFragment", "Username is null")
            }
        }
    }


}