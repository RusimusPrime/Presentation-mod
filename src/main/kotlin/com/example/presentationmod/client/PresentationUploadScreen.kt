package com.example.presentationmod.client

import com.example.presentationmod.network.ModNetwork
import com.example.presentationmod.util.ScreenMath
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.Screen
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraftforge.fml.loading.FMLPaths
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
            Button.builder(Component.literal("Upload presentation")) { choosePresentationFile() }
                .pos(left, top).size(200, 20).build()
        )

        addRenderableWidget(
            Button.builder(Component.literal("Close")) { onClose() }
                .pos(left, top + 28).size(200, 20).build()
        )
    }

    override fun render(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        renderBackground(guiGraphics)
        super.render(guiGraphics, mouseX, mouseY, partialTick)

        val boundsText = minecraft?.level?.let {
            val b = ScreenMath.measure(it, screenPos)
            "Screen size: ${b.width} x ${b.height}"
        } ?: "Screen size: -"

        guiGraphics.drawCenteredString(font, title, width / 2, height / 2 - 58, 0xFFFFFF)
        guiGraphics.drawCenteredString(font, status, width / 2, height / 2 + 28, 0xBFBFBF)
        guiGraphics.drawCenteredString(font, Component.literal(boundsText), width / 2, height / 2 + 42, 0x8FA8FF)
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
            val dir = FMLPaths.GAMEDIR.get().resolve("config/presentationmod/presentations")
            Files.createDirectories(dir)
            val target = dir.resolve(file.name)
            Files.copy(file.toPath(), target, StandardCopyOption.REPLACE_EXISTING)
            target.toFile()
        }.getOrNull()
    }
}
