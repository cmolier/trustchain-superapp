package nl.tudelft.trustchain.offlineeuro.ui

import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import nl.tudelft.trustchain.offlineeuro.R
import nl.tudelft.trustchain.offlineeuro.communication.IPV8CommunicationProtocol
import nl.tudelft.trustchain.offlineeuro.community.OfflineEuroCommunity
import nl.tudelft.trustchain.offlineeuro.cryptography.BilinearGroup
import nl.tudelft.trustchain.offlineeuro.cryptography.PairingTypes
import nl.tudelft.trustchain.offlineeuro.db.AddressBookManager
import nl.tudelft.trustchain.offlineeuro.entity.TTP

class TTPHomeFragment : OfflineEuroBaseFragment(R.layout.fragment_ttp_home) {
    private lateinit var ttp: TTP
    private lateinit var iPV8CommunicationProtocol: IPV8CommunicationProtocol
    private lateinit var community: OfflineEuroCommunity

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        if (ParticipantHolder.ttp != null) {
            ttp = ParticipantHolder.ttp!!
        } else {
            activity?.title = "TTP"
            community = getIpv8().getOverlay<OfflineEuroCommunity>()!!
            val group = BilinearGroup(PairingTypes.FromFile, context = context)
            val addressBookManager = AddressBookManager(context, group)
            iPV8CommunicationProtocol = IPV8CommunicationProtocol(addressBookManager, community)
            ttp = TTP("TTP", group, iPV8CommunicationProtocol, context, onDataChangeCallback = onDataChangeCallback)
        }
        onDataChangeCallback(null)

        view.findViewById<Button>(R.id.GenerateQRCodeButton).setOnClickListener {
            showUserSelectionDialog()
        }
    }

    fun showUserSelectionDialog() {
        val users = ttp.getRegisteredUsers()
        if (users.isEmpty()) {
            AlertDialog.Builder(requireContext())
                .setTitle("No Users")
                .setMessage("There are no registered users to generate QR codes for.")
                .setPositiveButton("OK", null)
                .show()
            return
        }

        // Get the most recently registered user
        val latestUser = users.last()
        val qrString = latestUser.googleKey
        val fragment = QRCodeFullScreenFragment.newInstance(
            qrString = qrString,
        )
        requireActivity().supportFragmentManager.beginTransaction()
            .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out)
            .add(android.R.id.content, fragment)
            .addToBackStack(null)
            .commit()
    }

    private val onDataChangeCallback: (String?) -> Unit = { message ->
        if (this::ttp.isInitialized) {
            requireActivity().runOnUiThread {
                val context = requireContext()
                CallbackLibrary.ttpCallback(context, message, requireView(), ttp)
            }
        }
    }
}
