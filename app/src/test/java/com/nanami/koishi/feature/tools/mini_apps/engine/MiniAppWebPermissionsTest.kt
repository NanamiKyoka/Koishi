package com.nanami.koishi.feature.tools.mini_apps.engine

import android.Manifest
import android.webkit.PermissionRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class MiniAppWebPermissionsTest {

    @Test
    fun `capture resources map to camera and microphone`() {
        val permissions = MiniAppWebPermission.fromResources(
            arrayOf(
                PermissionRequest.RESOURCE_VIDEO_CAPTURE,
                PermissionRequest.RESOURCE_AUDIO_CAPTURE
            )
        )

        assertEquals(
            listOf(MiniAppWebPermission.CAMERA, MiniAppWebPermission.MICROPHONE),
            permissions
        )
    }

    @Test
    fun `protected media can be granted without a system permission`() {
        val resources = arrayOf(PermissionRequest.RESOURCE_PROTECTED_MEDIA_ID)

        assertTrue(MiniAppWebPermission.fromResources(resources).isEmpty())
        assertTrue(MiniAppWebPermission.canGrantWithoutSystemPermission(resources))
    }

    @Test
    fun `unknown resources are never granted silently`() {
        assertTrue(MiniAppWebPermission.fromResources(arrayOf("android.webkit.resource.UNKNOWN")).isEmpty())
        assertFalse(
            MiniAppWebPermission.canGrantWithoutSystemPermission(
                arrayOf("android.webkit.resource.UNKNOWN")
            )
        )
        assertFalse(
            MiniAppWebPermission.canGrantWithoutSystemPermission(
                arrayOf(PermissionRequest.RESOURCE_VIDEO_CAPTURE)
            )
        )
        assertFalse(MiniAppWebPermission.canGrantWithoutSystemPermission(emptyArray()))
    }

    @Test
    fun `camera needs an exact grant`() {
        assertTrue(MiniAppWebPermission.CAMERA.isGrantedBy(mapOf(Manifest.permission.CAMERA to true)))
        assertFalse(MiniAppWebPermission.CAMERA.isGrantedBy(mapOf(Manifest.permission.CAMERA to false)))
        assertFalse(MiniAppWebPermission.CAMERA.isGrantedBy(mapOf(Manifest.permission.RECORD_AUDIO to true)))
    }

    @Test
    fun `location is granted by either the precise or the coarse permission`() {
        assertTrue(
            MiniAppWebPermission.LOCATION.isGrantedBy(
                mapOf(Manifest.permission.ACCESS_COARSE_LOCATION to true)
            )
        )
        assertTrue(
            MiniAppWebPermission.LOCATION.isGrantedBy(
                mapOf(
                    Manifest.permission.ACCESS_FINE_LOCATION to false,
                    Manifest.permission.ACCESS_COARSE_LOCATION to true
                )
            )
        )
        assertFalse(MiniAppWebPermission.LOCATION.isGrantedBy(emptyMap()))
    }
}
