package com.yo1000.demo

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody
import java.util.*
import java.util.jar.Attributes
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.Manifest

@SpringBootApplication
class DemoApplication

fun main(args: Array<String>) {
	System.setProperty("applicationVersion", getImplementationVersion() ?: "NOT-FOUND")
	System.setProperty("buildParentVersion", getBuildInfoProperties()["build.parent.version"]?.toString() ?: "NOT-FOUND")
	runApplication<DemoApplication>(*args)
}

fun getBuildInfoProperties(): Properties {
	val jarPath: String = getSelfJarPath() ?: return Properties()
	val jarFile = JarFile(jarPath)
	val buildInfoEntry: JarEntry = jarFile.getJarEntry("META-INF/build-info.properties")

	jarFile.getInputStream(buildInfoEntry).use { inputStream ->
		return Properties().also {
			it.load(inputStream)
		}
	}
}

fun getImplementationVersion(): String? {
	val jarPath: String = getSelfJarPath() ?: return null
	val manifest: Manifest = JarFile(jarPath).manifest

	val implVersionName = Attributes.Name("Implementation-Version")
	return manifest.mainAttributes.takeIf {
		it.containsKey(implVersionName)
	}?.let {
		it[implVersionName].toString()
	}
}

fun getSelfJarPath(): String? {
	val resource = DemoApplication::class.java.protectionDomain.codeSource.location

	if (resource.protocol.toLowerCase() != "jar" || !resource.path.matches(Regex("^file:/[^!]+!/.*$"))) {
		return null
	}

	val matched: MatchResult = Regex("^file:(/[^!]+)!/.*").find(resource.path) ?: return null
	return matched.groupValues[1]
}

@Configuration
@ConfigurationProperties(prefix = "demo")
class DemoProps(
		var applicationVersion: String = "",
		var buildParentVersion: String = ""
)

@Controller
@RequestMapping("/demo")
class DemoController(
		val demoProps: DemoProps
) {
	@GetMapping
	@ResponseBody
	fun get(): String = """
		applicationVersion: ${demoProps.applicationVersion}<br>
		buildParentVersion: ${demoProps.buildParentVersion}
	""".trimIndent()
}
