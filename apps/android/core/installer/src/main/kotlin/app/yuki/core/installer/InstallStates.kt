package app.yuki.core.installer

import app.yuki.core.model.InstallState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

fun InstallProgressStore.observeActiveStates(): Flow<Map<Long, InstallState>> =
    observeActive().map { active -> active.associate { it.githubRepoId to it.state } }
