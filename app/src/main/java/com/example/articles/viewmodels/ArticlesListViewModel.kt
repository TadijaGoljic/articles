package com.example.articles.viewmodels

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkManager
import com.example.articles.models.ModelMapper
import com.example.articles.models.UIArticle
import com.example.articles.repositories.ArticleRepository
import com.example.articles.sync.SyncManager
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class ArticlesListViewModel @Inject constructor(
    private val articleRepository: ArticleRepository,
    private val modelMapper: ModelMapper,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    companion object {
        const val ARTICLES_COLLECTION_NAME = "articles"
        const val USERS_COLLECTION_NAME = "users"
    }

    private var state = MutableStateFlow(ArticlesListState())
    val uiState = state

    private val articlesCollection = Firebase.firestore.collection(ARTICLES_COLLECTION_NAME)

    init {
        viewModelScope.launch {
            articleRepository.getAllArticles().collectLatest { articles ->
                state.update { currentState ->
                    currentState.copy(
                        articles = articles
                            .map { modelMapper.mapToUIArticle(it) }
                            .filter { !it.deleted }
                    )
                }
            }
            syncData()
        }
    }

    fun syncData() {
        WorkManager.getInstance(context).enqueueUniqueWork(
            SyncManager.NAME,
            ExistingWorkPolicy.KEEP,
            SyncManager.sync()
        )
    }

    fun updateCheckStatus(index: Int, newValue: Boolean) {
        uiState.update { currentState ->
            currentState.copy(
                articles = currentState.articles.mapIndexed { i, article ->
                    if (i == index) article.copy(checked = newValue) else article
                }
            )
        }
    }

    fun togglePhoto(index: Int) {
        uiState.update { currentState ->
            currentState.copy(
                articles = currentState.articles.mapIndexed { i, article ->
                    if (i == index) article.copy(photoVisible = !article.photoVisible) else article
                }
            )
        }
    }

    fun startShopping(index: Int?) {
        uiState.update { currentState ->
            currentState.copy(
                shoppingModeActive = true,
            )
        }
        if (index != null) {
            uiState.update { currentState ->
                currentState.copy(
                    articles = currentState.articles.mapIndexed { i, article ->
                        if (i == index) article.copy(checked = true) else article
                    }
                )
            }
        }
    }

    fun stopShopping(deleteArticles: Boolean) {
        viewModelScope.launch {
            if (deleteArticles) {
                uiState.value.articles.filter { it.checked }.forEach { article ->
                    articleRepository.delete(article.id)
                    deleteArticle(article.id.toString())
                }
            }
            uiState.update { currentState ->
                currentState.copy(
                    shoppingModeActive = false,
                    articles = currentState.articles.map { article ->
                        article.copy(checked = false)
                    }
                )
            }
        }
    }

    fun buy(article: UIArticle) {
        viewModelScope.launch {
            articleRepository.delete(article.id)
            deleteArticle(article.id.toString())
        }
    }

    private suspend fun deleteArticle(articleId: String) = try {
        val uid = FirebaseAuth.getInstance().uid ?: throw Exception("No user logged in")
        articlesCollection
            .firestore
            .collection(USERS_COLLECTION_NAME)
            .document(uid)
            .collection(ARTICLES_COLLECTION_NAME)
            .document(articleId)
            .delete()
            .await()
    } catch (e: Exception) {
        Log.e("my_result", "deleteArticleFromFirestore: ${e.message}")
    }

}

data class ArticlesListState(
    val articles: List<UIArticle> = emptyList(),
    val shoppingModeActive: Boolean = false,
)