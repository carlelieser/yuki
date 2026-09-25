package app.yuki

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner
import androidx.work.testing.WorkManagerTestInitHelper
import dagger.hilt.android.testing.HiltTestApplication

class YukiTestRunner : AndroidJUnitRunner() {
    override fun newApplication(
        classLoader: ClassLoader?,
        className: String?,
        context: Context?,
    ): Application = super.newApplication(classLoader, HiltTestApplication::class.java.name, context)

    override fun callApplicationOnCreate(app: Application) {
        WorkManagerTestInitHelper.initializeTestWorkManager(app)
        super.callApplicationOnCreate(app)
    }
}
