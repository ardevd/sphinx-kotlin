package chat.sphinx.newsletter_detail.ui

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import chat.sphinx.concept_repository_chat.ChatRepository
import chat.sphinx.newsletter_detail.R
import chat.sphinx.newsletter_detail.navigation.NewsletterDetailNavigator
import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_common.feed.toFeedUrl
import chat.sphinx.wrapper_common.feed.toSubscribed
import chat.sphinx.wrapper_feed.Feed
import chat.sphinx.wrapper_feed.FeedItem
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_navigation.util.navArgs
import io.matthewnelson.android_feature_viewmodel.BaseViewModel
import io.matthewnelson.android_feature_viewmodel.updateViewState
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

internal inline val NewsletterDetailFragmentArgs.chatId: ChatId
    get() = ChatId(argChatId)

@HiltViewModel
internal class NewsletterDetailViewModel @Inject constructor(
    dispatchers: CoroutineDispatchers,
    savedStateHandle: SavedStateHandle,
    private val app: Application,
    private val chatRepository: ChatRepository,
    val navigator: NewsletterDetailNavigator
): BaseViewModel<NewsletterDetailViewState>(dispatchers, NewsletterDetailViewState.Idle)
{
    private val args: NewsletterDetailFragmentArgs by savedStateHandle.navArgs()

    private val newsletterSharedFlow: SharedFlow<Feed?> = flow {
        emitAll(chatRepository.getFeedByChatId(args.chatId))
    }.distinctUntilChanged().shareIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(2_000),
        replay = 1,
    )

    init {
        viewModelScope.launch(mainImmediate) {
            newsletterSharedFlow.collect { feed ->
                feed?.let { nnFeed ->
                    updateViewState(
                        NewsletterDetailViewState.FeedLoaded(
                            nnFeed.imageUrlToShow,
                            nnFeed.title,
                            nnFeed.description,
                            nnFeed.items
                        )
                    )
                }
            }
        }

        updateFeedContentInBackground()
    }

    private fun updateFeedContentInBackground() {
        viewModelScope.launch(mainImmediate) {
            chatRepository.getChatById(args.chatId).firstOrNull()?.let { chat ->
                chat.host?.let { chatHost ->
                    args.argFeedUrl.toFeedUrl()?.let { feedUrl ->
                        chatRepository.updateFeedContent(
                            chatId = chat.id,
                            host = chatHost,
                            feedUrl = feedUrl,
                            chatUUID = chat.uuid,
                            true.toSubscribed(),
                            currentEpisodeId = null
                        )
                    }
                }
            }
        }
    }

    fun newsletterItemSelected(item: FeedItem) {
        viewModelScope.launch(mainImmediate) {
            navigator.toWebViewDetail(
                app.getString(R.string.newsletter_article),
                item.enclosureUrl
            )
        }
    }
}