package com.example.presentationmod.util

import java.io.File

object ServerFileUtil {

    fun presentationsRoot(serverDir: File): File {
        val dir = File(serverDir, "config/presentationmod/presentations")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }
}