package nl.tudelft.trustchain.offlineeuro.community.message

import nl.tudelft.ipv8.Peer

class VerificationRequestMessage(
    val sendingRequestUsername: String,
    val hash: String,
    val requestingPeer: Peer
) : ICommunityMessage {
    override val messageType = CommunityMessageType.VerificationRequestMessage
}
