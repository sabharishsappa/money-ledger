package com.example.data

object DefaultLedgerSeed {

    data class MainGroupTemplate(
        val id: String,
        val name: String,
        val nature: AccountNature,
        val sortOrder: Long
    )

    data class GroupTemplate(
        val id: String,
        val mainGroupId: String,
        val name: String,
        val nature: AccountNature,
        val sortOrder: Long
    )

    data class AccountTemplate(
        val id: String,
        val groupId: String,
        val name: String,
        val nature: AccountNature,
        val sortOrder: Long
    )

    data class CategoryTemplate(
        val id: String,
        val name: String,
        val type: CategoryType,
        val colorHex: String,
        val subcategories: List<String>,
        val sortOrder: Long
    )

    val defaultMainGroups = listOf(
        MainGroupTemplate(MainAccountGroup.DEFAULT_ASSETS_ID, "Assets", AccountNature.ASSET, 0),
        MainGroupTemplate(MainAccountGroup.DEFAULT_LIABILITIES_ID, "Liabilities", AccountNature.LIABILITY, 1)
    )

    val defaultGroups = listOf(
        GroupTemplate("grp_cash", MainAccountGroup.DEFAULT_ASSETS_ID, "Cash", AccountNature.ASSET, 0),
        GroupTemplate("grp_bank", MainAccountGroup.DEFAULT_ASSETS_ID, "Bank Accounts", AccountNature.ASSET, 1),
        GroupTemplate("grp_cards", MainAccountGroup.DEFAULT_LIABILITIES_ID, "Credit Cards", AccountNature.LIABILITY, 2),
        GroupTemplate("grp_investments", MainAccountGroup.DEFAULT_ASSETS_ID, "Investments", AccountNature.ASSET, 3),
        GroupTemplate("grp_loans", MainAccountGroup.DEFAULT_LIABILITIES_ID, "Loans", AccountNature.LIABILITY, 4),
        GroupTemplate("grp_others", MainAccountGroup.DEFAULT_ASSETS_ID, "Others", AccountNature.ASSET, 5)
    )

    val defaultAccounts = listOf(
        AccountTemplate("acc_cash_main", "grp_cash", "Cash", AccountNature.ASSET, 0),
        AccountTemplate("acc_bank_main", "grp_bank", "Primary Savings", AccountNature.ASSET, 1),
        AccountTemplate("acc_card_main", "grp_cards", "Credit Card", AccountNature.LIABILITY, 2),
        AccountTemplate("acc_invest_main", "grp_investments", "Mutual Funds", AccountNature.ASSET, 3)
    )

    val defaultCategories = listOf(
        // Expense Categories
        CategoryTemplate("cat_exp_food", "Food", CategoryType.EXPENSE, "#F06458", listOf("Groceries", "Dining Out", "Snacks", "Coffee"), 0),
        CategoryTemplate("cat_exp_finance", "Finance Cost", CategoryType.EXPENSE, "#F5974E", listOf("Loan Interest", "Bank Charges", "Tax", "Advisory"), 1),
        CategoryTemplate("cat_exp_vehicle", "Vehicle", CategoryType.EXPENSE, "#F4CE47", listOf("Fuel / Petrol", "Insurance", "Service", "Parking"), 2),
        CategoryTemplate("cat_exp_home", "Home - Expenses", CategoryType.EXPENSE, "#A8D456", listOf("Rent", "Electricity", "Internet", "Maintenance"), 3),
        CategoryTemplate("cat_exp_personal", "Personal", CategoryType.EXPENSE, "#4CC9B0", listOf("Travel / Food", "Clothing", "Grooming", "Subscriptions"), 4),
        CategoryTemplate("cat_exp_health", "Health & Insurance", CategoryType.EXPENSE, "#5C9DF5", listOf("Medical", "Pharmacy", "Health Policy", "Fitness"), 5),
        CategoryTemplate("cat_exp_shopping", "Shopping", CategoryType.EXPENSE, "#7B8AF7", listOf("Electronics", "Household", "Gifts"), 6),
        CategoryTemplate("cat_exp_other", "Other Expenses", CategoryType.EXPENSE, "#A875F2", listOf("Relatives", "Exceptional", "Miscellaneous"), 7),

        // Income Categories
        CategoryTemplate("cat_inc_salary", "Salary", CategoryType.INCOME, "#F06458", listOf("Monthly Pay", "Bonus", "Variable Pay"), 0),
        CategoryTemplate("cat_inc_business", "Profession / Freelance", CategoryType.INCOME, "#F5974E", listOf("Consulting", "Client Receipt"), 1),
        CategoryTemplate("cat_inc_invest", "Investment Returns", CategoryType.INCOME, "#F4CE47", listOf("Dividends", "Interest", "Capital Gains"), 2),
        CategoryTemplate("cat_inc_other", "Other Income", CategoryType.INCOME, "#A8D456", listOf("Cashback", "Refund", "Gift"), 3)
    )
}
