package com.example.data.local

import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.CategoryEntity
import com.example.domain.model.AccountType
import com.example.domain.model.CategoryType
import java.util.UUID

object DefaultData {
    val defaultAccounts = listOf(
        AccountEntity(
            id = "default_acc_cash",
            name = "Cash Wallet",
            type = AccountType.CASH,
            currency = "INR",
            openingBalance = 0.0,
            colorHex = "#3BAF72",
            iconName = "payments",
            notes = "Physical cash"
        ),
        AccountEntity(
            id = "default_acc_bank",
            name = "Bank Account",
            type = AccountType.BANK,
            currency = "INR",
            openingBalance = 0.0,
            colorHex = "#4A6572",
            iconName = "account_balance",
            notes = "Primary bank account"
        )
    )

    val defaultExpenseCategories = listOf(
        CategoryEntity(id = "cat_food", name = "Food & Dining", type = CategoryType.EXPENSE, iconName = "restaurant", colorHex = "#F59E0B", displayOrder = 1),
        CategoryEntity(id = "cat_groceries", name = "Groceries", type = CategoryType.EXPENSE, iconName = "shopping_cart", colorHex = "#10B981", displayOrder = 2, parentId = "cat_food"),
        CategoryEntity(id = "cat_restaurants", name = "Restaurants & Bars", type = CategoryType.EXPENSE, iconName = "local_cafe", colorHex = "#F59E0B", displayOrder = 3, parentId = "cat_food"),
        CategoryEntity(id = "cat_transport", name = "Transport", type = CategoryType.EXPENSE, iconName = "directions_car", colorHex = "#3B82F6", displayOrder = 4),
        CategoryEntity(id = "cat_fuel", name = "Fuel & Gas", type = CategoryType.EXPENSE, iconName = "local_gas_station", colorHex = "#EF4444", displayOrder = 5, parentId = "cat_transport"),
        CategoryEntity(id = "cat_shopping", name = "Shopping", type = CategoryType.EXPENSE, iconName = "shopping_bag", colorHex = "#EC4899", displayOrder = 6),
        CategoryEntity(id = "cat_entertainment", name = "Entertainment", type = CategoryType.EXPENSE, iconName = "movie", colorHex = "#8B5CF6", displayOrder = 7),
        CategoryEntity(id = "cat_bills", name = "Bills & Utilities", type = CategoryType.EXPENSE, iconName = "receipt_long", colorHex = "#64748B", displayOrder = 8),
        CategoryEntity(id = "cat_rent", name = "Rent / Mortgage", type = CategoryType.EXPENSE, iconName = "home", colorHex = "#0EA5E9", displayOrder = 9),
        CategoryEntity(id = "cat_health", name = "Health & Medical", type = CategoryType.EXPENSE, iconName = "medical_services", colorHex = "#14B8A6", displayOrder = 10),
        CategoryEntity(id = "cat_education", name = "Education", type = CategoryType.EXPENSE, iconName = "school", colorHex = "#6366F1", displayOrder = 11),
        CategoryEntity(id = "cat_travel", name = "Travel & Holiday", type = CategoryType.EXPENSE, iconName = "flight", colorHex = "#06B6D4", displayOrder = 12),
        CategoryEntity(id = "cat_insurance", name = "Insurance", type = CategoryType.EXPENSE, iconName = "verified_user", colorHex = "#475569", displayOrder = 13),
        CategoryEntity(id = "cat_subscriptions", name = "Subscriptions", type = CategoryType.EXPENSE, iconName = "subscriptions", colorHex = "#A855F7", displayOrder = 14),
        CategoryEntity(id = "cat_personal", name = "Personal Care", type = CategoryType.EXPENSE, iconName = "face", colorHex = "#F43F5E", displayOrder = 15),
        CategoryEntity(id = "cat_gifts", name = "Gifts & Donations", type = CategoryType.EXPENSE, iconName = "card_giftcard", colorHex = "#FB7185", displayOrder = 16),
        CategoryEntity(id = "cat_taxes", name = "Taxes", type = CategoryType.EXPENSE, iconName = "account_balance", colorHex = "#78716C", displayOrder = 17),
        CategoryEntity(id = "cat_other_expense", name = "Other Expense", type = CategoryType.EXPENSE, iconName = "more_horiz", colorHex = "#94A3B8", displayOrder = 18)
    )

    val defaultIncomeCategories = listOf(
        CategoryEntity(id = "cat_salary", name = "Salary / Wages", type = CategoryType.INCOME, iconName = "payments", colorHex = "#10B981", displayOrder = 1),
        CategoryEntity(id = "cat_freelance", name = "Freelance / Consulting", type = CategoryType.INCOME, iconName = "laptop_mac", colorHex = "#3B82F6", displayOrder = 2),
        CategoryEntity(id = "cat_business", name = "Business Revenue", type = CategoryType.INCOME, iconName = "storefront", colorHex = "#6366F1", displayOrder = 3),
        CategoryEntity(id = "cat_interest", name = "Interest & Dividends", type = CategoryType.INCOME, iconName = "trending_up", colorHex = "#F59E0B", displayOrder = 4),
        CategoryEntity(id = "cat_investments", name = "Capital Gains", type = CategoryType.INCOME, iconName = "query_stats", colorHex = "#8B5CF6", displayOrder = 5),
        CategoryEntity(id = "cat_refund", name = "Refund / Reimbursement", type = CategoryType.INCOME, iconName = "replay", colorHex = "#14B8A6", displayOrder = 6),
        CategoryEntity(id = "cat_gift_income", name = "Gifts Received", type = CategoryType.INCOME, iconName = "card_giftcard", colorHex = "#EC4899", displayOrder = 7),
        CategoryEntity(id = "cat_other_income", name = "Other Income", type = CategoryType.INCOME, iconName = "attach_money", colorHex = "#10B981", displayOrder = 8)
    )

    val allDefaultCategories = defaultExpenseCategories + defaultIncomeCategories
}
