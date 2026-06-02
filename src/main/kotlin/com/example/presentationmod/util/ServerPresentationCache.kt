package com.example.presentationmod.util

import com.example.presentationmod.PresentationMod
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import javax.imageio.ImageIO

object ServerPresentationCache {

    private const val MAX_SLIDE_SIZE = 960

    private data class CachedPresentation(
        val sourcePath: String,
        val sourceModified: Long,
        val slides: List<BufferedImage>,
    )

    private val cache = mutableMapOf<String, CachedPresentation>()

    fun slideCount(file: File): Int = load(file).slides.size

    fun slideBytes(file: File, slideIndex: Int): ByteArray? {
        val slides = load(file).slides
        if (slides.isEmpty()) return null

        val image = slides[slideIndex.mod(slides.size)]
        val rgb = if (image.type == BufferedImage.TYPE_INT_RGB) image else {
            val converted = BufferedImage(image.width, image.height, BufferedImage.TYPE_INT_RGB)
            val graphics = converted.createGraphics()
            graphics.drawImage(image, 0, 0, null)
            graphics.dispose()
            converted
        }

        return ByteArrayOutputStream().use { output ->
            ImageIO.write(rgb, "jpg", output)
            output.toByteArray()
        }
    }

    private fun load(file: File): CachedPresentation {
        val key = file.absolutePath
        val cached = cache[key]
        if (cached != null && cached.sourceModified == file.lastModified()) return cached

        val slides = PresentationLoader.loadPresentation(file).map(::scaleForNetwork)
        PresentationMod.LOGGER.info("Server cached {} slide image(s) from {}", slides.size, file.absolutePath)

        return CachedPresentation(file.absolutePath, file.lastModified(), slides).also {
            cache[key] = it
        }
    }

    private fun scaleForNetwork(image: BufferedImage): BufferedImage {
        val longest = maxOf(image.width, image.height)
        if (longest <= MAX_SLIDE_SIZE) return image

        val scale = MAX_SLIDE_SIZE.toDouble() / longest.toDouble()
        val width = (image.width * scale).toInt().coerceAtLeast(1)
        val height = (image.height * scale).toInt().coerceAtLeast(1)
        val scaled = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        val graphics = scaled.createGraphics()
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
        graphics.drawImage(image, 0, 0, width, height, null)
        graphics.dispose()
        return scaled
    }
}
