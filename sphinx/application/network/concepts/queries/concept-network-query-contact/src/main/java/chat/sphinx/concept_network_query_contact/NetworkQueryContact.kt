package chat.sphinx.concept_network_query_contact

import chat.sphinx.concept_network_query_contact.model.GetContactsResponse
import chat.sphinx.kotlin_response.ResponseError
import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.wrapper_relay.AuthorizationToken
import chat.sphinx.wrapper_relay.RelayUrl
import kotlinx.coroutines.flow.Flow

abstract class NetworkQueryContact {

    ///////////
    /// GET ///
    ///////////
    abstract fun getContacts(
        relayData: Pair<AuthorizationToken, RelayUrl>? = null
    ): Flow<LoadResponse<GetContactsResponse, ResponseError>>
//    abstract fun getContacts(): Flow<LoadResponse<GetContactsResponse, ResponseError>>
//
//    abstract fun getContacts(
//        authorizationToken: AuthorizationToken,
//        relayUrl: RelayUrl
//    ): Flow<LoadResponse<GetContactsResponse, ResponseError>>

    ///////////
    /// PUT ///
    ///////////
//    app.put('/contacts/:id', contacts.updateContact)

    ////////////
    /// POST ///
    ////////////
//    app.post('/contacts/tokens', contacts.generateToken)
//    app.post('/contacts/:id/keys', contacts.exchangeKeys)
//    app.post('/contacts', contacts.createContact)

    //////////////
    /// DELETE ///
    //////////////
//    app.delete('/contacts/:id', contacts.deleteContact)
}