package com.example.articles.ai

import com.example.articles.models.UIArticle
import com.example.articles.models.UIGroup

sealed interface GroupAgentCommand {
    data class CreateGroup(val group: UIGroup) : GroupAgentCommand
    data class RenameGroup(val group: UIGroup, val newName: String) : GroupAgentCommand
    data class DeleteGroup(val group: UIGroup) : GroupAgentCommand
    data class AddArticle(val group: UIGroup, val article: UIArticle) : GroupAgentCommand
    data class RenameArticle(val article: UIArticle, val groupName: String, val newName: String) : GroupAgentCommand
    data class RemoveArticle(val article: UIArticle, val groupName: String) : GroupAgentCommand
    data class Unresolved(val message: String) : GroupAgentCommand
}
