package app.yuki.core.designsystem.theme

import androidx.compose.animation.core.SnapSpec
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.TweenSpec
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Test

class YukiMotionTest {
    @After
    fun resetMotion() {
        YukiMotion.isReduced = false
    }

    @Test
    fun progressUsesASpringWhenMotionIsAllowed() {
        YukiMotion.isReduced = false

        assertTrue(YukiMotion.progress<Float>() is SpringSpec)
    }

    @Test
    fun screenEnterUsesATweenWhenMotionIsAllowed() {
        YukiMotion.isReduced = false

        assertTrue(YukiMotion.enter<Float>() is TweenSpec)
    }

    @Test
    fun progressSnapsWhenMotionIsReduced() {
        YukiMotion.isReduced = true

        assertTrue(YukiMotion.progress<Float>() is SnapSpec)
    }

    @Test
    fun everySpecSnapsWhenMotionIsReduced() {
        YukiMotion.isReduced = true

        val specs = listOf(
            YukiMotion.enter<Float>(),
            YukiMotion.exit(),
            YukiMotion.fade(),
            YukiMotion.resize(),
            YukiMotion.stateFade(),
            YukiMotion.progress(),
            YukiMotion.spatial(),
        )

        assertTrue(specs.all { spec -> spec is SnapSpec })
    }
}
