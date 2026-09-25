package app.yuki.navigation

import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import app.yuki.R
import app.yuki.core.designsystem.component.YukiDetailScreen
import app.yuki.feature.account.SignInNavigation
import app.yuki.feature.account.SignInRoute as SignInScreenRoute
import app.yuki.feature.account.SignUpRoute as SignUpScreenRoute

internal fun NavGraphBuilder.signInDestination(navigator: YukiNavigator) {
    composable<SignInRoute> {
        YukiDetailScreen(
            title = stringResource(R.string.app_sign_in_title),
            onBackClick = navigator::navigateUp,
        ) {
            SignInScreenRoute(
                navigation = SignInNavigation(
                    onSignedIn = navigator::navigateUp,
                    onCreateAccountClick = navigator::swapToSignUp,
                ),
            )
        }
    }
}

internal fun NavGraphBuilder.signUpDestination(navigator: YukiNavigator) {
    composable<SignUpRoute> {
        YukiDetailScreen(
            title = stringResource(R.string.app_sign_up_title),
            onBackClick = navigator::navigateUp,
        ) {
            SignUpScreenRoute(onSignInClick = navigator::swapToSignIn)
        }
    }
}
