package app.yuki

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import app.yuki.core.auth.BrowserAuthResults
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class BrowserAuthRedirectActivity : ComponentActivity() {
    @Inject
    lateinit var results: BrowserAuthResults

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        results.deliver(intent?.dataString)

        val returnToApp = Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        startActivity(returnToApp)
        finish()
    }
}
