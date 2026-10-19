package com.mapaurbano.mobile.feature.common

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun ReportPhoto(bytes: ByteArray?, identity: String) {
    val bitmap by produceState<android.graphics.Bitmap?>(null, bytes, identity) {
        value = null
        if (bytes != null) value = withContext(Dispatchers.Default) {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
            if (options.outWidth !in 1..12000 || options.outHeight !in 1..12000) null
            else {
                options.inSampleSize = 1
                while (maxOf(options.outWidth, options.outHeight) / options.inSampleSize > 1200) options.inSampleSize *= 2
                options.inJustDecodeBounds = false
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
            }
        }
    }
    bitmap?.let {
        Image(it.asImageBitmap(), "Fotografía del reporte", modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp), contentScale = ContentScale.Fit)
    } ?: Text(if (bytes == null) "La fotografía se carga al abrir el reporte." else "No se pudo mostrar la fotografía.")
}
