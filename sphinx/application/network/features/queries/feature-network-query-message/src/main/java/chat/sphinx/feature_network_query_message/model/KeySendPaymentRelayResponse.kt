package chat.sphinx.feature_network_query_message.model

import chat.sphinx.concept_network_query_message.model.KeySendPaymentDto
import chat.sphinx.concept_network_relay_call.RelayResponse
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class KeySendPaymentRelayResponse(
    override val success: Boolean,
    override val response: KeySendPaymentDto?,
    override val error: String?
): RelayResponse<KeySendPaymentDto>()