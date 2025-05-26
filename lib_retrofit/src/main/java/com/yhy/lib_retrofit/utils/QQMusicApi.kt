package com.yhy.lib_retrofit.utils

/**
 * desc:
 **
 * user: xujj
 * time: 2025/3/18 10:21
 **/

/**
 * 专辑封面图
 */
fun getAlbumPicUrl(albummid: String, size: Int = 300) =
    "https://y.gtimg.cn/music/photo_new/T002R${size}x${size}M000$albummid.webp"

/**
 * 歌手封面图
 */
fun getArtistPicUrl(artistmid: String, size: Int = 300) =
    "https://y.qq.com/music/photo_new/T001R${size}x${size}M000$artistmid.webp"

/**
 * 播放地址
 */
const val musicUrl = "http://ws.stream.qqmusic.qq.com/"