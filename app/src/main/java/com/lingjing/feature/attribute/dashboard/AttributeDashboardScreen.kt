package com.lingjing.feature.attribute.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.lingjing.core.common.constant.DbConstants
import com.lingjing.domain.model.Attribute
import com.lingjing.domain.model.Realm
import com.lingjing.domain.repository.AttributeRepository
import com.lingjing.feature.attribute.engine.RealmCalculator
import com.lingjing.feature.attribute.components.RadarChart
import com.lingjing.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AttributeDashboardViewModel @Inject constructor(
    private val attributeRepository: AttributeRepository,
    private val realmCalculator: RealmCalculator
) : ViewModel() {

    data class UiState(
        val realm: Realm = Realm.QI_REFINING,
        val realmProgress: RealmCalculator.RealmProgress? = null,
        val attributes: List<Attribute> = emptyList(),
        val baseAttributes: List<Attribute> = emptyList(),
        val customAttributes: List<Attribute> = emptyList(),
        val isLoading: Boolean = true
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        loadAttributes()
    }

    fun loadAttributes() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            // Ensure default attributes exist before collecting
            val avgLevel = try { attributeRepository.getAverageLevel() } catch (_: Exception) { 0f }
            if (avgLevel <= 0f) {
                try {
                    attributeRepository.initDefaultAttributes()
                } catch (_: Exception) { /* already initialized */ }
            }

            attributeRepository.getAllAttributes().collect { attrs ->
                val base = attrs.filter { it.isBase }
                val custom = attrs.filter { !it.isBase }
                val realm = realmCalculator.getCurrentRealm()
                val progress = realmCalculator.getProgressToNextRealm()

                _state.update {
                    it.copy(
                        isLoading = false,
                        realm = realm,
                        realmProgress = progress,
                        attributes = attrs,
                        baseAttributes = base,
                        customAttributes = custom
                    )
                }
            }
        }
    }
}

@Composable
fun AttributeDashboardScreen(
    @Suppress("UNUSED_PARAMETER") navController: NavController,
    viewModel: AttributeDashboardViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    if (state.isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = FlowerCyan)
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 境界标题
        item {
            RealmHeader(
                realm = state.realm,
                progress = state.realmProgress
            )
        }

        // 五行雷达图
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "五行属性",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    RadarChart(
                        attributes = state.baseAttributes
                    )
                }
            }
        }

        // 属性详细卡片
        items(state.baseAttributes) { attr ->
            AttributeDetailCard(attribute = attr)
        }

        // 自定义属性（如有）
        if (state.customAttributes.isNotEmpty()) {
            item {
                Text(
                    "自定义属性",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            items(state.customAttributes) { attr ->
                AttributeDetailCard(attribute = attr)
            }
        }

        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
fun RealmHeader(realm: Realm, progress: RealmCalculator.RealmProgress?) {
    val realmColor = when (realm) {
        Realm.QI_REFINING -> RealmColors.QiRefining
        Realm.FOUNDATION -> RealmColors.Foundation
        Realm.GOLDEN_CORE -> RealmColors.GoldenCore
        Realm.NASCENT_SOUL -> RealmColors.NascentSoul
        Realm.SPIRIT_TRANSFORMATION -> RealmColors.SpiritTransformation
    }
    val title = when (realm) {
        Realm.QI_REFINING -> "炼气期"
        Realm.FOUNDATION -> "筑基期"
        Realm.GOLDEN_CORE -> "金丹期"
        Realm.NASCENT_SOUL -> "元婴期"
        Realm.SPIRIT_TRANSFORMATION -> "化神期"
    }
    val index = Realm.entries.indexOf(realm) + 1
    val totalRealms = Realm.entries.size

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 境界大标题
            Text(
                text = "【$title】",
                style = MaterialTheme.typography.headlineMedium,
                color = realmColor,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 3.sp
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "第${index}境界 / 共${totalRealms}境界",
                style = MaterialTheme.typography.labelSmall,
                color = LightInk
            )

            if (progress != null) {
                Spacer(Modifier.height(12.dp))

                // 突破进度
                val pct = "%.0f".format(progress.progress * 100)
                Text(
                    text = "突破进度 $pct%",
                    style = MaterialTheme.typography.titleLarge,
                    color = realmColor,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { progress.progress },
                    modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)),
                    color = realmColor,
                    trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                )

                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("综合等级", style = MaterialTheme.typography.labelSmall, color = LightInk)
                        Text("${"%.1f".format(progress.currentAvgLevel)}", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = realmColor)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("突破需求", style = MaterialTheme.typography.labelSmall, color = LightInk)
                        Text("${"%.0f".format(progress.requiredAvgLevel)}", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = realmColor)
                    }
                }
            } else {
                Spacer(Modifier.height(8.dp))
                Text("已达至高境界", style = MaterialTheme.typography.bodyMedium, color = Gamboge)
            }
        }
    }
}

@Composable
fun AttributeDetailCard(attribute: Attribute) {
    val attrColor = getAttrColor(attribute.key)

    Card(
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(attrColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(getAttrEmoji(attribute.key), fontSize = 20.sp)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = attribute.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "总经验: ${attribute.totalExpEarned}",
                            style = MaterialTheme.typography.labelSmall,
                            color = LightInk
                        )
                    }
                }

                // 等级徽章
                Surface(
                    color = attrColor,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Lv.${attribute.level}",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // 经验条
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("经验进度", style = MaterialTheme.typography.labelSmall, color = LightInk)
                    Text(
                        "${"%.0f".format(attribute.getProgress() * 100)}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = attrColor,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { attribute.getProgress() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp)),
                    color = attrColor,
                    trackColor = attrColor.copy(alpha = 0.08f)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${attribute.currentExp} / ${attribute.expToNextLevel()} EXP → 下一级 Lv.${attribute.level + 1}",
                    style = MaterialTheme.typography.labelSmall,
                    color = LightInk
                )
            }
        }
    }
}

fun getAttrColor(key: String): Color = when (key) {
    "wisdom" -> AttributeColors.Wisdom
    "physique" -> AttributeColors.Physique
    "perception" -> AttributeColors.Perception
    "energy" -> AttributeColors.Energy
    "will" -> AttributeColors.Will
    else -> FlowerCyan
}

fun getAttrEmoji(key: String): String = when (key) {
    "wisdom" -> "📚"
    "physique" -> "💪"
    "perception" -> "🧘"
    "energy" -> "⚡"
    "will" -> "🔥"
    else -> "✨"
}
