package nl.tudelft.trustchain.offlineeuro.ui

import android.content.Context
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import nl.tudelft.trustchain.offlineeuro.R
import nl.tudelft.trustchain.offlineeuro.communication.IPV8CommunicationProtocol
import nl.tudelft.trustchain.offlineeuro.entity.Bank
import nl.tudelft.trustchain.offlineeuro.entity.TTP
import nl.tudelft.trustchain.offlineeuro.entity.User

object CallbackLibrary {
    fun bankCallback(
        context: Context,
        message: String?,
        view: View,
        bank: Bank
    ) {
        if (message != null) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
        val table = view.findViewById<LinearLayout>(R.id.bank_home_deposited_list)
        TableHelpers.removeAllButFirstRow(table)
        TableHelpers.addDepositedEurosToTable(table, bank)
    }

    fun ttpCallback(
        context: Context,
        message: String?,
        view: View,
        ttp: TTP
    ) {
        if (message != null) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
        if (message != null && message.contains("xd")) {
            val activity = context as? FragmentActivity

            val navHostFragment = activity?.supportFragmentManager?.fragments?.firstOrNull { it is androidx.navigation.fragment.NavHostFragment } as? androidx.navigation.fragment.NavHostFragment
            val currentFragment = navHostFragment?.childFragmentManager?.fragments?.firstOrNull()
            var fragment = currentFragment as? TTPHomeFragment

            // If not found, check if current fragment is AllRolesFragment
            if (fragment == null && currentFragment is AllRolesFragment) {
                val childFragments = currentFragment.childFragmentManager.fragments
                fragment = childFragments.firstOrNull { it is TTPHomeFragment } as? TTPHomeFragment
            }
            fragment?.showUserSelectionDialog()
        }
        updateUserList(view, ttp)
    }

    private fun updateUserList(
        view: View,
        ttp: TTP
    ) {
        val table = view.findViewById<LinearLayout>(R.id.tpp_home_registered_user_list) ?: return
        val users = ttp.getRegisteredUsers()
        TableHelpers.removeAllButFirstRow(table)
        TableHelpers.addRegisteredUsersToTable(table, users)
    }

    fun userCallback(
        context: Context,
        message: String?,
        view: View,
        communicationProtocol: IPV8CommunicationProtocol,
        user: User
    ) {
        if (message != null) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
        val balanceField = view.findViewById<TextView>(R.id.user_home_balance)
        balanceField.text = user.getBalance().toString()
        val addressList = view.findViewById<LinearLayout>(R.id.user_home_addresslist)
        val addresses = communicationProtocol.addressBookManager.getAllAddresses()
        TableHelpers.addAddressesToTable(addressList, addresses, user, context)
        view.refreshDrawableState()
    }
}
