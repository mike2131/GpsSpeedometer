package com.example.gpsspeedometer

import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

object ShareUtils {
    fun shareGpx(context: android.content.Context, gpxContent: String, filename: String) {
        val file = File(context.cacheDir, filename)
        file.writeText(gpxContent)
        
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/gpx+xml"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "GPXを共有"))
    }
}
