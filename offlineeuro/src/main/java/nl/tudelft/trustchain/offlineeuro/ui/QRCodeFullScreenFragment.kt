package nl.tudelft.trustchain.offlineeuro.ui

import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import nl.tudelft.trustchain.common.util.QRCodeUtils
import nl.tudelft.trustchain.offlineeuro.R

class QRCodeFullScreenFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_qr_fullscreen, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val qrCodeImageView = view.findViewById<ImageView>(R.id.qrCodeImageView)
        val qrCodeSecretTextView = view.findViewById<TextView>(R.id.qrCodeSecretTextView)
        val closeButton = view.findViewById<ImageButton>(R.id.closeButton)

        val qrString = arguments?.getString("qrString") ?: ""

        val qrBitmap: Bitmap? = QRCodeUtils(requireContext()).createQR(qrString)
        qrBitmap?.let {
            qrCodeImageView.setImageBitmap(it)
        }
        qrCodeSecretTextView.text = qrString

        closeButton.setOnClickListener {
            requireActivity().supportFragmentManager.beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                .remove(this)
                .commit()
        }
    }

    companion object {
        fun newInstance(qrString: String,): QRCodeFullScreenFragment {
            val fragment = QRCodeFullScreenFragment()
            val args = Bundle()
            args.putString("qrString", qrString)
            fragment.arguments = args
            return fragment
        }
    }
}
