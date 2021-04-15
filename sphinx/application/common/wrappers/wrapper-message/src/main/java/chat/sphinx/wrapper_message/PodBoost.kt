package chat.sphinx.wrapper_message

import chat.sphinx.wrapper_common.ItemId
import chat.sphinx.wrapper_common.lightning.Sat
import chat.sphinx.wrapper_common.message.FeedId
import com.squareup.moshi.JsonClass
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.Moshi

@Suppress("NOTHING_TO_INLINE")
inline fun String.toPodBoostOrNull(moshi: Moshi): PodBoost? =
    try {
        this.toPodBoost(moshi)
    } catch (e: Exception) {
        null
    }

@Throws(
    IllegalArgumentException::class,
    JsonDataException::class
)
fun String.toPodBoost(moshi: Moshi): PodBoost =
    moshi.adapter(PodBoostMoshi::class.java)
        .fromJson(this)
        ?.let {
            PodBoost(
                FeedId(it.feedID),
                ItemId(it.itemID),
                it.ts,
                Sat(it.amount)
            )
        }
        ?: throw IllegalArgumentException("Provided Json was invalid")

@Throws(AssertionError::class)
fun PodBoost.toJson(moshi: Moshi): String =
    moshi.adapter(PodBoostMoshi::class.java)
        .toJson(
            PodBoostMoshi(
                feedId.value,
                itemId.value,
                timeSeconds,
                amount.value
            )
        )

data class PodBoost(
    val feedId: FeedId,
    val itemId: ItemId,
    val timeSeconds: Int,
    val amount: Sat
)

// "{\"feedID\":226249,\"itemID\":1997782557,\"ts\":1396,\"amount\":100}"
@JsonClass(generateAdapter = true)
internal data class PodBoostMoshi(
    val feedID: Long,
    val itemID: Long,
    val ts: Int,
    val amount: Long
)