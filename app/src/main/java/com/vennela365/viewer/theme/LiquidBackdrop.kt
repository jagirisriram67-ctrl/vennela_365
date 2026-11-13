package com.vennela365.viewer.theme

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Shader
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Canvas as ComposeCanvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * A pre-rasterised, heavily down-scaled copy of the app atmosphere.
 *
 * Glass surfaces sample this to produce a real frosted backdrop. Keeping the
 * copy small (roughly one ninth of the screen) is what makes true backdrop blur
 * affordable on mid and low-end hardware while still looking convincingly like
 * glass.
 */
class BackdropImage internal constructor(
    internal val bitmap: Bitmap,
    /** Backdrop bitmap pixels per screen pixel. Always <= 1. */
    internal val scale: Float,
    internal val image: ImageBitmap
) {
    internal fun recycle() {
        if (!bitmap.isRecycled) bitmap.recycle()
    }
}

internal val LocalBackdropImage = staticCompositionLocalOf<BackdropImage?> { null }

/** Target width of the down-scaled backdrop. Lower = cheaper and fuzzier. */
private const val BACKDROP_TARGET_WIDTH = 180

/**
 * Renders [render] into a low-resolution bitmap once per size change, draws it
 * full-bleed behind [content], and publishes it so [LiquidGlass] surfaces can
 * frost it.
 *
 * [render] receives a native canvas already scaled so that drawing in
 * screen-pixel coordinates fills the whole backdrop.
 */
@Composable
fun LiquidBackdrop(
    modifier: Modifier = Modifier,
    drawAtmosphere: Boolean = true,
    render: ComposeCanvas.(size: androidx.compose.ui.geometry.Size) -> Unit,
    content: @Composable BoxScope.() -> Unit
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val widthPx = constraints.maxWidth
        val heightPx = constraints.maxHeight

        val backdrop = remember(widthPx, heightPx) {
            val w = max(1, (widthPx * (BACKDROP_TARGET_WIDTH.toFloat() / widthPx)).roundToInt())
            val scale = BACKDROP_TARGET_WIDTH.toFloat() / widthPx
            val h = max(1, (heightPx * scale).roundToInt())
            val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val native = Canvas(bmp)
            val save = native.save()
            native.scale(scale, scale)
            render(ComposeCanvas(native), androidx.compose.ui.geometry.Size(widthPx.toFloat(), heightPx.toFloat()))
            native.restoreToCount(save)
            BackdropImage(bmp, scale, bmp.asImageBitmap())
        }

        DisposableEffect(backdrop) {
            onDispose { backdrop.recycle() }
        }

        CompositionLocalProvider(LocalBackdropImage provides backdrop) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (drawAtmosphere) {
                            Modifier.drawBehind {
                                drawImage(
                                    image = backdrop.image,
                                    dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt())
                                )
                            }
                        } else Modifier
                    )
            ) {
                content()
            }
        }
    }
}

/* ------------------------------------------------------------------------- */
/* Blur pipeline.                                                             */
/* ------------------------------------------------------------------------- */

internal object BackdropBlur {

    /** Converts a requested frost radius (dp) into backdrop-bitmap pixels. */
    fun blurRadiusPx(requestedDp: Float, backdropScale: Float, density: Float): Float =
        (requestedDp * density * backdropScale).coerceIn(0.6f, 22f)

    /**
     * Crops [src] to [srcRect] (source pixels), rescales to [outWidth] x
     * [outHeight] and blurs it.
     */
    fun cropScaleBlur(
        src: Bitmap,
        srcRect: Rect,
        outWidth: Int,
        outHeight: Int,
        radiusPx: Float
    ): Bitmap {
        val w = max(1, outWidth)
        val h = max(1, outHeight)
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
        if (radiusPx >= 0.6f) {
            // Software blur first so even API 26–30 sees real frosting.
            paint.maskFilter = android.graphics.BlurMaskFilter(
                radiusPx,
                android.graphics.BlurMaskFilter.Blur.NORMAL
            )
        }
        canvas.drawBitmap(src, srcRect, Rect(0, 0, w, h), paint)
        return out
    }

    fun shaderFor(bitmap: Bitmap, dest: androidx.compose.ui.geometry.Size): Shader {
        val shader = BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        val matrix = Matrix()
        matrix.setScale(
            if (bitmap.width == 0) 1f else dest.width / bitmap.width,
            if (bitmap.height == 0) 1f else dest.height / bitmap.height
        )
        shader.setLocalMatrix(matrix)
        return shader
    }
}

/**
 * Frosts the backdrop inside an already-clipped region and returns the shader
 * that produced it, or null when no backdrop is available.
 *
 * Coordinates come from [BackdropTarget], which records the glass bounds in
 * root space so the correct slice of the backdrop is sampled.
 */
internal fun androidx.compose.ui.draw.CacheDrawScope.frostBackdrop(
    backdrop: BackdropImage?,
    target: BackdropTarget,
    radiusDp: Float,
    density: Float
): Shader? {
    if (backdrop == null || backdrop.bitmap.isRecycled) return null
    if (target.size.width <= 0 || target.size.height <= 0) return null

    val s = backdrop.scale
    val srcLeft = (target.position.x * s * DOWNSAMPLE).toInt()
    val srcTop = (target.position.y * s * DOWNSAMPLE).toInt()
    val srcW = max(1, (target.size.width * s * DOWNSAMPLE).toInt())
    val srcH = max(1, (target.size.height * s * DOWNSAMPLE).toInt())

    // Inflate the sample so the blur kernel has neighbouring pixels to pull in.
    val inflate = (radiusDp * density * s * 2f).toInt()
    val full = Rect(0, 0, backdrop.bitmap.width, backdrop.bitmap.height)
    val src = Rect(srcLeft, srcTop, srcLeft + srcW, srcTop + srcH)
    val srcClamped = Rect(
        max(full.left, src.left - inflate),
        max(full.top, src.top - inflate),
        minOf(full.right, src.right + inflate),
        minOf(full.bottom, src.bottom + inflate)
    )

    val radiusPx = BackdropBlur.blurRadiusPx(radiusDp, s, density)
    val outW = max(1, (size.width / DOWNSAMPLE).toInt())
    val outH = max(1, (size.height / DOWNSAMPLE).toInt())

    val sampled = BackdropBlur.cropScaleBlur(backdrop.bitmap, srcClamped, outW, outH, radiusPx)
    val shader = BackdropBlur.shaderFor(sampled, size)
    return shader
}

/** Down-sampling for the glass layer itself; keeps the frost GPU-cheap. */
internal const val DOWNSAMPLE = 1f

/** Glass bounds recorded in root (screen) coordinates for backdrop sampling. */
internal data class BackdropTarget(
    val position: IntOffset = IntOffset.Zero,
    val size: IntSize = IntSize.Zero
)
