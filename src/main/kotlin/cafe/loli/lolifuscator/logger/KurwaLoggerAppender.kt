package cafe.loli.lolifuscator.logger

import Sanity
import org.apache.log4j.AppenderSkeleton
import org.apache.log4j.Layout
import org.apache.log4j.net.SimpleSocketServer
import org.apache.log4j.spi.LoggingEvent
import java.io.FileNotFoundException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.concurrent.Executors
import java.util.function.Consumer

class KurwaLoggerAppender : AppenderSkeleton {
    constructor()
    constructor(layout: Layout?) {
        this.layout = layout
    }

    override fun append(event: LoggingEvent) {
        LOG_CONSUMER?.accept(layout.format(event))
    }

    override fun close() {
        closed = true
    }

    override fun requiresLayout(): Boolean = true

    companion object {
        var LOG_CONSUMER: Consumer<String>? = null

        init {
            try {
                KurwaLoggerAppender::class.java.getResourceAsStream("/log4j-server.properties").use {
                    if (it == null) throw FileNotFoundException()
                    val tempFile = Sanity.removeOnExit(Files.createTempFile(null, null))
                    Files.copy(it, tempFile, StandardCopyOption.REPLACE_EXISTING)
                    Executors.newSingleThreadExecutor()
                        .execute { SimpleSocketServer.main(arrayOf("6969", tempFile.toAbsolutePath().toString())) }
                }
            } catch (e: Exception) {
                throw RuntimeException(e)
            }
        }
    }
}
