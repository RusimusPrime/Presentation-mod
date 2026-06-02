package com.example.presentationmod.client

import com.example.presentationmod.PresentationMod
import com.mojang.blaze3d.platform.NativeImage
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.texture.DynamicTexture
import net.minecraft.resources.ResourceLocation
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.concurrent.atomic.AtomicInteger
import javax.imageio.ImageIO

object SlideTextureCache {

    private val nextId = AtomicInteger()
    private val textures = mutableMapOf<String, ResourceLocation>()

    fun get(image: BufferedImage, cacheKey: String): ResourceLocation {
        return textures.getOrPut(cacheKey) {
            val id = nextId.incrementAndGet()
            val native = toNativeImage(image)
            val dynamic = DynamicTexture(native)
            Minecraft.getInstance().textureManager.register("presentation_slide_$id", dynamic)
        }
    }

    fun clear() {
        val manager = Minecraft.getInstance().textureManager
        textures.values.forEach { manager.release(it) }
        textures.clear()
    }

    fun getSolidColor(red: Int, green: Int, blue: Int): ResourceLocation {
        val key = "solid_${red}_${green}_$blue"
        textures[key]?.let { return it }

        val image = BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB)
        val rgb = (red shl 16) or (green shl 8) or blue
        for (y in 0 until 4) {
            for (x in 0 until 4) {
                image.setRGB(x, y, rgb)
            }
        }
        return get(image, key)
    }

    private fun toNativeImage(image: BufferedImage): NativeImage {
        val argb = if (image.type == BufferedImage.TYPE_INT_ARGB) image else {
            val converted = BufferedImage(image.width, image.height, BufferedImage.TYPE_INT_ARGB)
            val g = converted.createGraphics()
            g.drawImage(image, 0, 0, null)
            g.dispose()
            converted
        }

        val bytes = ByteArrayOutputStream()
        ImageIO.write(argb, "png", bytes)
        return NativeImage.read(ByteArrayInputStream(bytes.toByteArray()))
    }
}
