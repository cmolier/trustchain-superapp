package nl.tudelft.trustchain.offlineeuro.community.payload

import nl.tudelft.ipv8.messaging.Deserializable
import nl.tudelft.ipv8.messaging.Serializable
import nl.tudelft.ipv8.messaging.deserializeVarLen
import nl.tudelft.ipv8.messaging.serializeVarLen

class VerificationRequestPayload(
    val hash: ByteArray,
) : Serializable {
    override fun serialize(): ByteArray {
        var payload = ByteArray(0)
        payload += serializeVarLen(hash)
        return payload
    }

    companion object Deserializer : Deserializable<VerificationRequestPayload> {
        override fun deserialize(
            buffer: ByteArray,
            offset: Int
        ): Pair<VerificationRequestPayload, Int> {
            var localOffset = offset

            val (hashBytes, hashSize) = deserializeVarLen(buffer, localOffset)

            localOffset += hashSize

            return Pair(
                VerificationRequestPayload(hashBytes),
                localOffset - offset
            )
        }
    }
}
