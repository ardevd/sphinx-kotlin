package chat.sphinx.wrapper_message

import chat.sphinx.wrapper_common.*
import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_common.dashboard.ContactId
import chat.sphinx.wrapper_common.lightning.LightningPaymentHash
import chat.sphinx.wrapper_common.lightning.LightningPaymentRequest
import chat.sphinx.wrapper_common.lightning.Sat
import chat.sphinx.wrapper_common.message.*
import chat.sphinx.wrapper_message_media.MessageMedia
import chat.sphinx.wrapper_message_media.isAudio
import chat.sphinx.wrapper_message_media.isImage
import chat.sphinx.wrapper_message_media.isSphinxText
import chat.sphinx.wrapper_message_media.token.MediaUrl

@Suppress("NOTHING_TO_INLINE")
inline fun Message.retrieveTextToShow(): String? =
    messageContentDecrypted?.let { decrypted ->
        // TODO Handle podcast clips `clip::.....`
        if (giphyData != null) {
            return giphyData?.text
        }
        if (podBoost != null) {
            return null
        }
        if (isSphinxCallLink) {
            return null
        }
        if (type.isBotRes()) {
            return null
        }
        if (type.isInvoice()) {
            return null
        }
        decrypted.value
    }

//Invoice memo shows on a different TextView than messageContent
@Suppress("NOTHING_TO_INLINE")
inline fun Message.retrieveInvoiceTextToShow(): String? =
    messageContentDecrypted?.let { decrypted ->
        if (type.isInvoice() && !isExpiredInvoice) {
            return decrypted.value
        }
        return null
    }

@Suppress("NOTHING_TO_INLINE")
inline fun Message.retrieveBotResponseHtmlString(): String? =
    messageContentDecrypted?.let { decrypted ->
        if (type.isBotRes()) {
            return decrypted.value
        }
        return null
    }

@Suppress("NOTHING_TO_INLINE")
inline fun Message.retrievePaidTextAttachmentUrlAndMessageMedia(): Pair<String, MessageMedia?>? {
    var mediaData: Pair<String, MessageMedia?>? = null

    messageMedia?.let { media ->
        if (media.mediaType.isSphinxText) {
            mediaData = retrieveUrlAndMessageMedia()
        }
    }
    return mediaData
}

@Suppress("NOTHING_TO_INLINE")
inline fun Message.retrieveImageUrlAndMessageMedia(): Pair<String, MessageMedia?>? {
    var mediaData: Pair<String, MessageMedia?>? = null

    giphyData?.let { giphyData ->
        mediaData = giphyData.retrieveImageUrlAndMessageMedia()
    } ?: messageMedia?.let { media ->
        if (media.mediaType.isImage) {
            mediaData = retrieveUrlAndMessageMedia()
        }
    }
    return mediaData
}

@Suppress("NOTHING_TO_INLINE")
inline fun Message.retrieveUrlAndMessageMedia(): Pair<String, MessageMedia?>? {
    var mediaData: Pair<String, MessageMedia?>? = null

    messageMedia?.let { media ->
        val purchaseAcceptItem: Message? = if (isPaidMessage) {
            val item = retrievePurchaseItemOfType(MessageType.Purchase.Accepted)

            if (item?.messageMedia?.mediaKey?.value.isNullOrEmpty()) {
                null
            } else {
                item
            }
        } else {
            null
        }

        val url: MediaUrl? = if (this.type.isDirectPayment()) {
            media.templateUrl
        } else {
            purchaseAcceptItem?.messageMedia?.url ?: media.url
        }

        val messageMedia: MessageMedia = purchaseAcceptItem?.messageMedia ?: media

        if (messageMedia.localFile != null) {
            mediaData = Pair(
                url?.value?.let { if (it.isEmpty()) null else it } ?: "http://127.0.0.1",
                messageMedia,
            )
        } else {
            url?.let { mediaUrl ->
                mediaData = Pair(mediaUrl.value, messageMedia)
            }
        }
    }

    return mediaData
}

@Suppress("NOTHING_TO_INLINE")
inline fun Message.retrievePurchaseItemOfType(purchaseType: MessageType.Purchase): Message? {
    purchaseItems?.let { nnPurchaseItems ->
        if (nnPurchaseItems.isNotEmpty()) {
            for (item in nnPurchaseItems) {
                if (item.type == purchaseType) {
                    return item
                }
            }
        }
    }
    return null
}

@Suppress("NOTHING_TO_INLINE")
inline fun Message.retrievePurchaseStatus(): PurchaseStatus? {

    if (!isPaidMessage) {
        return null
    }

    var purchaseItem : Message? = null
    var purchaseAcceptItem : Message? = null
    var purchaseDenyItem : Message? = null

    purchaseItems?.let { nnPurchaseItems ->
        if (nnPurchaseItems.isNotEmpty()) {
            for (item in nnPurchaseItems) {
                if (item.type.isPurchaseProcessing()) {
                    purchaseItem = item
                }

                if (item.type.isPurchaseAccepted()) {
                    purchaseAcceptItem = item
                }

                if (item.type.isPurchaseDenied()) {
                    purchaseDenyItem = item
                }
            }
        }
    }

    purchaseAcceptItem?.let {
        return PurchaseStatus.Accepted
    } ?: purchaseDenyItem?.let {
        return PurchaseStatus.Denied
    } ?: purchaseItem?.let {
        return PurchaseStatus.Processing
    }

    return PurchaseStatus.Pending
}

@Suppress("NOTHING_TO_INLINE")
inline fun Message.retrieveSphinxCallLink(): SphinxCallLink? =
    messageContentDecrypted?.value?.toSphinxCallLink()

@Suppress("NOTHING_TO_INLINE")
inline fun Message.getColorKey(): String {
    senderAlias?.let { senderAlias ->
        return "message-${sender.value}-${senderAlias.value}-color"
    }
    return "message-${sender.value}-color"
}

@Suppress("NOTHING_TO_INLINE")
inline fun Message.hasSameSenderThanMessage(message: Message): Boolean {
    val hasSameSenderId = this.sender.value == message.sender.value
    val hasSameSenderAlias = (this.senderAlias?.value ?: "") == (message.senderAlias?.value ?: "")
    val hasSameSenderPicture = (this.senderPic?.value ?: "") == (message.senderPic?.value ?: "")

    return hasSameSenderId && hasSameSenderAlias && hasSameSenderPicture
}

@Suppress("NOTHING_TO_INLINE")
inline fun Message.shouldAvoidGrouping(): Boolean {
    return status.isPending() || status.isFailed() || status.isDeleted() ||
            type.isInvoice() || type.isInvoicePayment() || type.isGroupAction()
}

//Message Actions
inline val Message.isBoostAllowed: Boolean
    get() = status.isReceived() &&
            !type.isInvoice() &&
            !type.isDirectPayment() &&
            (uuid?.value ?: "").isNotEmpty()

inline val Message.isMediaAttachmentAvailable: Boolean
    get() = type.canContainMedia &&
            (retrieveImageUrlAndMessageMedia()?.second?.mediaKeyDecrypted?.value?.isNullOrEmpty() == false)

inline val Message.isCopyAllowed: Boolean
    get() = (this.retrieveTextToShow() ?: "").isNotEmpty() || (this.retrieveInvoiceTextToShow() ?: "").isNotEmpty()

inline val Message.isReplyAllowed: Boolean
    get() = (type.isAttachment() || type.isMessage() || type.isBotRes()) &&
            (uuid?.value ?: "").isNotEmpty()

inline val Message.isResendAllowed: Boolean
    get() = type.isMessage() && status.isFailed()

//Paid types
inline val Message.isPaidMessage: Boolean
    get() = type.isAttachment() && (messageMedia?.price?.value ?: 0L) > 0L

inline val Message.isPaidPendingMessage: Boolean
    get() = type.isAttachment() &&
            (messageMedia?.price?.value ?: 0L) > 0L &&
            (retrievePurchaseStatus()?.isPurchaseAccepted() != true)

inline val Message.isPaidTextMessage: Boolean
    get() = type.isAttachment() && messageMedia?.mediaType?.isSphinxText == true && (messageMedia?.price?.value ?: 0L) > 0L

inline val Message.isSphinxCallLink: Boolean
    get() = type.isMessage() && (messageContentDecrypted?.value?.isValidSphinxCallLink == true)

inline val Message.isAudioMessage: Boolean
    get() = type.isAttachment() && messageMedia?.mediaType?.isAudio == true

inline val Message.isPodcastBoost: Boolean
    get() = type.isBoost() && podBoost != null

inline val Message.isExpiredInvoice: Boolean
    get() = type.isInvoice() && !status.isConfirmed() && expirationDate != null && expirationDate!!.time < System.currentTimeMillis()

inline val Message.isPaidInvoice: Boolean
    get() = type.isInvoice() && status.isConfirmed()

abstract class Message {
    abstract val id: MessageId
    abstract val uuid: MessageUUID?
    abstract val chatId: ChatId
    abstract val type: MessageType
    abstract val sender: ContactId
    abstract val receiver: ContactId?
    abstract val amount: Sat
    abstract val paymentHash: LightningPaymentHash?
    abstract val paymentRequest: LightningPaymentRequest?
    abstract val date: DateTime
    abstract val expirationDate: DateTime?
    abstract val messageContent: MessageContent?
    abstract val status: MessageStatus
    abstract val seen: Seen
    abstract val senderAlias: SenderAlias?
    abstract val senderPic: PhotoUrl?
    abstract val originalMUID: MessageMUID?
    abstract val replyUUID: ReplyUUID?

    abstract val messageContentDecrypted: MessageContentDecrypted?
    abstract val messageDecryptionError: Boolean
    abstract val messageDecryptionException: Exception?
    abstract val messageMedia: MessageMedia?
    abstract val podBoost: PodBoost?
    abstract val giphyData: GiphyData?
    abstract val reactions: List<Message>?
    abstract val purchaseItems: List<Message>?
    abstract val replyMessage: Message?

    override fun equals(other: Any?): Boolean {
        return  other                               is Message                      &&
                other.id                            == id                           &&
                other.uuid                          == uuid                         &&
                other.chatId                        == chatId                       &&
                other.type                          == type                         &&
                other.sender                        == sender                       &&
                other.receiver                      == receiver                     &&
                other.amount                        == amount                       &&
                other.paymentHash                   == paymentHash                  &&
                other.paymentRequest                == paymentRequest               &&
                other.date                          == date                         &&
                other.expirationDate                == expirationDate               &&
                other.messageContent                == messageContent               &&
                other.status                        == status                       &&
                other.seen                          == seen                         &&
                other.senderAlias                   == senderAlias                  &&
                other.senderPic                     == senderPic                    &&
                other.originalMUID                  == originalMUID                 &&
                other.replyUUID                     == replyUUID                    &&
                other.messageContentDecrypted       == messageContentDecrypted      &&
                other.messageDecryptionError        == messageDecryptionError       &&
                other.messageDecryptionException    == messageDecryptionException   &&
                other.messageMedia                  == messageMedia                 &&
                other.podBoost                      == podBoost                     &&
                other.giphyData                     == giphyData                    &&
                other.reactions.let { a ->
                    reactions.let { b ->
                        (a.isNullOrEmpty() && b.isNullOrEmpty()) ||
                        (a?.containsAll(b ?: emptyList()) == true && b?.containsAll(a) == true)
                    }
                }                                                                   &&
                other.purchaseItems.let { a ->
                    purchaseItems.let { b ->
                        (a.isNullOrEmpty() && b.isNullOrEmpty()) ||
                                (a?.containsAll(b ?: emptyList()) == true && b?.containsAll(a) == true)
                    }
                }                                                                   &&
                other.replyMessage                  == replyMessage
    }

    companion object {
        @Suppress("ObjectPropertyName")
        private const val _17 = 17
        @Suppress("ObjectPropertyName")
        private const val _31 = 31
    }

    override fun hashCode(): Int {
        var result = _17
        result = _31 * result + id.hashCode()
        result = _31 * result + uuid.hashCode()
        result = _31 * result + chatId.hashCode()
        result = _31 * result + type.hashCode()
        result = _31 * result + sender.hashCode()
        result = _31 * result + receiver.hashCode()
        result = _31 * result + amount.hashCode()
        result = _31 * result + paymentHash.hashCode()
        result = _31 * result + paymentRequest.hashCode()
        result = _31 * result + date.hashCode()
        result = _31 * result + expirationDate.hashCode()
        result = _31 * result + messageContent.hashCode()
        result = _31 * result + status.hashCode()
        result = _31 * result + seen.hashCode()
        result = _31 * result + senderAlias.hashCode()
        result = _31 * result + senderPic.hashCode()
        result = _31 * result + originalMUID.hashCode()
        result = _31 * result + replyUUID.hashCode()
        result = _31 * result + messageContentDecrypted.hashCode()
        result = _31 * result + messageDecryptionError.hashCode()
        result = _31 * result + messageDecryptionException.hashCode()
        result = _31 * result + messageMedia.hashCode()
        result = _31 * result + podBoost.hashCode()
        result = _31 * result + giphyData.hashCode()
        reactions?.forEach { result = _31 * result + it.hashCode() }
        purchaseItems?.forEach { result = _31 * result + it.hashCode() }
        result = _31 * result + replyMessage.hashCode()
        return result
    }

    override fun toString(): String {
        return "Message(id=$id,uuid=$uuid,chatId=$chatId,type=$type,sender=$sender,"            +
                "receiver=$receiver,amount=$amount,paymentHash=$paymentHash,"                   +
                "paymentRequest=$paymentRequest,date=$date,expirationDate=$expirationDate,"     +
                "messageContent=$messageContent,status=$status,seen=$seen,"                     +
                "senderAlias=$senderAlias,senderPic=$senderPic,originalMUID=$originalMUID,"     +
                "replyUUID=$replyUUID,messageContentDecrypted=$messageContentDecrypted,"        +
                "messageDecryptionError=$messageDecryptionError,"                               +
                "messageDecryptionException=$messageDecryptionException,"                       +
                "messageMedia=$messageMedia,podBoost=$podBoost,giphyData=$giphyData,"           +
                "reactions=$reactions,purchaseItems=$purchaseItems,replyMessage=$replyMessage)"
    }
}
