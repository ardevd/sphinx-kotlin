package chat.sphinx.concept_network_query_message.model

import chat.sphinx.concept_network_query_chat.model.BaseChatDto
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class MsgsChatDto(
    override val id: Long,
    override val uuid: String,
    override val name: String?,
    override val photo_url: String?,
    override val type: Int,
    override val status: Int?,
    override val contact_ids: List<Long>,
    override val is_muted: Boolean?,
    override val created_at: String,
    override val updated_at: String,
    override val deleted: Int,
    override val group_key: String?,
    override val host: String?,
    override val price_to_join: Long?,
    override val price_per_message: Long?,
    override val escrow_millis: Long?,
    override val unlisted: Boolean,
    override val private: Boolean?,
    override val owner_pub_key: String?,
    override val seen: Boolean,
    override val app_url: String?,
    override val feed_url: String?,
    override val meta: String?,
    override val my_photo_url: String?,
    override val my_alias: String?,
    override val skip_broadcast_joins: Int?,
    override val pending_contact_ids: List<Long>?
): BaseChatDto<Boolean?, Boolean, Boolean?, Boolean>()
