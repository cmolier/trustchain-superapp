package nl.tudelft.trustchain.offlineeuro.entity

import it.unisa.dia.gas.jpbc.Element
import nl.tudelft.trustchain.offlineeuro.cryptography.BilinearGroup
import nl.tudelft.trustchain.offlineeuro.cryptography.CRS
import nl.tudelft.trustchain.offlineeuro.cryptography.GrothSahai
import nl.tudelft.trustchain.offlineeuro.cryptography.Schnorr
import nl.tudelft.trustchain.offlineeuro.cryptography.SchnorrSignature
import nl.tudelft.trustchain.offlineeuro.libraries.SchnorrSignatureSerializer
import java.util.UUID

/**
 * Extended transaction details with ephemeral key support
 * The linkage is a Schnorr signature that connects the ephemeral key to the user's real identity
 */
data class TransactionDetailsEphemeral(
    val digitalEuro: DigitalEuro,
    val ephemeralPublicKey: Element,
    val linkage: SchnorrSignature, // Schnorr signature linking ephemeral key to real identity
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toTransactionDetailsEphemeralBytes(): TransactionDetailsEphemeralBytes {
        return TransactionDetailsEphemeralBytes(
            digitalEuro.toDigitalEuroBytes(),
            ephemeralPublicKey.toBytes(),
            SchnorrSignatureSerializer.serializeSchnorrSignature(linkage),
            timestamp
        )
    }
}

data class TransactionDetailsEphemeralBytes(
    val digitalEuroBytes: DigitalEuroBytes,
    val ephemeralPublicKeyBytes: ByteArray,
    val linkageBytes: ByteArray,
    val timestamp: Long
) {
    fun toTransactionDetailsEphemeral(group: BilinearGroup): TransactionDetailsEphemeral {
        return TransactionDetailsEphemeral(
            digitalEuroBytes.toDigitalEuro(group),
            group.gElementFromBytes(ephemeralPublicKeyBytes),
            SchnorrSignatureSerializer.deserializeSchnorrSignatureBytes(linkageBytes)!!,
            timestamp
        )
    }
}

/**
 * Result of ephemeral transaction verification
 */
enum class EphemeralTransactionResult(val valid: Boolean, val description: String) {
    VALID_TRANSACTION(true, "Valid ephemeral transaction"),
    INVALID_BANK_SIGNATURE(false, "Invalid bank signature"),
    INVALID_EPHEMERAL_LINKAGE(false, "Invalid ephemeral key linkage"),
    EURO_ALREADY_SPENT(false, "Digital euro already spent")
}

object TransactionEphemeral {
    /**
     * Create a transaction using an ephemeral key while linking it to the real identity
     */
    fun createTransactionEphemeral(
        privateKey: Element,
        publicKey: Element,
        walletEntry: WalletEntry,
        bilinearGroup: BilinearGroup
    ): TransactionDetailsEphemeral {
        val digitalEuro = walletEntry.digitalEuro

        // Generate ephemeral key pair for this transaction
        val ephemeralPrivateKey = bilinearGroup.getRandomZr()
        val ephemeralPublicKey = bilinearGroup.g.powZn(ephemeralPrivateKey).immutable

        // Create linkage - sign the ephemeral public key with the real private key
        // This allows the TTP to later verify the connection
        val linkage = Schnorr.schnorrSignature(privateKey, ephemeralPublicKey.toBytes(), bilinearGroup)

        return TransactionDetailsEphemeral(
            digitalEuro,
            ephemeralPublicKey,
            linkage
        )
    }

    /**
     * Validate an ephemeral transaction
     */
    fun validate(
        transaction: TransactionDetailsEphemeral,
        publicKeyBank: Element,
        bilinearGroup: BilinearGroup
    ): EphemeralTransactionResult {
        // Verify digital euro is valid
        val digitalEuro = transaction.digitalEuro
        if (!digitalEuro.verifySignature(publicKeyBank, bilinearGroup) ||
            !digitalEuro.signature.signedMessage.contentEquals(digitalEuro.serialNumber.toByteArray() + digitalEuro.firstTheta1.toBytes())
        ) {
            return EphemeralTransactionResult.INVALID_BANK_SIGNATURE
        }

        // The ephemeral key is verified at TTP level, not during normal transaction validation
        return EphemeralTransactionResult.VALID_TRANSACTION
    }

    /**
     * For Trusted Third Party (TTP) to verify the linkage between ephemeral key and user identity
     * Returns the real public key if linkage is valid, null otherwise
     */
    fun verifyIdentity(
        transaction: TransactionDetailsEphemeral,
        candidatePublicKeys: List<Element>,
        bilinearGroup: BilinearGroup
    ): Element? {
        val ephemeralPublicKey = transaction.ephemeralPublicKey
        val linkage = transaction.linkage

        // Try each candidate public key to see if it verifies the linkage
        for (candidateKey in candidatePublicKeys) {
            if (Schnorr.verifySchnorrSignature(linkage, ephemeralPublicKey.toBytes(), candidateKey, bilinearGroup)) {
                return candidateKey // Found matching public key
            }
        }

        return null // No matching public key found
    }
}
