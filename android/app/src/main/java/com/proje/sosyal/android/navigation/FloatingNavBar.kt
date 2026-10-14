package com.proje.sosyal.android.navigation

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

private val BarHeight = 64.dp
private val BarMargin = 12.dp
private val BarShape = RoundedCornerShape(percent = 50)

/**
 * Ekranların, yüzen çubuğun altında kalmaması için en altta bırakması gereken boşluk.
 * İçerik çubuğun arkasından akar (cam efekti); son öğe ise çubuğun üstünde kalır.
 */
val LocalBottomBarInset = compositionLocalOf { 0.dp }

@Composable
fun bottomBarInset(): Dp =
    BarHeight + BarMargin * 2 + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

/**
 * iOS tarzı yüzen "cam" alt çubuk: kenarlardan ayrık kapsül, arkasındaki içerik bulanık,
 * seçili sekmenin arkasında kayan yarı saydam vurgu. Yazı yok, yalnızca simge.
 */
@Composable
fun FloatingNavBar(
    destinations: List<TopLevelDestination>,
    selected: TopLevelDestination?,
    onSelect: (TopLevelDestination) -> Unit,
    hazeState: HazeState,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val glass = HazeStyle(
        backgroundColor = colors.surface,
        tint = HazeTint(colors.surface.copy(alpha = 0.55f)),
        blurRadius = 24.dp,
        noiseFactor = 0.04f,
        // Bulanıklık desteklenmeyen cihazlarda (Android 11 ve altı) daha opak bir yüzey
        fallbackTint = HazeTint(colors.surfaceContainer.copy(alpha = 0.94f)),
    )

    Box(
        modifier = modifier
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = BarMargin)
            .widthIn(max = 480.dp)
            .fillMaxWidth()
            .height(BarHeight)
            .shadow(elevation = 18.dp, shape = BarShape, ambientColor = Color.Black, spotColor = Color.Black)
            .clip(BarShape)
            .hazeEffect(state = hazeState, style = glass)
            .border(0.5.dp, colors.onSurface.copy(alpha = 0.14f), BarShape),
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize().padding(6.dp)) {
            val itemWidth = maxWidth / destinations.size
            val selectedIndex = destinations.indexOf(selected)

            // Kayan vurgu: sekme değişince yaylı animasyonla yer değiştirir.
            if (selectedIndex >= 0) {
                val indicatorOffset by animateDpAsState(
                    targetValue = itemWidth * selectedIndex,
                    animationSpec = spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMediumLow),
                    label = "navIndicator",
                )
                Box(
                    modifier = Modifier
                        .offset(x = indicatorOffset)
                        .width(itemWidth)
                        .fillMaxHeight()
                        .clip(BarShape)
                        .background(colors.onSurface.copy(alpha = 0.12f)),
                )
            }

            Row(modifier = Modifier.fillMaxSize()) {
                destinations.forEach { destination ->
                    NavBarItem(
                        destination = destination,
                        isSelected = destination == selected,
                        onClick = { onSelect(destination) },
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
                }
            }
        }
    }
}

@Composable
private fun NavBarItem(
    destination: TopLevelDestination,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    val label = stringResource(destination.labelRes)
    val tint = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f)

    Box(
        modifier = modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null, // vurgu animasyonu geri bildirimi zaten veriyor
            ) {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
            .semantics {
                role = Role.Tab
                selected = isSelected
                contentDescription = label
            },
        contentAlignment = Alignment.Center,
    ) {
        if (destination == TopLevelDestination.CREATE) {
            // Ortadaki oluştur düğmesi: yuvarlatılmış kare çerçeve içinde "+"
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .border(2.dp, tint, RoundedCornerShape(9.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(destination.selectedIcon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
            }
        } else {
            Icon(
                imageVector = if (isSelected) destination.selectedIcon else destination.unselectedIcon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(28.dp),
            )
        }
    }
}
