package com.example.presentationmod.util

import com.example.presentationmod.PresentationMod
import java.awt.Color
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

object PresentationLoader {

    private val IMAGE_EXTENSIONS = setOf("png", "jpg", "jpeg", "webp", "bmp", "gif")

    fun loadSlides(file: File): List<BufferedImage> {
        if (!file.exists()) return emptyList()
        return try {
            when {
                file.isDirectory -> loadFromDirectory(file)
                file.extension.lowercase() == "pdf" -> loadPdf(file)
                file.extension.lowercase() == "pptx" -> loadPptx(file)
                file.extension.lowercase() in IMAGE_EXTENSIONS -> listOfNotNull(ImageIO.read(file))
                else -> emptyList()
            }
        } catch (t: Throwable) {
            PresentationMod.LOGGER.error("Failed to load slides from {}", file.absolutePath, t)
            emptyList()
        }
    }

    fun resolvePresentationSource(file: File): File {
        if (!file.isFile) return file
        val base = file.parentFile ?: return file
        val stem = file.nameWithoutExtension
        val siblingDir = File(base, stem)
        if (siblingDir.isDirectory) return siblingDir
        val slidesDir = File(base, "$stem slides")
        if (slidesDir.isDirectory) return slidesDir
        return file
    }

    fun loadPresentation(file: File): List<BufferedImage> = loadSlides(resolvePresentationSource(file))

    private fun loadFromDirectory(dir: File): List<BufferedImage> =
        dir.listFiles()
            ?.filter { it.isFile && it.extension.lowercase() in IMAGE_EXTENSIONS }
            ?.sortedBy { it.name.lowercase() }
            ?.mapNotNull { ImageIO.read(it) }
            .orEmpty()

    private fun loadPdf(file: File): List<BufferedImage> {
        val loader = resolveClassLoader("org.apache.pdfbox.pdmodel.PDDocument")
        if (loader == null) {
            PresentationMod.LOGGER.warn("PDFBox unavailable. Put PNG slides in folder {}", file.nameWithoutExtension)
            return emptyList()
        }

        val pdDocumentClass = Class.forName("org.apache.pdfbox.pdmodel.PDDocument", true, loader)
        val document = pdDocumentClass.getMethod("load", File::class.java).invoke(null, file)
        try {
            val rendererClass = Class.forName("org.apache.pdfbox.rendering.PDFRenderer", true, loader)
            val renderer = rendererClass.getConstructor(pdDocumentClass).newInstance(document)
            val pageCount = pdDocumentClass.getMethod("getNumberOfPages").invoke(document) as Int
            val renderMethod = rendererClass.getMethod("renderImageWithDPI", Int::class.javaPrimitiveType, Float::class.javaPrimitiveType)
            return (0 until pageCount).map { page ->
                renderMethod.invoke(renderer, page, 120f) as BufferedImage
            }
        } finally {
            pdDocumentClass.getMethod("close").invoke(document)
        }
    }

    private fun loadPptx(file: File): List<BufferedImage> {
        val loader = resolveClassLoader("org.apache.poi.xslf.usermodel.XMLSlideShow")
        if (loader == null) {
            PresentationMod.LOGGER.warn("POI unavailable. Use PNG slide folder fallback.")
            return emptyList()
        }

        file.inputStream().use { input ->
            val showClass = Class.forName("org.apache.poi.xslf.usermodel.XMLSlideShow", true, loader)
            val show = showClass.getConstructor(java.io.InputStream::class.java).newInstance(input)
            try {
                val pageSize = showClass.getMethod("getPageSize").invoke(show) as java.awt.Dimension
                val width = pageSize.width.coerceAtLeast(1)
                val height = pageSize.height.coerceAtLeast(1)
                val slides = showClass.getMethod("getSlides").invoke(show) as List<*>
                return slides.map { slide ->
                    val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
                    val graphics = image.createGraphics()
                    graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
                    graphics.color = Color.WHITE
                    graphics.fillRect(0, 0, width, height)
                    slide!!.javaClass.getMethod("draw", Graphics2D::class.java).invoke(slide, graphics)
                    graphics.dispose()
                    image
                }
            } finally {
                showClass.getMethod("close").invoke(show)
            }
        }
    }

    private fun resolveClassLoader(className: String): ClassLoader? {
        val candidates = listOfNotNull(
            Thread.currentThread().contextClassLoader,
            PresentationLoader::class.java.classLoader,
            ClassLoader.getSystemClassLoader()
        ).distinct()

        for (loader in candidates) {
            try {
                Class.forName(className, false, loader)
                return loader
            } catch (_: Throwable) {
            }
        }
        return null
    }
}