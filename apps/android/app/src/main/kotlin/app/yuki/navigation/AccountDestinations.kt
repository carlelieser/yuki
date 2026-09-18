package app.yuki.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import app.yuki.core.designsystem.component.YukiDetailScreen
import app.yuki.feature.account.AccountRoute as AccountScreenRoute
import app.yuki.feature.account.SignInNavigation
import app.yuki.feature.account.SignInRoute as SignInScreenRoute
import app.yuki.feature.account.SignUpRoute as SignUpScreenRoute

private const val ACCOUNT_TITLE = "Account"
private const val SIGN_IN_TITLE = "Sign in"
private const val SIGN_UP_TITLE = "Create an account"

internal fun NavGraphBuilder.accountDestination(
    navigator: YukiNavigator,
    bottomBarPadding: PaddingValues,
) {
    composable<AccountRoute> {
        YukiDetailScreen(title = ACCOUNT_TITLE, onBackClick = navigator::navigateUp) {
            AccountScreenRoute(
                onSignInClick = navigator::openSignIn,
                contentPadding = bottomBarPadding,
            )
        }
    }
}

internal fun NavGraphBuilder.signInDestination(navigator: YukiNavigator) {
    composable<SignInRoute> {
        YukiDetailScreen(title = SIGN_IN_TITLE, onBackClick = navigator::navigateUp) {
            SignInScreenRoute(
                navigation = SignInNavigation(
                    onSignedIn = navigator::navigateUp,
                    onCreateAccountClick = navigator::openSignUp,
                ),
            )
        }
    }
}

internal fun NavGraphBuilder.signUpDestination(navigator: YukiNavigator) {
    composable<SignUpRoute> {
        YukiDetailScreen(title = SIGN_UP_TITLE, onBackClick = navigator::navigateUp) {
            SignUpScreenRoute(onSignInClick = navigator::navigateUp)
        }
    }
}
