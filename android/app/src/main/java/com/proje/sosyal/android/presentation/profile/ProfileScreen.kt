package com.proje.sosyal.android.presentation.profile

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.proje.sosyal.android.R
import com.proje.sosyal.android.core.ui.components.EmptyView
import com.proje.sosyal.android.core.ui.components.ErrorView
import com.proje.sosyal.android.core.ui.components.LoadingView
import com.proje.sosyal.android.core.ui.theme.SosyalTheme
import com.proje.sosyal.android.core.ui.theme.Spacing
import com.proje.sosyal.android.domain.model.UserProfile
import java.text.NumberFormat
import java.util.Locale

private val TURKISH: Locale = Locale.forLanguageTag("tr-TR")

@Composable
fun ProfileRouteScreen(viewModel: ProfileViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var menuOpen by rememberSaveable { mutableStateOf(false) }

    when (val s = state) {
        ProfileUiState.Loading -> LoadingView()
        is ProfileUiState.Error -> ErrorView(error = s.error, onRetry = viewModel::retry)
        is ProfileUiState.Loaded -> {
            ProfileContent(
                profile = s.profile,
                onEdit = viewModel::openEditor,
                onMenu = { menuOpen = true },
            )
            if (s.edit.isOpen) {
                EditProfileSheet(
                    profile = s.profile,
                    edit = s.edit,
                    onSave = viewModel::save,
                    onDismiss = viewModel::closeEditor,
                )
            }
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
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(modifier = Modifier.widthIn(max = 640.dp).fillMaxWidth()) {
            // Üst çubuk: solda düzenle, sağda menü
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.profile_edit))
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onMenu) {
                    Icon(Icons.Filled.Menu, contentDescription = stringResource(R.string.profile_menu))
                }
            }

            // Başlık: solda ad ve sayaçlar, sağda profil görseli
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
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
                Spacer(Modifier.width(Spacing.md))
                ProfileAvatar(name = profile.displayName, size = 104)
            }

            // Biyografi
            Box(modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.md)) {
                val bio = profile.bio
                if (bio.isNullOrBlank()) {
                    Surface(
                        onClick = onEdit,
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainer,
                    ) {
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

            // Sekmeler: simgeli, TikTok benzeri
            PrimaryTabRow(selectedTabIndex = selectedTab, containerColor = MaterialTheme.colorScheme.surface) {
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

            // Gönderi ızgarası Aşama 3'te gerçek veriyle dolacak; sahte içerik gösterilmez.
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
        }
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

/** Fotoğraf yükleme olmadığı için (MVP) görünen adın baş harfi gösterilir. */
@Composable
fun ProfileAvatar(name: String, size: Int, modifier: Modifier = Modifier) {
    val initial = name.trim().firstOrNull()?.toString()?.uppercase(TURKISH) ?: "?"
    val ring = Brush.sweepGradient(
        listOf(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.secondary,
            MaterialTheme.colorScheme.primary,
        ),
    )
    Box(
        modifier = modifier
            .size(size.dp)
            .border(BorderStroke(3.dp, ring), CircleShape)
            .padding(6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.fillMaxSize()) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    initial,
                    style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
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
