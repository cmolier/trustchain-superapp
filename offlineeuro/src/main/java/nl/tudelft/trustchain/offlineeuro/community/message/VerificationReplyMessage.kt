package nl.tudelft.trustchain.offlineeuro.community.message

class VerificationReplyMessage(
    val result: String,
) : ICommunityMessage {
    override val messageType = CommunityMessageType.VerificationReplyMessage
}
