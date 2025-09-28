package com.example.articles.viewmodels

import androidx.lifecycle.ViewModel
import com.example.articles.repositories.ArticleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AddNewArticleViewModel @Inject constructor(
    private val articleRepository: ArticleRepository
) : ViewModel() {

}