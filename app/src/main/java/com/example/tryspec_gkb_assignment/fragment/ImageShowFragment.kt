package com.example.tryspec_gkb_assignment.fragment

import android.Manifest
import android.content.Intent
import android.graphics.*
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.tryspec_gkb_assignment.R
import com.example.tryspec_gkb_assignment.databinding.FragmentImageShowBinding
import com.example.tryspec_gkb_assignment.viewmodel.MainViewModel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceContour
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.FaceLandmark


class ImageShowFragment : Fragment() {

    lateinit var imageShowBinding: FragmentImageShowBinding

    private var finalPhoto: Bitmap? = null

    private var savedImagePath: String? = null
    val viewModel: MainViewModel by lazy {
        ViewModelProvider(requireActivity())[MainViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        imageShowBinding = FragmentImageShowBinding.inflate(inflater, container, false)
        return imageShowBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ActivityCompat.requestPermissions(
            requireActivity(),
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ),
            1233
        )

        val imageUri = viewModel.imageOutput!!.savedUri


        val bitmap: Bitmap =
            MediaStore.Images.Media.getBitmap(requireActivity().contentResolver, imageUri)

        val specBitmap = BitmapFactory.decodeResource(
            resources,
            R.drawable.spec_two,
        )

        val cx = bitmap.width / 2f
        val cy = bitmap.height / 2f
        val flippedBitmap = bitmap.flip(-1f, 1f, cx, cy)
        imageShowBinding.imageShowId.setImageBitmap(flippedBitmap)

        val personImage = imageShowBinding.imageShowId.drawable.toBitmap()

        val perfectImage = personImage


        val highAccuracyOpts = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .build()

// Real-time contour detection
        val realTimeOpts = FaceDetectorOptions.Builder()
            .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .build()

        val image = InputImage.fromBitmap(perfectImage, 0)

        val detector = FaceDetection.getClient(realTimeOpts)

        val result = detector.process(image)
            .addOnSuccessListener { faces ->
                // Task completed successfully
                // ...

                for (face in faces) {
                    val bounds = face.boundingBox
                    val rotY = face.headEulerAngleY // Head is rotated to the right rotY degrees
                    val rotZ = face.headEulerAngleZ // Head is tilted sideways rotZ degrees

                    // If landmark detection was enabled (mouth, ears, eyes, cheeks, and
                    // nose available):
                    val leftEar = face.getLandmark(FaceLandmark.LEFT_EAR)
                    val rightEar = face.getLandmark(FaceLandmark.RIGHT_EAR)!!.position
                    val leftEye = face.getLandmark(FaceLandmark.LEFT_EYE)!!.position
                    var mx: Float? = null
                    var specWidth: Int? = null
                    leftEar?.let {
                        val leftEarPos = leftEar.position
                        mx = leftEarPos.x
                        imageShowBinding.specImageView.x = leftEarPos.x
                        imageShowBinding.specImageView.updateLayoutParams {
                            width = (rightEar.x - leftEarPos.x).toInt()
                            specWidth = (rightEar.x - leftEarPos.x).toInt()
                        }
                        Log.d("telina", "x is : ${leftEarPos.x} and y is => ${leftEarPos.y}")

                    }

//                     If contour detection was enabled:
                    val leftEyeContour = face.getContour(FaceContour.LEFT_EYEBROW_TOP)?.points

                    leftEyeContour?.let {

                        val selfieWithSpec =
                            perfectImage.overlay(specBitmap, mx!!, it[0].y, specWidth)
                        finalPhoto = selfieWithSpec
                        imageShowBinding.imageShowId.setImageBitmap(selfieWithSpec)
                        imageShowBinding.specImageView.y = it[0].y

                    }
                }


            }
            .addOnFailureListener { e ->

            }




        imageShowBinding.saveBtn.setOnClickListener {
            savedImagePath = MediaStore.Images.Media.insertImage(
                requireContext().contentResolver,
                finalPhoto,
                System.currentTimeMillis().toString(),
                null
            )
            Toast.makeText(requireContext(), "Photo Saved in Gallery", Toast.LENGTH_SHORT).show()
        }

        imageShowBinding.shareBtn.setOnClickListener {

            if (savedImagePath == null) {
                savedImagePath = MediaStore.Images.Media.insertImage(
                    requireContext().contentResolver,
                    finalPhoto,
                    System.currentTimeMillis().toString(),
                    null
                )

            }
            try {

                val path = MediaStore.Images.Media.insertImage(
                    requireContext().contentResolver,
                    finalPhoto,
                    "Title",
                    null
                )

                val intent = Intent(Intent.ACTION_SEND)
                intent.type = "image/jpeg"
                intent.putExtra(Intent.EXTRA_STREAM, Uri.parse(savedImagePath))
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                startActivity(Intent.createChooser(intent, "Share Image"))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }


    private fun showOverlay(x: Float, y: Float) {
        val imageview = ImageView(requireContext())
        imageview.setImageResource(R.drawable.circle)
        imageview.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        imageview.x = x
        imageview.y = y
        imageShowBinding.rootView.addView(imageview)
    }
}


fun Bitmap.flip(x: Float, y: Float, cx: Float, cy: Float): Bitmap {
    val matrix = Matrix().apply { postScale(x, y, cx, cy) }
    return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
}

fun Bitmap.overlay(overlay: Bitmap, x: Float, y: Float, specWidth: Int?): Bitmap? {
    val bmOverlay = Bitmap.createBitmap(this.width, this.height, this.config)
    val canvas = Canvas(bmOverlay)
    canvas.drawBitmap(this, Matrix(), null)
    Log.d("telina", "overlay: x => $x and y => $y")
    val sb = Bitmap.createScaledBitmap(overlay, specWidth!!, specWidth / 3, true)
    canvas.drawBitmap(sb, x, y, null)
    return bmOverlay
}