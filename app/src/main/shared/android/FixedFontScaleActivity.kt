package com.winlator.cmod.shared.android

import android.content.res.Configuration
import androidx.activity.ComponentActivity
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentActivity

private const val LOCKED_APP_FONT_SCALE = 1f

private fun lockFontScale(configuration: Configuration?): Configuration? =
    configuration?.let {
        Configuration(it).apply {
            fontScale = LOCKED_APP_FONT_SCALE
        }
    }

open class FixedFontScaleAppCompatActivity : AppCompatActivity() {
    override fun applyOverrideConfiguration(overrideConfiguration: Configuration?) {
        // Let AppCompat own base-context locale setup. Rewriting the base context here
        // can interfere with per-app language recreation after setApplicationLocales().
        super.applyOverrideConfiguration(lockFontScale(overrideConfiguration))
    }
}

open class FixedFontScaleComponentActivity : ComponentActivity() {
    override fun applyOverrideConfiguration(overrideConfiguration: Configuration?) {
        super.applyOverrideConfiguration(lockFontScale(overrideConfiguration))
    }
}

open class FixedFontScaleFragmentActivity : FragmentActivity() {
    override fun applyOverrideConfiguration(overrideConfiguration: Configuration?) {
        super.applyOverrideConfiguration(lockFontScale(overrideConfiguration))
    }
}
