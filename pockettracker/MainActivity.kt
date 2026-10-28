package com.example.pockettracker

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.json.JSONArray
import org.json.JSONObject
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId

data class Expense(
    val id: Long,
    val amount: Double,
    val category: String,
    val note: String,
    val date: String
)

private val categories = listOf(
    "Food", "Transport", "Shopping",
    "Bills", "Health", "Other"
)

private val rupiah = NumberFormat.getCurrencyInstance(Locale("id", "ID"))

private fun money(amount: Double): String {
    rupiah.maximumFractionDigits = 0
    return rupiah.format(amount)
}

private fun today(): String =
    SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

private fun month(): String =
    SimpleDateFormat("yyyy-MM", Locale.US).format(Date())

private val LightColors = lightColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFF187A52),
    onPrimary = androidx.compose.ui.graphics.Color.White,
    secondary = androidx.compose.ui.graphics.Color(0xFF4C8068),
    background = androidx.compose.ui.graphics.Color(0xFFF5F7F5),
    surface = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
    surfaceVariant = androidx.compose.ui.graphics.Color(0xFFE8F0EA)
)

private val DarkColors = darkColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFF83D6AA),
    onPrimary = androidx.compose.ui.graphics.Color(0xFF003920),
    secondary = androidx.compose.ui.graphics.Color(0xFFA4D8BA),
    background = androidx.compose.ui.graphics.Color(0xFF101713),
    surface = androidx.compose.ui.graphics.Color(0xFF19221C),
    surfaceVariant = androidx.compose.ui.graphics.Color(0xFF26352C)
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val prefs = remember {
                getSharedPreferences("pockettrack", Context.MODE_PRIVATE)
            }
            var darkMode by remember {
                mutableStateOf(prefs.getBoolean("dark_mode", false))
            }

            MaterialTheme(
                colorScheme = if (darkMode) DarkColors else LightColors
            ) {
                PocketTrack(
                    context = this,
                    darkMode = darkMode,
                    onThemeChange = { enabled ->
                        darkMode = enabled
                        prefs.edit().putBoolean("dark_mode", enabled).apply()
                    }
                )
            }
        }
    }
}

@Composable
fun PocketTrack(
    context: Context,
    darkMode: Boolean,
    onThemeChange: (Boolean) -> Unit
) {
    val prefs = remember {
        context.getSharedPreferences("pockettrack", Context.MODE_PRIVATE)
    }

    fun loadExpenses(): List<Expense> {
        val array = JSONArray(prefs.getString("expenses", "[]"))
        return (0 until array.length()).map { i ->
            val obj = array.getJSONObject(i)
            Expense(
                obj.getLong("id"),
                obj.getDouble("amount"),
                obj.getString("category"),
                obj.getString("note"),
                obj.getString("date")
            )
        }.sortedByDescending { it.id }
    }

    var expenses by remember { mutableStateOf(loadExpenses()) }
    var budget by remember {
        mutableDoubleStateOf(prefs.getFloat("budget", 1000000f).toDouble())
    }
    var page by remember { mutableStateOf("Home") }

    val monthlyExpenses = expenses.filter { it.date.startsWith(month()) }
    val total = monthlyExpenses.sumOf { it.amount }
    val remaining = budget - total

    fun saveExpenses(updated: List<Expense>) {
        val array = JSONArray()
        updated.forEach { expense ->
            array.put(JSONObject().apply {
                put("id", expense.id)
                put("amount", expense.amount)
                put("category", expense.category)
                put("note", expense.note)
                put("date", expense.date)
            })
        }
        prefs.edit().putString("expenses", array.toString()).apply()
        expenses = updated
    }

    Scaffold(
        modifier = Modifier.semantics {
            testTagsAsResourceId = true
        },
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 12.dp, top = 16.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        "PocketTrack",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Your money, made clearer.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(
                    onClick = { onThemeChange(!darkMode) },
                    modifier = Modifier.testTag("dark_mode_toggle")) {
                    Text(if (darkMode) "☀ Light" else "☾ Dark")
                }
            }
        },
        bottomBar = {
            NavigationBar {
                listOf("Home", "History", "Add", "Budget", "Insights").forEach { destination ->
                    NavigationBarItem(
                        modifier = Modifier.testTag(
                            "${destination.lowercase()}_nav"
                        ),
                        selected = page == destination,
                        onClick = { page = destination },
                        icon = {
                            Text(
                                when (destination) {
                                    "Home" -> "⌂"
                                    "History" -> "≡"
                                    "Add" -> "+"
                                    "Budget" -> "＄"
                                    else -> "%"
                                }
                            )
                        },
                        label = { Text(destination) }
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            when (page) {
                "Home" -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(Modifier.padding(20.dp)) {
                            Text(
                                "MONTHLY SPENDING",
                                style = MaterialTheme.typography.labelLarge
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                money(total),
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(Modifier.height(18.dp))
                            Text("Budget: ${money(budget)}")
                            Text(
                                if (remaining >= 0)
                                    "Remaining: ${money(remaining)}"
                                else
                                    "Over budget: ${money(-remaining)}",
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.height(10.dp))
                            LinearProgressIndicator(
                                progress = {
                                    if (budget > 0) (total / budget).toFloat().coerceIn(0f, 1f)
                                    else 0f
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Text("Recent expenses", style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold)
                    if (expenses.isEmpty()) {
                        EmptyState("No expenses yet. Tap Add to record your first expense.")
                    } else {
                        expenses.take(5).forEach { expense -> ExpenseItem(expense) }
                    }
                }

                "Add" -> {
                    var amount by remember { mutableStateOf("") }
                    var note by remember { mutableStateOf("") }
                    var category by remember { mutableStateOf(categories[0]) }
                    var error by remember { mutableStateOf("") }

                    Text("Add an expense", style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = it },
                        label = { Text("Amount (Rp)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("amount_input"),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("Description") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("description_input"),
                        singleLine = true
                    )
                    Text("Choose a category", style = MaterialTheme.typography.titleMedium)
                    Card {
                        Column(Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
                            categories.forEach { item ->
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .clickable { category = item }
                                        .padding(vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = category == item,
                                        onClick = { category = item }
                                    )
                                    Text(item)
                                }
                            }
                        }
                    }
                    if (error.isNotEmpty()) {
                        Text(error, color = MaterialTheme.colorScheme.error)
                    }
                    Button(
                        onClick = {
                            val value = amount.replace(",", ".").toDoubleOrNull()
                            if (value == null || value <= 0) {
                                error = "Enter a valid amount."
                            } else {
                                saveExpenses(
                                    listOf(
                                        Expense(
                                            System.currentTimeMillis(),
                                            value,
                                            category,
                                            note.ifBlank { category },
                                            today()
                                        )
                                    ) + expenses
                                )
                                page = "Home"
                            }
                        },
                        Modifier
                            .fillMaxWidth()
                            .testTag("save_expense_button")
                    ) { Text("Save expense") }
                }


                "History" -> {
                    Text(
                        "Transaction history",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )

                    if (expenses.isEmpty()) {
                        EmptyState("Your transactions will appear here.")
                    } else {
                        expenses.forEach { expense ->
                            ExpenseItem(
                                expense = expense,
                                onDelete = {
                                    saveExpenses(
                                        expenses.filterNot { it.id == expense.id }
                                    )
                                }
                            )
                        }
                    }
                }

                "Budget" -> {
                    var input by remember(budget) { mutableStateOf(budget.toLong().toString()) }
                    var message by remember { mutableStateOf("") }

                    Text("Monthly budget", style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold)
                    Card {
                        Column(Modifier.padding(16.dp)) {
                            Text("Spent this month", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(money(total), style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                if (remaining >= 0) "Remaining: ${money(remaining)}"
                                else "Over budget: ${money(-remaining)}"
                            )
                            Spacer(Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = {
                                    if (budget > 0) (total / budget).toFloat().coerceIn(0f, 1f)
                                    else 0f
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                    OutlinedTextField(
                        value = input,
                        onValueChange = { input = it },
                        label = { Text("Budget (Rp)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("budget_input"),
                        singleLine = true
                    )
                    Button(
                        onClick = {
                            val value = input.toDoubleOrNull()
                            if (value != null && value > 0) {
                                budget = value
                                prefs.edit().putFloat("budget", value.toFloat()).apply()
                                message = "Budget saved!"
                            } else {
                                message = "Enter a valid budget."
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("save_budget_button")
                    ) { Text("Save budget") }
                    if (message.isNotEmpty()) Text(message)
                }

                "Insights" -> {
                    Text("Spending insights", style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold)
                    Text("Category breakdown this month",
                        color = MaterialTheme.colorScheme.onSurfaceVariant)

                    val breakdown = monthlyExpenses
                        .groupBy { it.category }
                        .mapValues { entry -> entry.value.sumOf { it.amount } }
                        .toList()
                        .sortedByDescending { it.second }

                    if (breakdown.isEmpty()) {
                        EmptyState("Add expenses to see your category insights.")
                    } else {
                        breakdown.forEach { (category, amount) ->
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(category, fontWeight = FontWeight.SemiBold)
                                        Text(money(amount))
                                    }
                                    Spacer(Modifier.height(10.dp))
                                    LinearProgressIndicator(
                                        progress = {
                                            if (total > 0) (amount / total).toFloat().coerceIn(0f, 1f)
                                            else 0f
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        "${if (total > 0) (amount / total * 100).toInt() else 0}% of monthly spending",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
fun EmptyState(message: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Text(
            message,
            modifier = Modifier.padding(18.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}


@Composable
fun ExpenseItem(
    expense: Expense,
    onDelete: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    expense.note,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(Modifier.height(3.dp))

                Text(
                    "${expense.category} • ${expense.date}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    "-${money(expense.amount)}",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )

                if (onDelete != null) {
                    TextButton(
                        onClick = onDelete,
                        modifier = Modifier.testTag("delete_expense_${expense.id}")) {
                        Text(
                            "Delete",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}