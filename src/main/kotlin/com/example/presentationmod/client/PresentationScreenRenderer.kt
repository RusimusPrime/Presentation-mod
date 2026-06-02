package com.example.presentationmod.client

import com.example.presentationmod.block.entity.PresentationScreenBlockEntity
import com.example.presentationmod.PresentationMod
import com.example.presentationmod.util.ScreenMath
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.client.renderer.LightTexture
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import com.mojang.math.Matrix4f
import net.minecraft.core.Direction

class PresentationScreenRenderer(
    context: BlockEntityRendererProvider.Context
) : BlockEntityRenderer<PresentationScreenBlockEntity> {

    private val loggedRendered = mutableSetOf<String>()
    private val slideDepth = 0.02f
    private val screenFaceOffset = 0.5f - 3f / 16f
    private val onePixel = 1f / 16f
    private val screenPlaneBackOffset = 12f / 16f
    private val frameInsetPixels = 2f
    private val slideInsetPixels = 2f

    override fun render(
        be: PresentationScreenBlockEntity,
        partialTick: Float,
        poseStack: PoseStack,
        bufferSource: MultiBufferSource,
        combinedLight: Int,
        combinedOverlay: Int
    ) {
        if (!be.active) return
        val level = be.level ?: return
        val bounds = ScreenMath.measure(level, be.blockPos)
        if (be.blockPos != bounds.controller) return

        val controller = be
        val fullBright = LightTexture.pack(15, 15)

        val image = PresentationRenderer.getSlide(controller)
        if (image != null) {
            val renderKey = "${controller.blockPos}:${controller.fileName}:${controller.currentSlideIndex}"
            if (loggedRendered.add(renderKey)) {
                PresentationMod.LOGGER.info(
                    "Rendering slide file={}, index={}, image={}x{}, screen={}x{}, facing={}",
                    controller.fileName,
                    controller.currentSlideIndex,
                    image.width,
                    image.height,
                    bounds.width,
                    bounds.height,
                    bounds.facing,
                )
            }
            val slideCount = controller.fileName?.let { fileName ->
                ClientPresentationCache.networkSlideCount(controller.blockPos, fileName)
                    .takeIf { it > 0 }
                    ?: if (ClientPresentationCache.hasLocalPresentation(fileName)) {
                        ClientPresentationCache.slideCount(fileName)
                    } else {
                        0
                    }
            } ?: 0
            val textureIndex = if (slideCount > 0) controller.currentSlideIndex.mod(slideCount) else controller.currentSlideIndex
            val cacheKey = ClientPresentationCache.textureKey(controller.fileName, textureIndex, image)
            val texture = SlideTextureCache.get(image, cacheKey)
            val planeAspect = bounds.width.toFloat() / bounds.height.toFloat()
            val imageAspect = image.width.toFloat() / image.height.toFloat()
            val (halfW, halfH) = fitAspect(planeAspect, imageAspect, bounds.width, bounds.height)
            drawBackdrop(bufferSource, poseStack.last().pose(), bounds, fullBright, combinedOverlay)
            val buffer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(texture))
            drawScreenQuad(buffer, poseStack.last().pose(), bounds, fullBright, combinedOverlay, halfW, halfH, slideNormalOffset(), flipHorizontal = true)
        } else if (controller.fileName != null) {
            val placeholder = SlideTextureCache.getSolidColor(180, 40, 40)
            drawBackdrop(bufferSource, poseStack.last().pose(), bounds, fullBright, combinedOverlay)
            val buffer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(placeholder))
            drawScreenQuad(buffer, poseStack.last().pose(), bounds, fullBright, combinedOverlay, 0.5f, 0.5f, slideNormalOffset())
        }
    }

    private fun fitAspect(planeAspect: Float, imageAspect: Float, width: Int, height: Int): Pair<Float, Float> {
        val insetWidth = ((width * 16f - slideInsetPixels * 2f) / (width * 16f)).coerceIn(0.1f, 0.98f)
        val insetHeight = ((height * 16f - slideInsetPixels * 2f) / (height * 16f)).coerceIn(0.1f, 0.98f)
        return if (imageAspect > planeAspect) {
            val halfHeight = 0.5f * (planeAspect / imageAspect) * insetHeight
            0.5f * insetWidth to halfHeight
        } else {
            val halfWidth = 0.5f * (imageAspect / planeAspect) * insetWidth
            halfWidth to 0.5f * insetHeight
        }
    }

    private fun drawBackdrop(
        bufferSource: MultiBufferSource,
        matrix: Matrix4f,
        bounds: com.example.presentationmod.util.ScreenBounds,
        light: Int,
        overlay: Int,
    ) {
        val texture = SlideTextureCache.getSolidColor(72, 72, 72)
        val buffer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(texture))
        val insetWidth = ((bounds.width * 16f - frameInsetPixels * 2f) / (bounds.width * 16f)).coerceIn(0.1f, 1f)
        val insetHeight = ((bounds.height * 16f - frameInsetPixels * 2f) / (bounds.height * 16f)).coerceIn(0.1f, 1f)
        drawScreenQuad(buffer, matrix, bounds, light, overlay, 0.5f * insetWidth, 0.5f * insetHeight, backdropNormalOffset())
    }

    private fun backdropNormalOffset(): Float = screenFaceOffset + onePixel * 2f - screenPlaneBackOffset

    private fun slideNormalOffset(): Float = backdropNormalOffset() + onePixel

    private fun drawScreenQuad(
        consumer: VertexConsumer,
        matrix: Matrix4f,
        bounds: com.example.presentationmod.util.ScreenBounds,
        light: Int,
        overlay: Int,
        halfWidth: Float,
        halfHeight: Float,
        normalOffset: Float,
        flipHorizontal: Boolean = false,
        flipVertical: Boolean = false,
    ) {
        val normal = vector(bounds.facing)
        val right = vector(bounds.right)
        val up = vector(bounds.up)
        val center = Vec3f(
            0.5f + right.x * (bounds.width - 1) * 0.5f + up.x * (bounds.height - 1) * 0.5f + normal.x * normalOffset,
            0.5f + right.y * (bounds.width - 1) * 0.5f + up.y * (bounds.height - 1) * 0.5f + normal.y * normalOffset,
            0.5f + right.z * (bounds.width - 1) * 0.5f + up.z * (bounds.height - 1) * 0.5f + normal.z * normalOffset,
        )

        val halfRight = right.scale(bounds.width * halfWidth)
        val halfUp = up.scale(bounds.height * halfHeight)
        val depth = normal.scale(slideDepth * 0.5f)
        val front = center.plus(depth)
        val back = center.minus(depth)

        val leftU = if (flipHorizontal) 1f else 0f
        val rightU = if (flipHorizontal) 0f else 1f
        val topV = if (flipVertical) 1f else 0f
        val bottomV = if (flipVertical) 0f else 1f

        vertex(consumer, matrix, front.minus(halfRight).plus(halfUp), leftU, topV, light, overlay, normal)
        vertex(consumer, matrix, front.plus(halfRight).plus(halfUp), rightU, topV, light, overlay, normal)
        vertex(consumer, matrix, front.plus(halfRight).minus(halfUp), rightU, bottomV, light, overlay, normal)
        vertex(consumer, matrix, front.minus(halfRight).minus(halfUp), leftU, bottomV, light, overlay, normal)

        val backNormal = normal.scale(-1f)
        vertex(consumer, matrix, back.minus(halfRight).minus(halfUp), leftU, bottomV, light, overlay, backNormal)
        vertex(consumer, matrix, back.plus(halfRight).minus(halfUp), rightU, bottomV, light, overlay, backNormal)
        vertex(consumer, matrix, back.plus(halfRight).plus(halfUp), rightU, topV, light, overlay, backNormal)
        vertex(consumer, matrix, back.minus(halfRight).plus(halfUp), leftU, topV, light, overlay, backNormal)
    }

    private fun vertex(
        consumer: VertexConsumer,
        matrix: Matrix4f,
        pos: Vec3f,
        u: Float,
        v: Float,
        light: Int,
        overlay: Int,
        normal: Vec3f,
    ) {
        consumer.vertex(matrix, pos.x, pos.y, pos.z).color(255, 255, 255, 255).uv(u, v)
            .overlayCoords(overlay).uv2(light).normal(normal.x, normal.y, normal.z).endVertex()
    }

    override fun shouldRenderOffScreen(be: PresentationScreenBlockEntity): Boolean = true

    private fun vector(direction: Direction): Vec3f =
        Vec3f(direction.stepX.toFloat(), direction.stepY.toFloat(), direction.stepZ.toFloat())

    private data class Vec3f(val x: Float, val y: Float, val z: Float) {
        fun plus(other: Vec3f): Vec3f = Vec3f(x + other.x, y + other.y, z + other.z)
        fun minus(other: Vec3f): Vec3f = Vec3f(x - other.x, y - other.y, z - other.z)
        fun scale(value: Float): Vec3f = Vec3f(x * value, y * value, z * value)
    }
}
