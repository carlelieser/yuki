package app.yuki.core.shizuku

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserServiceVersion @Inject constructor() {
    val code: Int = BuildConfig.VERSION_CODE
}
