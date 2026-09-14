package com.example.articles.ai

import com.example.articles.models.UIGroup

sealed interface GroupAgentCommand {
    data class CreateGroup(val name: String) : GroupAgentCommand
    data class RenameGroup(val group: UIGroup, val newName: String) : GroupAgentCommand
    data class DeleteGroup(val group: UIGroup) : GroupAgentCommand
    data class Unresolved(val message: String) : GroupAgentCommand
}
