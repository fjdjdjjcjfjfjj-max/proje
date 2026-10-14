package com.proje.sosyal.android.presentation.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.proje.sosyal.android.R
import com.proje.sosyal.android.core.media.RemoteImage
import com.proje.sosyal.android.core.ui.components.EmptyView
import com.proje.sosyal.android.core.ui.components.ErrorView
import com.proje.sosyal.android.core.ui.components.LoadingView
import com.proje.sosyal.android.core.ui.theme.SosyalTheme
import com.proje.sosyal.android.core.ui.theme.Spacing
import com.proje.sosyal.android.domain.model.UserProfile
import com.proje.sosyal.android.navigation.LocalBottomBarInset
import java.text.NumberFormat
import java.util.Locale

private val TURKISH: Locale = Locale.forLanguageTag("tr-TR")
private val BannerHeight = 170.dp
private val AvatarSize = 108.dp

@Composable
fun ProfileRouteScreen(onEditProfile: () -> Unit, viewModel: ProfileViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var menuOpen by rememberSaveable { mutableStateOf(false) }

    when (val s = state) {
        ProfileUiState.Loading -> LoadingView()
        is ProfileUiState.Error -> ErrorView(error = s.error, onRetry = viewModel::refresh)
        is ProfileUiState.Loaded -> {
            ProfileContent(profile = s.profile, onEdit = onEditProfile, onMenu = { menuOpen = true })
            if (menuOpen) {
                ProfileMenuSheet(
                    profile = s.profile,
                    onLogout = {
                        menuOpen = false
                        viewModel.logout()
                    },
                    onDismiss = { menuOpen = false },
                )
            }
        }
    }
}

@Composable
fun ProfileContent(profile: UserProfile, onEdit: () -> Unit, onMenu: () -> Unit) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(modifier = Modifier.widthIn(max = 640.dp).fillMaxWidth()) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Column {
                    ProfileBanner(url = profile.bannerUrl)
                    // Solda ad, kullanıcı adı ve sayaçlar; sağda (üst üste binen) profil fotoğrafı için yer
                    Row(modifier = Modifier.fillMaxWidth().padding(start = Spacing.md, end = Spacing.md, top = Spacing.xs)) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = profile.displayName.uppercase(TURKISH),
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 3.sp,
                                ),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "@${profile.username}",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                if (profile.isProfilePrivate) {
                                    Spacer(Modifier.width(Spacing.xs))
                                    Icon(
                                        Icons.Filled.Lock,
                                        contentDescription = stringResource(R.string.profile_private_badge),
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            Spacer(Modifier.height(Spacing.md))
                            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.lg)) {
                                StatItem(profile.postCount, stringResource(R.string.profile_stat_posts))
                                StatItem(profile.likesReceived, stringResource(R.string.profile_stat_likes))
                            }
                        }
                        Spacer(Modifier.width(AvatarSize + Spacing.sm))
                    }
                }

                // Kapak üzerinde yarı saydam yuvarlak düğmeler
                Row(
                    modifier = Modifier.fillMaxWidth().padding(Spacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OverlayIconButton(Icons.Filled.Edit, stringResource(R.string.profile_edit), onEdit)
                    Spacer(Modifier.weight(1f))
                    OverlayIconButton(Icons.Filled.Menu, stringResource(R.string.profile_menu), onMenu)
                }

                ProfileAvatar(
                    name = profile.displayName,
                    imageUrl = profile.avatarUrl,
                    size = AvatarSize,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = BannerHeight - AvatarSize / 2, end = Spacing.md),
                )
            }

            // Biyografi
            Box(modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.md)) {
                val bio = profile.bio
                if (bio.isNullOrBlank()) {
                    Surface(onClick = onEdit, shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainer) {
                        Row(
                            modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(Spacing.xs))
                            Text(stringResource(R.string.profile_add_bio), style = MaterialTheme.typography.labelLarge)
                        }
                    }
                } else {
                    Text(bio, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.clickable(onClick = onEdit))
                }
            }

            if (profile.isMinor) {
                Text(
                    stringResource(R.string.profile_minor_notice),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = Spacing.md).padding(bottom = Spacing.sm),
                )
            }

            PrimaryTabRow(selectedTabIndex = selectedTab, containerColor = MaterialTheme.colorScheme.background) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = stringResource(R.string.profile_tab_posts)) },
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Filled.FavoriteBorder, contentDescription = stringResource(R.string.profile_tab_liked)) },
                )
            }
            HorizontalDivider()

            // Gönderi ızgarası gönderiler eklendiğinde gerçek veriyle dolacak; sahte içerik gösterilmez.
            Box(modifier = Modifier.fillMaxWidth().height(260.dp)) {
                if (selectedTab == 0) {
                    EmptyView(
                        title = stringResource(R.string.profile_posts_empty_title),
                        message = stringResource(R.string.profile_posts_empty_message),
                    )
                } else {
                    EmptyView(
                        title = stringResource(R.string.profile_liked_empty_title),
                        message = stringResource(R.string.profile_liked_empty_message),
                    )
                }
            }
            // Yüzen alt çubuğun altında içerik kalmasın
            Spacer(Modifier.height(LocalBottomBarInset.current))
        }
    }
}

/** Kapak görseli; yoksa sade bir renk geçişi. Alt kısmı arka plana yumuşakça karışır. */
@Composable
fun ProfileBanner(url: String?, modifier: Modifier = Modifier, height: androidx.compose.ui.unit.Dp = BannerHeight) {
    val colors = MaterialTheme.colorScheme
    Box(modifier = modifier.fillMaxWidth().height(height)) {
        RemoteImage(
            url = url,
            contentDescription = stringResource(R.string.profile_banner),
            modifier = Modifier.matchParentSize(),
            placeholder = {
                Box(
                    Modifier.matchParentSize().background(
                        Brush.linearGradient(listOf(colors.primaryContainer, colors.secondaryContainer.copy(alpha = 0.6f))),
                    ),
                )
            },
        )
        Box(
            Modifier.matchParentSize().background(
                Brush.verticalGradient(0.55f to Color.Transparent, 1f to colors.background),
            ),
        )
    }
}

@Composable
private fun OverlayIconButton(icon: ImageVector, description: String, onClick: () -> Unit) {
    Surface(shape = CircleShape, color = Color.Black.copy(alpha = 0.35f), contentColor = Color.White) {
        IconButton(onClick = onClick) { Icon(icon, contentDescription = description) }
    }
}

@Composable
private fun StatItem(value: Long, label: String) {
    Column {
        Text(
            NumberFormat.getIntegerInstance(TURKISH).format(value),
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
        )
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Profil fotoğrafı; yoksa görünen adın baş harfi. Arka planla ayrışması için kalın bir çerçeve. */
@Composable
fun ProfileAvatar(name: String, imageUrl: String?, size: androidx.compose.ui.unit.Dp, modifier: Modifier = Modifier) {
    val initial = name.trim().firstOrNull()?.toString()?.uppercase(TURKISH) ?: "?"
    Box(
        modifier = modifier
            .size(size)
            .border(BorderStroke(4.dp, MaterialTheme.colorScheme.background), CircleShape)
            .padding(4.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        RemoteImage(
            url = imageUrl,
            contentDescription = stringResource(R.string.profile_photo),
            modifier = Modifier.fillMaxSize(),
            placeholder = {
                Text(
                    initial,
                    style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            },
        )
    }
}

@Preview(showBackground = true, heightDp = 800)
@Composable
private fun ProfilePreview() {
    SosyalTheme(darkTheme = true) {
        Surface {
            ProfileContent(
                UserProfile(
                    id = "id", username = "ornek", displayName = "Örnek Kullanıcı", email = "ornek@example.com",
                    bio = null, isMinor = false, isAgeVerified = false, isProfilePrivate = false,
                    postCount = 3, likesReceived = 1362,
                ),
                onEdit = {},
                onMenu = {},
            )
        }
    }
}
