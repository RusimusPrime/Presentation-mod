package com.example.presentationmod.client

import com.example.presentationmod.network.ModNetwork
import com.example.presentationmod.util.ScreenMath
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.Screen
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import com.mojang.blaze3d.vertex.PoseStack
import net.fabricmc.loader.api.FabricLoader
import org.lwjgl.util.tinyfd.TinyFileDialogs
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

class PresentationUploadScreen(private val screenPos: BlockPos) : Screen(Component.literal("Presentation Screen")) {

    private var status: Component = Component.literal("No presentation selected")

    override fun init() {
        val left = width / 2 - 100
        val top = height / 2 - 28

        addRenderableWidget(
            Button(left, top, 200, 20, Component.literal("Upload presentation")) { choosePresentationFile() }
        )

        addRenderableWidget(
            Button(left, top + 28, 200, 20, Component.literal("Close")) { onClose() }
        )
    }

    override fun render(poseStack: PoseStack, mouseX: Int, mouseY: Int, partialTick: Float) {
        renderBackground(poseStack)
        super.render(poseStack, mouseX, mouseY, partialTick)

        val boundsText = minecraft?.level?.let {
            val b = ScreenMath.measure(it, screenPos)
            "Screen size: ${b.width} x ${b.height}"
        } ?: "Screen size: -"

        drawCenteredString(poseStack, font, title, width / 2, height / 2 - 58, 0xFFFFFF)
        drawCenteredString(poseStack, font, status, width / 2, height / 2 + 28, 0xBFBFBF)
        drawCenteredString(poseStack, font, Component.literal(boundsText), width / 2, height / 2 + 42, 0x8FA8FF)
    }

    private fun choosePresentationFile() {
        val selectedPath = TinyFileDialogs.tinyfd_openFileDialog(
            "Choose presentation",
            "",
            null,
            "Presentations and images",
            false
        )
        if (selectedPath.isNullOrBlank()) return

        val selectedFile = File(selectedPath as String)
        val copiedFile = copyToPresentationsFolder(selectedFile)
        if (copiedFile == null) {
            status = Component.literal("Could not copy selected file")
            return
        }

        val bytes = runCatching { selectedFile.readBytes() }.getOrNull()
        if (bytes == null) {
            status = Component.literal("Could not read selected file")
            return
        }

        status = Component.literal("Uploading: ${copiedFile.name}")
        ModNetwork.uploadPresentation(screenPos, copiedFile.name, bytes)
        onClose()
    }

    private fun copyToPresentationsFolder(file: File): File? {
        return runCatching {
            val dir = FabricLoader.getInstance().gameDir.resolve("config/presentationmod/presentations")
            Files.createDirectories(dir)
            val target = dir.resolve(file.name)
            Files.copy(file.toPath(), target, StandardCopyOption.REPLACE_EXISTING)
            target.toFile()
        }.getOrNull()
    }
}
