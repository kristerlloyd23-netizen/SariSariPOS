package com.sarisari.pos.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sarisari.pos.data.repository.ProductImageRepository

@Composable
fun ProductPhoto(
    imagePath: String?,
    imageRepository: ProductImageRepository,
    fallback: String,
    size: Dp,
    modifier: Modifier = Modifier
) {
    val bitmap = produceState<android.graphics.Bitmap?>(null, imagePath) {
        value = imageRepository.loadBitmap(imagePath)
    }.value
    Box(
        modifier.size(size).clip(RoundedCornerShape(12.dp)).background(androidx.compose.material3.MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(bitmap.asImageBitmap(), contentDescription = "Product photo", modifier = Modifier.size(size), contentScale = ContentScale.Crop)
        } else androidx.compose.material3.Text(fallback, style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
    }
}
