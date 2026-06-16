package com.example

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.provider.Settings
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Unique data class containing launching app details.
 */
data class AppInfo(
    val label: String,
    val packageName: String,
    val icon: Drawable?,
    val launchIntent: Intent?
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("launcher_prefs", Context.MODE_PRIVATE) }
    
    // Persistent state: should we show the scary prank screen as launcher home
    var showPrank by remember {
        mutableStateOf(sharedPrefs.getBoolean("show_prank", true))
    }
    
    var searchQuery by rememberSaveable { mutableStateOf("") }
    
    // List of installed applications on-device
    var appList by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
    var isAppsLoading by remember { mutableStateOf(true) }
    
    // Trigger reloading the installed apps asynchronously
    LaunchedEffect(Unit) {
        isAppsLoading = true
        withContext(Dispatchers.IO) {
            val pm = context.packageManager
            val intent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val list = pm.queryIntentActivities(intent, 0)
            val tempAppsList = mutableListOf<AppInfo>()
            for (resolveInfo in list) {
                val label = resolveInfo.loadLabel(pm).toString()
                val pkgName = resolveInfo.activityInfo.packageName
                val icon = resolveInfo.loadIcon(pm)
                val launchIntent = pm.getLaunchIntentForPackage(pkgName)
                
                // Exclude ourselves from cluttering the app list
                if (pkgName != context.packageName) {
                    tempAppsList.add(AppInfo(label, pkgName, icon, launchIntent))
                }
            }
            tempAppsList.sortBy { it.label.lowercase() }
            withContext(Dispatchers.Main) {
                appList = tempAppsList
                isAppsLoading = false
            }
        }
    }
    
    // Disable hardware back key during prank to look completely convincing!
    BackHandler {
        if (showPrank) {
            Toast.makeText(context, "⚠️ ERROR: FIRMWARE LOCK ENGAGED BY TAWHID BRO", Toast.LENGTH_SHORT).show()
        } else {
            if (searchQuery.isNotEmpty()) {
                searchQuery = ""
            }
        }
    }
    
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF030504) // Deep cyber terminal black
    ) {
        Crossfade(targetState = showPrank, label = "ScreenTransition") { isPrankActive ->
            if (isPrankActive) {
                HackerPrankView(
                    onOpenAppsPressed = {
                        sharedPrefs.edit().putBoolean("show_prank", false).apply()
                        showPrank = false
                    }
                )
            } else {
                AppDrawerView(
                    appList = appList,
                    isLoading = isAppsLoading,
                    searchQuery = searchQuery,
                    onSearchQueryChange = { searchQuery = it },
                    onReactivatePrank = {
                        sharedPrefs.edit().putBoolean("show_prank", true).apply()
                        showPrank = true
                    },
                    onOpenApp = { app ->
                        if (app.launchIntent != null) {
                            try {
                                context.startActivity(app.launchIntent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Could not open ${app.label}", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(context, "App cannot be opened", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
        }
    }
}

/**
 * Highly realistic and convincing custom hacker-ransomware lock screen layout.
 */
@Composable
fun HackerPrankView(
    onOpenAppsPressed: () -> Unit
) {
    val logs = remember { mutableStateListOf<String>() }
    
    // Fake progress bar loading indicator state
    var hackProgress by remember { mutableStateOf(0.12f) }
    
    val infiniteTransition = rememberInfiniteTransition(label = "WarningFlasher")
    
    // Smooth emergency warning glow multiplier
    val glowColorShift by infiniteTransition.animateColor(
        initialValue = Color(0xFF2C0202),
        targetValue = Color(0xFFE50914),
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "WarningGlow"
    )

    // Animated scanline vertical progression
    val scanlinePercent by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ScanlineMotion"
    )
    
    // Simulated live security takeover output scripts
    val loggingSteps = remember {
        listOf(
            "Initializing secure AES encryption handshake...",
            "Encrypting system folder maps [/Android/data/*] [SUCCESS]",
            "Mapping private storage and gallery packages...",
            "Uploading private backup blocks to Tawhid Cloud...",
            "Overriding system recovery registers...",
            "System UI takeover complete.",
            "Host network socket established through safe VPN tunnel.",
            "Blocking generic device lock overrides [SUCCESS]",
            "Awaiting contact for remote token key generation..."
        )
    }
    
    // Simulated logs streamer
    LaunchedEffect(Unit) {
        logs.add("CRITICAL HOST TAKEOVER ENGAGED...")
        var idx = 0
        while (idx < loggingSteps.size) {
            delay((800..1800).random().toLong())
            logs.add(loggingSteps[idx])
            idx++
        }
    }
    
    // Simulated progress loader
    LaunchedEffect(Unit) {
        while (hackProgress < 0.99f) {
            delay((1000..2500).random().toLong())
            hackProgress = (hackProgress + (0.05f + java.util.Random().nextFloat() * 0.10f)).coerceAtMost(0.99f)
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF030504))
    ) {
        // Scrolling binary digital ambient lines
        Box(modifier = Modifier.fillMaxSize()) {
            HackerDigitalRain()
        }

        // Animated red scanning line overlay
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val scanY = scanlinePercent * maxHeight.value
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .offset(y = scanY.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(Color.Transparent, Color(0xAAFF0000), Color.Transparent)
                        )
                    )
            )
        }
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header Warning Block
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(2.dp, glowColorShift, RoundedCornerShape(12.dp))
                    .background(Color(0xE6080202))
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Takeover warning",
                        tint = Color.Red,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "WARNING: ENCRYPTED FILE SYSTEM",
                        color = Color.Red,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                }
                
                Spacer(modifier = Modifier.height(10.dp))
                
                // YOUR PHONE IS HACKED BY TAWHID BRO
                Text(
                    text = "YOUR PHONE IS HACKED\nBY TAWHID BRO",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 20.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 28.sp,
                )
            }
            
            // Middle section - Main glowing hacker logo
            Card(
                modifier = Modifier
                    .size(130.dp)
                    .clip(CircleShape)
                    .border(3.dp, Color(0xFF00FF41), CircleShape),
                colors = CardDefaults.cardColors(containerColor = Color.Black)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    val context = LocalContext.current
                    val drawableId = context.resources.getIdentifier("hacker_logo", "drawable", context.packageName)
                    if (drawableId != 0) {
                        androidx.compose.foundation.Image(
                            painter = painterResource(id = drawableId),
                            contentDescription = "Core system locked",
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Core Locked",
                            tint = Color.Red,
                            modifier = Modifier.size(64.dp)
                        )
                    }
                }
            }
            
            // Real-time terminal log feed to look authentic
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .border(1.dp, Color(0xFF1B311E), RoundedCornerShape(8.dp)),
                colors = CardDefaults.cardColors(containerColor = Color(0xF2030604))
            ) {
                val listState = rememberLazyListState()
                
                LaunchedEffect(logs.size) {
                    if (logs.isNotEmpty()) {
                        listState.animateScrollToItem(logs.size - 1)
                    }
                }
                
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(10.dp),
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(logs) { log ->
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "root@tawhid_bro:~# ",
                                color = Color(0x9900FF41),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = log,
                                color = if (log.contains("SUCCESS") || log.contains("complete")) Color(0xFF00FF41) else Color.White,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }
            }
            
            // Progress Bar / Decryption keys status
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "ENCRYPTION PROGRESS",
                        color = Color.Gray,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                    Text(
                        text = "${(hackProgress * 100).toInt()}%",
                        color = Color.Red,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { hackProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = Color.Red,
                    trackColor = Color(0xFF1B0303)
                )
            }
            
            // Custom instructions with phone contact details as explicitly requested by the user
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF140303)),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, Color.Red, RoundedCornerShape(10.dp)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = "Call Contact",
                            tint = Color.Red,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = "CONTACT TO DECRYPT DEVICE",
                            color = Color.White,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                    
                    Text(
                        text = "যদি তুমি তোমার ফোনের লক ছাড়াতে চাও তাহলে এই নম্বরে যোগাযোগ করো:",
                        color = Color.LightGray,
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )
                    
                    // High-contrast highlighted phone number box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF2E0505), RoundedCornerShape(6.dp))
                            .border(1.dp, Color(0xFFFF5252), RoundedCornerShape(6.dp))
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "01635027635",
                            color = Color(0xFFFF8A80),
                            fontSize = 18.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 2.sp
                        )
                    }
                }
            }
            
            // Fully integrated secure button disguised as "SYSTEM BYPASS / APPS" to let the builder escape safely
            Button(
                onClick = onOpenAppsPressed,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .testTag("open_apps_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1B311E),
                    contentColor = Color(0xFF00FF41)
                ),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00FF41))
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "BYPASS INTERNAL CONTROLS",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}

/**
 * Robust scrollable hacker/matrix vertical falling columns implemented via pure Compose components.
 * This is 100% compile-safe and works on any Android platform SDK target.
 */
@Composable
fun HackerDigitalRain() {
    val characters = listOf("0", "1", "X", "$", "🔐", "☠️", "SYSTEM_LOCK", "ACCESS_DENIED", "TAWHID_BRO")
    val random = remember { java.util.Random() }
    
    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        repeat(7) { columnIndex ->
            // Let each column have slightly different scroll speed
            val scrollSpeed = remember { (1200..2800).random() }
            val columnTransition = rememberInfiniteTransition(label = "Col_$columnIndex")
            
            val offsetProgress by columnTransition.animateFloat(
                initialValue = -150f,
                targetValue = 600f,
                animationSpec = infiniteRepeatable(
                    animation = tween(scrollSpeed, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "Progress"
            )
            
            val columnChars = remember {
                List(10) { characters[random.nextInt(characters.size)] }
            }
            
            Column(
                modifier = Modifier
                    .offset(y = offsetProgress.dp)
                    .width(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                columnChars.forEachIndexed { i, char ->
                    val opacity = (i.toFloat() / columnChars.size).coerceIn(0.1f, 0.7f)
                    Text(
                        text = char,
                        color = Color(0x3300FF41),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 2.dp),
                        style = TextStyle(color = Color(0xFF00FF41).copy(alpha = opacity))
                    )
                }
            }
        }
    }
}

/**
 * Functional elegant Home Launcher layout / App Drawer with search query and direct launch interface
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDrawerView(
    appList: List<AppInfo>,
    isLoading: Boolean,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onReactivatePrank: () -> Unit,
    onOpenApp: (AppInfo) -> Unit
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    
    val filteredApps = remember(appList, searchQuery) {
        if (searchQuery.trim().isEmpty()) {
            appList
        } else {
            appList.filter { it.label.contains(searchQuery, ignoreCase = true) }
        }
    }
    
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            Column(
                modifier = Modifier
                    .background(Color(0xFF070A08))
                    .statusBarsPadding()
                    .padding(top = 12.dp, bottom = 8.dp)
            ) {
                // App Title and Prank Trigger utilities
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "SAFE LAUNCHER",
                            color = Color(0xFF00FF41),
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "Functional Launcher Menu",
                            color = Color.LightGray,
                            fontSize = 12.sp
                        )
                    }
                    
                    // Quick controls to easily prank friends again!
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = onReactivatePrank,
                            modifier = Modifier
                                .border(1.dp, Color.Red, RoundedCornerShape(8.dp))
                                .background(Color(0x37FF0000))
                                .size(36.dp)
                                .testTag("reactivate_prank_btn"),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Prank Settings",
                                tint = Color.Red,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        
                        IconButton(
                            onClick = {
                                // Direct path shortcut utility to system Settings panel
                                try {
                                    val intent = Intent(Settings.ACTION_HOME_SETTINGS).apply {
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    }
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    try {
                                        val intent = Intent(Settings.ACTION_SETTINGS).apply {
                                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                        }
                                        context.startActivity(intent)
                                    } catch (ex: Exception) {
                                        Toast.makeText(context, "Settings not reachable", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            modifier = Modifier
                                .border(1.dp, Color(0xFF00FF41), RoundedCornerShape(8.dp))
                                .background(Color(0x1100FF41))
                                .size(36.dp)
                                .testTag("settings_shortcut_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = Color(0xFF00FF41),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(14.dp))
                
                // Real-time app search input bar
                TextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .testTag("app_search_field"),
                    placeholder = {
                        Text(
                            text = "Search installed apps...",
                            color = Color.Gray,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 14.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = Color(0xFF00FF41)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear search",
                                    tint = Color.White
                                )
                            }
                        }
                    },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF0E1310),
                        unfocusedContainerColor = Color(0xFF0E1310),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = Color(0xFF00FF41),
                        focusedIndicatorColor = Color(0xFF00FF41),
                        unfocusedIndicatorColor = Color(0xFF1B421E)
                    ),
                    textStyle = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { keyboardController?.hide() })
                )
            }
        },
        containerColor = Color(0xFF070A08)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (isLoading) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = Color(0xFF00FF41))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "LOADING INSTALLED APPS...",
                        color = Color(0xFF00FF41),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else if (filteredApps.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "No apps found",
                        tint = Color.DarkGray,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "NO MATCHING APPLICATIONS FOUND",
                        color = Color.Gray,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 85.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp)
                        .testTag("apps_grid"),
                    contentPadding = PaddingValues(top = 10.dp, bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(filteredApps) { app ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onOpenApp(app) }
                                .padding(8.dp)
                                .testTag("app_item_${app.packageName}"),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (app.icon != null) {
                                AndroidView(
                                    factory = { ctx ->
                                        ImageView(ctx).apply {
                                            scaleType = ImageView.ScaleType.FIT_CENTER
                                        }
                                    },
                                    update = { imageView ->
                                        imageView.setImageDrawable(app.icon)
                                    },
                                    modifier = Modifier
                                        .size(46.dp)
                                        .padding(4.dp)
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .background(Color(0xFF222222), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Home,
                                        contentDescription = null,
                                        tint = Color.Gray
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(6.dp))
                            
                            Text(
                                text = app.label,
                                color = Color.White,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Standard visual Greeting function preserved to guarantee compatibility with pre-existing greeting snapshot tests!
 */
@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Hello $name!",
                color = Color(0xFF00FF41),
                fontSize = 20.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Hacker Launcher Active",
                color = Color.LightGray,
                fontSize = 14.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
