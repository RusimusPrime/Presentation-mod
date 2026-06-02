package com.example.presentationmod.block

import net.minecraft.util.StringRepresentable

enum class ScreenPart(private val serializedName: String) : StringRepresentable {
    SINGLE("single"),
    MIDDLE("middle"),
    LEFT("left"),
    RIGHT("right"),
    UP("up"),
    DOWN("down"),
    LEFT_UP("left_up"),
    LEFT_DOWN("left_down"),
    RIGHT_UP("right_up"),
    RIGHT_DOWN("right_down");

    override fun getSerializedName(): String = serializedName
}
