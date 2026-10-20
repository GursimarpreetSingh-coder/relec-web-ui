package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import com.example.R
import com.example.data.ReLectRepository

@Composable
fun BrandLogoView(
    repository: ReLectRepository? = null,
    customLogoUri: String? = null,
    isRevolt: Boolean = false,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    contentDescription: String? = null
) {
    val activeCustomLogo = if (repository != null) {
        val blueUri by repository.customBlueLogoUri.collectAsState()
        val redUri by repository.customRedLogoUri.collectAsState()
        if (isRevolt) redUri else blueUri
    } else {
        customLogoUri
    }

    if (!activeCustomLogo.isNullOrBlank()) {
        AsyncImage(
            model = activeCustomLogo,
            contentDescription = contentDescription ?: if (isRevolt) "RE-VOLT Logo" else "ReLec Logo",
            modifier = modifier,
            contentScale = contentScale
        )
    } else {
        val defaultDrawable = if (isRevolt) {
            R.drawable.img_revolt_red_logo_1791094396822
        } else {
            R.drawable.img_relec_blue_logo_1791094376351
        }
        Image(
            painter = painterResource(id = defaultDrawable),
            contentDescription = contentDescription ?: if (isRevolt) "RE-VOLT Logo" else "ReLec Logo",
            modifier = modifier,
            contentScale = contentScale
        )
    }
}
