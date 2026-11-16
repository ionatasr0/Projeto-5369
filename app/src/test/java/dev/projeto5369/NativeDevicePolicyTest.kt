package dev.projeto5369

import com.liskovsoft.googlecommon.common.models.auth.UserCode
import com.liskovsoft.googlecommon.common.models.auth.AccessToken
import com.liskovsoft.googlecommon.common.models.auth.ErrorResponse
import com.liskovsoft.youtubeapi.auth.V2.OndaDevicePolicy
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowSystemClock
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(manifest=Config.NONE,sdk=[35])
class NativeDevicePolicyTest {
    private fun code(url: String="https://www.google.com/device"): UserCode = UserCode().apply {
        fun field(name: String,value: Any) { javaClass.getDeclaredField(name).apply { isAccessible=true }.set(this,value) }
        field("mDeviceCode","TEST-DEVICE-NOT-A-REAL-CREDENTIAL")
        field("mUserCode","TEST-CODE");field("mVerificationUrl",url);field("mInterval",7);field("mExpiresIn",30)
    }
    private fun response(error: String) = AccessToken().apply {
        ErrorResponse::class.java.getDeclaredField("mError").apply { isAccessible=true }.set(this,error)
    }
    @Test fun rawCodeAndServerIntervalArePreserved() {
        val result=OndaDevicePolicy.observe(code())
        assertEquals("TEST-CODE",result.userCode)
        assertEquals(7000,OndaDevicePolicy.pollIntervalMs())
        assertEquals("https://www.google.com/device",OndaDevicePolicy.getVerificationUrl())
    }
    @Test fun expirationStopsPolling() {
        OndaDevicePolicy.observe(code()); ShadowSystemClock.advanceBy(Duration.ofSeconds(31))
        assertThrows(IllegalStateException::class.java) { OndaDevicePolicy.pollIntervalMs() }
    }
    @Test fun serverSlowDownIsRespectedAndDeniedStops() {
        OndaDevicePolicy.observe(code()); OndaDevicePolicy.checkResponse(response("slow_down"))
        assertEquals(12000,OndaDevicePolicy.pollIntervalMs())
        assertThrows(IllegalStateException::class.java) { OndaDevicePolicy.checkResponse(response("access_denied")) }
    }
    @Test fun unexpectedAuthorizationOriginsAreRejected() {
        assertThrows(IllegalStateException::class.java) { OndaDevicePolicy.observe(code("https://google.com.attacker.invalid/device")) }
        assertThrows(IllegalStateException::class.java) { OndaDevicePolicy.observe(code("http://www.google.com/device")) }
    }
}
