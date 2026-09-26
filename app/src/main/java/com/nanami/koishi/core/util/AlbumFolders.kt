package com.nanami.koishi.core.util

/**
 * Pictures 目录下各图片应用的保存子目录，集中维护以便各工具相互独立、互不混杂。
 */
object AlbumFolders {

    const val ROOT = "Koishi"

    const val IMAGE_OBFUSCATION = "$ROOT/ImageObfuscation"

    const val IMAGE_STITCHING = "$ROOT/ImageStitching"

    const val GRID_SPLIT = "$ROOT/GridSplit"

    const val MIRAGE_TANK = "$ROOT/MirageTank"

    const val WATERMARK = "$ROOT/Watermark"

    const val QR_CODE = "$ROOT/QrCode"

    const val IMAGE_SKETCH = "$ROOT/ImageSketch"
    const val VIDEO_TO_GIF = "$ROOT/VideoToGif"
    const val BILI_COVER = "$ROOT/BiliCover"
}
