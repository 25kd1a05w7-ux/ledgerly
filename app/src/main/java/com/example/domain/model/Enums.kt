package com.example.domain.model

enum class AccountType(val displayName: String) {
    CASH("Cash"),
    BANK("Bank Account"),
    SAVINGS("Savings"),
    CREDIT_CARD("Credit Card"),
    WALLET("Digital Wallet"),
    INVESTMENT("Investment"),
    LOAN("Loan / Debt"),
    OTHER("Other")
}

enum class TransactionType(val displayName: String) {
    EXPENSE("Expense"),
    INCOME("Income"),
    TRANSFER("Transfer"),
    REFUND("Refund"),
    ADJUSTMENT("Balance Adjustment")
}

enum class CategoryType {
    EXPENSE,
    INCOME
}

enum class BudgetPeriod(val displayName: String) {
    WEEKLY("Weekly"),
    MONTHLY("Monthly"),
    YEARLY("Yearly"),
    CUSTOM("Custom Range")
}

enum class RecurrenceFrequency(val displayName: String) {
    DAILY("Daily"),
    WEEKLY("Weekly"),
    BIWEEKLY("Every 2 Weeks"),
    MONTHLY("Monthly"),
    QUARTERLY("Every 3 Months"),
    YEARLY("Yearly")
}

enum class AppThemeMode(val displayName: String) {
    SYSTEM("System Default"),
    LIGHT("Light Theme"),
    DARK("Dark Theme")
}

enum class SecurityLockTimeout(val displayName: String, val millis: Long) {
    IMMEDIATELY("Immediately", 0L),
    ONE_MINUTE("1 Minute", 60_000L),
    FIVE_MINUTES("5 Minutes", 300_000L),
    FIFTEEN_MINUTES("15 Minutes", 900_000L)
}

data class CurrencyItem(
    val code: String,
    val symbol: String,
    val name: String
)

object Currencies {
    val supported = listOf(
        CurrencyItem("INR", "₹", "Indian Rupee (₹)"),
        CurrencyItem("USD", "$", "US Dollar ($)"),
        CurrencyItem("EUR", "€", "Euro (€)"),
        CurrencyItem("GBP", "£", "British Pound (£)"),
        CurrencyItem("AED", "AED", "UAE Dirham"),
        CurrencyItem("SGD", "S$", "Singapore Dollar"),
        CurrencyItem("CAD", "CA$", "Canadian Dollar"),
        CurrencyItem("AUD", "A$", "Australian Dollar"),
        CurrencyItem("JPY", "¥", "Japanese Yen (¥)"),
        CurrencyItem("CHF", "CHF", "Swiss Franc")
    )
    val SUPPORTED = supported

    fun getSymbol(code: String): String {
        return supported.find { it.code.equals(code, ignoreCase = true) }?.symbol ?: code
    }
}
