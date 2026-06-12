package com.lingjing.data.local.entity

import com.lingjing.core.common.constant.DbConstants

/**
 * 预定义的成就列表
 */
object AchievementDefinitions {

    data class AchievementDef(
        val key: String,
        val name: String,
        val description: String,
        val conditionDescription: String,
        val rewardType: String,
        val rewardValue: Float,
        val category: String,
        val displayOrder: Int
    )

    val ALL = listOf(
        AchievementDef(
            key = "seven_day_builder",
            name = "七日筑基",
            description = "连续7天完成率不低于80%",
            conditionDescription = "连续7天完成率 ≥ 80%",
            rewardType = "exp_bonus",
            rewardValue = 1.5f,
            category = "streak",
            displayOrder = 1
        ),
        AchievementDef(
            key = "golden_core_path",
            name = "金丹大道",
            description = "达到金丹期",
            conditionDescription = "境界达到金丹期",
            rewardType = "temp_buff",
            rewardValue = 2.0f,
            category = "realm",
            displayOrder = 2
        ),
        AchievementDef(
            key = "iron_will",
            name = "意志如铁",
            description = "丹心·意志达到20级",
            conditionDescription = "丹心·意志 ≥ 20级",
            rewardType = "exp_bonus",
            rewardValue = 1.2f,
            category = "attribute",
            displayOrder = 3
        ),
        AchievementDef(
            key = "hundred_tasks",
            name = "百炼成仙",
            description = "累计完成100个任务",
            conditionDescription = "累计完成任务数 ≥ 100",
            rewardType = "plan_exemption",
            rewardValue = 1.0f,
            category = "general",
            displayOrder = 4
        ),
        AchievementDef(
            key = "thirty_day_streak",
            name = "一月不辍",
            description = "连续30天有完成任务记录",
            conditionDescription = "连续30天有完成记录",
            rewardType = "exp_bonus",
            rewardValue = 2.0f,
            category = "streak",
            displayOrder = 5
        ),
        AchievementDef(
            key = "nascent_soul_master",
            name = "元婴大成",
            description = "达到元婴期",
            conditionDescription = "境界达到元婴期",
            rewardType = "temp_buff",
            rewardValue = 3.0f,
            category = "realm",
            displayOrder = 6
        ),
        AchievementDef(
            key = "first_review",
            name = "首次内省",
            description = "完成第一次修炼札记",
            conditionDescription = "完成至少一次复盘",
            rewardType = "exp_bonus",
            rewardValue = 1.1f,
            category = "general",
            displayOrder = 7
        ),
        AchievementDef(
            key = "perfect_week",
            name = "完美一周",
            description = "连续7天完成率100%",
            conditionDescription = "连续7天完成率 = 100%",
            rewardType = "plan_exemption",
            rewardValue = 2.0f,
            category = "streak",
            displayOrder = 8
        )
    )
}
