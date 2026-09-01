package com.example.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LaptopMac
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.Currencies
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.TransferBlue
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

object CurrencyFormatter {
    fun format(amount: Double, currency: String = "INR", showSign: Boolean = false, decimalPlaces: Int = 2): String {
        val symbol = Currencies.getSymbol(currency)
        val absAmount = Math.abs(amount)
        
        val formattedNumber = if (currency.equals("INR", ignoreCase = true)) {
            formatIndianNumber(absAmount, decimalPlaces)
        } else {
            val pattern = if (decimalPlaces <= 0) "#,##0" else "#,##0." + "0".repeat(decimalPlaces)
            val df = DecimalFormat(pattern, DecimalFormatSymbols(Locale.US))
            df.format(absAmount)
        }

        return when {
            amount < 0 -> "-$symbol$formattedNumber"
            showSign && amount > 0 -> "+$symbol$formattedNumber"
            else -> "$symbol$formattedNumber"
        }
    }

    fun formatCompact(amount: Double, currency: String = "INR"): String {
        val symbol = Currencies.getSymbol(currency)
        val abs = Math.abs(amount)
        val sign = if (amount < 0) "-" else ""
        
        if (currency.equals("INR", ignoreCase = true)) {
            return when {
                abs >= 10_000_000 -> "$sign$symbol${String.format(Locale.US, "%.2f Cr", abs / 10_000_000)}"
                abs >= 100_000 -> "$sign$symbol${String.format(Locale.US, "%.2f L", abs / 100_000)}"
                abs >= 1_000 -> "$sign$symbol${String.format(Locale.US, "%.1f K", abs / 1_000)}"
                else -> "$sign$symbol${String.format(Locale.US, "%.0f", abs)}"
            }
        }

        return when {
            abs >= 1_000_000 -> "$sign$symbol${String.format(Locale.US, "%.1fM", abs / 1_000_000)}"
            abs >= 1_000 -> "$sign$symbol${String.format(Locale.US, "%.1fK", abs / 1_000)}"
            else -> "$sign$symbol${String.format(Locale.US, "%.0f", abs)}"
        }
    }

    private fun formatIndianNumber(amount: Double, decimalPlaces: Int): String {
        val longVal = amount.toLong()
        val frac = amount - longVal
        val numStr = longVal.toString()

        val formattedInt = if (numStr.length <= 3) {
            numStr
        } else {
            val lastThree = numStr.substring(numStr.length - 3)
            val remaining = numStr.substring(0, numStr.length - 3)
            val sb = StringBuilder()
            var count = 0
            for (i in remaining.length - 1 downTo 0) {
                sb.append(remaining[i])
                count++
                if (count == 2 && i != 0) {
                    sb.append(",")
                    count = 0
                }
            }
            sb.reverse().append(",").append(lastThree).toString()
        }

        if (decimalPlaces <= 0) return formattedInt
        val fracStr = String.format(Locale.US, "%.${decimalPlaces}f", frac).substring(1) // gets .xx
        return formattedInt + fracStr
    }
}

fun getIconForName(iconName: String): ImageVector {
    return when (iconName.lowercase()) {
        "restaurant", "food" -> Icons.Default.Restaurant
        "shopping_cart", "groceries" -> Icons.Default.ShoppingCart
        "local_cafe", "coffee" -> Icons.Default.LocalCafe
        "directions_car", "transport" -> Icons.Default.DirectionsCar
        "local_gas_station", "fuel" -> Icons.Default.LocalGasStation
        "shopping_bag", "shopping" -> Icons.Default.ShoppingBag
        "movie", "entertainment" -> Icons.Default.Movie
        "receipt_long", "bills" -> Icons.Default.ReceiptLong
        "home", "rent" -> Icons.Default.Home
        "medical_services", "health" -> Icons.Default.MedicalServices
        "school", "education" -> Icons.Default.School
        "flight", "travel" -> Icons.Default.Flight
        "verified_user", "insurance" -> Icons.Default.VerifiedUser
        "subscriptions" -> Icons.Default.Subscriptions
        "face", "personal" -> Icons.Default.Face
        "card_giftcard", "gifts" -> Icons.Default.CardGiftcard
        "payments", "salary" -> Icons.Default.Payments
        "laptop_mac", "freelance" -> Icons.Default.LaptopMac
        "storefront", "business" -> Icons.Default.Storefront
        "trending_up", "interest" -> Icons.Default.TrendingUp
        "query_stats", "investments" -> Icons.Default.QueryStats
        "replay", "refund" -> Icons.Default.Replay
        "account_balance", "bank" -> Icons.Default.AccountBalance
        "savings" -> Icons.Default.Savings
        "credit_card" -> Icons.Default.CreditCard
        "account_balance_wallet", "wallet" -> Icons.Default.AccountBalanceWallet
        "attach_money" -> Icons.Default.AttachMoney
        else -> Icons.Default.Category
    }
}

fun parseColorSafe(hex: String, fallback: Color = Color(0xFF10B981)): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (e: Exception) {
        fallback
    }
}

@Composable
fun CategoryBadge(
    iconName: String,
    colorHex: String,
    size: Dp = 44.dp,
    iconSize: Dp = 22.dp,
    modifier: Modifier = Modifier
) {
    val baseColor = parseColorSafe(colorHex)
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(baseColor.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = getIconForName(iconName),
            contentDescription = null,
            tint = baseColor,
            modifier = Modifier.size(iconSize)
        )
    }
}

@Composable
fun AppleCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    cornerRadius: Dp = 24.dp,
    borderWidth: Dp = 1.dp,
    borderColor: Color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
    content: @Composable () -> Unit
) {
    Card(
        shape = RoundedCornerShape(cornerRadius),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        border = androidx.compose.foundation.BorderStroke(borderWidth, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
    ) {
        content()
    }
}

@Composable
fun EmptyStateView(
    icon: ImageVector,
    title: String,
    description: String,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(36.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (actionText != null && onAction != null) {
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = onAction,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.testTag("empty_state_action_button")
            ) {
                Text(
                    text = actionText,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

@Composable
fun TopNavBar(
    title: String,
    subtitle: String? = null,
    showBack: Boolean = false,
    onBack: () -> Unit = {},
    actions: @Composable () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            if (showBack) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
            }
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        actions()
    }
}

@Composable
fun ProgressBarWithIndicator(
    progress: Float, // 0.0 to 1.0+
    color: Color = MaterialTheme.colorScheme.primary,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    height: Dp = 8.dp,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        label = "progress"
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(height / 2))
            .background(backgroundColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(animatedProgress)
                .height(height)
                .clip(RoundedCornerShape(height / 2))
                .background(color)
        )
    }
}
