package nl.tudelft.trustchain.offlineeuro.libraries

import nl.tudelft.trustchain.offlineeuro.cryptography.SchnorrSignature
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable
import java.math.BigInteger

private data class SchnorrSignatureBytes(
    val signature: ByteArray,
    val encryption: ByteArray,
    val message: ByteArray
) : Serializable {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SchnorrSignatureBytes

        if (!signature.contentEquals(other.signature)) return false
        if (!encryption.contentEquals(other.encryption)) return false
        return message.contentEquals(other.message)
    }
}

object SchnorrSignatureSerializer {
    fun serializeSchnorrSignatures(signatures: List<SchnorrSignature>): ByteArray {
        val signaturesBytes = signatures.map { x -> schnorrSignatureToBytes(x) }
        return serializeSchnorrBytesList(signaturesBytes)
    }

    fun serializeSchnorrSignature(signature: SchnorrSignature?): ByteArray {
        if (signature == null) return ByteArray(0)
        val signatureAsBytes = schnorrSignatureToBytes(signature)
        return serializeSchnorrBytes(signatureAsBytes)
    }

    fun deserializeSchnorrSignatureBytes(bytes: ByteArray?): SchnorrSignature? {
        if (bytes == null || bytes.contentEquals(ByteArray(0))) return null
        val signatureBytes = deserializeSignatureBytes(bytes)
        return bytesToSchnorrSignature(signatureBytes)
    }

    fun deserializeSignatureListBytes(bytes: ByteArray): MutableList<SchnorrSignature> {
        val signatureBytesList = deserializeSignatureBytesList(bytes)
        return ArrayList(signatureBytesList.map { x -> bytesToSchnorrSignature(x) })
    }

    private fun serializeSchnorrBytes(signatureBytes: SchnorrSignatureBytes): ByteArray {
        val byteArrayOutputStream = ByteArrayOutputStream()
        val objectOutputStream = ObjectOutputStream(byteArrayOutputStream)
        objectOutputStream.writeObject(signatureBytes)
        objectOutputStream.close()
        return byteArrayOutputStream.toByteArray()
    }

    private fun serializeSchnorrBytesList(signatureList: List<SchnorrSignatureBytes>): ByteArray {
        val byteArrayOutputStream = ByteArrayOutputStream()
        val objectOutputStream = ObjectOutputStream(byteArrayOutputStream)
        objectOutputStream.writeObject(signatureList)
        objectOutputStream.close()
        return byteArrayOutputStream.toByteArray()
    }

    private fun deserializeSignatureBytesList(bytes: ByteArray): List<SchnorrSignatureBytes> {
        val byteArrayInputStream = ByteArrayInputStream(bytes)
        val objectInputStream = ObjectInputStream(byteArrayInputStream)
        val proofList = objectInputStream.readObject() as List<SchnorrSignatureBytes>
        objectInputStream.close()
        return proofList
    }

    private fun deserializeSignatureBytes(bytes: ByteArray): SchnorrSignatureBytes {
        val byteArrayInputStream = ByteArrayInputStream(bytes)
        val objectInputStream = ObjectInputStream(byteArrayInputStream)
        val signatureBytes = objectInputStream.readObject() as SchnorrSignatureBytes
        objectInputStream.close()
        return signatureBytes
    }

    private fun schnorrSignatureToBytes(schnorrSignature: SchnorrSignature): SchnorrSignatureBytes {
        val (signature, encryption, message) = schnorrSignature
        return SchnorrSignatureBytes(
            signature.toByteArray(),
            encryption.toByteArray(),
            message
        )
    }

    private fun bytesToSchnorrSignature(signatureBytes: SchnorrSignatureBytes): SchnorrSignature {
        val (signature, encryption, message) = signatureBytes
        return SchnorrSignature(
            BigInteger(signature),
            BigInteger(encryption),
            message
        )
    }
}
