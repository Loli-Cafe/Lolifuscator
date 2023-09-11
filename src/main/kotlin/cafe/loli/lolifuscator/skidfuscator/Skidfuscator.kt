package cafe.loli.lolifuscator.skidfuscator

import Sanity
import java.lang.ProcessBuilder.Redirect
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.function.Predicate
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream

/*
    I know that using ProcessBuilder isn't ideal but there is no other way for now
 */
class Skidfuscator(val path: Path) {

    val workingDir: Path
    val version: String
    var skidfuscatorPatchedPath: Path? = null

    init {
        workingDir =
            (if (path.parent == null || path.parent.toAbsolutePath() == WORKING_PATH.toAbsolutePath()) WORKING_PATH else path.parent)/*.resolve("lolifuscator")*/

        version = getVersion0()
    }

    private fun getVersion0(): String {
        val processBuilder = ProcessBuilder().command("java", "-jar", path.toAbsolutePath().toString(), "-V")
            .directory(workingDir.toFile())

        var process: Process? = null
        try {
            process = Sanity.closeOnExit(processBuilder.start())
            process.inputReader(Charsets.UTF_8).use {
                return it.readLine() ?: "Unknown"
            }
        } finally {
            process?.children()?.forEach { it.destroy() }
            process?.destroy()
        }
    }

    fun generateStarter(choice: String, onComplete: () -> Unit): ExecutorService {
        val inputPath = Sanity.removeOnExit(Files.createTempFile(null, null))
        Files.writeString(inputPath, choice + "\n", Charsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING)

        val processBuilder = ProcessBuilder().command(
            "java",
            "-Dorg.beryx.textio.TextTerminal=org.beryx.textio.system.SystemTextTerminal",
            "-jar",
            path.toAbsolutePath().toString(),
            "starter"
        )
            .redirectInput(inputPath.toFile())
            .redirectOutput(Redirect.DISCARD)
            .directory(workingDir.toFile())

        val executor = Executors.newSingleThreadExecutor()
        executor.execute {
            var process: Process? = null
            val complete: () -> Unit = {
                process?.children()?.forEach { it.destroy() }
                process?.destroy()
                executor.shutdown()
                onComplete.invoke()
            }

            try {
                process = Sanity.closeOnExit(processBuilder.start())
                process.waitFor()
                process.errorReader(Charsets.UTF_8).use {
                    if (it.lines().count() > 0)
                        complete()
                }
            } finally {
                complete()
            }
        }

        return executor
    }

    fun obfuscate(
        configPath: String,
        inputFile: String,
        outputFile: String,
        libsDir: String,
        exemptFile: String,
        rtFile: String,
        fuckIt: Boolean,
        lc: Boolean,
        ph: Boolean,
        re: Boolean,
        networkError: () -> Unit,
        logAppender: () -> Unit,
        onComplete: () -> Unit
    ): ExecutorService {
        val executor = Executors.newSingleThreadExecutor()
        executor.execute {
//            if (checkInternetAccess().not()) {
//                networkError.invoke()
//                executor.shutdown()
//                onComplete.invoke()
//            }

            if (skidfuscatorPatchedPath == null) {
                skidfuscatorPatchedPath = Sanity.removeOnExit(rewriteSkidfuscator(path))
            }

            val arguments = ArrayList<String>()
            arguments.addAll(
                listOf(
                    "java",
                    "-Dorg.beryx.textio.TextTerminal=org.beryx.textio.system.SystemTextTerminal",
                    "-jar",
                    skidfuscatorPatchedPath?.toAbsolutePath().toString(),
                    "obfuscate"
                )
            )
            arguments.add("-cfg=${configPath}")
            if (outputFile.isNotEmpty() && outputFile.isNotBlank()) {
                val tempOutput = Path.of(outputFile)
                if (Files.isDirectory(tempOutput).not())
                    arguments.add("-o=${tempOutput.toAbsolutePath()}")
            }

            if (libsDir.isNotEmpty() && libsDir.isNotBlank()) {
                val tempLibs = Path.of(libsDir)
                if (Files.exists(tempLibs) && Files.isDirectory(tempLibs))
                    arguments.add("-li=${tempLibs.toAbsolutePath()}")
            }

            if (exemptFile.isNotEmpty() && exemptFile.isNotBlank()) {
                val exemptTemp = Path.of(exemptFile)
                if (Files.exists(exemptTemp) && Files.isDirectory(exemptTemp).not())
                    arguments.add("-ex=${exemptTemp.toAbsolutePath()}")
            }

            if (rtFile.isNotEmpty() && rtFile.isNotBlank()) {
                val rtTemp = Path.of(rtFile)
                if (Files.exists(rtTemp) && Files.isDirectory(rtTemp).not())
                    arguments.add("-rt=${rtTemp.toAbsolutePath()}")
            }

            if (fuckIt) arguments.add("-fuckit")
            if (lc) arguments.add("-lc")
            if (ph) arguments.add("-ph")
            if (re) arguments.add("-re")

            arguments.add(Path.of(inputFile).toAbsolutePath().toString())

            val mappings = workingDir.resolve("mappings").resolve("jvm")
            val oldMappings = workingDir.resolve("mappings").resolve(".old_jvm${System.currentTimeMillis()}")
            if (Files.exists(mappings) && Files.isDirectory(mappings)) {
                Files.move(mappings, oldMappings)
            }

            logAppender.invoke()
            val processBuilder = ProcessBuilder()
                .command(arguments)
                .directory(workingDir.toFile())

            var process: Process? = null
            val complete: () -> Unit = {
                process?.children()?.forEach { it.destroy() }
                process?.destroy()
                executor.shutdown()
                onComplete.invoke()
            }
            try {
                process = Sanity.closeOnExit(processBuilder.start())
                process.waitFor()
                process.errorReader(Charsets.UTF_8).use {
                    if (it.lines().count() > 0)
                        complete()
                }
            } finally {
                complete()
            }
        }

        return executor
    }

    //TODO: Move it to separate class maybe
    companion object {
        const val MAIN_CLASS: String = "dev.skidfuscator.client.Client"
        const val LOG4J_FILE_NAME: String = "log4j.properties"

        val WORKING_PATH = Path.of("./")
        var skidfuscatorJar = findSkidfuscator()
        val log4jProperties = Sanity.removeOnExit(Files.createTempFile(null, null))

        init {
            Skidfuscator::class.java.getResourceAsStream("/.${LOG4J_FILE_NAME}").use {
                it?.let {
                    Files.copy(it, log4jProperties, StandardCopyOption.REPLACE_EXISTING)
                }
            }
        }

        private fun findSkidfuscator(): Path? {
            Files.walk(WORKING_PATH, 1).use {
                return it?.filter(Predicate.not(Files::isDirectory))?.filter(Skidfuscator::isSkidfuscator)?.findFirst()
                    ?.orElse(null)
            }
        }

        fun isSkidfuscator(path: Path): Boolean {
            try {
                JarFile(path.toString()).use {
                    it.manifest?.let { manifest ->
                        manifest.mainAttributes.getValue("Main-Class")?.let { mainClass ->
                            return mainClass.equals(MAIN_CLASS)
                        }
                    }
                }
            } catch (ignored: Exception) {
            }
            return false
        }

        private fun rewriteSkidfuscator(path: Path): Path {
            val tempFile = Sanity.removeOnExit(Files.createTempFile(null, null))
            JarFile(path.toString()).use { input ->
                JarOutputStream(Files.newOutputStream(tempFile)).use { output ->
                    input.entries().toList().forEach { jarEntry ->
                        if (jarEntry.name.equals(LOG4J_FILE_NAME)) {
                            output.putNextEntry(JarEntry(LOG4J_FILE_NAME))
                            output.write(Files.readAllBytes(log4jProperties))
                        } else {
                            input.getInputStream(jarEntry)?.use { stream ->
                                output.putNextEntry(JarEntry(jarEntry.name))
                                output.write(stream.readAllBytes())
                            }
                        }
                    }
                }
            }

            return tempFile
        }

//        fun checkInternetAccess(): Boolean {
//            var connection: HttpURLConnection? = null
//            try {
//                connection = URL("https://skidfuscator.dev/").openConnection() as HttpURLConnection
//                return connection.responseCode == 200
//            } catch (ignored: Exception) {
//
//            } finally {
//                connection?.disconnect()
//                connection = null
//            }
//
//            return false
//        }
    }
}