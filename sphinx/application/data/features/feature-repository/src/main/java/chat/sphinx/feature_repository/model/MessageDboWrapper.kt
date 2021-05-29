package chat.sphinx.feature_repository.model

import chat.sphinx.conceptcoredb.MessageDbo
import chat.sphinx.wrapper_common.DateTime
import chat.sphinx.wrapper_common.PhotoUrl
import chat.sphinx.wrapper_common.Seen
import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_common.dashboard.ContactId
import chat.sphinx.wrapper_common.lightning.LightningPaymentHash
import chat.sphinx.wrapper_common.lightning.LightningPaymentRequest
import chat.sphinx.wrapper_common.lightning.Sat
import chat.sphinx.wrapper_common.message.MessageId
import chat.sphinx.wrapper_message.*
import chat.sphinx.wrapper_message.media.MessageMedia

class MessageDboWrapper(val messageDbo: MessageDbo): Message() {
    override val id: MessageId
        get() = messageDbo.id
    override val uuid: MessageUUID?
        get() = messageDbo.uuid
    override val chatId: ChatId
        get() = messageDbo.chat_id
    override val type: MessageType
        get() = messageDbo.type
    override val sender: ContactId
        get() = messageDbo.sender
    override val receiver: ContactId?
        get() = messageDbo.receiver_
    override val amount: Sat
        get() = messageDbo.amount
    override val paymentHash: LightningPaymentHash?
        get() = messageDbo.payment_hash
    override val paymentRequest: LightningPaymentRequest?
        get() = messageDbo.payment_request
    override val date: DateTime
        get() = messageDbo.date
    override val expirationDate: DateTime?
        get() = messageDbo.expiration_date
    override val messageContent: MessageContent?
        get() = messageDbo.message_content
    override val status: MessageStatus
        get() = messageDbo.status
    override val seen: Seen
        get() = messageDbo.seen
    override val senderAlias: SenderAlias?
        get() = messageDbo.sender_alias
    override val senderPic: PhotoUrl?
        get() = messageDbo.sender_pic
    override val originalMUID: MessageMUID?
        get() = messageDbo.original_muid
    override val replyUUID: ReplyUUID?
        get() = messageDbo.reply_uuid

    @Volatile
    @Suppress("PropertyName")
    var _messageContentDecrypted: MessageContentDecrypted? = messageDbo.message_content_decrypted
    override val messageContentDecrypted: MessageContentDecrypted?
        get() = _messageContentDecrypted

    @Volatile
    @Suppress("PropertyName")
    var _messageDecryptionError: Boolean = false
    override val messageDecryptionError: Boolean
        get() = _messageDecryptionError

    @Volatile
    @Suppress("PropertyName")
    var _messageDecryptionException: Exception? = null
    override val messageDecryptionException: Exception?
        get() = _messageDecryptionException

    @Volatile
    @Suppress("PropertyName")
    var _messageMedia: MessageMedia? = null
    override val messageMedia: MessageMedia?
        get() = _messageMedia

    @Volatile
    @Suppress("PropertyName")
    var _podBoost: PodBoost? = null
    override val podBoost: PodBoost?
        get() = _podBoost

    @Volatile
    @Suppress("PropertyName")
    var _giphyData: GiphyData? = null
    override val giphyData: GiphyData?
        get() = _giphyData

    @Volatile
    @Suppress("PropertyName")
    var _reactions: List<Message>? = null
    override val reactions: List<Message>?
        get() = _reactions
}