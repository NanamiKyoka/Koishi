package com.nanami.koishi.feature.tools.mini_apps.engine

import android.Manifest
import android.webkit.PermissionRequest
import androidx.annotation.StringRes
import com.nanami.koishi.R

/**
 * 网页权限请求与系统权限的映射，DRM 无需系统授权因此单独区分
 */
enum class MiniAppWebPermission(
    @StringRes val labelRes: Int,
    val androidPermissions: List<String>
) {
    CAMERA(
        R.string.mini_apps_permission_camera,
        listOf(Manifest.permission.CAMERA)
    ),
    MICROPHONE(
        R.string.mini_apps_permission_microphone,
        listOf(Manifest.permission.RECORD_AUDIO)
    ),
    LOCATION(
        R.string.mini_apps_permission_location,
        listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    );

    /**
     * 位置权限可能只授予粗略定位，因此任一命中即视为通过
     */
    fun isGrantedBy(granted: Map<String, Boolean>): Boolean =
        androidPermissions.any { granted[it] == true }

    companion object {

        fun fromResources(resources: Array<String>): List<MiniAppWebPermission> = buildList {
            resources.forEach { resource ->
                when (resource) {
                    PermissionRequest.RESOURCE_VIDEO_CAPTURE -> add(CAMERA)
                    PermissionRequest.RESOURCE_AUDIO_CAPTURE -> add(MICROPHONE)
                }
            }
        }.distinct()

        /**
         * 仅请求 DRM 这类无需系统授权的资源时可以直接放行，其余未知资源一律拒绝
         */
        fun canGrantWithoutSystemPermission(resources: Array<String>): Boolean =
            resources.isNotEmpty() &&
                    resources.all { it == PermissionRequest.RESOURCE_PROTECTED_MEDIA_ID }
    }
}
