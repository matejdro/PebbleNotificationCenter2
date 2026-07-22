package util

import com.android.build.api.dsl.CommonExtension
import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.api.variant.Variant
import com.android.build.api.variant.VariantBuilder
import org.gradle.api.Action
import org.gradle.api.Project

/**
 * android {} block that can be used without applying specific android plugin
 */
fun Project.commonAndroid(
   block: CommonExtension.() -> Unit,
) {
   (this as org.gradle.api.plugins.ExtensionAware).extensions.configure("android", block)
}

fun Project.isAndroidProject(): Boolean {
   return pluginManager.hasPlugin("com.android.base")
}

/**
 * androidComponents {} block that can be used without applying specific android plugin
 */
fun Project.commonAndroidComponents(
   block: Action<AndroidComponentsExtension<Unit, VariantBuilder, Variant>>,
) {
   (this as org.gradle.api.plugins.ExtensionAware).extensions.configure("androidComponents", block)
}
