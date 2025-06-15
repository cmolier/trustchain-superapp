package nl.tudelft.trustchain.offlineeuro.community.payload

import nl.tudelft.ipv8.messaging.Deserializable
import nl.tudelft.ipv8.messaging.Serializable
import nl.tudelft.ipv8.messaging.deserializeVarLen
import nl.tudelft.ipv8.messaging.serializeVarLen

class VerificationRequestPayload(
    val sendingRequestUsername: String,
    val hash: String,
) : Serializable {
    override fun serialize(): ByteArray {
        var payload = ByteArray(0)
        payload += serializeVarLen(sendingRequestUsername.toByteArray(Charsets.UTF_8))
        payload += serializeVarLen(hash.toByteArray(Charsets.UTF_8))
        return payload
    }

    companion object Deserializer : Deserializable<VerificationRequestPayload> {
        override fun deserialize(
            buffer: ByteArray,
            offset: Int
        ): Pair<VerificationRequestPayload, Int> {
            var localOffset = offset

            val (usernameBytes, usernameSize) = deserializeVarLen(buffer, localOffset)
            localOffset += usernameSize

            val (hashBytes, hashSize) = deserializeVarLen(buffer, localOffset)
            localOffset += hashSize

            return Pair(
                VerificationRequestPayload(
                    String(usernameBytes, Charsets.UTF_8),
                    String(hashBytes, Charsets.UTF_8)
                ),
                localOffset - offset
            )
        }
    }
}
