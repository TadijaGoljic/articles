package com.example.articles.ui

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.articles.ui.composables.utils.OneTextFieldDialog
import com.example.articles.ui.theme.ArticlesTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OneTextFieldDialogTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var confirmed = 0
    private var dismissed = 0

    private fun show(text: String, isError: Boolean = false) {
        val state = TextFieldState(text)
        composeRule.setContent {
            ArticlesTheme {
                OneTextFieldDialog(
                    title = "Create a group",
                    onDismissRequest = { dismissed++ },
                    textFieldState = state,
                    isError = isError,
                    errorMessage = "Group already exists",
                    dismissButtonText = "Cancel",
                    confirmButtonText = "Save",
                    confirmButtonEnabled = state.text.isNotBlank(),
                    onConfirmButtonClick = { confirmed++ },
                    isConfirmButtonLoading = false
                )
            }
        }
    }

    @Test
    fun whitespaceOnlyNamesCannotBeSaved() {
        show(text = "   ")
        composeRule.onNodeWithText("Save").assertIsNotEnabled()
    }

    @Test
    fun aNameCanBeSaved() {
        show(text = "Groceries")
        composeRule.onNodeWithText("Save").assertIsEnabled().performClick()
        assertEquals(1, confirmed)
    }

    @Test
    fun theErrorIsShownWhenFlagged() {
        show(text = "Groceries", isError = true)
        composeRule.onNodeWithText("Group already exists").assertIsDisplayed()
    }

    @Test
    fun cancelDismisses() {
        show(text = "Groceries")
        composeRule.onNodeWithText("Cancel").performClick()
        assertEquals(1, dismissed)
    }
}
