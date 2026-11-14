package com.example.data

import com.example.base.FirestoreEmulatorTestBase
import com.example.model.ConnectionConfig
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class CloudTunnelRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun exportAndImportCloudConfig_authenticatedUsers_succeeds() = runBlocking {
        signInTestUser("alice@synax.test")
        val aliceRepo = CloudTunnelRepository(firestore, auth)
        val shareCode = "TEST-${UUID.randomUUID().toString().take(6).uppercase()}"

        val exportResult = withTimeout(5000L) {
            aliceRepo.exportCloudConfig(
                name = "Alice SNI Config",
                author = "Alice",
                notes = "TLS 443",
                config = ConnectionConfig(sniHost = "m.tiktok.com"),
                isPublic = true,
                requestedCode = shareCode,
            )
        }
        assertTrue(exportResult.isSuccess)
        assertEquals(shareCode, exportResult.getOrThrow())

        signInTestUser("bob@synax.test")
        val bobRepo = CloudTunnelRepository(firestore, auth)
        val imported = withTimeout(5000L) {
            bobRepo.importCloudConfigByCode(shareCode)
        }
        assertTrue(imported.isSuccess)
        assertEquals("m.tiktok.com", imported.getOrThrow().sniHost)
    }

    @Test
    fun publishCloudTweak_nonAdminUser_failsWithPermissionDenied() = runBlocking {
        signInTestUser("alice@synax.test")
        val repo = CloudTunnelRepository(firestore, auth)

        val result = withTimeout(5000L) {
            repo.publishCloudTweak(
                name = "Unauthorized Tweak",
                carrierTag = "TestNet",
                notes = "Should fail",
                config = ConnectionConfig(),
            )
        }
        assertTrue(result.isFailure)
        val fsEx = result.exceptionOrNull()?.unwrapFirestoreException()
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx?.code)
    }

    @Test
    fun observeActiveTweaks_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repo = CloudTunnelRepository(firestore, auth)

        var caught: Throwable? = null
        try {
            withTimeout(3000L) { repo.observeActiveTweaks().first() }
        } catch (e: Exception) {
            caught = e
        }
        val fsEx = caught?.unwrapFirestoreException()
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx?.code)
    }
}
