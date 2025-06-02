package nl.tudelft.trustchain.offlineeuro.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanIntentResult
import com.journeyapps.barcodescanner.ScanOptions
import nl.tudelft.trustchain.offlineeuro.R
import androidx.navigation.fragment.findNavController

class QRScanHomeFragment : OfflineEuroBaseFragment(R.layout.fragment_qr_scan) {

    private var userName: String? = null

    private val requestPermissionLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            startQRScanner()
        } else {
            Toast.makeText(context, "Camera permission is required to scan QR codes", Toast.LENGTH_LONG).show()
        }
    }

    private val barcodeLauncher = registerForActivityResult(ScanContract()) { result: ScanIntentResult ->
        if (result.contents == null) {
            Toast.makeText(context, "Scan cancelled", Toast.LENGTH_SHORT).show()
        } else {
            handleQRResult(result.contents, userName)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        userName = arguments?.getString("userName")
        val qrCodeSecretTextView = view.findViewById<TextView>(R.id.qrCodeSecretTextView)
        qrCodeSecretTextView.text = userName ?: "No username provided"

        val closeButton = view.findViewById<View>(R.id.closeButton)
        closeButton.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        val scanButton = view.findViewById<Button>(R.id.scanButton)
        scanButton.setOnClickListener {
            checkCameraPermissionAndScan()
        }
    }

    private fun checkCameraPermissionAndScan() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                startQRScanner()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                Toast.makeText(
                    context,
                    "Camera permission is needed to scan QR codes",
                    Toast.LENGTH_LONG
                ).show()
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun startQRScanner() {
        val options = ScanOptions().apply {
            setDesiredBarcodeFormats(ScanOptions.QR_CODE)
            setPrompt("Scan QR Code")
            setCameraId(0)
            setBeepEnabled(true)
            setBarcodeImageEnabled(true)
            setOrientationLocked(true)
        }
        barcodeLauncher.launch(options)
    }

    private fun handleQRResult(qrContent: String, userName: String?) {
        Toast.makeText(context, "Scanned: $qrContent", Toast.LENGTH_LONG).show()

        // Update the TextView with scanned content
        view?.findViewById<TextView>(R.id.qrCodeSecretTextView)?.text = qrContent

        val bundle = bundleOf(
            "qrContent" to qrContent,
            "userName" to userName
        )

        findNavController().navigate(R.id.nav_home_userhome, bundle)
    }
}
