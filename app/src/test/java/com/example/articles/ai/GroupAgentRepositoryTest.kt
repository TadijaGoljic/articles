package com.example.articles.ai

import android.content.Context
import com.example.articles.R
import com.example.articles.models.UIArticle
import com.example.articles.models.UIGroup
import com.example.articles.models.UILocation
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.util.Locale

class GroupAgentRepositoryTest {

    private val context = mockk<Context> {
        every { getString(any()) } answers { "string:${firstArg<Int>()}" }
        every { getString(any(), *anyVararg()) } answers {
            "string:${firstArg<Int>()}:${secondArg<Array<Any?>>().joinToString()}"
        }
    }

    private fun message(resId: Int) = "string:$resId"
    private fun message(resId: Int, arg: String) = "string:$resId:$arg"

    private val requests = mutableListOf<Pair<String, String>>()
    private val responses = ArrayDeque<(okhttp3.Request) -> Response>()

    private val interceptor = Interceptor { chain ->
        val request = chain.request()
        val body = Buffer().also { request.body?.writeTo(it) }.readUtf8()
        requests += request.url.toString() to body
        responses.removeFirst().invoke(request)
    }

    private val repository = GroupAgentRepository(
        context = context,
        json = Json { ignoreUnknownKeys = true },
        okHttpClient = OkHttpClient.Builder().addInterceptor(interceptor).build()
    )

    private val defaultLocale = Locale.getDefault()

    @After
    fun restoreLocale() = Locale.setDefault(defaultLocale)

    private fun respond(code: Int, body: String) {
        responses += { request ->
            Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(code)
                .message("status $code")
                .body(body.toResponseBody("application/json".toMediaType()))
                .build()
        }
    }

    private fun respondWithText(text: String) {
        val candidate = JSONObject()
            .put("content", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", text))))
        respond(200, JSONObject().put("candidates", JSONArray().put(candidate)).toString())
    }

    private fun actions(vararg actions: Map<String, String>): String {
        val array = JSONArray()
        actions.forEach { action ->
            array.put(JSONObject().apply {
                put("action", action["action"])
                put("groupName", action["groupName"] ?: "")
                put("articleName", action["articleName"] ?: "")
                put("newName", action["newName"] ?: "")
            })
        }
        return JSONObject().put("actions", array).toString()
    }

    private val milk = UIArticle(name = "Milk", price = 1.5)
    private val bread = UIArticle(name = "Bread")
    private val groceries = UIGroup(name = "Groceries", articles = listOf(milk.copy(), bread.copy()))
        .let { group -> group.copy(articles = group.articles.map { it.copy(groupId = group.id) }) }
    private val household = UIGroup(name = "Household")
        .let { group -> group.copy(articles = listOf(UIArticle(name = "Soap", groupId = group.id))) }
    private val groups = listOf(groceries, household)

    @Test
    fun `creates a group and adds articles into it in the same batch`() {
        val commands = repository.parseResponse(
            actions(
                mapOf("action" to "create_group", "groupName" to "Party"),
                mapOf("action" to "add_article", "groupName" to "Party", "articleName" to "Chips"),
                mapOf("action" to "add_article", "groupName" to "Party", "articleName" to "Pizza"),
                mapOf("action" to "add_article", "groupName" to "Party", "articleName" to "Cake")
            ),
            groups
        )

        val create = commands[0] as GroupAgentCommand.CreateGroup
        val add1 = commands[1] as GroupAgentCommand.AddArticle
        val add2 = commands[2] as GroupAgentCommand.AddArticle
        val add3 = commands[3] as GroupAgentCommand.AddArticle
        assertEquals("Party", create.group.name)
        assertEquals(create.group.id, add1.group.id)
        assertEquals(create.group.id, add1.article.groupId)
        assertEquals(create.group.id, add2.group.id)
        assertEquals(create.group.id, add2.article.groupId)
        assertEquals(create.group.id, add3.group.id)
        assertEquals(create.group.id, add3.article.groupId)
        assertEquals("Chips", add1.article.name)
        assertEquals("Pizza", add2.article.name)
        assertEquals("Cake", add3.article.name)
    }

    @Test
    fun `creating an existing group is rejected case-insensitively`() {
        val commands = repository.parseResponse(
            actions(mapOf("action" to "create_group", "groupName" to "GROCERIES")),
            groups
        )
        assertEquals(listOf(GroupAgentCommand.Unresolved(message(R.string.group_already_exists))), commands)
    }

    @Test
    fun `renaming an unknown group reports it by name`() {
        val commands = repository.parseResponse(
            actions(mapOf("action" to "rename_group", "groupName" to "Garden", "newName" to "Yard")),
            groups
        )
        assertEquals(
            listOf(GroupAgentCommand.Unresolved(message(R.string.agent_group_not_found, "Garden"))),
            commands
        )
    }

    @Test
    fun `renames a group and later actions see the new name`() {
        val commands = repository.parseResponse(
            actions(
                mapOf("action" to "rename_group", "groupName" to "Groceries", "newName" to "Food"),
                mapOf("action" to "add_article", "groupName" to "Food", "articleName" to "Apples")
            ),
            groups
        )
        val rename = commands[0] as GroupAgentCommand.RenameGroup
        val add = commands[1] as GroupAgentCommand.AddArticle
        assertEquals(groceries.id, rename.group.id)
        assertEquals("Food", rename.newName)
        assertEquals(groceries.id, add.article.groupId)
    }

    @Test
    fun `a deleted group can no longer be targeted`() {
        val commands = repository.parseResponse(
            actions(
                mapOf("action" to "delete_group", "groupName" to "Household"),
                mapOf("action" to "add_article", "groupName" to "Household", "articleName" to "Sponge")
            ),
            groups
        )
        assertEquals(household.id, (commands[0] as GroupAgentCommand.DeleteGroup).group.id)
        assertEquals(GroupAgentCommand.Unresolved(message(R.string.agent_group_not_found, "Household")), commands[1])
    }

    @Test
    fun `an article name present in two groups is ambiguous without a group hint`() {
        val withDuplicate = groups.map { group ->
            if (group.id == household.id) group.copy(articles = group.articles + UIArticle(name = "Milk", groupId = group.id))
            else group
        }

        val ambiguous = repository.parseResponse(
            actions(mapOf("action" to "remove_article", "articleName" to "milk")),
            withDuplicate
        )
        assertEquals(listOf(GroupAgentCommand.Unresolved(message(R.string.agent_article_ambiguous, "milk"))), ambiguous)

        val resolved = repository.parseResponse(
            actions(mapOf("action" to "remove_article", "groupName" to "Groceries", "articleName" to "milk")),
            withDuplicate
        )
        val remove = resolved.single() as GroupAgentCommand.RemoveArticle
        assertEquals("Groceries", remove.groupName)
        assertEquals(groceries.articles.first { it.name == "Milk" }.id, remove.article.id)
    }

    @Test
    fun `a removed article can no longer be renamed`() {
        val commands = repository.parseResponse(
            actions(
                mapOf("action" to "remove_article", "articleName" to "Bread"),
                mapOf("action" to "rename_article", "articleName" to "Bread", "newName" to "Toast")
            ),
            groups
        )
        assertTrue(commands[0] is GroupAgentCommand.RemoveArticle)
        assertEquals(GroupAgentCommand.Unresolved(message(R.string.agent_article_not_found, "Bread")), commands[1])
    }

    @Test
    fun `rename article without a new name is not understood`() {
        val commands = repository.parseResponse(
            actions(mapOf("action" to "rename_article", "articleName" to "Bread")),
            groups
        )
        assertEquals(listOf(GroupAgentCommand.Unresolved(message(R.string.agent_could_not_understand))), commands)
    }

    @Test
    fun `unknown actions keep their slot while understood parts still go through`() {
        val commands = repository.parseResponse(
            actions(
                mapOf("action" to "unknown"),
                mapOf("action" to "add_article", "groupName" to "Household", "articleName" to "Sponge")
            ),
            groups
        )
        assertEquals(GroupAgentCommand.Unresolved(message(R.string.agent_could_not_understand)), commands[0])
        assertTrue(commands[1] is GroupAgentCommand.AddArticle)
    }

    @Test
    fun `malformed or empty responses are not understood`() {
        val notUnderstood = listOf(GroupAgentCommand.Unresolved(message(R.string.agent_could_not_understand)))
        assertEquals(notUnderstood, repository.parseResponse("not json", groups))
        assertEquals(notUnderstood, repository.parseResponse("", groups))
        assertEquals(notUnderstood, repository.parseResponse("""{"actions": []}""", groups))
        assertEquals(notUnderstood, repository.parseResponse("""{"somethingElse": 1}""", groups))
    }

    @Test
    fun `question prompt lists every group with article prices`() {
        val prompt = repository.buildQuestionPrompt("I have 10, what should I buy?", groups, null)

        assertTrue(prompt, prompt.contains("- Groceries: Milk (1.50), Bread"))
        assertTrue(prompt, prompt.contains("- Household: Soap"))
        assertTrue(prompt, prompt.contains("User question: \"I have 10, what should I buy?\""))
    }

    @Test
    fun `question prompt prices use a dot even in a decimal-comma locale`() {
        Locale.setDefault(Locale.forLanguageTag("sr-RS"))
        val prompt = repository.buildQuestionPrompt("budget?", groups, null)
        assertTrue(prompt, prompt.contains("Milk (1.50)"))
        assertFalse(prompt, prompt.contains("1,50"))
    }

    @Test
    fun `question prompt marks bought articles so they can be left out`() {
        val list = listOf(groceries.copy(articles = listOf(milk.copy(bought = true), bread.copy())))
        val prompt = repository.buildQuestionPrompt("how much is left to buy?", list, null)
        assertTrue(prompt, prompt.contains("Milk (1.50) [bought]"))
        assertTrue(prompt, prompt.contains("Bread"))
        assertFalse(prompt, prompt.contains("Bread [bought]"))
        assertTrue(prompt, prompt.contains("use only articles that are not marked [bought]"))
    }

    @Test
    fun `question prompt marks empty groups and an empty account`() {
        assertTrue(repository.buildQuestionPrompt("q", listOf(UIGroup(name = "Empty")), null).contains("- Empty: (no articles)"))
        assertTrue(repository.buildQuestionPrompt("q", emptyList(), null).contains("(no groups yet)"))
    }

    @Test
    fun `question prompt includes the location only when known`() {
        val withLocation = repository.buildQuestionPrompt("q", groups, UILocation(44.81, 20.46))
        assertTrue(withLocation.contains("latitude 44.81, longitude 20.46"))

        val withoutLocation = repository.buildQuestionPrompt("q", groups, null)
        assertTrue(withoutLocation.contains("location is unknown"))
    }

    @Test
    fun `a recognised command is returned without asking a question`() = runTest {
        respondWithText(actions(mapOf("action" to "create_group", "groupName" to "Party")))

        val commands = repository.handlePrompt("create group Party", groups, null)

        assertTrue(commands.single() is GroupAgentCommand.CreateGroup)
        assertEquals(1, requests.size)
        assertTrue(requests.single().first.contains(":generateContent"))
        assertTrue("uses structured output", requests.single().second.contains("responseSchema"))
    }

    @Test
    fun `an unrecognised request falls back to a free-form answer`() = runTest {
        respondWithText(actions(mapOf("action" to "unknown")))
        respondWithText("Buy Milk and Bread.")

        val commands = repository.handlePrompt("I have 10, what should I buy?", groups, null)

        assertEquals(
            listOf(
                GroupAgentCommand.AnswerQuestion(answer = "Buy Milk and Bread.")
            ),
            commands
        )
        assertEquals(2, requests.size)
        val questionBody = requests[1].second
        assertFalse("does not use google search grounding", questionBody.contains("google_search"))
        assertTrue("sends the user's lists", questionBody.contains("Milk (1.50)"))
    }

    @Test
    fun `an overloaded model is reported as busy and not retried as a question`() = runTest {
        respond(503, """{"error":{"code":503,"status":"UNAVAILABLE"}}""")

        val commands = repository.handlePrompt("create group Party", groups, null)

        assertEquals(listOf(GroupAgentCommand.Unresolved(message(R.string.agent_service_busy))), commands)
        assertEquals("only one request is sent", 1, requests.size)
    }

    @Test
    fun `rate limiting is reported as busy`() = runTest {
        respond(429, """{"error":{"code":429}}""")
        val commands = repository.handlePrompt("create group Party", groups, null)
        assertEquals(listOf(GroupAgentCommand.Unresolved(message(R.string.agent_service_busy))), commands)
    }

    @Test
    fun `network failures are reported as a connection problem`() = runTest {
        responses += { throw IOException("timeout") }

        val commands = repository.handlePrompt("create group Party", groups, null)

        assertEquals(listOf(GroupAgentCommand.Unresolved(message(R.string.agent_network_error))), commands)
        assertEquals(1, requests.size)
    }

    @Test
    fun `other HTTP errors are reported generically`() = runTest {
        respond(400, """{"error":{"code":400}}""")
        val commands = repository.handlePrompt("create group Party", groups, null)
        assertEquals(listOf(GroupAgentCommand.Unresolved(message(R.string.something_went_wrong))), commands)
    }

    @Test
    fun `a failing question request is reported with the matching message`() = runTest {
        respondWithText(actions(mapOf("action" to "unknown")))
        respond(429, """{"error":{"code":429}}""")

        val commands = repository.handlePrompt("what's the weather?", groups, null)

        assertEquals(listOf(GroupAgentCommand.Unresolved(message(R.string.agent_service_busy))), commands)
    }

    @Test
    fun `an empty answer is reported generically`() = runTest {
        respondWithText(actions(mapOf("action" to "unknown")))
        respondWithText("   ")

        val commands = repository.handlePrompt("?", groups, null)

        assertEquals(listOf(GroupAgentCommand.Unresolved(message(R.string.something_went_wrong))), commands)
    }

}
