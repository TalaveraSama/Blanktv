package app.blanktv.tv.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import app.blanktv.tv.view.LibraryViewModel
import app.blanktv.tv.view.PlayerViewModel

/**
 * Builds ViewModels over the process-wide [AppContainer]. Every ViewModel takes the container as
 * its only constructor argument, so adding one is a single line here.
 */
class AppViewModelFactory(private val container: AppContainer) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when (modelClass) {
        LibraryViewModel::class.java -> LibraryViewModel(container)
        PlayerViewModel::class.java -> PlayerViewModel(container)
        else -> throw IllegalArgumentException("No binding registered for ${modelClass.name}")
    } as T
}
