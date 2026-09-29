package com.example.articles.viewmodels

import com.example.articles.models.UIArticle
import com.example.articles.models.UILocation
import com.example.articles.nominatim.AddressResponse
import com.example.articles.nominatim.NominatimAPI
import com.example.articles.nominatim.NominatimResponse
import com.example.articles.repositories.ArticleRepository
import com.example.articles.testing.MainDispatcherRule
import com.example.articles.testing.releaseRoute
import com.example.articles.testing.savedStateHandleFor
import com.example.articles.ui.composables.ArticlesDestinations
import com.example.articles.utils.FileManager
import com.example.articles.utils.TriggerSync
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException
import java.util.UUID

class AddNewArticleViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val groupId = UUID.randomUUID()

    private val articleRepository = mockk<ArticleRepository>(relaxed = true) {
        coEvery { getNextArticlePosition(groupId) } returns 5000.0
    }
    private val nominatimAPI = mockk<NominatimAPI>()
    private val triggerSync = mockk<TriggerSync>(relaxed = true)

    @After
    fun tearDown() = releaseRoute()

    private fun viewModel(articleId: UUID? = null) = AddNewArticleViewModel(
        savedStateHandle = savedStateHandleFor(
            ArticlesDestinations.AddNewArticleDestination(groupId = groupId.toString(), articleId = articleId?.toString())
        ),
        articleRepository = articleRepository,
        fileManager = mockk<FileManager>(relaxed = true),
        nominatimAPI = nominatimAPI,
        triggerSync = triggerSync
    )

    private fun AddNewArticleViewModel.type(
        name: String = "Milk",
        quantity: String = "",
        price: String = "",
        link: String = ""
    ) {
        with(uiState.value) {
            articleName.edit { replace(0, length, name) }
            articleQuantity.edit { replace(0, length, quantity) }
            this.price.edit { replace(0, length, price) }
            articleWebsiteLink.edit { replace(0, length, link) }
        }
    }

    @Test
    fun `saves a new article at the end of the group`() {
        val viewModel = viewModel()
        viewModel.type(name = "Milk", quantity = "2", price = "1.50")

        viewModel.handleAction(AddNewArticleAction.SaveArticle)

        coVerify {
            articleRepository.upsertArticle(match {
                it.name == "Milk" && it.groupId == groupId && it.quantity == 2.0 && it.price == 1.5 && it.position == 5000.0
            })
        }
        assertTrue(viewModel.uiState.value.okToSave)
    }

    @Test
    fun `a decimal comma is accepted for price and quantity - regression`() {
        val viewModel = viewModel()
        viewModel.type(name = "Cheese", quantity = "0,5", price = "1,50")

        viewModel.handleAction(AddNewArticleAction.SaveArticle)

        coVerify { articleRepository.upsertArticle(match { it.price == 1.5 && it.quantity == 0.5 }) }
    }

    @Test
    fun `a blank name is rejected`() {
        val viewModel = viewModel()
        viewModel.type(name = "   ")

        viewModel.handleAction(AddNewArticleAction.SaveArticle)

        assertTrue(viewModel.uiState.value.articleNameError)
        coVerify(exactly = 0) { articleRepository.upsertArticle(any()) }
    }

    @Test
    fun `editing keeps the article's existing position`() {
        val existing = UIArticle(name = "Bread", groupId = groupId, price = 0.8, position = 1234.0)
        every { articleRepository.getArticleById(existing.id) } returns flowOf(existing)
        val viewModel = viewModel(articleId = existing.id)

        assertTrue(viewModel.uiState.value.isEditMode)
        assertEquals("Bread", viewModel.uiState.value.articleName.text.toString())
        viewModel.handleAction(AddNewArticleAction.SaveArticle)

        coVerify { articleRepository.upsertArticle(match { it.id == existing.id && it.position == 1234.0 }) }
        coVerify(exactly = 0) { articleRepository.getNextArticlePosition(any()) }
    }

    @Test
    fun `picking a location shows its address`() {
        coEvery { nominatimAPI.reverseGeocode(any(), any(), any()) } returns NominatimResponse(
            address = AddressResponse(road = "Knez Mihailova", houseNumber = "5", city = "Belgrade")
        )
        val viewModel = viewModel()

        viewModel.handleAction(AddNewArticleAction.UpdateLocation(UILocation(44.8176, 20.4569)))

        assertEquals("Knez Mihailova 5, Belgrade", viewModel.uiState.value.locationString)
        assertEquals(UILocation(44.8176, 20.4569), viewModel.uiState.value.article.location)
    }

    @Test
    fun `picking a location offline keeps it and shows coordinates - regression for crash`() {
        coEvery { nominatimAPI.reverseGeocode(any(), any(), any()) } throws IOException("Unable to resolve host")
        val viewModel = viewModel()

        viewModel.handleAction(AddNewArticleAction.UpdateLocation(UILocation(44.8176, 20.4569)))

        with(viewModel.uiState.value) {
            assertEquals(UILocation(44.8176, 20.4569), article.location)
            assertEquals("44.81760, 20.45690", locationString)
            assertFalse(isLocationStringLoading)
        }
    }

    @Test
    fun `a failing save does not crash and allows retrying`() {
        coEvery { articleRepository.upsertArticle(any()) } throws IOException("offline")
        val viewModel = viewModel()
        viewModel.type(name = "Milk")

        viewModel.handleAction(AddNewArticleAction.SaveArticle)

        assertFalse(viewModel.uiState.value.isLoading)
        assertFalse(viewModel.uiState.value.okToSave)
    }
}
