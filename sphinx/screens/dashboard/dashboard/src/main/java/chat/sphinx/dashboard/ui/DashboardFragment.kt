package chat.sphinx.dashboard.ui

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.fragment.findNavController
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.viewpager2.widget.ViewPager2
import app.cash.exhaustive.Exhaustive
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.concept_image_loader.Disposable
import chat.sphinx.concept_image_loader.ImageLoader
import chat.sphinx.concept_image_loader.ImageLoaderOptions
import chat.sphinx.concept_image_loader.Transformation
import chat.sphinx.concept_user_colors_helper.UserColorsHelper
import chat.sphinx.dashboard.R
import chat.sphinx.dashboard.databinding.FragmentDashboardBinding
import chat.sphinx.dashboard.ui.viewstates.CreateTribeButtonViewState
import chat.sphinx.dashboard.ui.viewstates.DashboardTabsViewState
import chat.sphinx.dashboard.ui.viewstates.DeepLinkPopupViewState
import chat.sphinx.dashboard.ui.viewstates.NavDrawerViewState
import chat.sphinx.insetter_activity.InsetterActivity
import chat.sphinx.insetter_activity.addNavigationBarPadding
import chat.sphinx.insetter_activity.addStatusBarPadding
import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.Response
import chat.sphinx.resources.SphinxToastUtils
import chat.sphinx.wrapper_common.lightning.asFormattedString
import chat.sphinx.wrapper_common.lightning.toSat
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.navigation.CloseAppOnBackPress
import io.matthewnelson.android_feature_screens.ui.motionlayout.MotionLayoutFragment
import io.matthewnelson.android_feature_screens.util.*
import io.matthewnelson.android_feature_viewmodel.currentViewState
import io.matthewnelson.android_feature_viewmodel.updateViewState
import io.matthewnelson.concept_views.viewstate.collect
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
internal class DashboardFragment : MotionLayoutFragment<
        Any,
        Context,
        ChatListSideEffect,
        NavDrawerViewState,
        DashboardViewModel,
        FragmentDashboardBinding
        >(R.layout.fragment_dashboard), SwipeRefreshLayout.OnRefreshListener
{
    @Inject
    @Suppress("ProtectedInFinal")
    protected lateinit var imageLoader: ImageLoader<ImageView>

    @Inject
    @Suppress("ProtectedInFinal")
    protected lateinit var userColorsHelper: UserColorsHelper

    override val viewModel: DashboardViewModel by viewModels()
    override val binding: FragmentDashboardBinding by viewBinding(FragmentDashboardBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.screenInit()

        findNavController().addOnDestinationChangedListener(CloseDrawerOnDestinationChange())

        setupViewPager()
        setupDashboardHeader()
        setupNavBar()
        setupNavDrawer()
        setupPopups()
    }

    override fun onResume() {
        super.onResume()

        activity?.intent?.dataString?.let { deepLink ->
            viewModel.handleDeepLink(deepLink)
            activity?.intent?.data = null
        }
    }

    override fun onRefresh() {
        binding.swipeRefreshLayoutDataReload.isRefreshing = false
        viewModel.networkRefresh()
    }

    fun closeDrawerIfOpen(): Boolean {
        if (viewModel.currentViewState is NavDrawerViewState.Open) {
            viewModel.updateViewState(NavDrawerViewState.Closed)
            return true
        }
        return false
    }

    private inner class CloseDrawerOnDestinationChange: NavController.OnDestinationChangedListener {
        override fun onDestinationChanged(
            controller: NavController,
            destination: NavDestination,
            arguments: Bundle?
        ) {
            controller.removeOnDestinationChangedListener(this)
            viewModel.updateViewState(NavDrawerViewState.Closed)
        }
    }

    private fun setupViewPager() {
        binding.apply {
            swipeRefreshLayoutDataReload.setOnRefreshListener(this@DashboardFragment)

            val dashboardFragmentsAdapter = DashboardFragmentsAdapter(
                this@DashboardFragment
            )

            viewPagerDashboardTabs.adapter = dashboardFragmentsAdapter
            viewPagerDashboardTabs.isUserInputEnabled = false

            val tabs = tabLayoutDashboardTabs

            TabLayoutMediator(tabs, viewPagerDashboardTabs) { tab, position ->
                tab.text = dashboardFragmentsAdapter.getPageTitle(position)
            }.attach()

            viewPagerDashboardTabs.offscreenPageLimit = 3
            
            viewPagerDashboardTabs.post {
                viewPagerDashboardTabs.currentItem = viewModel.getCurrentPagePosition()

                viewPagerDashboardTabs.registerOnPageChangeCallback(object: ViewPager2.OnPageChangeCallback() {
                    override fun onPageScrolled(
                        position: Int,
                        positionOffset: Float,
                        positionOffsetPixels: Int
                    ) { }

                    override fun onPageSelected(position: Int) {
                        viewModel.updateTabsState(
                            feedActive = position == DashboardFragmentsAdapter.FEED_TAB_POSITION,
                            friendsActive = position == DashboardFragmentsAdapter.FRIENDS_TAB_POSITION,
                            tribesActive = position == DashboardFragmentsAdapter.TRIBES_TAB_POSITION,
                        )
                    }

                    override fun onPageScrollStateChanged(state: Int) { }
                })
            }

            val feedTab: View = LayoutInflater.from(this@DashboardFragment.context)
                .inflate(R.layout.layout_dashboard_custom_tab, tabs, false)
            tabs.getTabAt(DashboardFragmentsAdapter.FEED_TAB_POSITION)?.customView = feedTab

            val feedTitle = DashboardFragmentsAdapter.TAB_TITLES[DashboardFragmentsAdapter.FEED_TAB_POSITION]
            feedTab?.findViewById<TextView>(R.id.text_view_tab_title)?.text = getString(feedTitle)

            val friendsTab: View = LayoutInflater.from(this@DashboardFragment.context)
                .inflate(R.layout.layout_dashboard_custom_tab, tabs, false)
            tabs.getTabAt(DashboardFragmentsAdapter.FRIENDS_TAB_POSITION)?.customView = friendsTab

            val friendsTitle = DashboardFragmentsAdapter.TAB_TITLES[DashboardFragmentsAdapter.FRIENDS_TAB_POSITION]
            friendsTab?.findViewById<TextView>(R.id.text_view_tab_title)?.text = getString(friendsTitle)

            val tribesTab: View = LayoutInflater.from(this@DashboardFragment.context)
                .inflate(R.layout.layout_dashboard_custom_tab, tabs, false)
            tabs.getTabAt(DashboardFragmentsAdapter.TRIBES_TAB_POSITION)?.customView = tribesTab

            val tribesTitle = DashboardFragmentsAdapter.TAB_TITLES[DashboardFragmentsAdapter.TRIBES_TAB_POSITION]
            tribesTab?.findViewById<TextView>(R.id.text_view_tab_title)?.text = getString(tribesTitle)
        }
    }

    private fun setupDashboardHeader() {
        binding.layoutDashboardHeader.let { header ->
            val activity = (requireActivity() as InsetterActivity)

            activity.addStatusBarPadding(header.layoutConstraintDashboardHeader)

            val newHeaderHeight = header.layoutConstraintDashboardHeader.layoutParams.height + activity.statusBarInsetHeight.top

            binding.layoutMotionDashboard.getConstraintSet(R.id.motion_scene_dashboard_drawer_closed)?.let { constraintSet ->
                constraintSet.constrainHeight(R.id.layout_dashboard_header, newHeaderHeight)
            }

            binding.layoutMotionDashboard.getConstraintSet(R.id.motion_scene_dashboard_drawer_open)?.let { constraintSet ->
                constraintSet.constrainHeight(R.id.layout_dashboard_header, newHeaderHeight)
            }

            header.imageViewNavDrawerMenu.setOnClickListener {
                viewModel.updateViewState(NavDrawerViewState.Open)
            }

            header.textViewDashboardHeaderUpgradeApp.setOnClickListener {
                viewModel.goToAppUpgrade()
            }
            header.textViewDashboardHeaderNetwork.setOnClickListener {
                viewModel.toastIfNetworkConnected()
            }
        }
    }

    private fun setupNavBar() {
        binding.layoutDashboardNavBar.let { navBar ->

            (requireActivity() as InsetterActivity)
                .addNavigationBarPadding(navBar.layoutConstraintDashboardNavBar)

            navBar.navBarButtonPaymentReceive.setOnClickListener {
                lifecycleScope.launch { viewModel.navBarNavigator.toPaymentReceiveDetail() }
            }
            navBar.navBarButtonTransactions.setOnClickListener {
                lifecycleScope.launch { viewModel.navBarNavigator.toTransactionsDetail() }
            }
            navBar.navBarButtonScanner.setOnClickListener {
                viewModel.toScanner()
            }
            navBar.navBarButtonPaymentSend.setOnClickListener {
                lifecycleScope.launch { viewModel.navBarNavigator.toPaymentSendDetail() }
            }
        }
    }

    private fun setupNavDrawer() {
        binding.dashboardNavDrawerInputLock.setOnClickListener {
            viewModel.updateViewState(NavDrawerViewState.Closed)
        }

        binding.layoutDashboardNavDrawer.let { navDrawer ->
            (requireActivity() as InsetterActivity)
                .addStatusBarPadding(navDrawer.layoutConstraintDashboardNavDrawer)
                .addNavigationBarPadding(navDrawer.layoutConstraintDashboardNavDrawer)

            navDrawer.layoutConstraintDashboardNavDrawer.setOnClickListener { viewModel }

//            navDrawer.navDrawerButtonAddSats.setOnClickListener {
//                lifecycleScope.launch { viewModel.navDrawerNavigator.toAddSatsScreen() }
//            }
            navDrawer.navDrawerButtonContacts.setOnClickListener {
                lifecycleScope.launch { viewModel.navDrawerNavigator.toAddressBookScreen() }
            }
            navDrawer.navDrawerButtonProfile.setOnClickListener {
                lifecycleScope.launch { viewModel.navDrawerNavigator.toProfileScreen() }
            }
            navDrawer.layoutButtonAddFriend.layoutConstraintButtonAddFriend.setOnClickListener {
                lifecycleScope.launch { viewModel.navDrawerNavigator.toAddFriendDetail() }
            }
            navDrawer.layoutButtonCreateTribe.layoutConstraintButtonCreateTribe.setOnClickListener {
                lifecycleScope.launch { viewModel.navDrawerNavigator.toCreateTribeDetail() }
            }
            navDrawer.navDrawerButtonSupportTicket.setOnClickListener {
                lifecycleScope.launch { viewModel.navDrawerNavigator.toSupportTicketDetail() }
            }
            // TODO: Implement Private chat mode
//            navDrawer.navDrawerButtonLogout.setOnClickListener {
//                lifecycleScope.launch { viewModel.navDrawerNavigator.logout() }
//            }
        }
    }

    private fun setupPopups() {
        binding.layoutDashboardPopup.layoutDashboardAuthorizePopup.apply {
            textViewDashboardPopupClose.setOnClickListener {
                viewModel.deepLinkPopupViewStateContainer.updateViewState(
                    DeepLinkPopupViewState.PopupDismissed
                )
            }

            buttonAuthorize.setOnClickListener {
                viewModel.authorizeExternal()
            }
        }

        binding.layoutDashboardPopup.layoutDashboardPeopleProfilePopup.apply {
            textViewDashboardPopupClose.setOnClickListener {
                viewModel.deepLinkPopupViewStateContainer.updateViewState(
                    DeepLinkPopupViewState.PopupDismissed
                )
            }

            buttonSaveProfile.setOnClickListener {
                onStopSupervisor.scope.launch(viewModel.mainImmediate) {
                    viewModel.updatePeopleProfile()
                }
            }
        }

        binding.layoutDashboardPopup.layoutDashboardConnectPopup.apply {
            textViewDashboardPopupClose.setOnClickListener {
                viewModel.deepLinkPopupViewStateContainer.updateViewState(
                    DeepLinkPopupViewState.PopupDismissed
                )
            }

            buttonConnect.setOnClickListener {
                viewModel.connectToContact(
                    editTextDashboardPeoplePopupMessage.text?.toString()
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()

        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            viewModel.newVersionAvailable.asStateFlow().collect { newVersionAvailable ->
                binding.layoutDashboardHeader.textViewDashboardHeaderUpgradeApp.goneIfFalse(
                    newVersionAvailable
                )
            }
        }

        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            viewModel.currentVersion.asStateFlow().collect { currentVersion ->
                binding.layoutDashboardNavDrawer.textViewNavDrawerVersionNumber.apply {
                    text = currentVersion
                }
            }
        }

        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            viewModel.getAccountBalance().collect { nodeBalance ->
                if (nodeBalance == null) return@collect

                nodeBalance.balance.asFormattedString().let { balance ->
                    binding.layoutDashboardHeader.textViewDashboardHeaderBalance.text = balance
                    binding.layoutDashboardNavDrawer.navDrawerTextViewSatsBalance.text = balance
                }
            }
        }

        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            viewModel.networkStateFlow.collect { loadResponse ->
                binding.layoutDashboardHeader.let { dashboardHeader ->
                    @Exhaustive
                    when (loadResponse) {
                        is LoadResponse.Loading -> {
                            dashboardHeader.progressBarDashboardHeaderNetwork.invisibleIfFalse(true)
                            dashboardHeader.textViewDashboardHeaderNetwork.invisibleIfFalse(false)
                        }
                        is Response.Error -> {
                            dashboardHeader.progressBarDashboardHeaderNetwork.invisibleIfFalse(false)
                            dashboardHeader.textViewDashboardHeaderNetwork.invisibleIfFalse(true)
                            dashboardHeader.textViewDashboardHeaderNetwork.setTextColor(
                                ContextCompat.getColor(
                                    binding.root.context,
                                    R.color.primaryRed
                                )
                            )
                        }
                        is Response.Success -> {
                            dashboardHeader.progressBarDashboardHeaderNetwork.invisibleIfFalse(false)
                            dashboardHeader.textViewDashboardHeaderNetwork.invisibleIfFalse(true)
                            dashboardHeader.textViewDashboardHeaderNetwork.setTextColor(
                                ContextCompat.getColor(
                                    binding.root.context,
                                    R.color.primaryGreen
                                )
                            )
                        }
                    }
                }
            }
        }

        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            viewModel.accountOwnerStateFlow.collect { contactOwner ->
                contactOwner?.let { owner ->
                    owner.photoUrl?.value?.let { url ->
                        imageLoader.load(
                            binding.layoutDashboardNavDrawer.navDrawerImageViewUserProfilePicture,
                            url,
                            ImageLoaderOptions.Builder()
                                .placeholderResId(R.drawable.ic_profile_avatar_circle)
                                .transformation(Transformation.CircleCrop)
                                .build()
                        )
                    } ?: binding.layoutDashboardNavDrawer
                        .navDrawerImageViewUserProfilePicture
                        .setImageDrawable(
                            ContextCompat.getDrawable(
                                binding.root.context,
                                R.drawable.ic_profile_avatar_circle
                            )
                        )
                    binding.layoutDashboardNavDrawer.navDrawerTextViewProfileName.text =
                        owner.alias?.value ?: ""
                }
            }
        }
    }

    override suspend fun onViewStateFlowCollect(viewState: NavDrawerViewState) {
        @Exhaustive
        when (viewState) {
            NavDrawerViewState.Closed -> {
                binding.layoutMotionDashboard.setTransitionDuration(150)
            }
            NavDrawerViewState.Open -> {
                binding.layoutMotionDashboard.setTransitionDuration(300)
            }
        }
        viewState.transitionToEndSet(binding.layoutMotionDashboard)
    }

    private var disposable: Disposable? = null
    private var imageJob: Job? = null

    override fun subscribeToViewStateFlow() {
        super.subscribeToViewStateFlow()

        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            viewModel.tabsViewStateContainer.collect { viewState ->
                when (viewState) {
                    is DashboardTabsViewState.Idle -> {}

                    is DashboardTabsViewState.TabsState -> {
                        val tabs = binding.tabLayoutDashboardTabs

                        val feedTab = tabs.getTabAt(DashboardFragmentsAdapter.FEED_TAB_POSITION)?.customView
                        val friendsTab = tabs.getTabAt(DashboardFragmentsAdapter.FRIENDS_TAB_POSITION)?.customView
                        val tribesTab = tabs.getTabAt(DashboardFragmentsAdapter.TRIBES_TAB_POSITION)?.customView

                        feedTab?.findViewById<TextView>(R.id.text_view_tab_title)?.setTextColor(
                            ContextCompat.getColor(
                                binding.root.context,
                                if (viewState.feedActive) R.color.text else R.color.secondaryText
                            )
                        )

                        friendsTab?.findViewById<TextView>(R.id.text_view_tab_title)?.setTextColor(
                            ContextCompat.getColor(
                                binding.root.context,
                                if (viewState.friendsActive) R.color.text else R.color.secondaryText
                            )
                        )

                        friendsTab?.findViewById<View>(R.id.view_unseen_messages_dot)?.goneIfFalse(
                            viewState.friendsBadgeVisible
                        )

                        tribesTab?.findViewById<TextView>(R.id.text_view_tab_title)?.setTextColor(
                            ContextCompat.getColor(
                                binding.root.context,
                                if (viewState.tribesActive) R.color.text else R.color.secondaryText
                            )
                        )

                        tribesTab?.findViewById<View>(R.id.view_unseen_messages_dot)?.goneIfFalse(
                            viewState.tribesBadgeVisible
                        )
                    }
                }
            }
        }

        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            viewModel.deepLinkPopupViewStateContainer.collect { viewState ->
                @Exhaustive
                when (viewState) {
                    is DeepLinkPopupViewState.ExternalAuthorizePopup -> {
                        binding.layoutDashboardPopup.layoutDashboardAuthorizePopup.apply {
                            textViewDashboardPopupAuthorizeName.text = viewState.link.host
                            layoutConstraintAuthorizePopup.visible
                            root.visible
                        }
                        binding.layoutDashboardPopup.root.visible
                    }
                    is DeepLinkPopupViewState.ExternalAuthorizePopupProcessing -> {
                        binding.layoutDashboardPopup.layoutDashboardAuthorizePopup.progressBarAuthorize.visible
                    }
                    is DeepLinkPopupViewState.LoadingPeopleProfilePopup -> {
                        binding.layoutDashboardPopup.layoutDashboardPeopleProfilePopup.apply {
                            layoutConstraintLoadingProfile.visible
                            root.visible
                        }
                        binding.layoutDashboardPopup.root.visible
                    }
                    is DeepLinkPopupViewState.SavePeopleProfilePopup -> {
                        binding.layoutDashboardPopup.layoutDashboardPeopleProfilePopup.apply {
                            layoutConstraintLoadingProfile.gone

                            textViewDashboardPopupPeopleProfileHost.text = viewState.link.host

                            textViewDashboardPopupPeopleProfileTitle.text = getString(R.string.dashboard_save_profile_popup_title)
                            buttonSaveProfile.text = getString(R.string.dashboard_save_profile_button)
                        }
                        binding.layoutDashboardPopup.root.visible
                    }
                    is DeepLinkPopupViewState.DeletePeopleProfilePopup -> {
                        binding.layoutDashboardPopup.layoutDashboardPeopleProfilePopup.apply {
                            layoutConstraintLoadingProfile.gone

                            textViewDashboardPopupPeopleProfileHost.text = viewState.link.host

                            textViewDashboardPopupPeopleProfileTitle.text = getString(R.string.dashboard_delete_profile_popup_title)
                            buttonSaveProfile.text = getString(R.string.dashboard_delete_profile_button)
                        }

                        binding.layoutDashboardPopup.root.visible
                    }
                    is DeepLinkPopupViewState.SaveProfilePopupProcessing -> {
                        binding.layoutDashboardPopup.layoutDashboardPeopleProfilePopup.progressBarSaveProfile.visible
                    }
                    is DeepLinkPopupViewState.PeopleConnectPopupLoadingPersonInfo -> {
                        disposable?.dispose()
                        imageJob?.cancel()

                        binding.layoutDashboardPopup.layoutDashboardConnectPopup.apply {
                            layoutConstraintDashboardConnectLoading.visible
                            root.visible
                        }
                        binding.layoutDashboardPopup.root.visible
                    }
                    is DeepLinkPopupViewState.PeopleConnectPopup -> {
                        disposable?.dispose()
                        imageJob?.cancel()

                        binding.layoutDashboardPopup.layoutDashboardConnectPopup.apply {

                            val alias = viewState.alias
                            textViewDashboardPeoplePopupName.text = alias

                            editTextDashboardPeoplePopupMessage.hint = getString(R.string.dashboard_connect_initial_message_hint, alias)
                            textViewDashboardPeoplePopupDescription.text = viewState.description

                            val priceToMeet = (viewState.priceToMeet).toSat()?.asFormattedString(appendUnit = true) ?: ""
                            textViewDashboardPeoplePopupPriceToMeet.text = getString(R.string.dashboard_connect_price_to_meet, priceToMeet)

                            viewState.photoUrl?.let { url ->

                                lifecycleScope.launch {
                                    imageLoader.load(
                                        imageViewProfilePicture,
                                        url,
                                        ImageLoaderOptions.Builder()
                                            .placeholderResId(R.drawable.ic_profile_avatar_circle)
                                            .transformation(Transformation.CircleCrop)
                                            .build()
                                    ).also {
                                        disposable = it
                                    }
                                }.let { job ->
                                    imageJob = job
                                }

                            } ?: imageViewProfilePicture.setImageDrawable(
                                ContextCompat.getDrawable(
                                    binding.root.context,
                                    R.drawable.ic_profile_avatar_circle
                                )
                            )

                            layoutConstraintDashboardConnectLoading.gone
                            root.visible
                        }
                        binding.layoutDashboardPopup.root.visible
                    }
                    is DeepLinkPopupViewState.PeopleConnectPopupProcessing -> {
                        binding.layoutDashboardPopup.layoutDashboardConnectPopup.progressBarConnect.visible
                    }
                    is DeepLinkPopupViewState.PopupDismissed -> {
                        binding.layoutDashboardPopup.layoutDashboardAuthorizePopup.apply {
                            root.gone
                            progressBarAuthorize.gone
                        }

                        binding.layoutDashboardPopup.layoutDashboardPeopleProfilePopup.apply {
                            root.gone
                            progressBarSaveProfile.gone
                        }

                        binding.layoutDashboardPopup.layoutDashboardConnectPopup.apply {
                            root.gone
                            progressBarConnect.gone
                        }

                        binding.layoutDashboardPopup.root.gone
                    }
                }
            }
        }

        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            viewModel.createTribeButtonViewStateContainer.collect { viewState ->
                binding.layoutDashboardNavDrawer.let { navDrawer ->
                    @Exhaustive
                    when (viewState) {
                        is CreateTribeButtonViewState.Visible -> {
                            navDrawer.layoutButtonCreateTribe.layoutConstraintButtonCreateTribe.visible
                        }
                        is CreateTribeButtonViewState.Hidden -> {
                            navDrawer.layoutButtonCreateTribe.layoutConstraintButtonCreateTribe.gone
                        }
                    }
                }
            }
        }
    }

    override fun onViewCreatedRestoreMotionScene(
        viewState: NavDrawerViewState,
        binding: FragmentDashboardBinding
    ) {
        viewState.restoreMotionScene(binding.layoutMotionDashboard)
    }

    override fun getMotionLayouts(): Array<MotionLayout> {
        return arrayOf(binding.layoutMotionDashboard)
    }

    override suspend fun onSideEffectCollect(sideEffect: ChatListSideEffect) {
        sideEffect.execute(binding.root.context)
    }
}
