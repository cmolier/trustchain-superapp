package nl.tudelft.trustchain.offlineeuro.entity

import android.content.Context
import it.unisa.dia.gas.jpbc.Element
import nl.tudelft.trustchain.offlineeuro.communication.ICommunicationProtocol
import nl.tudelft.trustchain.offlineeuro.cryptography.BilinearGroup
import nl.tudelft.trustchain.offlineeuro.cryptography.Schnorr
import nl.tudelft.trustchain.offlineeuro.db.WalletManager
import java.util.UUID

class User(
    name: String,
    group: BilinearGroup,
    context: Context?,
    private var walletManager: WalletManager? = null,
    communicationProtocol: ICommunicationProtocol,
    runSetup: Boolean = true,
    onDataChangeCallback: ((String?) -> Unit)? = null
) : Participant(communicationProtocol, name, onDataChangeCallback) {
    val wallet: Wallet

    init {
        communicationProtocol.participant = this
        this.group = group

        if (runSetup) {
            setUp()
        } else {
            generateKeyPair()
        }
        if (walletManager == null) {
            walletManager = WalletManager(context, group)
        }

        wallet = Wallet(privateKey, publicKey, walletManager!!)
    }

    fun sendDigitalEuroTo(nameReceiver: String, hash: String, skipVerification: Boolean = false): String {
        val hashInput = if (name == "test") "testHash" else hash
        val verificationResult: String = if (skipVerification) {
            "YES"
        } else {
            communicationProtocol.requestVerification(name, hashInput, nameTTP = "TTP")
        }
        if (verificationResult == "YES") {
            onDataChangeCallback?.invoke("Transaction verification succeeded")
            val randomizationElements =
                communicationProtocol.requestTransactionRandomness(nameReceiver, group)
            val transactionDetails =
                wallet.spendEuro(randomizationElements, group, crs)
                    ?: throw Exception("No euro to spend")

            val result =
                communicationProtocol.sendTransactionDetails(nameReceiver, transactionDetails)
            onDataChangeCallback?.invoke(result)
            return result
        } else {
            return "Transaction verification failed, please try again."
        }
    }

    fun doubleSpendDigitalEuroTo(nameReceiver: String): String {
        val randomizationElements = communicationProtocol.requestTransactionRandomness(nameReceiver, group)
        val transactionDetails = wallet.doubleSpendEuro(randomizationElements, group, crs)
        val result = communicationProtocol.sendTransactionDetails(nameReceiver, transactionDetails!!)
        onDataChangeCallback?.invoke(result)
        return result
    }

    fun withdrawDigitalEuro(bank: String): DigitalEuro {
        val serialNumber = UUID.randomUUID().toString()
        val firstT = group.getRandomZr()
        val tInv = firstT.mul(-1)
        val initialTheta = group.g.powZn(tInv).immutable

        val ephemeralPrivateKey = group.getRandomZr()
        val ephemeralPublicKey = group.g.powZn(ephemeralPrivateKey)

        val ephemeralKeySignature = Schnorr.schnorrSignature(
            privateKey,
            ephemeralPublicKey.toBytes(),
            group
        )

        val bytesToSign = serialNumber.toByteArray() + initialTheta.toBytes()

        val bankRandomness = communicationProtocol.getBlindSignatureRandomness(publicKey, bank, group)
        val bankPublicKey = communicationProtocol.getPublicKeyOf(bank, group)

        val blindedChallenge = Schnorr.createBlindedChallenge(bankRandomness, bytesToSign, bankPublicKey, group)
        val blindSignature = communicationProtocol.requestBlindSignature(publicKey, bank, blindedChallenge.blindedChallenge)
        val signature = Schnorr.unblindSignature(blindedChallenge, blindSignature)
        val digitalEuro = DigitalEuro(serialNumber, initialTheta, signature, arrayListOf(), mutableListOf(ephemeralKeySignature))

        wallet.addToWallet(digitalEuro, firstT, ephemeralPrivateKey)
        onDataChangeCallback?.invoke("Withdrawn ${digitalEuro.serialNumber} successfully!")
        return digitalEuro
    }

    fun getBalance(): Int {
        return walletManager!!.getWalletEntriesToSpend().count()
    }

    override fun onReceivedTransaction(
        transactionDetails: TransactionDetails,
        publicKeyBank: Element,
        publicKeySender: Element
    ): String {
        val usedRandomness = lookUpRandomness(publicKeySender) ?: return "Randomness Not found!"
        removeRandomness(publicKeySender)
        val transactionResult = Transaction.validate(transactionDetails, publicKeyBank, group, crs)

        if (transactionResult.valid) {
            val ephemeralPrivateKey = group.getRandomZr()
            val ephemeralPublicKey = group.g.powZn(ephemeralPrivateKey)

            val ephemeralKeySignature = Schnorr.schnorrSignature(
                privateKey,
                ephemeralPublicKey.toBytes(),
                group
            )

            transactionDetails.digitalEuro.ephemeralKeySignatures.add(ephemeralKeySignature)

            wallet.addToWallet(transactionDetails, usedRandomness, ephemeralPrivateKey)
            onDataChangeCallback?.invoke("Received an euro from $publicKeySender")
            return transactionResult.description
        }
        onDataChangeCallback?.invoke(transactionResult.description)
        return transactionResult.description
    }

    override fun reset() {
        randomizationElementMap.clear()
        walletManager!!.clearWalletEntries()
        setUp()
    }
}
