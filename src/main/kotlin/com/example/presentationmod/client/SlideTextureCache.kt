package com.example.presentationmod.client

import com.example.presentationmod.PresentationMod
import com.mojang.blaze3d.platform.NativeImage
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.texture.DynamicTexture
import net.minecraft.resources.ResourceLocation
import java.awt.image.BufferedImage
import java.util.concurrent.atomic.AtomicInteger

object SlideTextureCache {

    private val nextId = AtomicInteger()
    private val textures = mutableMapOf<String, ResourceLocation>()

    fun get(image: BufferedImage, cacheKey: String): ResourceLocation {
        return textures.getOrPut(cacheKey) {
            val id = nextId.incrementAndGet()
            val location = ResourceLocation(PresentationMod.MODID, "slide_$id")
            val native = toNativeImage(image)
            val dynamic = DynamicTexture(native)
            Minecraft.getInstance().textureManager.register(location, dynamic)
            location
        }
    }

    fun clear() {
        val manager = Minecraft.getInstance().textureManager
        textures.values.forEach { manager.release(it) }
        textures.clear()
    }

    fun getSolidColor(red: Int, green: Int, blue: Int): ResourceLocation {
        val image = BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB)
        val rgb = (red shl 16) or (green shl 8) or blue
        for (y in 0 until 4) {
            for (x in 0 until 4) {
                image.setRGB(x, y, rgb)
            }
        }
        return get(image, "solid_${red}_${green}_$blue")
    }

    private fun toNativeImage(image: BufferedImage): NativeImage {
        val rgb = if (image.type == BufferedImage.TYPE_INT_ARGB) image else {
            val converted = BufferedImage(image.width, image.height, BufferedImage.TYPE_INT_ARGB)
            val g = converted.createGraphics()
            g.drawImage(image, 0, 0, null)
            g.dispose()
            converted
        }
        val width = rgb.width
        val height = rgb.height
        val native = NativeImage(width, height, false)
        for (y in 0 until height) {
            for (x in 0 until width) {
                val argb = rgb.getRGB(x, y)
                val a = (argb ushr 24) and 0xFF
                val r = (argb ushr 16) and 0xFF
                val g = (argb ushr 8) and 0xFF
                val b = argb and 0xFF
                native.setPixelRGBA(x, y, (a shl 24) or (b shl 16) or (g shl 8) or r)
            }
        }
        return native
    }
}
