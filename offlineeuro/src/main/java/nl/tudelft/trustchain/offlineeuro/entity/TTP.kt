package nl.tudelft.trustchain.offlineeuro.entity

import android.content.Context
import it.unisa.dia.gas.jpbc.Element
import nl.tudelft.trustchain.offlineeuro.communication.ICommunicationProtocol
import nl.tudelft.trustchain.offlineeuro.cryptography.BilinearGroup
import nl.tudelft.trustchain.offlineeuro.cryptography.CRSGenerator
import nl.tudelft.trustchain.offlineeuro.cryptography.GrothSahaiProof
import nl.tudelft.trustchain.offlineeuro.cryptography.Schnorr
import nl.tudelft.trustchain.offlineeuro.cryptography.SchnorrSignature
import nl.tudelft.trustchain.offlineeuro.db.RegisteredUserManager

class TTP(
    name: String = "TTP",
    group: BilinearGroup,
    communicationProtocol: ICommunicationProtocol,
    context: Context?,
    private val registeredUserManager: RegisteredUserManager = RegisteredUserManager(context, group),
    onDataChangeCallback: ((String?) -> Unit)? = null
) : Participant(communicationProtocol, name, onDataChangeCallback) {
    val crsMap: Map<Element, Element>

    init {
        communicationProtocol.participant = this
        this.group = group
        val generatedCRS = CRSGenerator.generateCRSMap(group)
        this.crs = generatedCRS.first
        this.crsMap = generatedCRS.second
        generateKeyPair()
    }

    fun registerUser(
        name: String,
        publicKey: Element
    ): Boolean {
        val result = registeredUserManager.addRegisteredUser(name, publicKey)
        onDataChangeCallback?.invoke("Registered $name")
        return result
    }

    fun getRegisteredUsers(): List<RegisteredUser> {
        return registeredUserManager.getAllRegisteredUsers()
    }

    override fun onReceivedTransaction(
        transactionDetails: TransactionDetails,
        publicKeyBank: Element,
        publicKeySender: Element
    ): String {
        TODO("Not yet implemented")
    }

    fun getUserFromProof(grothSahaiProof: GrothSahaiProof, schnorrSignature: SchnorrSignature): RegisteredUser? {
        val crsExponent = crsMap[crs.u]
        val test = group.g.powZn(crsExponent)
        val ephemeralPublicKey =
            grothSahaiProof.c1.powZn(crsExponent!!.mul(-1)).mul(grothSahaiProof.c2).immutable
        val publicKey = getPublicKeyFromEphemeralPublicKey(ephemeralPublicKey, schnorrSignature, group)
        if (publicKey == null) {
            onDataChangeCallback?.invoke("Invalid proof received!")
            return null
        }
        return registeredUserManager.getRegisteredUserByPublicKey(publicKey)
    }
    // Get public key from ephemeral public key using the Schnorr signature
    fun getPublicKeyFromEphemeralPublicKey(
        ephemeralPublicKey: Element,
        schnorrSignature: SchnorrSignature,
        bilinearGroup: BilinearGroup
    ): Element? {
        // First verify the signature message matches the ephemeral public key
        if (!ephemeralPublicKey.toBytes().contentEquals(schnorrSignature.signedMessage)) {
            return null
        }

        try {
            // Simple approach: check all registered users to find whose public key
            // correctly verifies this signature
            for (user in getRegisteredUsers()) {
                if (Schnorr.verifySchnorrSignature(schnorrSignature, user.publicKey, bilinearGroup)) {
                    return user.publicKey
                }
            }
            return null
        } catch (e: Exception) {
            return null
        }
    }

    fun getUserFromProofs(
        firstProof: GrothSahaiProof,
        secondProof: GrothSahaiProof,
        euroSchnorrSignature: SchnorrSignature,
        doubleSpendSchnorrSignature: SchnorrSignature
    ): String {
        val firstPK = getUserFromProof(firstProof, euroSchnorrSignature)
        val secondPK = getUserFromProof(secondProof, doubleSpendSchnorrSignature)

        return if (firstPK != null && firstPK == secondPK) {
            onDataChangeCallback?.invoke("Found proof that  ${firstPK.name} committed fraud!")
            "Double spending detected. Double spender is ${firstPK.name} with PK: ${firstPK.publicKey}"
        } else {
            onDataChangeCallback?.invoke("Invalid fraud request received!")
            "No double spending detected"
        }
    }

    override fun reset() {
        registeredUserManager.clearAllRegisteredUsers()
    }
}
