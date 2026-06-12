package com.lingjing.feature.achievement

import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.lingjing.core.database.dao.AchievementDao
import com.lingjing.data.local.entity.AchievementEntity
import com.lingjing.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AchievementViewModel @Inject constructor(
    private val achievementDao: AchievementDao,
    private val achievementEngine: AchievementEngine
) : ViewModel() {

    data class UiState(
        val achievements: List<AchievementEntity> = emptyList(),
        val unlockedCount: Int = 0,
        val totalCount: Int = 0,
        val isLoading: Boolean = true
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        loadAchievements()
    }

    private fun loadAchievements() {
        viewModelScope.launch {
            // 初始化成就（如果首次启动）
            achievementEngine.initAchievements()

            achievementDao.getAllAchievements().collect { achievements ->
                val unlocked = achievements.count { it.isUnlocked }
                _state.update {
                    UiState(
                        isLoading = false,
                        achievements = achievements,
                        unlockedCount = unlocked,
                        totalCount = achievements.size
                    )
                }
            }
        }
    }
}

@Composable
fun AchievementScreen(
    @Suppress("UNUSED_PARAMETER") navController: NavController,
    viewModel: AchievementViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    if (state.isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = FlowerCyan)
        }
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // 头部统计
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 1.dp
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "机缘录",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "已解锁 ${state.unlockedCount} / ${state.totalCount}",
                    style = MaterialTheme.typography.titleMedium,
                    color = Gamboge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { if (state.totalCount > 0) state.unlockedCount.toFloat() / state.totalCount
                        else 0f },
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = Gamboge,
                    trackColor = Gamboge.copy(alpha = 0.15f)
                )
            }
        }

        // 成就网格
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(state.achievements, key = { it.achievementKey }) { achievement ->
                AchievementCard(achievement = achievement)
            }
        }
    }
}

@Composable
fun AchievementCard(achievement: AchievementEntity) {
    val categoryIcon = when (achievement.category) {
        "streak" -> "🔥"
        "realm" -> "🏔️"
        "attribute" -> "💪"
        else -> "⭐"
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (achievement.isUnlocked) 2.dp else 0.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (achievement.isUnlocked)
                MaterialTheme.colorScheme.surface
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 图标
            Text(
                text = if (achievement.isUnlocked) categoryIcon else "🔒",
                fontSize = 32.sp
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = achievement.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = if (achievement.isUnlocked) InkBlack else LightInk
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = if (achievement.isUnlocked)
                    achievement.description
                else
                    achievement.conditionDescription,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                color = if (achievement.isUnlocked) LightInk else LightInk.copy(alpha = 0.6f),
                maxLines = 3
            )

            // 奖励标签
            if (achievement.isUnlocked && achievement.rewardType != null) {
                Spacer(Modifier.height(8.dp))
                Surface(
                    color = Gamboge.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = when (achievement.rewardType) {
                            "exp_bonus" -> "经验 +${"%.0f".format((achievement.rewardValue - 1) * 100)}%"
                            "temp_buff" -> "临时 buff ×${"%.1f".format(achievement.rewardValue)}"
                            "plan_exemption" -> "计划豁免权"
                            else -> ""
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = Gamboge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
