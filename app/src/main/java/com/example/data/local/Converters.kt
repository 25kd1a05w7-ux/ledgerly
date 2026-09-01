package com.example.data.local

import androidx.room.TypeConverter
import com.example.domain.model.AccountType
import com.example.domain.model.BudgetPeriod
import com.example.domain.model.CategoryType
import com.example.domain.model.RecurrenceFrequency
import com.example.domain.model.TransactionType

class Converters {
    @TypeConverter
    fun fromAccountType(value: AccountType?): String? = value?.name

    @TypeConverter
    fun toAccountType(value: String?): AccountType? =
        value?.let { runCatching { AccountType.valueOf(it) }.getOrDefault(AccountType.CASH) }

    @TypeConverter
    fun fromTransactionType(value: TransactionType?): String? = value?.name

    @TypeConverter
    fun toTransactionType(value: String?): TransactionType? =
        value?.let { runCatching { TransactionType.valueOf(it) }.getOrDefault(TransactionType.EXPENSE) }

    @TypeConverter
    fun fromCategoryType(value: CategoryType?): String? = value?.name

    @TypeConverter
    fun toCategoryType(value: String?): CategoryType? =
        value?.let { runCatching { CategoryType.valueOf(it) }.getOrDefault(CategoryType.EXPENSE) }

    @TypeConverter
    fun fromBudgetPeriod(value: BudgetPeriod?): String? = value?.name

    @TypeConverter
    fun toBudgetPeriod(value: String?): BudgetPeriod? =
        value?.let { runCatching { BudgetPeriod.valueOf(it) }.getOrDefault(BudgetPeriod.MONTHLY) }

    @TypeConverter
    fun fromRecurrenceFrequency(value: RecurrenceFrequency?): String? = value?.name

    @TypeConverter
    fun toRecurrenceFrequency(value: String?): RecurrenceFrequency? =
        value?.let { runCatching { RecurrenceFrequency.valueOf(it) }.getOrDefault(RecurrenceFrequency.MONTHLY) }

    @TypeConverter
    fun fromStringList(value: List<String>?): String =
        value?.joinToString("||") ?: ""

    @TypeConverter
    fun toStringList(value: String?): List<String> =
        if (value.isNullOrEmpty()) emptyList() else value.split("||").filter { it.isNotBlank() }
}
