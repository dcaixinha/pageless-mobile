package live.pageless.mobile.ui.components

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

/**
 * Canonical location of the published privacy policy.
 *
 * The page is generated from `docs/privacy/privacy-policy.md` in this
 * repository, so the wording users read is the wording in version control. It
 * is deliberately *not* duplicated into the app: bundled policy text drifts
 * from the published page, and the published page is the one Google Play and
 * the store listing point at.
 */
const val PRIVACY_POLICY_URL = "https://pageless.live/privacy"

/**
 * Opens [PRIVACY_POLICY_URL] in whatever browser the user has.
 *
 * A device with no component able to handle an `https` VIEW intent is unusual
 * but possible (a stripped ROM, or a work profile that blocks browsing). A
 * toast is the right weight for the failure: it is visible, and there is no
 * in-app action the user could take instead, which is what a snackbar would
 * imply.
 */
private fun openPrivacyPolicy(context: Context) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(PRIVACY_POLICY_URL))
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        Toast
            .makeText(
                context,
                "No app can open $PRIVACY_POLICY_URL",
                Toast.LENGTH_LONG,
            ).show()
    }
}

/** Settings-style row linking to the privacy policy. */
@Composable
fun PrivacyPolicyRow(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clickable { openPrivacyPolicy(context) }
                .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Privacy policy", style = MaterialTheme.typography.bodyLarge)
            Text(
                "What the app sends to your server, and what stays on this device.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(
            Icons.AutoMirrored.Filled.OpenInNew,
            contentDescription = "Opens in your browser",
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Compact privacy policy link for the sign-in screen.
 *
 * Signed-out users cannot reach Settings, and the sign-in screen is where they
 * hand over an email address and password for the first time, so it is the one
 * place outside Settings where the policy has to be reachable.
 */
@Composable
fun PrivacyPolicyLink(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    TextButton(
        onClick = { openPrivacyPolicy(context) },
        modifier = modifier,
    ) {
        Text(
            "Privacy policy",
            style = MaterialTheme.typography.bodySmall,
        )
    }
}
