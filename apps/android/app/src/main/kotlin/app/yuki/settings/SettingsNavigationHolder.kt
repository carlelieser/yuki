package app.yuki.settings

import app.yuki.feature.account.AccountSettingsNavigation
import app.yuki.feature.library.LibrarySettingsNavigation
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsNavigationHolder @Inject constructor() :
    AccountSettingsNavigation,
    LibrarySettingsNavigation {
    private var signIn: () -> Unit = {}
    private var viewLibrary: () -> Unit = {}

    override val onSignInClick: () -> Unit get() = { signIn() }

    override val onViewLibraryClick: () -> Unit get() = { viewLibrary() }

    fun bind(onSignIn: () -> Unit, onViewLibrary: () -> Unit) {
        signIn = onSignIn
        viewLibrary = onViewLibrary
    }
}
