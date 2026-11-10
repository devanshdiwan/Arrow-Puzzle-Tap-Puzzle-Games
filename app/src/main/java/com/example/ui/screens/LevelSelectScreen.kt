package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.LevelProgressEntity
import com.example.ui.theme.ArrowBlue
import com.example.ui.theme.ArrowNavy
import com.example.ui.theme.GameBackground
import com.example.ui.theme.StarGold
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.ui.viewmodels.HomeViewModel

@Composable
fun LevelSelectScreen(
    viewModel: HomeViewModel,
    onLevelSelected: (Int) -> Unit,
    onBackClicked: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTierIndex by remember { mutableIntStateOf(0) }

    val tiers = listOf(
        "Tutorial" to (1..10),
        "Easy" to (11..30),
        "Medium" to (31..60),
        "Hard" to (61..100)
    )

    val currentRange = tiers[selectedTierIndex].second
    val levelsMap = uiState.allLevels.associateBy { it.levelId }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GameBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("level_select_screen")
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackClicked,
                modifier = Modifier.testTag("level_select_back")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary
                )
            }
            Text(
                text = "Select Level",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                ),
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        // Difficulty Tier Tabs
        ScrollableTabRow(
            selectedTabIndex = selectedTierIndex,
            containerColor = GameBackground,
            edgePadding = 16.dp,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTierIndex]),
                    color = ArrowBlue
                )
            }
        ) {
            tiers.forEachIndexed { index, (title, range) ->
                Tab(
                    selected = selectedTierIndex == index,
                    onClick = { selectedTierIndex = index },
                    text = {
                        Text(
                            text = "$title (${range.first}–${range.last})",
                            fontWeight = if (selectedTierIndex == index) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTierIndex == index) ArrowBlue else TextSecondary,
                            fontSize = 14.sp
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Level Grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(currentRange.toList()) { levelNum ->
                val progress = levelsMap[levelNum] ?: LevelProgressEntity(levelId = levelNum, isUnlocked = (levelNum == 1))
                val isUnlocked = progress.isUnlocked
                val stars = progress.stars

                LevelGridItem(
                    levelNumber = levelNum,
                    isUnlocked = isUnlocked,
                    stars = stars,
                    onClick = {
                        if (isUnlocked) onLevelSelected(levelNum)
                    }
                )
            }
        }
    }
}

@Composable
fun LevelGridItem(
    levelNumber: Int,
    isUnlocked: Boolean,
    stars: Int,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isUnlocked) Color.White else Color(0xFFECEFF5)
        ),
        elevation = CardDefaults.cardElevation(if (isUnlocked) 3.dp else 0.dp),
        modifier = Modifier
            .aspectRatio(1f)
            .testTag("level_item_$levelNumber")
            .clickable(enabled = isUnlocked, onClick = onClick)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (isUnlocked) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "$levelNumber",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = ArrowNavy
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        for (i in 1..3) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = if (i <= stars) StarGold else Color(0xFFD3D8E2),
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            } else {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Locked",
                    tint = TextTertiary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
