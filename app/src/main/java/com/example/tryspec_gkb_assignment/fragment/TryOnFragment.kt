package com.example.tryspec_gkb_assignment.fragment

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat.*
import androidx.core.content.ContextCompat
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.tryspec_gkb_assignment.R
import com.example.tryspec_gkb_assignment.databinding.FragmentTryOnBinding
import com.example.tryspec_gkb_assignment.viewmodel.MainViewModel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors


class TryOnFragment : Fragment() {


    lateinit var tryBinding: FragmentTryOnBinding
    private lateinit var requestPermissionCamera: ActivityResultLauncher<String>
    private lateinit var actionCamera: ActivityResultLauncher<Intent>
    private var preview: Preview? = null
    private var imageCapture: ImageCapture? = null
    private val cameraExecutor = Executors.newSingleThreadExecutor()
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

        tryBinding = FragmentTryOnBinding.inflate(inflater, container, false)
        return tryBinding.root


        requestPermissionCamera.launch(Manifest.permission.CAMERA)
        requestPermissionCamera =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) {
                if (it) {
                    val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                    if (cameraIntent.resolveActivity(requireActivity().packageManager) != null) {
                        actionCamera.launch(cameraIntent)
                    }
                } else {
                    Toast.makeText(requireActivity(), "Permission not granted", Toast.LENGTH_SHORT)
                        .show()
                }
            }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissions(
                REQUIRED_PERMISSIONS,
                REQUEST_CODE_PERMISSIONS
            )
        }

        tryBinding.captureBtn.setOnClickListener {
            takePicture()
        }

    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun startCamera() {

        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        val cameraSelector =
            CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_FRONT).build()
        cameraProviderFuture.addListener({
            preview = Preview.Builder().apply {
                setTargetResolution(
                    Size(
                        resources.displayMetrics.widthPixels,
                        resources.displayMetrics.heightPixels
                    )
                )
                setTargetRotation(tryBinding.cameraPreview.display.rotation)
            }.build()

            imageCapture = ImageCapture.Builder().apply {
                setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                setFlashMode(ImageCapture.FLASH_MODE_OFF)
            }.build()


            val cameraProvider = cameraProviderFuture.get()
            val camera = cameraProvider.bindToLifecycle(
                viewLifecycleOwner,
                cameraSelector,
                imageCapture,
                preview
            )
            tryBinding.cameraPreview.implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            preview?.setSurfaceProvider(tryBinding.cameraPreview.surfaceProvider)
        }, ContextCompat.getMainExecutor(requireActivity()))
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
//                finish()
            }
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            requireActivity(),
            it
        ) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)

    }

    private fun takePicture() {

        val filenames = File(
            getExternalFilesDirs(requireContext(), "images")[0],
            System.currentTimeMillis().toString() + ".jpeg"
        )
        val outputFileOptions = ImageCapture.OutputFileOptions.Builder(filenames).build()
        imageCapture?.takePicture(outputFileOptions, cameraExecutor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(error: ImageCaptureException) {
                    Log.d("telina", "onError: $error")
                    Toast.makeText(requireContext(), "error", Toast.LENGTH_SHORT).show()
                }

                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    viewModel.imageOutput = outputFileResults
                    requireActivity().runOnUiThread {
                        findNavController().navigate(TryOnFragmentDirections.actionTryOnFragmentToImageShowFragment())
                    }


                }
            })

    }

}