package com.example.articles.ui

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.articles.R
import com.example.articles.ai.GroupAgentCommand
import com.example.articles.models.UIGroup
import com.example.articles.ui.composables.screens.AiAgentDialog
import com.example.articles.ui.theme.ArticlesTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AiAgentDialogTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private fun string(id: Int, vararg args: Any) = context.getString(id, *args)

    private var submitted: String? = null
    private var confirmed = 0
    private var cancelled = 0
    private var dismissed = 0

    private fun show(
        prompt: String = "",
        pendingCommand: GroupAgentCommand? = null,
        errorMessage: String? = null,
        isProcessing: Boolean = false
    ) {
        composeRule.setContent {
            ArticlesTheme {
                AiAgentDialog(
                    promptState = TextFieldState(prompt),
                    isProcessing = isProcessing,
                    errorMessage = errorMessage,
                    pendingCommand = pendingCommand,
                    onDismissRequest = { dismissed++ },
                    onSubmit = { submitted = it },
                    onConfirmPending = { confirmed++ },
                    onCancelPending = { cancelled++ }
                )
            }
        }
    }

    @Test
    fun sendIsDisabledUntilSomethingIsTyped() {
        show(prompt = "")
        composeRule.onNodeWithText(string(R.string.send)).assertIsNotEnabled()
    }

    @Test
    fun sendSubmitsThePrompt() {
        show(prompt = "I have 20, what should I buy?")

        composeRule.onNodeWithText(string(R.string.send)).assertIsEnabled().performClick()

        assertEquals("I have 20, what should I buy?", submitted)
    }

    @Test
    fun cancelDismissesTheDialog() {
        show(prompt = "anything")
        composeRule.onNodeWithText(string(R.string.cancel)).performClick()
        assertEquals(1, dismissed)
    }

    @Test
    fun errorsAreShownUnderThePrompt() {
        show(prompt = "create group Party", errorMessage = string(R.string.agent_service_busy))
        composeRule.onNodeWithText(string(R.string.agent_service_busy)).assertIsDisplayed()
    }

    @Test
    fun destructiveCommandsAskForConfirmation() {
        val group = UIGroup(name = "Groceries")
        show(pendingCommand = GroupAgentCommand.DeleteGroup(group))

        composeRule.onNodeWithText(string(R.string.agent_confirm_delete, "Groceries")).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.confirm)).performClick()
        composeRule.onNodeWithText(string(R.string.cancel)).performClick()

        assertEquals(1, confirmed)
        assertEquals(1, cancelled)
        assertEquals(0, dismissed)
    }

    @Test
    fun answersAreShownWithTheirSourcesAndCanBeClosed() {
        show(
            pendingCommand = GroupAgentCommand.AnswerQuestion(
                answer = "With 10 you can buy Milk (1.50) and Bread (0.80).",
                sources = listOf("https://shop.example")
            )
        )

        composeRule.onNodeWithText("With 10 you can buy Milk (1.50) and Bread (0.80).", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("https://shop.example", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.close)).performClick()

        assertEquals(1, confirmed)
        assertTrue(submitted == null)
    }
}
