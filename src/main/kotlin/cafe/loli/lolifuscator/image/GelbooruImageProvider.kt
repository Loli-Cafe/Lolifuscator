package cafe.loli.lolifuscator.image

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.loadImageBitmap
import org.jsoup.Jsoup
import java.net.URL
import java.util.concurrent.atomic.AtomicInteger

class GelbooruImageProvider {

    private var maxPages = 0
    private var tags = DEFAULT_TAGS

    var sorting = ""
    var rating = ""

    private var sortingTags = ""
    private var ratingTags = ""

    private val indexes = HashMap<String, AtomicInteger>()
    private val pages = HashMap<String, AtomicInteger>()
    private val images = ArrayList<String>()

    private var loaded = false

    fun load() {
        loaded = false
        sortingTags = when (sorting) {
            "New" -> ""
            "Score" -> "sort:score:desc"
            else -> "sort:random"
        }

        ratingTags = when (rating) {
            "Questionable" -> "rating:q"
            "Safe" -> "rating:s"
            else -> ""
        }

        build()
        loaded = true
    }

    fun image(): ImageBitmap {
        return loadImageBitmap(URL(images.get(index().get())).openStream().buffered())
    }

    fun nextImage() {
        if (index().incrementAndGet() >= images.size) {
            index().set(0)
            if (page().incrementAndGet() >= maxPages) {
                page().set(0)
            }

            build()
        }
    }

    fun previousImage() {
        if (index().decrementAndGet() < 0) {
            if (page().decrementAndGet() < 0) {
                page().set(0)
                index().set(0)
            } else {
                build()
                index().set(images.size - 1)
            }
        }
    }

    fun clear() {
        indexes.clear()
        pages.clear()
        images.clear()
    }

    fun isLoaded(): Boolean = loaded

    private fun page(): AtomicInteger {
        pages.computeIfAbsent(tags()) { AtomicInteger() }
        return pages.get(tags())!!
    }

    private fun index(): AtomicInteger {
        indexes.computeIfAbsent(tags()) { AtomicInteger() }
        return indexes.get(tags())!!
    }

    private fun build() {
        val element = Jsoup.connect(API_URL.format(tags())).get()
        val count = Integer.parseInt(element.getElementsByTag("posts").first()!!.attr("count"))

        images.clear()
        images.addAll(element.getElementsByTag("post").map { it.select("file_url").text() }.toList())
        maxPages = if (count <= LIMIT) 0 else count / LIMIT + if (count % LIMIT != 0) 1 else 0
    }

    private fun tags(): String = "%s+%s+%s".format(tags, sortingTags, ratingTags)

    companion object {
        private const val LIMIT = 100
        private const val DEFAULT_TAGS = "loli+-animated+-ai-generated"
        private const val API_URL = "https://gelbooru.com/index.php?page=dapi&s=post&q=index&limit=$LIMIT&tags=%s"
    }
}