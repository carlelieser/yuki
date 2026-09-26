package app.yuki

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import app.yuki.navigation.LaunchRequests
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: YukiAppViewModel by viewModels()
    private val launchRequests = LaunchRequests()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        splashScreen.setKeepOnScreenCondition { viewModel.theme.value == null }
        enableEdgeToEdge()
        if (savedInstanceState == null) launchRequests.handle(intent)
        setContent { YukiApp(viewModel, launchRequests) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        launchRequests.handle(intent)
    }
}
