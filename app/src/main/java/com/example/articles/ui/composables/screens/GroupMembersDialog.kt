package com.example.articles.ui.composables.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.articles.R
import com.example.articles.repositories.GroupMember
import com.example.articles.ui.composables.utils.Toolbar
import com.example.articles.ui.composables.utils.UserInfoItem

@Composable
fun GroupMembersDialog(
    members: List<GroupMember>,
    isLoading: Boolean,
    currentUid: String?,
    onRemoveMember: (GroupMember) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            modifier = modifier
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                Toolbar(title = stringResource(id = R.string.group_members))
                if (isLoading) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                    ) {
                        members.forEach { member ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                UserInfoItem(
                                    email = member.email,
                                    modifier = Modifier.weight(1f)
                                )
                                if (member.uid != currentUid) {
                                    IconButton(onClick = { onRemoveMember(member) }) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = stringResource(id = R.string.remove_member)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                Button(
                    onClick = onDismiss,
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                ) {
                    Text(text = stringResource(id = R.string.close), fontSize = 14.sp)
                }
            }
        }
    }
}
