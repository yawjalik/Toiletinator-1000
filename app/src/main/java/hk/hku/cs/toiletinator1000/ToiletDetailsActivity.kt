package hk.hku.cs.toiletinator1000

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.storage

const val REQUEST_CODE = 42

class ToiletDetailsActivity : AppCompatActivity() {

    private var isAddReviewVisible = false

    private val storage = Firebase.storage
    private val db = Firebase.firestore

    // An activity result launcher for selecting images from the gallery
    private val uploadImageFromGallery =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
            if (uri != null) {
                FileUtils.uploadFile(uri, "images", "lmao").addOnSuccessListener {
                    Toast.makeText(this, "Upload successful", Toast.LENGTH_SHORT).show()
                    Log.d("ToiletDetailsActivity", "Upload successful")
                }.addOnFailureListener {
                    Toast.makeText(this, "Upload failed", Toast.LENGTH_SHORT).show()
                    Log.e("ToiletDetailsActivity", "Upload failed")
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_toilet_details)

        setSupportActionBar(findViewById(R.id.toolbar))

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val addReviewButton: Button = findViewById(R.id.button_add_review)
        addReviewButton.setOnClickListener {
            toggleAddReviewFragment()
        }

        val toiletId = intent.getStringExtra("toiletId")

        val toiletDetailsLocation: TextView = findViewById(R.id.toilet_details_location)
        val toiletDetailsStars: TextView = findViewById(R.id.toilet_details_stars)
        val toiletDetailsStatus: TextView = findViewById(R.id.toilet_details_status)

        // Query the toilet with the given toiletId
        db.collection("Toilet").document(toiletId!!).get().addOnSuccessListener { document ->
            val toilet = document.toObject(Toilet::class.java)
            if (toilet != null) {
                toiletDetailsLocation.text = toilet.floor + " " + toilet.building
                toiletDetailsStars.text = "Stars: ${toilet.stars} / 5"
                toiletDetailsStatus.text = "Status: ${toilet.status}"

                // Load the first image of the toilet
                val toiletDetailsImage: ImageView = findViewById(R.id.toilet_details_image)
                if (toilet.images.size > 0) {
                    val imageRef = storage.reference.child(toilet.images[0])
                    val ONE_MEGABYTE: Long = 1024 * 1024
                    imageRef.getBytes(ONE_MEGABYTE).addOnSuccessListener { bytes ->
                        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        toiletDetailsImage.setImageBitmap(bitmap)
                    }.addOnFailureListener {
                        Log.e("ToiletDetailsActivity", "Failed to get image")
                    }
                }
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Failed to get toilet details", Toast.LENGTH_SHORT).show()
        }

        // Report button
        val reportButton: ImageButton = findViewById(R.id.button_report)
        reportButton.setOnClickListener {
            // Dialog
            AlertDialog.Builder(this)
                .setTitle("Report Toilet Status")
                .setMessage("Select a status to report")
                .setPositiveButton("Available") { _, _ ->
                    db.collection("Toilet").document(toiletId).update("status", "Available")
                        .addOnSuccessListener {
                            Toast.makeText(this, "Reported", Toast.LENGTH_SHORT).show()
                            toiletDetailsStatus.text = "Status: Available"
                        }.addOnFailureListener {
                            Toast.makeText(this, "Failed to report", Toast.LENGTH_SHORT).show()
                        }
                }
                .setNegativeButton("Unavailable") { _, _ ->
                    db.collection("Toilet").document(toiletId).update("status", "Unavailable")
                        .addOnSuccessListener {
                            Toast.makeText(this, "Reported", Toast.LENGTH_SHORT).show()
                            toiletDetailsStatus.text = "Status: Unavailable"
                        }.addOnFailureListener {
                            Toast.makeText(this, "Failed to report", Toast.LENGTH_SHORT).show()
                        }
                }
                .show()
        }
    }

    private fun toggleAddReviewFragment() {
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        val fragment = ReviewsFragment()

        if (isAddReviewVisible) {
            fragmentTransaction.remove(fragment)
        } else {
            fragmentTransaction.replace(R.id.reviews_container, fragment)
        }

        fragmentTransaction.commit()
        isAddReviewVisible = !isAddReviewVisible
    }
}