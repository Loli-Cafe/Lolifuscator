import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.material.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.toPainter
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import cafe.loli.lolifuscator.image.GelbooruImageProvider
import cafe.loli.lolifuscator.logger.KurwaLoggerAppender
import cafe.loli.lolifuscator.skidfuscator.Skidfuscator
import com.darkrockstudios.libraries.mpfilepicker.DirectoryPicker
import com.darkrockstudios.libraries.mpfilepicker.FilePicker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.Desktop
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.ThreadLocalRandom
import java.util.function.Consumer
import javax.imageio.ImageIO

/*
    This class needs serious recode, however I'm lazy
 */

val defaultDirectory = Path.of(System.getProperty("user.home", "~"), ".config", "lolifuscator")
val latestConfigurationFile = defaultDirectory.resolve("latest.properties")
val warningFile = defaultDirectory.resolve(".warning")

fun main() = application {
    if (Files.notExists(defaultDirectory)) Files.createDirectories(defaultDirectory)

    var skidfuscatorPath by remember { mutableStateOf(Skidfuscator.skidfuscatorJar) }
    var skidfuscator: Skidfuscator? by remember { mutableStateOf(skidfuscatorPath?.let { Skidfuscator(it) }) }
    var showPicker by remember { mutableStateOf(false) }
    var showAlert by remember { mutableStateOf(skidfuscator == null) }

    Window(
        state = WindowState(size = DpSize(1000.dp, 800.dp)),
        icon = ImageIO.read(
            this.javaClass.getResourceAsStream(
                "/icons/icon${
                    ThreadLocalRandom.current().nextInt(0, 4)
                }.png"
            )
        ).toPainter(),
        title = "Lolifuscator | ${skidfuscator?.version ?: "Unknown"}",
        resizable = true,
        onCloseRequest = ::exitApplication
    ) {
        FilePicker(showPicker) { path ->
            path?.let {
                skidfuscatorPath = Path.of(it.path)
                skidfuscatorPath?.let {
                    if (Skidfuscator.isSkidfuscator(it)) {
                        showAlert = false
                        skidfuscator = Skidfuscator(it)
                    }
                }
            }
            showPicker = false
        }

        composeSkidfuscatorErrorAlert(showAlert, onSelect = {
            showPicker = true
        })

        correction(skidfuscator)
    }
}


@OptIn(ExperimentalComposeUiApi::class)
@Composable
@Preview
fun correction(skidfuscator: Skidfuscator?) {
    var starterForgeRunning by remember { mutableStateOf(false) }
    var starterSpigotRunning by remember { mutableStateOf(false) }
    var alert by remember { mutableStateOf(false) }
    var alertTitle by remember { mutableStateOf("") }
    var alertText by remember { mutableStateOf("") }

    var loadConfig by remember { mutableStateOf(false) }
    var saveConfig by remember { mutableStateOf(false) }

    var configPath by remember { mutableStateOf("") }
    var inputFile by remember { mutableStateOf("") }
    var outputFile by remember { mutableStateOf("") }
    var libsDir by remember { mutableStateOf("") }
    var exemptFile by remember { mutableStateOf("") }
    var rtFile by remember { mutableStateOf("") }

    var fuckItParameter by remember { mutableStateOf(false) }
    var lcParameter by remember { mutableStateOf(false) }
    var phParameter by remember { mutableStateOf(false) }
    var reParameter by remember { mutableStateOf(false) }

    val parameters = remember { SnapshotStateList<String>() }

    val tabs = listOf("Help", "Configuration", "Obfuscation", "ロリ・カフェ")
    var selectedTab by remember { mutableStateOf(0) }
    var previousSelectedTab by remember { mutableStateOf(0) }
    var parametersExpanded by remember { mutableStateOf(false) }

    var showFilePicker by remember { mutableStateOf(false) }
    var showDirectoryPicker by remember { mutableStateOf(false) }
    var fileSelectionType by remember { mutableStateOf(0) }

    val logs = remember { SnapshotStateList<String>() }
    var running by remember { mutableStateOf(false) }
    var stopping by remember { mutableStateOf(false) }
    var executor: ExecutorService? by remember { mutableStateOf(null) }

    val imageProvider by remember { mutableStateOf(GelbooruImageProvider()) }
    var image: ImageBitmap? by remember { mutableStateOf(null) }

    var reloadImage by remember { mutableStateOf(true) }
    var loadInstance by remember { mutableStateOf(true) }
    var disableButtons by remember { mutableStateOf(true) }

    var nsfwWarning by remember { mutableStateOf(Files.notExists(warningFile)) }
    var showNsfwWarning by remember { mutableStateOf(false) }

    var sorting by remember { mutableStateOf(false) }
    var rating by remember { mutableStateOf(false) }

    var sortingType by remember { mutableStateOf("New") }
    var ratingType by remember { mutableStateOf(if (nsfwWarning) "Explicit" else Files.readString(warningFile)) }

    fun loadConfiguration(path: Path) {
        if (Files.notExists(path)) return

        val properties = Properties()
        Files.newInputStream(path).use {
            properties.load(it)

            configPath = properties.getProperty("config", "")
            inputFile = properties.getProperty("input", "")
            outputFile = properties.getProperty("output", "")
            libsDir = properties.getProperty("libs", "")
            exemptFile = properties.getProperty("exempt", "")
            rtFile = properties.getProperty("rt", "")

            fuckItParameter = properties.getProperty("fuckIt", "false").toBoolean()
            lcParameter = properties.getProperty("lc", "false").toBoolean()
            phParameter = properties.getProperty("ph", "false").toBoolean()
            reParameter = properties.getProperty("re", "false").toBoolean()

            parameters.clear()
            if (fuckItParameter)
                parameters.add("FuckIt")

            if (lcParameter)
                parameters.add("Lowcon")

            if (phParameter)
                parameters.add("Phantom")

            if (reParameter)
                parameters.add("Renamer")
        }
    }

    fun saveConfiguration(path: Path) {
        Files.newOutputStream(path).use {
            val properties = Properties()
            properties.setProperty("config", configPath)
            properties.setProperty("input", inputFile)
            properties.setProperty("output", outputFile)
            properties.setProperty("libs", libsDir)
            properties.setProperty("exempt", exemptFile)
            properties.setProperty("rt", rtFile)

            properties.setProperty("fuckIt", fuckItParameter.toString())
            properties.setProperty("lc", lcParameter.toString())
            properties.setProperty("ph", phParameter.toString())
            properties.setProperty("re", reParameter.toString())

            properties.store(it, null)
        }
    }

    MaterialTheme {
        FilePicker(showFilePicker) { path ->
            showFilePicker = false
            path?.let {
                when (fileSelectionType) {
                    0 -> configPath = it.path
                    1 -> inputFile = it.path
                    2 -> outputFile = it.path
                    3 -> exemptFile = it.path
                    4 -> rtFile = it.path
                }
            }
        }

        FilePicker(saveConfig) { path ->
            saveConfig = false
            path?.let {
                saveConfiguration(Path.of(it.path))
                saveConfiguration(latestConfigurationFile)
            }
        }

        FilePicker(loadConfig) { path ->
            loadConfig = false
            path?.let {
                saveConfiguration(Path.of(it.path))
            }
        }

        DirectoryPicker(showDirectoryPicker) { path ->
            showDirectoryPicker = false
            path?.let {
                when (fileSelectionType) {
                    5 -> libsDir = it
                    6 -> {}//placeholder
                }
            }
        }

        composeErrorAlert(state = alert, title = alertTitle, message = alertText, onDismiss = {
            alert = false
        })

        val completeWarning: (String) -> Unit = {
            Files.writeString(warningFile, it)
            ratingType = it
            showNsfwWarning = false
            nsfwWarning = false
            selectedTab = 3
        }

        composeNsfwWarning(
            state = showNsfwWarning,
            onDismiss = {
                selectedTab = previousSelectedTab
                showNsfwWarning = false
            },
            onContinue = {
                completeWarning("Explicit")
            },
            onSafe = {
                completeWarning("Safe")
            },
            onQuestionable = {
                completeWarning("Questionable")
            }
        )

        Scaffold(topBar = {
            TabRow(
                selectedTabIndex = selectedTab,
                backgroundColor = pink,
                indicator = { tabPositions: List<TabPosition> ->
                    Box(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]).height(5.dp)
                            .padding(horizontal = 28.dp, vertical = 1.dp).offset(y = (-1).dp).clip(CircleShape)
                            .background(color = whitePink)
                    )
                }) {
                tabs.forEachIndexed { index, text ->
                    val isSelected = selectedTab == index
                    Tab(modifier = Modifier.background(pink),
                        selected = isSelected,
                        onClick = {
                            previousSelectedTab = selectedTab
                            if (nsfwWarning && index == 3) {
                                showNsfwWarning = true
                            } else {
                                selectedTab = index
                            }
                        },
                        text = {
                            Text(
                                text = text,
                                color = whitePink,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        })
                }
            }
        }, content = {
            Column {
                Box(modifier = Modifier.background(Color.DarkGray).fillMaxWidth().fillMaxHeight()) {
                    when (selectedTab) {
                        0 -> {
                            Column {
                                Row(
                                    modifier = Modifier.padding(
                                        start = 30.dp, end = 30.dp, top = 20.dp, bottom = 5.dp
                                    )
                                ) {
                                    Text(
                                        text = "Configuration:",
                                        fontWeight = FontWeight.Bold,
                                        color = white,
                                        modifier = Modifier.padding(end = 5.dp)
                                    )
                                    Text(
                                        text = "Path to the config file. (Required)",
                                        color = grey,
                                    )
                                }

                                Row(
                                    modifier = Modifier.padding(
                                        start = 30.dp, end = 30.dp, top = 5.dp, bottom = 5.dp
                                    )
                                ) {
                                    Text(
                                        text = "Input:",
                                        fontWeight = FontWeight.Bold,
                                        color = white,
                                        modifier = Modifier.padding(end = 5.dp)
                                    )
                                    Text(
                                        text = "The file which will be obfuscated. (Required)",
                                        color = grey,
                                    )
                                }

                                Row(
                                    modifier = Modifier.padding(
                                        start = 30.dp, end = 30.dp, top = 5.dp, bottom = 5.dp
                                    )
                                ) {
                                    Text(
                                        text = "Output:",
                                        fontWeight = FontWeight.Bold,
                                        color = white,
                                        modifier = Modifier.padding(end = 5.dp)
                                    )
                                    Text(
                                        text = "Path to the output jar location",
                                        color = grey,
                                    )
                                }

                                Row(
                                    modifier = Modifier.padding(
                                        start = 30.dp, end = 30.dp, top = 5.dp, bottom = 5.dp
                                    )
                                ) {
                                    Text(
                                        text = "Exempt:",
                                        fontWeight = FontWeight.Bold,
                                        color = white,
                                        modifier = Modifier.padding(end = 5.dp)
                                    )
                                    Text(
                                        text = "Path to the exempt file",
                                        color = grey,
                                    )
                                }

                                Row(
                                    modifier = Modifier.padding(
                                        start = 30.dp, end = 30.dp, top = 5.dp, bottom = 5.dp
                                    )
                                ) {
                                    Text(
                                        text = "Runtime:",
                                        fontWeight = FontWeight.Bold,
                                        color = white,
                                        modifier = Modifier.padding(end = 5.dp)
                                    )
                                    Text(
                                        text = "Path to the runtime jar",
                                        color = grey,
                                    )
                                }

                                Row(
                                    modifier = Modifier.padding(
                                        start = 30.dp, end = 30.dp, top = 5.dp, bottom = 5.dp
                                    )
                                ) {
                                    Text(
                                        text = "Libs:",
                                        fontWeight = FontWeight.Bold,
                                        color = white,
                                        modifier = Modifier.padding(end = 5.dp)
                                    )
                                    Text(
                                        text = "Path to the libs folder. (Might be needed)",
                                        color = grey,
                                    )
                                }


                                Row(
                                    modifier = Modifier.padding(
                                        start = 30.dp, end = 30.dp, top = 5.dp, bottom = 5.dp
                                    )
                                ) {
                                    Text(
                                        text = "Lowcon:",
                                        fontWeight = FontWeight.Bold,
                                        color = white,
                                        modifier = Modifier.padding(end = 5.dp)
                                    )
                                    Text(
                                        text = "Uses sharded packets to upload mappings",
                                        color = grey,
                                    )
                                }

                                Row(
                                    modifier = Modifier.padding(
                                        start = 30.dp, end = 30.dp, top = 5.dp, bottom = 5.dp
                                    )
                                ) {
                                    Text(
                                        text = "Phantom:",
                                        fontWeight = FontWeight.Bold,
                                        color = white,
                                        modifier = Modifier.padding(end = 5.dp)
                                    )
                                    Text(
                                        text = "Declare if phantom computation should be used",
                                        color = grey,
                                    )
                                }

                                Row(
                                    modifier = Modifier.padding(
                                        start = 30.dp, end = 30.dp, top = 5.dp, bottom = 30.dp
                                    )
                                ) {
                                    Text(
                                        text = "Renamer:",
                                        fontWeight = FontWeight.Bold,
                                        color = white,
                                        modifier = Modifier.padding(end = 5.dp)
                                    )
                                    Text(
                                        text = "Enables renamer for the obfuscation",
                                        color = grey,
                                    )
                                }

                                var active by remember { mutableStateOf(false) }
                                val color = if (active) pink else (whitePink)
                                Text(
                                    text = "https://skidfuscator.dev/docs/",
                                    style = TextStyle(textDecoration = TextDecoration.Underline),
                                    fontSize = 18.sp,
                                    color = color,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(5.dp).fillMaxWidth().clickable {
                                        Desktop.getDesktop().browse(URI.create("https://skidfuscator.dev/docs/"))
                                    }.onPointerEvent(PointerEventType.Enter) { active = true }
                                        .onPointerEvent(PointerEventType.Exit) { active = false },
                                )

                                Row(
                                    modifier = Modifier.padding(
                                        start = 20.dp, end = 20.dp, top = 5.dp, bottom = 20.dp
                                    )
                                ) {
                                    Button(
                                        onClick = {
                                            starterForgeRunning = true
                                            skidfuscator?.generateStarter("0") {
                                                starterForgeRunning = false
                                            }
                                        },
                                        enabled = starterForgeRunning.not(),
                                        modifier = Modifier.fillMaxWidth(fraction = .5f).padding(10.dp)
                                            .height(ButtonDefaults.MinHeight),
                                        colors = ButtonDefaults.buttonColors(
                                            backgroundColor = pink,
                                            disabledBackgroundColor = darkPink,
                                            contentColor = Color.White,
                                            disabledContentColor = Color.LightGray
                                        )
                                    ) {
                                        if (starterForgeRunning) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(25.dp).offset(y = -2.dp),
                                                color = whitePink,
                                                strokeWidth = 3.dp
                                            )
                                        } else {
                                            Text("Generate Forge 1.8.8 Starter")
                                        }
                                    }

                                    Button(
                                        onClick = {
                                            starterSpigotRunning = true
                                            skidfuscator?.generateStarter("1") {
                                                starterSpigotRunning = false
                                            }
                                        },
                                        enabled = starterSpigotRunning.not(),
                                        modifier = Modifier.fillMaxWidth().padding(10.dp)
                                            .height(ButtonDefaults.MinHeight),
                                        colors = ButtonDefaults.buttonColors(
                                            backgroundColor = pink,
                                            disabledBackgroundColor = darkPink,
                                            contentColor = Color.White,
                                            disabledContentColor = Color.LightGray
                                        )
                                    ) {
                                        if (starterSpigotRunning) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(25.dp).offset(y = -2.dp),
                                                color = whitePink,
                                                strokeWidth = 3.dp
                                            )
                                        } else {
                                            Text("Generate Spigot 1.8.8 Starter")
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.padding(
                                        start = 20.dp, end = 20.dp, top = 5.dp, bottom = 20.dp
                                    )
                                ) {
                                    Button(
                                        onClick = {
                                            saveConfig = true
                                        },
                                        modifier = Modifier.fillMaxWidth(fraction = .3f).padding(10.dp)
                                            .height(ButtonDefaults.MinHeight),
                                        colors = ButtonDefaults.buttonColors(
                                            backgroundColor = pink,
                                            disabledBackgroundColor = darkPink,
                                            contentColor = Color.White,
                                            disabledContentColor = Color.LightGray
                                        )
                                    ) {
                                        Text("Save configuration")
                                    }

                                    Button(
                                        onClick = {
                                            loadConfig = true
                                        },
                                        modifier = Modifier.fillMaxWidth(fraction = .5f).padding(10.dp)
                                            .height(ButtonDefaults.MinHeight),
                                        colors = ButtonDefaults.buttonColors(
                                            backgroundColor = pink,
                                            disabledBackgroundColor = darkPink,
                                            contentColor = Color.White,
                                            disabledContentColor = Color.LightGray
                                        )
                                    ) {
                                        Text("Load configuration")
                                    }

                                    Button(
                                        onClick = {
                                            loadConfiguration(latestConfigurationFile)
                                        },
                                        modifier = Modifier.fillMaxWidth().padding(10.dp)
                                            .height(ButtonDefaults.MinHeight),
                                        colors = ButtonDefaults.buttonColors(
                                            backgroundColor = pink,
                                            disabledBackgroundColor = darkPink,
                                            contentColor = Color.White,
                                            disabledContentColor = Color.LightGray
                                        )
                                    ) {
                                        Text("Load last configuration")
                                    }
                                }
                            }
                        }

                        1 -> {
                            Column {
                                composePath(title = "Configuration File",
                                    placeholder = "skidfuscator.config",
                                    path = configPath,
                                    paddingValues = PaddingValues(
                                        start = 20.dp, top = 10.dp, end = 20.dp, bottom = 5.dp
                                    ),
                                    onSearchClick = {
                                        fileSelectionType = 0
                                        showFilePicker = true
                                    },
                                    onValueChange = { configPath = it })

                                composePath(title = "Input File",
                                    placeholder = "input.jar",
                                    path = inputFile,
                                    paddingValues = PaddingValues(
                                        start = 20.dp, top = 5.dp, end = 20.dp, bottom = 5.dp
                                    ),
                                    onSearchClick = {
                                        fileSelectionType = 1
                                        showFilePicker = true
                                    },
                                    onValueChange = { inputFile = it })

                                composePath(title = "Output File",
                                    placeholder = "output.jar",
                                    path = outputFile,
                                    paddingValues = PaddingValues(
                                        start = 20.dp, top = 5.dp, end = 20.dp, bottom = 5.dp
                                    ),
                                    onSearchClick = {
                                        fileSelectionType = 2
                                        showFilePicker = true
                                    },
                                    onValueChange = { outputFile = it })

                                composePath(title = "Exempt File",
                                    placeholder = "exclusions.json",
                                    path = exemptFile,
                                    paddingValues = PaddingValues(
                                        start = 20.dp, top = 5.dp, end = 20.dp, bottom = 5.dp
                                    ),
                                    onSearchClick = {
                                        fileSelectionType = 3
                                        showFilePicker = true
                                    },
                                    onValueChange = { exemptFile = it })

                                composePath(title = "Runtime File",
                                    placeholder = "rt.jar",
                                    path = rtFile,
                                    paddingValues = PaddingValues(
                                        start = 20.dp, top = 5.dp, end = 20.dp, bottom = 5.dp
                                    ),
                                    onSearchClick = {
                                        fileSelectionType = 4
                                        showFilePicker = true
                                    },
                                    onValueChange = { rtFile = it })

                                composePath(title = "Libs Directory",
                                    placeholder = "libs/",
                                    path = libsDir,
                                    paddingValues = PaddingValues(
                                        start = 20.dp, top = 5.dp, end = 20.dp, bottom = 5.dp
                                    ),
                                    onSearchClick = {
                                        fileSelectionType = 5
                                        showDirectoryPicker = true
                                    },
                                    onValueChange = { libsDir = it })

                                composeDropDown(value = if (parameters.isEmpty()) "None" else (parameters.joinToString(
                                    separator = ", "
                                )),
                                    label = "Skidfuscator Options",
                                    expanded = parametersExpanded,
                                    paddingValues = PaddingValues(
                                        start = 20.dp, top = 10.dp, end = 20.dp, bottom = 20.dp
                                    ),
                                    onFocus = {
                                        parametersExpanded = true
                                    },
                                    onDismissRequest = {
                                        parametersExpanded = false
                                    },
                                    onIconClick = {
                                        parametersExpanded = parametersExpanded.not()
                                    },
                                    content = {
                                        DropdownMenuItem(onClick = {
                                            fuckItParameter = fuckItParameter.not()
                                            if (parameters.contains("FuckIt")) {
                                                parameters.remove("FuckIt")
                                            } else {
                                                parameters.add("FuckIt")
                                            }
                                        }) {
                                            Text(
                                                text = (if (fuckItParameter) "✔ " else "") + "FuckIt",
                                                color = if (fuckItParameter) pink else Color.LightGray
                                            )
                                        }

                                        DropdownMenuItem(onClick = {
                                            lcParameter = lcParameter.not()
                                            if (parameters.contains("Lowcon")) {
                                                parameters.remove("Lowcon")
                                            } else {
                                                parameters.add("Lowcon")
                                            }
                                        }) {
                                            Text(
                                                text = (if (lcParameter) "✔ " else "") + "Lowcon",
                                                color = if (lcParameter) pink else Color.LightGray
                                            )
                                        }

                                        DropdownMenuItem(onClick = {
                                            phParameter = phParameter.not()
                                            if (parameters.contains("Phantom")) {
                                                parameters.remove("Phantom")
                                            } else {
                                                parameters.add("Phantom")
                                            }
                                        }) {
                                            Text(
                                                text = (if (phParameter) "✔ " else "") + "Phantom",
                                                color = if (phParameter) pink else Color.LightGray
                                            )
                                        }

                                        DropdownMenuItem(onClick = {
                                            reParameter = reParameter.not()
                                            if (parameters.contains("Renamer")) {
                                                parameters.remove("Renamer")
                                            } else {
                                                parameters.add("Renamer")
                                            }
                                        }) {
                                            Text(
                                                text = (if (reParameter) "✔ " else "") + "Renamer",
                                                color = if (reParameter) pink else Color.LightGray
                                            )
                                        }
                                    })

                            }
                        }

                        2 -> {
                            Column {
                                val scrollState = rememberScrollState(0)
                                OutlinedTextField(
                                    value = logs.joinToString(separator = "\n"),
                                    onValueChange = { },
                                    label = { Text("Logs") },
                                    modifier = Modifier.fillMaxWidth().fillMaxHeight(0.8f)
                                        .padding(horizontal = 20.dp, vertical = 20.dp).verticalScroll(scrollState),
                                    readOnly = true,
                                    colors = TextFieldDefaults.outlinedTextFieldColors(
                                        textColor = Color.LightGray,
                                        unfocusedBorderColor = pink,
                                        focusedBorderColor = lightPink,
                                        focusedLabelColor = Color.LightGray,
                                        unfocusedLabelColor = Color.LightGray,
                                        cursorColor = Color.White
                                    )
                                )

                                LaunchedEffect(scrollState.maxValue) {
                                    scrollState.scrollTo(scrollState.maxValue)
                                }

                                Row(
                                    modifier = Modifier.padding(
                                        start = 20.dp, end = 20.dp, top = 5.dp, bottom = 20.dp
                                    )
                                ) {
                                    Button(
                                        onClick = {
                                            val errorMessage: () -> Unit = {
                                                alertTitle = "Configuration Error"
                                                alertText =
                                                    "Either the configuration path or input file is invalid. Please fix the issue and try again."
                                                alert = true
                                            }

                                            if (configPath.isNotBlank() && configPath.isNotEmpty() && inputFile.isNotBlank() && inputFile.isNotEmpty()) {
                                                val tempConfig = Path.of(configPath)
                                                val tempInput = Path.of(inputFile)

                                                if (Files.exists(tempConfig) && Files.exists(tempInput) && Files.isDirectory(
                                                        tempConfig
                                                    ).not() && Files.isDirectory(tempInput).not()
                                                ) {
                                                    saveConfiguration(latestConfigurationFile)

                                                    logs.clear()
                                                    running = true
                                                    executor = skidfuscator?.obfuscate(configPath = configPath,
                                                        inputFile = inputFile,
                                                        outputFile = outputFile,
                                                        libsDir = libsDir,
                                                        exemptFile = exemptFile,
                                                        rtFile = rtFile,
                                                        fuckIt = fuckItParameter,
                                                        lc = lcParameter,
                                                        ph = phParameter,
                                                        re = reParameter,
                                                        networkError = {

                                                        },
                                                        logAppender = {
                                                            KurwaLoggerAppender.LOG_CONSUMER = Consumer {
                                                                logs.add(it)
                                                            }
                                                        }) {
                                                        running = false
                                                        executor?.shutdown()
                                                        executor = null
                                                    }
                                                } else errorMessage()
                                            } else errorMessage()
                                        },
                                        enabled = running.not().and(stopping.not()),
                                        modifier = Modifier.fillMaxWidth(fraction = .5f).padding(10.dp)
                                            .height(ButtonDefaults.MinHeight),
                                        colors = ButtonDefaults.buttonColors(
                                            backgroundColor = pink,
                                            disabledBackgroundColor = darkPink,
                                            contentColor = Color.White,
                                            disabledContentColor = Color.LightGray
                                        )
                                    ) {
                                        Text("Obfuscate")
                                    }

                                    Button(
                                        onClick = {
                                            stopping = true
                                            executor?.let {
                                                Sanity.close()
                                                it.shutdown()
                                                running = false
                                                stopping = false
                                            }
                                        },
                                        enabled = running.and(stopping.not()),
                                        modifier = Modifier.fillMaxWidth().padding(10.dp)
                                            .height(ButtonDefaults.MinHeight),
                                        colors = ButtonDefaults.buttonColors(
                                            backgroundColor = pink,
                                            disabledBackgroundColor = darkPink,
                                            contentColor = Color.White,
                                            disabledContentColor = Color.LightGray
                                        )
                                    ) {
                                        Text("Cancel")
                                    }
                                }

                                if (running) {
                                    LinearProgressIndicator(
                                        modifier = Modifier.padding(
                                            top = 10.dp, bottom = 30.dp, start = 30.dp, end = 30.dp
                                        ).fillMaxWidth().height(5.dp), backgroundColor = darkPink, color = pink
                                    )
                                } else if (stopping) {
                                    LinearProgressIndicator(
                                        progress = 1f, modifier = Modifier.padding(
                                            top = 10.dp, bottom = 30.dp, start = 30.dp, end = 30.dp
                                        ).fillMaxWidth().height(5.dp), backgroundColor = darkPink, color = pink
                                    )
                                }
                            }
                        }

                        3 -> {
                            val reset: () -> Unit = {
                                alertTitle = "Connection Error"
                                alertText =
                                    "Could not connect to the Gelbooru.com. Please check your internet connection."
                                alert = true

                                selectedTab = previousSelectedTab
                                imageProvider.clear()
                                reloadImage = reloadImage.not()
                                loadInstance = loadInstance.not()
                                disableButtons = true
                            }

                            Column(
                                modifier = Modifier.align(Alignment.TopCenter)
                            ) {
                                Row(
                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                ) {
                                    Button(
                                        onClick = {
                                            imageProvider.previousImage()
                                            reloadImage = reloadImage.not()
                                            disableButtons = true
                                        },
                                        enabled = disableButtons.not().and(imageProvider.isLoaded()),
                                        modifier = Modifier.padding(
                                            top = 25.dp,
                                            bottom = 10.dp,
                                            end = 10.dp,
                                            start = 10.dp
                                        ).height(ButtonDefaults.MinHeight)
                                            .width(100.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            backgroundColor = pink,
                                            disabledBackgroundColor = darkPink,
                                            contentColor = Color.White,
                                            disabledContentColor = Color.LightGray
                                        )
                                    ) {
                                        Text("Previous")
                                    }

                                    Button(
                                        onClick = {
                                            imageProvider.nextImage()
                                            reloadImage = reloadImage.not()
                                            disableButtons = true
                                        },
                                        enabled = disableButtons.not().and(imageProvider.isLoaded()),
                                        modifier = Modifier.padding(
                                            top = 25.dp,
                                            bottom = 10.dp,
                                            end = 10.dp,
                                            start = 10.dp
                                        ).height(ButtonDefaults.MinHeight)
                                            .width(100.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            backgroundColor = pink,
                                            disabledBackgroundColor = darkPink,
                                            contentColor = Color.White,
                                            disabledContentColor = Color.LightGray
                                        )
                                    ) {
                                        Text("Next")
                                    }

                                    Button(
                                        onClick = {
                                            reloadImage = reloadImage.not()
                                            loadInstance = loadInstance.not()
                                            disableButtons = true
                                        },
                                        enabled = disableButtons.not().and(imageProvider.isLoaded()),
                                        modifier = Modifier.padding(
                                            top = 25.dp,
                                            bottom = 10.dp,
                                            end = 10.dp,
                                            start = 10.dp
                                        ).height(ButtonDefaults.MinHeight)
                                            .width(100.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            backgroundColor = pink,
                                            disabledBackgroundColor = darkPink,
                                            contentColor = Color.White,
                                            disabledContentColor = Color.LightGray
                                        )
                                    ) {
                                        Text("Reload")
                                    }

                                    composeSizedDropDown(
                                        width = 150.dp,
                                        height = 60.dp,
                                        enabled = disableButtons.not().and(imageProvider.isLoaded()),
                                        value = sortingType,
                                        label = "Sorting",
                                        expanded = sorting.and(disableButtons.not().and(imageProvider.isLoaded())),
                                        paddingValues = PaddingValues(
                                            start = 10.dp, top = 10.dp, end = 10.dp, bottom = 10.dp
                                        ),
                                        onFocus = {
                                            sorting = true
                                        },
                                        onDismissRequest = {
                                            sorting = false
                                        },
                                        onIconClick = {
                                            sorting = sorting.not()
                                        },
                                        content = {
                                            DropdownMenuItem(onClick = {
                                                sortingType = "New"
                                                sorting = false
                                            }) {
                                                Text(
                                                    text = "New",
                                                    color = Color.LightGray
                                                )
                                            }

                                            DropdownMenuItem(onClick = {
                                                sortingType = "Score"
                                                sorting = false
                                            }) {
                                                Text(
                                                    text = "Score",
                                                    color = Color.LightGray
                                                )
                                            }

                                            DropdownMenuItem(onClick = {
                                                sortingType = "Random"
                                                sorting = false
                                            }) {
                                                Text(
                                                    text = "Random",
                                                    color = Color.LightGray
                                                )
                                            }
                                        })

                                    composeSizedDropDown(
                                        width = 150.dp,
                                        height = 60.dp,
                                        enabled = disableButtons.not().and(imageProvider.isLoaded()),
                                        value = ratingType,
                                        label = "Rating",
                                        expanded = rating.and(disableButtons.not().and(imageProvider.isLoaded())),
                                        paddingValues = PaddingValues(
                                            start = 10.dp, top = 10.dp, end = 10.dp, bottom = 10.dp
                                        ),
                                        onFocus = {
                                            rating = true
                                        },
                                        onDismissRequest = {
                                            rating = false
                                        },
                                        onIconClick = {
                                            rating = rating.not()
                                        },
                                        content = {
                                            DropdownMenuItem(onClick = {
                                                ratingType = "Explicit"
                                                rating = false
                                            }) {
                                                Text(
                                                    text = "Explicit",
                                                    color = Color.LightGray
                                                )
                                            }

                                            DropdownMenuItem(onClick = {
                                                ratingType = "Safe"
                                                rating = false
                                            }) {
                                                Text(
                                                    text = "Safe",
                                                    color = Color.LightGray
                                                )
                                            }

                                            DropdownMenuItem(onClick = {
                                                ratingType = "Questionable"
                                                rating = false
                                            }) {
                                                Text(
                                                    text = "Questionable",
                                                    color = Color.LightGray
                                                )
                                            }
                                        })


                                    LaunchedEffect(key1 = ratingType) {
                                        imageProvider.rating = ratingType
                                        reloadImage = reloadImage.not()
                                        loadInstance = loadInstance.not()
                                        disableButtons = true
                                    }

                                    LaunchedEffect(key1 = sortingType) {
                                        imageProvider.sorting = sortingType
                                        reloadImage = reloadImage.not()
                                        loadInstance = loadInstance.not()
                                        disableButtons = true
                                    }
                                }

                                LaunchedEffect(key1 = loadInstance) {
                                    withContext(Dispatchers.IO) {
                                        try {
                                            imageProvider.load()
                                        } catch (ignored: Exception) {
                                            reset()
                                        }
                                    }

                                    image = withContext(Dispatchers.IO) {
                                        disableButtons = false

                                        try {
                                            imageProvider.image()
                                        } catch (ignored: Exception) {
                                            reset()
                                            null
                                        }
                                    }
                                }

                                LaunchedEffect(key1 = reloadImage) {
                                    image = withContext(Dispatchers.IO) {
                                        if (imageProvider.isLoaded()) {
                                            try {
                                                imageProvider.image()
                                            } catch (ignored: Exception) {
                                                reset()
                                                null
                                            }
                                        } else null
                                    }
                                }

                                LaunchedEffect(key1 = image) {
                                    disableButtons = false
                                }

                                image?.let {
                                    Image(
                                        painter = BitmapPainter(it),
                                        contentDescription = "Uohhhhhhhhh",
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier.fillMaxHeight().fillMaxWidth()
                                            .padding(bottom = 20.dp, start = 20.dp, end = 20.dp)
                                            .align(Alignment.CenterHorizontally)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        })
    }
}