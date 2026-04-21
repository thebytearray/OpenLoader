@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package org.thebytearray.app.android.openloader.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import org.thebytearray.app.android.openloader.core.designsystem.theme.Dimens

data class OlHeroAction(
    val icon: ImageVector,
    val contentDescription: String?,
    val onClick: () -> Unit,
)

@Composable
fun OlHeroHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    actions: List<OlHeroAction> = emptyList(),
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = Dimens.paddingLarge,
                vertical = Dimens.paddingSmall,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.paddingSmall),
    ) {
        androidx.compose.foundation.layout.Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmallEmphasized,
                modifier = Modifier.alpha(0.95f),
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        actions.forEach { action ->
            IconButton(
                onClick = action.onClick,
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            ) {
                Icon(
                    imageVector = action.icon,
                    contentDescription = action.contentDescription,
                    modifier = Modifier.size(Dimens.iconSizeLarge),
                )
            }
        }
    }
}
