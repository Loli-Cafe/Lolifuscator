import java.nio.file.Files
import java.nio.file.Path

class Sanity {
    companion object {
        val processes = ArrayList<Process?>()
        val files = ArrayList<Path>()

        init {
            Runtime.getRuntime().addShutdownHook(Thread {
                close()
                remove()
            })
        }

        fun remove() {
            files.forEach {
                Files.deleteIfExists(it)
            }
        }

        fun close() {
            processes.forEach {
                it?.children()?.forEach { it.destroy() }
                it?.destroy()
            }
        }

        fun removeOnExit(path: Path): Path {
            files.add(path)
            return path
        }

        fun closeOnExit(process: Process): Process {
            processes.add(process)
            return process
        }
    }
}