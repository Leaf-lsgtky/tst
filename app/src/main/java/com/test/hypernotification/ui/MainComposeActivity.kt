package com.test.hypernotification.ui

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.test.hypernotification.RecognitionService
import com.test.hypernotification.ui.theme.HyperNotificationTheme

class MainComposeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HyperNotificationTheme {
                MainScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    var selectedTab by remember { mutableIntStateOf(0) }
    val navigationItems = listOf(
        NavigationItem("📊", "状态"),
        NavigationItem("⚙️", "设置"),
        NavigationItem("📝", "日志"),
        NavigationItem("🧪", "测试")
    )

    Scaffold(
        topBar = {
            MiuiTopBar(title = "HyperNotification")
        },
        bottomBar = {
            MiuiBottomNavigation(
                selectedIndex = selectedTab,
                items = navigationItems,
                onItemSelected = { selectedTab = it }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (selectedTab) {
                0 -> StatusScreen()
                1 -> SettingsScreen()
                2 -> LogScreen()
                3 -> TestScreen()
            }
        }
    }
}

@Composable
fun StatusScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        MiuiCard {
            Text(
                text = "服务状态",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            StatusItem("通知监听服务", "运行中", true)
            StatusItem("屏幕截图权限", "已授权", true)
            StatusItem("焦点通知权限", "已开启", true)
            StatusItem("Root权限", "已获取", true)
        }

        MiuiCard {
            Text(
                text = "识别统计",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            StatusItem("今日识别次数", "12", false)
            StatusItem("成功率", "95%", false)
            StatusItem("平均耗时", "2.3秒", false)
        }
    }
}

@Composable
fun StatusItem(label: String, value: String, isStatus: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 14.sp
        )
        Text(
            text = value,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            color = if (isStatus && (value == "运行中" || value == "已授权" || value == "已开启" || value == "已获取")) {
                Color(0xFF4CAF50)
            } else {
                MaterialTheme.colorScheme.onSurface
            }
        )
    }
}

@Composable
fun SettingsScreen() {
    val viewModel: SettingsViewModel = viewModel()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.loadSettings(context)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        MiuiCard {
            Text(
                "AI配置",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            OutlinedTextField(
                value = viewModel.apiToken.collectAsState().value,
                onValueChange = {
                    viewModel.updateApiToken(it)
                    viewModel.saveSettings(context)
                },
                label = { Text("智谱AI Token") },
                placeholder = { Text("请输入GLM-4V API Token") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
        }

        MiuiCard {
            Text(
                "识别设置",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            MiuiSwitchPreference(
                title = "启用焦点通知",
                description = "在屏幕顶部显示识别结果",
                checked = viewModel.enableFocusNotification.collectAsState().value,
                onCheckedChange = {
                    viewModel.updateFocusNotification(it)
                    viewModel.saveSettings(context)
                }
            )

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            MiuiSwitchPreference(
                title = "振动反馈",
                description = "识别完成时振动提醒",
                checked = viewModel.vibrationFeedback.collectAsState().value,
                onCheckedChange = {
                    viewModel.updateVibrationFeedback(it)
                    viewModel.saveSettings(context)
                }
            )
        }

        MiuiCard {
            Text(
                "磁贴设置",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Text(
                "延迟时间: ${viewModel.tileDelay.collectAsState().value}ms",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Slider(
                value = viewModel.tileDelay.collectAsState().value.toFloat(),
                onValueChange = {
                    viewModel.updateTileDelay(it.toInt())
                    viewModel.saveSettings(context)
                },
                valueRange = 0f..3000f,
                steps = 5,
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                "设置点击快捷磁贴后的延迟时间",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SwitchPreference(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
fun LogScreen() {
    val logs = remember {
        mutableStateListOf(
            "2024-01-20 10:30:15 - 开始识别",
            "2024-01-20 10:30:16 - 截图成功",
            "2024-01-20 10:30:17 - 转换Base64完成",
            "2024-01-20 10:30:19 - AI识别成功",
            "2024-01-20 10:30:19 - 取餐码: A12",
            "2024-01-20 10:30:19 - 商家: 麦当劳"
        )
    }

    MiuiCard(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            "运行日志",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            logs.forEach { log ->
                Text(
                    text = log,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
fun TestScreen() {
    var isRecognizing by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        MiuiCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Text(
                    "测试功能",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    "点击下方按钮测试取餐码识别功能",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                MiuiButton(
                    text = if (isRecognizing) "识别中..." else "开始识别",
                    onClick = {
                        if (!isRecognizing) {
                            isRecognizing = true
                            // 启动识别服务
                            val intent = Intent(context, RecognitionService::class.java)
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                context.startForegroundService(intent)
                            } else {
                                context.startService(intent)
                            }
                            // 3秒后重置状态
                            Handler(Looper.getMainLooper()).postDelayed({
                                isRecognizing = false
                            }, 3000)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isRecognizing
                )

                if (isRecognizing) {
                    MiuiProgressIndicator()
                }
            }
        }
    }
}