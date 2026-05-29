package com.example.presentationmod.client

import com.example.presentationmod.PresentationMod
import com.example.presentationmod.util.PresentationLoader
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import net.minecraftforge.fml.loading.FMLPaths
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.File
import javax.imageio.ImageIO

@OnlyIn(Dist.CLIENT)
object ClientPresentationCache {

    private const val MAX_SLIDE_SIZE = 1280

    private var slides: List<BufferedImage> = emptyList()
    private var loadedFileName: String? = null
    private val networkPresentations = mutableMapOf<BlockPos, NetworkPresentation>()
    private val requestedSlides = mutableSetOf<String>()

    private data class NetworkPresentation(
        val fileName: String,
        val slideCount: Int,
        val slides: MutableMap<Int, BufferedImage>,
    )

    fun invalidate() {
        slides = emptyList()
        loadedFileName = null
        SlideTextureCache.clear()
    }

    fun preload(fileName: String?) {
        if (fileName == null) return
        ensureLoaded(fileName)
    }

    fun hasLocalPresentation(fileName: String): Boolean =
        File(presentationsDir(), fileName).isFile

    fun getSlide(fileName: String, index: Int): BufferedImage? {
        ensureLoaded(fileName)
        if (slides.isEmpty()) return null
        return slides[index.mod(slides.size)]
    }

    fun getNetworkSlide(pos: BlockPos, fileName: String?, index: Int): BufferedImage? {
        if (fileName == null) return null
        val presentation = networkPresentations[pos] ?: return null
        if (presentation.fileName != fileName || presentation.slideCount <= 0) return null
        return presentation.slides[index.mod(presentation.slideCount)]
    }

    fun slideCount(fileName: String): Int {
        ensureLoaded(fileName)
        return slides.size
    }

    fun networkSlideCount(pos: BlockPos, fileName: String?): Int {
        if (fileName == null) return 0
        val presentation = networkPresentations[pos] ?: return 0
        if (presentation.fileName != fileName) return 0
        return presentation.slideCount
    }

    fun shouldRequestNetworkSlide(pos: BlockPos, fileName: String, slideIndex: Int): Boolean =
        requestedSlides.add("${pos.asLong()}:$fileName:$slideIndex")

    fun acceptNetworkSlide(
        pos: BlockPos,
        fileName: String,
        slideIndex: Int,
        slideCount: Int,
        imageBytes: ByteArray,
    ) {
        val image = ImageIO.read(ByteArrayInputStream(imageBytes)) ?: return
        val presentation = networkPresentations.getOrPut(pos) {
            NetworkPresentation(fileName, slideCount, mutableMapOf())
        }

        if (presentation.fileName != fileName || presentation.slideCount != slideCount) {
            networkPresentations[pos] = NetworkPresentation(
                fileName,
                slideCount,
                mutableMapOf(slideIndex.mod(slideCount.coerceAtLeast(1)) to image),
            )
        } else {
            presentation.slides[slideIndex.mod(slideCount.coerceAtLeast(1))] = image
        }

        SlideTextureCache.get(image, textureKey(fileName, slideIndex.mod(slideCount.coerceAtLeast(1)), image))
        requestedSlides.remove("${pos.asLong()}:$fileName:$slideIndex")
        PresentationMod.LOGGER.info("Accepted network slide file={}, index={}, bytes={}", fileName, slideIndex, imageBytes.size)
    }

    private fun ensureLoaded(fileName: String) {
        if (loadedFileName == fileName) return
        loadedFileName = fileName

        val file = File(presentationsDir(), fileName)
        slides = if (file.exists()) {
            PresentationLoader.loadPresentation(file).map(::scaleForTexture)
        } else {
            emptyList()
        }

        if (slides.isEmpty()) {
            Minecraft.getInstance().player?.displayClientMessage(Component.literal("Failed to load slides: $fileName"), true)
            PresentationMod.LOGGER.warn("Failed to load slides from {}", file.absolutePath)
        } else {
            Minecraft.getInstance().player?.displayClientMessage(Component.literal("Loaded slides: ${slides.size}"), true)
            PresentationMod.LOGGER.info("Loaded {} slide image(s) from {}", slides.size, file.absolutePath)
            slides.forEachIndexed { index, image ->
                SlideTextureCache.get(image, textureKey(fileName, index, image))
            }
        }
    }

    private fun presentationsDir(): File =
        FMLPaths.GAMEDIR.get().resolve("config/presentationmod/presentations").toFile().also {
            if (!it.exists()) it.mkdirs()
        }

    private fun scaleForTexture(image: BufferedImage): BufferedImage {
        val longest = maxOf(image.width, image.height)
        if (longest <= MAX_SLIDE_SIZE) return image

        val scale = MAX_SLIDE_SIZE.toDouble() / longest.toDouble()
        val width = (image.width * scale).toInt().coerceAtLeast(1)
        val height = (image.height * scale).toInt().coerceAtLeast(1)
        val scaled = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        val graphics = scaled.createGraphics()
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
        graphics.drawImage(image, 0, 0, width, height, null)
        graphics.dispose()
        return scaled
    }

    fun textureKey(fileName: String?, index: Int, image: BufferedImage): String =
        "${fileName}_${index}_${image.width}x${image.height}"
}
