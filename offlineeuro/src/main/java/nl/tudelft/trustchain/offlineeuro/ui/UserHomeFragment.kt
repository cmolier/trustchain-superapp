package nl.tudelft.trustchain.offlineeuro.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanIntentResult
import com.journeyapps.barcodescanner.ScanOptions
import nl.tudelft.trustchain.common.ui.QRCodeActivityPortrait
import nl.tudelft.trustchain.offlineeuro.R
import nl.tudelft.trustchain.offlineeuro.communication.IPV8CommunicationProtocol
import nl.tudelft.trustchain.offlineeuro.community.OfflineEuroCommunity
import nl.tudelft.trustchain.offlineeuro.cryptography.BilinearGroup
import nl.tudelft.trustchain.offlineeuro.cryptography.PairingTypes
import nl.tudelft.trustchain.offlineeuro.db.AddressBookManager
import nl.tudelft.trustchain.offlineeuro.entity.User

class UserHomeFragment : OfflineEuroBaseFragment(R.layout.fragment_user_home) {
    private lateinit var user: User
    private lateinit var community: OfflineEuroCommunity
    private lateinit var communicationProtocol: IPV8CommunicationProtocol

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        if (ParticipantHolder.user != null) {
            user = ParticipantHolder.user!!
            communicationProtocol = user.communicationProtocol as IPV8CommunicationProtocol
            val userName: String = user.name
            val welcomeTextView = view.findViewById<TextView>(R.id.user_home_welcome_text)
            welcomeTextView.text = welcomeTextView.text.toString().replace("_name_", userName)
        } else {
            activity?.title = "User"
            val userName: String? = arguments?.getString("userName")
            val welcomeTextView = view.findViewById<TextView>(R.id.user_home_welcome_text)
            welcomeTextView.text = welcomeTextView.text.toString().replace("_name_", userName!!)
            community = getIpv8().getOverlay<OfflineEuroCommunity>()!!

            val group = BilinearGroup(PairingTypes.FromFile, context = context)
            val addressBookManager = AddressBookManager(context, group)
            communicationProtocol = IPV8CommunicationProtocol(addressBookManager, community)
            try {
                user = User(userName, group, context, null, communicationProtocol, onDataChangeCallback = onUserDataChangeCallBack)
                communicationProtocol.register(userName, user.publicKey, "TTP", source="userhome")
                communicationProtocol.scopePeers()
            } catch (e: Exception) {
                Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
            }
        }

        view.findViewById<Button>(R.id.user_home_reset_button).setOnClickListener {
            communicationProtocol.addressBookManager.clear()
            user.reset()
            val addressList = view.findViewById<LinearLayout>(R.id.user_home_addresslist)
            val addresses = communicationProtocol.addressBookManager.getAllAddresses()
            TableHelpers.addAddressesToTable(addressList, addresses, user, requireContext())
        }
        view.findViewById<Button>(R.id.user_home_sync_addresses).setOnClickListener {
            communicationProtocol.scopePeers()
        }
        val addressList = view.findViewById<LinearLayout>(R.id.user_home_addresslist)
        val addresses = communicationProtocol.addressBookManager.getAllAddresses()
        TableHelpers.addAddressesToTable(addressList, addresses, user, requireContext())
        onUserDataChangeCallBack(null)

        val qrCodeSecretTextView = view.findViewById<TextView>(R.id.qrCodeSecretTextView)
        qrCodeSecretTextView.text = user.name

        checkCameraPermissionAndScan()
    }

    private val requestPermissionLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            startQRScanner()
        } else {
            Toast.makeText(context, "Camera permission is required to scan QR codes", Toast.LENGTH_LONG).show()
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
            captureActivity = QRCodeActivityPortrait::class.java
        }
        barcodeLauncher.launch(options)
    }

    private val barcodeLauncher = registerForActivityResult(ScanContract()) { result: ScanIntentResult ->
        if (result.contents == null) {
            Toast.makeText(context, "Scan cancelled", Toast.LENGTH_SHORT).show()
        } else {
            handleQRResult(result.contents)
        }
    }

    private fun handleQRResult(qrContent: String) {
        Toast.makeText(context, "Scanned: $qrContent", Toast.LENGTH_LONG).show()

        view?.findViewById<TextView>(R.id.qrCodeSecretTextView)?.text = qrContent
        TableHelpers.googleKey = qrContent
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

    private val onUserDataChangeCallBack: (String?) -> Unit = { message ->
        requireActivity().runOnUiThread {
            val context = requireContext()
            if (this::user.isInitialized) {
                CallbackLibrary.userCallback(
                    context,
                    message,
                    requireView(),
                    communicationProtocol,
                    user
                )
            }
        }
    }
}
