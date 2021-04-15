package chat.sphinx.di

import android.content.Context
import chat.sphinx.concept_network_client.NetworkClient
import chat.sphinx.concept_network_client_cache.NetworkClientCache
import chat.sphinx.concept_network_query_chat.NetworkQueryChat
import chat.sphinx.concept_network_query_contact.NetworkQueryContact
import chat.sphinx.concept_network_query_invite.NetworkQueryInvite
import chat.sphinx.concept_network_query_lightning.NetworkQueryLightning
import chat.sphinx.concept_network_query_message.NetworkQueryMessage
import chat.sphinx.concept_network_query_subscription.NetworkQuerySubscription
import chat.sphinx.concept_network_relay_call.NetworkRelayCall
import chat.sphinx.concept_relay.RelayDataHandler
import chat.sphinx.feature_network_client.NetworkClientImpl
import chat.sphinx.feature_network_query_chat.NetworkQueryChatImpl
import chat.sphinx.feature_network_query_contact.NetworkQueryContactImpl
import chat.sphinx.feature_network_query_invite.NetworkQueryInviteImpl
import chat.sphinx.feature_network_query_lightning.NetworkQueryLightningImpl
import chat.sphinx.feature_network_query_message.NetworkQueryMessageImpl
import chat.sphinx.feature_network_query_subscription.NetworkQuerySubscriptionImpl
import chat.sphinx.feature_network_relay_call.NetworkRelayCallImpl
import chat.sphinx.feature_relay.RelayDataHandlerImpl
import chat.sphinx.logger.SphinxLogger
import coil.util.CoilUtils
import com.squareup.moshi.Moshi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.matthewnelson.build_config.BuildConfigDebug
import io.matthewnelson.concept_authentication.data.AuthenticationStorage
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import io.matthewnelson.concept_encryption_key.EncryptionKeyHandler
import io.matthewnelson.feature_authentication_core.AuthenticationCoreManager
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideRelayDataHandlerImpl(
        authenticationStorage: AuthenticationStorage,
        authenticationCoreManager: AuthenticationCoreManager,
        dispatchers: CoroutineDispatchers,
        encryptionKeyHandler: EncryptionKeyHandler,
    ): RelayDataHandlerImpl =
        RelayDataHandlerImpl(
            authenticationStorage,
            authenticationCoreManager,
            dispatchers,
            encryptionKeyHandler,
        )

    @Provides
    fun provideRelayDataHandler(
        relayDataHandlerImpl: RelayDataHandlerImpl
    ): RelayDataHandler =
        relayDataHandlerImpl

    @Provides
    @Singleton
    fun provideNetworkClientImpl(
        @ApplicationContext appContext: Context,
        buildConfigDebug: BuildConfigDebug
    ): NetworkClientImpl =
        NetworkClientImpl(
            buildConfigDebug,
            CoilUtils.createDefaultCache(appContext)
        )

    @Provides
    fun provideNetworkClient(
        networkClientImpl: NetworkClientImpl
    ): NetworkClient =
        networkClientImpl

    @Provides
    fun provideNetworkClientCache(
        networkClientImpl: NetworkClientImpl
    ): NetworkClientCache =
        networkClientImpl

    @Provides
    @Singleton
    fun provideNetworkRelayCallImpl(
        dispatchers: CoroutineDispatchers,
        moshi: Moshi,
        networkClient: NetworkClient,
        relayDataHandler: RelayDataHandler,
        sphinxLogger: SphinxLogger,
    ): NetworkRelayCallImpl =
        NetworkRelayCallImpl(
            dispatchers,
            moshi,
            networkClient,
            relayDataHandler,
            sphinxLogger
        )

    @Provides
    fun provideNetworkRelayCall(
        networkRelayCallImpl: NetworkRelayCallImpl
    ): NetworkRelayCall =
        networkRelayCallImpl

    @Provides
    @Singleton
    fun provideNetworkQueryChatImpl(
        networkRelayCall: NetworkRelayCall
    ): NetworkQueryChatImpl =
        NetworkQueryChatImpl(networkRelayCall)

    @Provides
    fun provideNetworkQueryChat(
        networkQueryChatImpl: NetworkQueryChatImpl
    ): NetworkQueryChat =
        networkQueryChatImpl

    @Provides
    @Singleton
    fun provideNetworkQueryContactImpl(
        networkRelayCall: NetworkRelayCall
    ): NetworkQueryContactImpl =
        NetworkQueryContactImpl(networkRelayCall)

    @Provides
    fun provideNetworkQueryContact(
        networkQueryContactImpl: NetworkQueryContactImpl
    ): NetworkQueryContact =
        networkQueryContactImpl

    @Provides
    @Singleton
    fun provideNetworkQueryInviteImpl(
        networkRelayCall: NetworkRelayCall
    ): NetworkQueryInviteImpl =
        NetworkQueryInviteImpl(networkRelayCall)

    @Provides
    fun provideNetworkQueryInvite(
        networkQueryInviteImpl: NetworkQueryInviteImpl
    ): NetworkQueryInvite =
        networkQueryInviteImpl

    @Provides
    @Singleton
    fun provideNetworkQueryLightningImpl(
        networkRelayCall: NetworkRelayCall
    ): NetworkQueryLightningImpl =
        NetworkQueryLightningImpl(networkRelayCall)

    @Provides
    fun provideNetworkQueryLightning(
        networkQueryLightningImpl: NetworkQueryLightningImpl
    ): NetworkQueryLightning =
        networkQueryLightningImpl

    @Provides
    @Singleton
    fun provideNetworkQueryMessageImpl(
        networkRelayCall: NetworkRelayCall
    ): NetworkQueryMessageImpl =
        NetworkQueryMessageImpl(networkRelayCall)

    @Provides
    fun provideNetworkQueryMessage(
        networkQueryMessageImpl: NetworkQueryMessageImpl
    ): NetworkQueryMessage =
        networkQueryMessageImpl

    @Provides
    @Singleton
    fun provideNetworkQuerySubscriptionImpl(
        networkRelayCall: NetworkRelayCall
    ): NetworkQuerySubscriptionImpl =
        NetworkQuerySubscriptionImpl(networkRelayCall)

    @Provides
    fun provideNetworkQuerySubscription(
        networkQuerySubscriptionImpl: NetworkQuerySubscriptionImpl
    ): NetworkQuerySubscription =
        networkQuerySubscriptionImpl
}