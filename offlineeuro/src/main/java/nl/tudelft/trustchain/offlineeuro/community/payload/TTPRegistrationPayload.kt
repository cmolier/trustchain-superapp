package nl.tudelft.trustchain.offlineeuro.community.payload

import nl.tudelft.ipv8.messaging.Deserializable
import nl.tudelft.ipv8.messaging.Serializable
import nl.tudelft.ipv8.messaging.deserializeVarLen
import nl.tudelft.ipv8.messaging.serializeVarLen

class TTPRegistrationPayload(
    val userName: String,
    val publicKey: ByteArray,
    val source: String
) : Serializable {
    override fun serialize(): ByteArray {
        var payload = ByteArray(0)
        payload += serializeVarLen(userName.toByteArray())
        payload += serializeVarLen(publicKey)
        payload += serializeVarLen(source.toByteArray())
        return payload
    }

    companion object Deserializer : Deserializable<TTPRegistrationPayload> {
        override fun deserialize(
            buffer: ByteArray,
            offset: Int
        ): Pair<TTPRegistrationPayload, Int> {
            var localOffset = offset

            val (nameBytes, nameSize) = deserializeVarLen(buffer, localOffset)
            localOffset += nameSize
            val (publicKeyBytes, publicKeyBytesSize) = deserializeVarLen(buffer, localOffset)
            localOffset += publicKeyBytesSize
            val (sourceBytes, sourceSize) = deserializeVarLen(buffer, localOffset)
            localOffset += sourceSize

            return Pair(
                TTPRegistrationPayload(
                    nameBytes.toString(Charsets.UTF_8),
                    publicKeyBytes,
                    sourceBytes.toString(Charsets.UTF_8)
                ),
                localOffset - offset
            )
        }
    }
}
