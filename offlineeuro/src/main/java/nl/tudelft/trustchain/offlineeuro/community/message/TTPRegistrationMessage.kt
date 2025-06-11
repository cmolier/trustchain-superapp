package nl.tudelft.trustchain.offlineeuro.community.message

class TTPRegistrationMessage(
    val userName: String,
    val userPKBytes: ByteArray,
    val peerPublicKeyBytes: ByteArray,
    val source: String
) : ICommunityMessage {
    override val messageType = CommunityMessageType.TTPRegistrationMessage
}
