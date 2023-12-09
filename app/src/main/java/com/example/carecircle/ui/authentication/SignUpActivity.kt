package com.example.carecircle.ui.authentication

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Patterns
import android.view.View
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.carecircle.databinding.ActivitySignUpBinding
import com.example.carecircle.ui.doctors.main.categorySelection.CategorySelectionActivity
import com.example.carecircle.ui.patients.main.MainActivity
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.io.IOException
import java.util.UUID


class SignUpActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var binding: ActivitySignUpBinding
    private lateinit var databaseRefrence: DatabaseReference
    private lateinit var loadingProgressBar: ProgressBar

    private var filePath: Uri? = null
    private val PICK_IMAGE_REQUEST: Int = 2023

    private lateinit var storage: FirebaseStorage
    private lateinit var storageRef: StorageReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)
        loadingProgressBar = binding.loadingProgressBar
        storage = FirebaseStorage.getInstance()
        storageRef = storage.reference
        initViews()
    }

    private fun initViews() {
        auth = Firebase.auth
        binding.profilePic.setOnClickListener {
            chooseImage()
        }
        binding.signupBtn.setOnClickListener {
            uploadImage()
            registerUser()
        }
    }

    private fun registerUser() {
        val email = binding.emailEt.text.toString()
        val password = binding.passwordEt.text.toString()
        val userName = binding.fullNameEt.text.toString()
        val phoneNumber = binding.numberEt.text.toString()
        // gender radio button data
        val genderContainer = binding.genderContainer
        val genId: Int = genderContainer.checkedRadioButtonId
        val radioButtonGender = findViewById<View>(genId) as RadioButton
        val gender = radioButtonGender.text.toString()
        // user type radio button data
        val userTypeContainer = binding.typeContainer
        val userTypeId: Int = userTypeContainer.checkedRadioButtonId
        val radioButtonType = findViewById<View>(userTypeId) as RadioButton
        val userType = radioButtonType.text.toString()
        if (checkAllFields()) {
            setUiEnabled(false)
            auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener {
                setUiEnabled(true)
                if (it.isSuccessful) {
                    val user: FirebaseUser? = auth.currentUser
                    val userId: String = user!!.uid
                    // Set the display name
                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setDisplayName(userName)
                        .build()
                    // Apply the display name to the current user
                    user?.updateProfile(profileUpdates)
                        ?.addOnCompleteListener { profileUpdateTask ->
                            if (profileUpdateTask.isSuccessful) {
                                databaseRefrence =
                                    FirebaseDatabase.getInstance().getReference("users")
                                        .child(userId)
                                var hashMap: HashMap<String, String> = HashMap()
                                FirebaseMessaging.getInstance().token.addOnCompleteListener { token ->
                                    if (token.isSuccessful) {
                                        hashMap.put("userId", userId)
                                        hashMap.put("userName", userName)
                                        hashMap.put("email", email)
                                        hashMap.put("profileImage", "")
                                        hashMap.put("gender", gender)
                                        hashMap.put("userType", userType)
                                        hashMap.put("phoneNumber", phoneNumber)
                                        hashMap["token"] = token.result
                                        databaseRefrence.setValue(hashMap)
                                            .addOnCompleteListener(this) {
                                                if (it.isSuccessful) {
                                                    // Fetch user data from the database
                                                    databaseRefrence.addListenerForSingleValueEvent(
                                                        object :
                                                            ValueEventListener {
                                                            override fun onDataChange(snapshot: DataSnapshot) {
                                                                val userTypeFromDatabase =
                                                                    snapshot.child("userType")
                                                                        .getValue(String::class.java)

                                                                // Check user type after fetching it from the database
                                                                if (userTypeFromDatabase == "Doctor") {
                                                                    val intent = Intent(
                                                                        this@SignUpActivity,
                                                                        CategorySelectionActivity::class.java
                                                                    )
                                                                    startActivity(intent)
                                                                } else if (userTypeFromDatabase == "Patient") {
                                                                    val intent = Intent(
                                                                        this@SignUpActivity,
                                                                        MainActivity::class.java
                                                                    )
                                                                    startActivity(intent)
                                                                }

                                                                Toast.makeText(
                                                                    this@SignUpActivity,
                                                                    "Successfully made account",
                                                                    Toast.LENGTH_LONG
                                                                ).show()
                                                                finish()
                                                            }

                                                            override fun onCancelled(error: DatabaseError) {
                                                                Toast.makeText(
                                                                    this@SignUpActivity,
                                                                    "Failed to fetch user data",
                                                                    Toast.LENGTH_LONG
                                                                ).show()
                                                            }
                                                        })
                                                } else {
                                                    Toast.makeText(
                                                        this@SignUpActivity,
                                                        "Failed to save user data",
                                                        Toast.LENGTH_LONG
                                                    ).show()
                                                }
                                            }
                                    }
                                }
                            }
                        }

                } else {
                    Toast.makeText(
                        this@SignUpActivity,
                        "There is already an account registered with this email",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }


    private fun checkAllFields(): Boolean {
        val email = binding.emailEt.text.toString()
        if (binding.fullNameEt.text.isBlank()) {
            binding.fullNameContainer.error = "required"
            return false
        }
        if (binding.emailEt.text.isBlank()) {
            binding.emailContainer.error = "required"
            return false
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailContainer.error = "wrong email format"
            return false
        }

        if (binding.numberEt.text.isBlank()) {
            binding.numberContainer.error = "required"
            return false
        }

        if (binding.passwordEt.text.length <= 6) {
            binding.passwordContainer.error = "must be more than 6 characters"
            binding.passwordContainer.errorIconDrawable = null
            return false
        }

        if (binding.passwordEt.text.toString() != binding.confirmPasswordEt.text.toString()) {
            binding.passwordContainer.error = "password do not match"
            return false
        }
        return true
    }

    private fun setUiEnabled(enabled: Boolean) {
        binding.fullNameEt.isEnabled = enabled
        binding.emailEt.isEnabled = enabled
        binding.numberEt.isEnabled = enabled
        binding.passwordEt.isEnabled = enabled
        binding.confirmPasswordEt.isEnabled = enabled
        binding.genderContainer.isEnabled = enabled
        binding.typeContainer.isEnabled = enabled
        binding.signupBtn.isEnabled = enabled
        loadingProgressBar.visibility = if (enabled) View.GONE else View.VISIBLE
    }

    private fun chooseImage() {
        val intent: Intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(Intent.createChooser(intent, "Select Image"), PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode != null) {
            filePath = data!!.data
            try {
                var bitmap: Bitmap = MediaStore.Images.Media.getBitmap(contentResolver, filePath)
                binding.profilePic.setImageBitmap(bitmap)
                binding.signupBtn.visibility = View.VISIBLE
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    private fun uploadImage() {
        if (filePath != null) {
            val ref: StorageReference = storageRef.child("image/" + UUID.randomUUID().toString())
            ref.putFile(filePath!!)
                .addOnSuccessListener { taskSnapshot ->
                    // Get the download URL
                    ref.downloadUrl.addOnSuccessListener { downloadUrl ->
                        // Use the download URL to update the profileImage in the Realtime Database
                        val hashMap: HashMap<String, Any> = HashMap()
                        hashMap["profileImage"] = downloadUrl.toString()
                        databaseRefrence.updateChildren(hashMap)

                        // Check if the activity is not finishing before interacting with UI
                        if (!isFinishing) {
                            binding.loadingProgressBar.visibility = View.GONE
                            Toast.makeText(applicationContext, "Image uploaded", Toast.LENGTH_SHORT)
                                .show()
                            binding.signupBtn.visibility = View.GONE
                        }
                    }.addOnFailureListener {
                        // Check if the activity is not finishing before interacting with UI
                        if (!isFinishing) {
                            binding.loadingProgressBar.visibility = View.GONE
                            Toast.makeText(
                                applicationContext,
                                "Failed to get download URL: ${it.localizedMessage}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
                .addOnFailureListener {
                    // Check if the activity is not finishing before interacting with UI
                    if (!isFinishing) {
                        binding.loadingProgressBar.visibility = View.GONE
                        Toast.makeText(
                            applicationContext,
                            "Failed to upload image: ${it.localizedMessage}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
        } else {
            Toast.makeText(applicationContext, "File path is null", Toast.LENGTH_SHORT).show()
        }
    }


}