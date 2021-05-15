package chat.sphinx.scanner.ui

import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_viewmodel.BaseViewModel
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import javax.inject.Inject

@HiltViewModel
internal class ScannerViewModel @Inject constructor(
    dispatchers: CoroutineDispatchers,
): BaseViewModel<ScannerViewState>(dispatchers, ScannerViewState.Idle)
{
}
