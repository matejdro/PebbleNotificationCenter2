import org.gradle.plugin.use.PluginDependenciesSpec
import org.gradle.plugin.use.PluginDependencySpec

inline val PluginDependenciesSpec.parcelize: PluginDependencySpec
   get() = id("kotlin-parcelize")
