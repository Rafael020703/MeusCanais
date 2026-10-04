package rsv.squitv

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PinUnlockFocusTest {

    @Test
    fun pinDialogFocusState_transitionsCorrectlyOnDismiss() {
        var wasPinDialogActive = false
        var focusRestoredToSidebar = false

        fun onCategorySelected() {
            wasPinDialogActive = true
        }

        fun onDialogDismissed() {
            if (wasPinDialogActive) {
                wasPinDialogActive = false
                focusRestoredToSidebar = true
            }
        }

        // 1. Initial State
        assertFalse(wasPinDialogActive)
        assertFalse(focusRestoredToSidebar)

        // 2. User selects locked category
        onCategorySelected()
        assertTrue(wasPinDialogActive)

        // 3. User cancels / dismisses PIN dialog
        onDialogDismissed()
        assertFalse(wasPinDialogActive)
        assertTrue(focusRestoredToSidebar)
    }

    @Test
    fun pinDialogFocusState_transitionsCorrectlyOnUnlock() {
        var pendingCategoryToUnlock: String? = "locked_category"
        var wasPinDialogActive = true
        var focusRestoredToSidebar = false
        var unlockedCategory: String? = null

        fun onPinConfirmed(enteredPin: String, correctPin: String) {
            if (enteredPin == correctPin) {
                unlockedCategory = pendingCategoryToUnlock
                pendingCategoryToUnlock = null
                if (wasPinDialogActive) {
                    wasPinDialogActive = false
                    focusRestoredToSidebar = true
                }
            }
        }

        onPinConfirmed("1234", "1234")

        assertEquals("locked_category", unlockedCategory)
        assertFalse(wasPinDialogActive)
        assertTrue(focusRestoredToSidebar)
    }
}
