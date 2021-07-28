package chat.sphinx.tribe_detail.navigation

import androidx.navigation.NavController
import chat.sphinx.wrapper_common.dashboard.ChatId
import io.matthewnelson.concept_navigation.BaseNavigationDriver
import io.matthewnelson.concept_navigation.Navigator

abstract class TribeDetailNavigator(
    navigationDriver: BaseNavigationDriver<NavController>
): Navigator<NavController>(navigationDriver) {
    abstract suspend fun closeDetailScreen()

    abstract suspend fun goBackToDashboard()

    abstract suspend fun toShareTribeScreen(
        qrText: String,
        viewTitle: String,
        description: String? = null,
    )

    abstract suspend fun toCreateTribeScreen(chatId: ChatId)
}