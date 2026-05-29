package com.example.presentationmod.client

import com.example.presentationmod.block.entity.PresentationScreenBlockEntity
import com.example.presentationmod.PresentationMod
import com.example.presentationmod.util.PresentationScreenHelper
import com.example.presentationmod.util.ScreenMath
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.client.renderer.LightTexture
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import org.joml.Matrix4f

class PresentationScreenRenderer(
    context: BlockEntityRendererProvider.Context
) : BlockEntityRenderer<PresentationScreenBlockEntity> {

    private val loggedRendered = mutableSetOf<String>()
    private val slideDepth = 0.08f

    override fun render(
        be: PresentationScreenBlockEntity,
        partialTick: Float,
        poseStack: PoseStack,
        bufferSource: MultiBufferSource,
        combinedLight: Int,
        combinedOverlay: Int
    ) {
        val level = be.level ?: return
        val bounds = ScreenMath.measure(level, be.blockPos)
        if (be.blockPos != bounds.controller) return

        val controller = PresentationScreenHelper.controllerEntity(level, be.blockPos) ?: return
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
            val (halfW, halfH) = fitAspect(planeAspect, imageAspect)
            val buffer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(texture))
            drawScreenQuad(buffer, poseStack.last().pose(), bounds, fullBright, combinedOverlay, halfW, halfH)
        } else if (controller.fileName != null) {
            val placeholder = SlideTextureCache.getSolidColor(180, 40, 40)
            val buffer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(placeholder))
            drawScreenQuad(buffer, poseStack.last().pose(), bounds, fullBright, combinedOverlay, 0.5f, 0.5f)
        }
    }

    private fun fitAspect(planeAspect: Float, imageAspect: Float): Pair<Float, Float> {
        return if (imageAspect > planeAspect) {
            val halfHeight = 0.5f * (planeAspect / imageAspect)
            0.5f to halfHeight
        } else {
            val halfWidth = 0.5f * (imageAspect / planeAspect)
            halfWidth to 0.5f
        }
    }

    private fun drawScreenQuad(
        consumer: VertexConsumer,
        matrix: Matrix4f,
        bounds: com.example.presentationmod.util.ScreenBounds,
        light: Int,
        overlay: Int,
        halfWidth: Float,
        halfHeight: Float
    ) {
        val cx = 0.5f + bounds.right.stepX * (bounds.width - 1) * 0.5f + bounds.facing.stepX * 0.64f
        val cy = 0.5f + (bounds.height - 1) * 0.5f
        val cz = 0.5f + bounds.right.stepZ * (bounds.width - 1) * 0.5f + bounds.facing.stepZ * 0.64f

        val hx = bounds.right.stepX * bounds.width * 0.94f * halfWidth
        val hz = bounds.right.stepZ * bounds.width * 0.94f * halfWidth
        val vy = bounds.height * 0.94f * halfHeight

        val nx = bounds.facing.stepX.toFloat()
        val nz = bounds.facing.stepZ.toFloat()
        val offsetX = nx * slideDepth * 0.5f
        val offsetZ = nz * slideDepth * 0.5f

        val frontX = cx + offsetX
        val frontZ = cz + offsetZ
        val backX = cx - offsetX
        val backZ = cz - offsetZ

        vertex(consumer, matrix, frontX - hx, cy + vy, frontZ - hz, 0f, 0f, light, overlay, nx, nz)
        vertex(consumer, matrix, frontX + hx, cy + vy, frontZ + hz, 1f, 0f, light, overlay, nx, nz)
        vertex(consumer, matrix, frontX + hx, cy - vy, frontZ + hz, 1f, 1f, light, overlay, nx, nz)
        vertex(consumer, matrix, frontX - hx, cy - vy, frontZ - hz, 0f, 1f, light, overlay, nx, nz)

        vertex(consumer, matrix, backX - hx, cy - vy, backZ - hz, 0f, 1f, light, overlay, -nx, -nz)
        vertex(consumer, matrix, backX + hx, cy - vy, backZ + hz, 1f, 1f, light, overlay, -nx, -nz)
        vertex(consumer, matrix, backX + hx, cy + vy, backZ + hz, 1f, 0f, light, overlay, -nx, -nz)
        vertex(consumer, matrix, backX - hx, cy + vy, backZ - hz, 0f, 0f, light, overlay, -nx, -nz)
    }

    private fun vertex(
        consumer: VertexConsumer,
        matrix: Matrix4f,
        x: Float,
        y: Float,
        z: Float,
        u: Float,
        v: Float,
        light: Int,
        overlay: Int,
        normalX: Float,
        normalZ: Float,
    ) {
        consumer.vertex(matrix, x, y, z).color(255, 255, 255, 255).uv(u, v)
            .overlayCoords(overlay).uv2(light).normal(normalX, 0f, normalZ).endVertex()
    }

    override fun shouldRenderOffScreen(be: PresentationScreenBlockEntity): Boolean = true
}
