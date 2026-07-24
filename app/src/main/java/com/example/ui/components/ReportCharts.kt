package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class CategorySliceData(
    val category: String,
    val amount: Double,
    val color: Color
)

data class BarChartPoint(
    val label: String,
    val income: Double,
    val expense: Double
)

data class LineTrendPoint(
    val label: String,
    val value: Double
)

data class CustomerBalanceItem(
    val name: String,
    val amount: Double,
    val isReceivable: Boolean
)

@Composable
fun IncomeVsExpenseChart(
    income: Double,
    expense: Double,
    modifier: Modifier = Modifier
) {
    val total = (income + expense).let { if (it == 0.0) 1.0 else it }
    val incomePct = (income / total).toFloat()
    val expensePct = (expense / total).toFloat()

    var animatedProgress by remember { mutableStateOf(0f) }
    val progress by animateFloatAsState(
        targetValue = animatedProgress,
        animationSpec = tween(durationMillis = 800),
        label = "income_expense_anim"
    )

    LaunchedEffect(income, expense) {
        animatedProgress = 1f
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("income_vs_expense_chart_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Income vs Expenses Breakdown",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Bar Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    if (income > 0) {
                        Box(
                            modifier = Modifier
                                .weight((incomePct * progress).coerceAtLeast(0.01f))
                                .fillMaxHeight()
                                .background(Color(0xFF00897B))
                        )
                    }
                    if (expense > 0) {
                        Box(
                            modifier = Modifier
                                .weight((expensePct * progress).coerceAtLeast(0.01f))
                                .fillMaxHeight()
                                .background(Color(0xFFE53935))
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF00897B))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text("Total Income", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            "PKR ${String.format("%.2f", income)} (${(incomePct * 100).toInt()}%)",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = Color(0xFF00897B))
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE53935))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Total Expenses", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            "PKR ${String.format("%.2f", expense)} (${(expensePct * 100).toInt()}%)",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = Color(0xFFE53935))
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MonthlyRevenueExpenseChart(
    dataPoints: List<BarChartPoint>,
    modifier: Modifier = Modifier
) {
    val maxVal = dataPoints.maxOfOrNull { maxOf(it.income, it.expense) }?.takeIf { it > 0 } ?: 1000.0

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("monthly_revenue_expense_chart"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Monthly Financial Volume",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (dataPoints.isEmpty()) {
                Text(
                    text = "No monthly activity recorded yet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Bottom
                ) {
                    dataPoints.takeLast(6).forEach { point ->
                        val incomeHeightRatio = (point.income / maxVal).toFloat().coerceIn(0.05f, 1f)
                        val expenseHeightRatio = (point.expense / maxVal).toFloat().coerceIn(0.05f, 1f)

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom,
                            modifier = Modifier.fillMaxHeight()
                        ) {
                            Row(
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                // Income bar
                                Box(
                                    modifier = Modifier
                                        .width(12.dp)
                                        .fillMaxHeight(incomeHeightRatio)
                                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                        .background(Color(0xFF00897B))
                                )
                                // Expense bar
                                Box(
                                    modifier = Modifier
                                        .width(12.dp)
                                        .fillMaxHeight(expenseHeightRatio)
                                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                        .background(Color(0xFFE53935))
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = point.label,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryBreakdownChart(
    slices: List<CategorySliceData>,
    modifier: Modifier = Modifier
) {
    val totalAmt = slices.sumOf { it.amount }.let { if (it == 0.0) 1.0 else it }
    var selectedCategory by remember { mutableStateOf<CategorySliceData?>(null) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("category_breakdown_chart"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Transaction Categories Breakdown",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (slices.isEmpty()) {
                Text(
                    text = "No category data available.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Donut Canvas
                    Box(
                        modifier = Modifier.size(120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.size(110.dp)) {
                            var startAngle = -90f
                            slices.forEach { slice ->
                                val sweepAngle = ((slice.amount / totalAmt) * 360f).toFloat()
                                drawArc(
                                    color = slice.color,
                                    startAngle = startAngle,
                                    sweepAngle = sweepAngle,
                                    useCenter = false,
                                    style = Stroke(width = 24f, cap = StrokeCap.Butt)
                                )
                                startAngle += sweepAngle
                            }
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${slices.size}",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Types",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Category Legend List
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        slices.take(5).forEach { slice ->
                            val pct = ((slice.amount / totalAmt) * 100).toInt()
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { selectedCategory = slice }
                                    .padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(slice.color)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = slice.category,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1
                                    )
                                }
                                Text(
                                    text = "PKR ${String.format("%.0f", slice.amount)} ($pct%)",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CashFlowTrendChart(
    points: List<LineTrendPoint>,
    modifier: Modifier = Modifier
) {
    val maxVal = points.maxOfOrNull { kotlin.math.abs(it.value) }?.takeIf { it > 0 } ?: 1000.0

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("cash_flow_trend_chart"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Net Cash Flow Trend",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (points.size < 2) {
                Text(
                    text = "Requires at least 2 data points to render cash flow trend.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                val lineColor = Color(0xFF1E88E5)
                val gridColor = MaterialTheme.colorScheme.outlineVariant

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                ) {
                    val width = size.width
                    val height = size.height
                    val spacing = width / (points.size - 1)

                    val path = Path()
                    points.forEachIndexed { index, pt ->
                        val x = index * spacing
                        val normalizedVal = (pt.value / maxVal).toFloat()
                        val y = height / 2 - (normalizedVal * (height / 2.2f))

                        if (index == 0) {
                            path.moveTo(x, y)
                        } else {
                            path.lineTo(x, y)
                        }

                        drawCircle(
                            color = lineColor,
                            radius = 6f,
                            center = Offset(x, y)
                        )
                    }

                    // Draw baseline
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, height / 2),
                        end = Offset(width, height / 2),
                        strokeWidth = 2f
                    )

                    drawPath(
                        path = path,
                        color = lineColor,
                        style = Stroke(width = 4f, cap = StrokeCap.Round)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    points.forEach { pt ->
                        Text(
                            text = pt.label,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TopCustomersChart(
    topCustomers: List<CustomerBalanceItem>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("top_customers_chart"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Top Customer Balances & Receivables",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (topCustomers.isEmpty()) {
                Text(
                    text = "No customer balance records available.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                val maxBal = topCustomers.maxOfOrNull { kotlin.math.abs(it.amount) }?.takeIf { it > 0 } ?: 1.0

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    topCustomers.take(5).forEachIndexed { index, item ->
                        val barPct = (kotlin.math.abs(item.amount) / maxBal).toFloat().coerceIn(0.05f, 1f)
                        val color = if (item.isReceivable) Color(0xFF00897B) else Color(0xFFE53935)

                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${index + 1}. ${item.name}",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                )
                                Text(
                                    text = "PKR ${String.format("%.2f", item.amount)}",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = color
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(barPct)
                                        .fillMaxHeight()
                                        .background(color)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
