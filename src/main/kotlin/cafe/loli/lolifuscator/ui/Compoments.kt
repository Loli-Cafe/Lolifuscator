import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize


val darkerGray = Color(0.20f, 0.20f, 0.20f, 1.0f, ColorSpaces.Srgb)

val lightPink = Color(255, 51, 127, 255)
val whitePink = Color(255, 172, 200, 255)
val pink = Color(255, 51, 153, 255)
val darkPink = Color(165, 36, 102, 255)
val white = Color(255, 255, 255, 255)
val grey = Color(200, 200, 200, 255)

/*
  Boilerplate
 */

@Composable
fun composePath(
    title: String,
    placeholder: String,
    path: String,
    paddingValues: PaddingValues,
    onSearchClick: () -> Unit,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = path,
        placeholder = { Text(placeholder) },
        onValueChange = onValueChange,
        label = { Text(title) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().padding(paddingValues),
        trailingIcon = {
            Row {
                IconButton(
                    onClick = onSearchClick
                ) {
                    Icon(
                        Icons.Filled.Search, contentDescription = "File Chooser", tint = Color.White
                    )
                }
            }
        },
        colors = TextFieldDefaults.outlinedTextFieldColors(
            textColor = Color.LightGray,
            unfocusedBorderColor = pink,
            focusedBorderColor = lightPink,
            focusedLabelColor = Color.LightGray,
            unfocusedLabelColor = Color.LightGray,
            cursorColor = Color.White
        )
    )
}

@Composable
fun composeDropDown(
    fraction: Float = 1f,
    value: String,
    label: String,
    expanded: Boolean,
    paddingValues: PaddingValues,
    onFocus: () -> Unit,
    onDismissRequest: () -> Unit,
    onIconClick: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    var fieldSize by remember { mutableStateOf(Size.Zero) }
    var clearFocus by remember { mutableStateOf(false) }
    if (clearFocus) LocalFocusManager.current.clearFocus(true)

    Column(Modifier.padding(paddingValues)) {
        OutlinedTextField(value = value,
            onValueChange = { },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(fraction = fraction)
                .onGloballyPositioned { coordinates -> fieldSize = coordinates.size.toSize() }.onFocusChanged {
                    if (expanded.not() && it.isFocused) {
                        onFocus.invoke()
                        clearFocus = true
                    }
                },
            label = { Text(label) },
            trailingIcon = {
                Icon(
                    if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = "Options",
                    modifier = Modifier.clickable(onClick = onIconClick),
                    tint = Color.White
                )
            },
            colors = TextFieldDefaults.outlinedTextFieldColors(
                textColor = Color.LightGray,
                unfocusedBorderColor = pink,
                focusedBorderColor = lightPink,
                focusedLabelColor = Color.LightGray,
                unfocusedLabelColor = Color.LightGray,
                cursorColor = Color.White
            )
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = onDismissRequest,
            modifier = Modifier.width(with(LocalDensity.current) { fieldSize.width.toDp() })
                .background(color = darkerGray, shape = RoundedCornerShape(0.dp)),
        ) {
            content.invoke(this)
        }
    }
}

@Composable
fun composeSizedDropDown(
    width: Dp,
    height: Dp,
    enabled: Boolean,
    value: String,
    label: String,
    expanded: Boolean,
    paddingValues: PaddingValues,
    onFocus: () -> Unit,
    onDismissRequest: () -> Unit,
    onIconClick: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    var fieldSize by remember { mutableStateOf(Size.Zero) }
    var clearFocus by remember { mutableStateOf(false) }
    if (clearFocus) LocalFocusManager.current.clearFocus(true)

    Column(Modifier.padding(paddingValues)) {
        OutlinedTextField(value = value,
            onValueChange = { },
            singleLine = true,
            enabled = enabled,
            modifier = Modifier.width(width).height(height)
                .onGloballyPositioned { coordinates -> fieldSize = coordinates.size.toSize() }.onFocusChanged {
                    if (expanded.not() && it.isFocused) {
                        onFocus.invoke()
                        clearFocus = true
                    }
                },
            label = { Text(label) },
            trailingIcon = {
                Icon(
                    if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = "Options",
                    modifier = Modifier.clickable(onClick = onIconClick),
                    tint = Color.White
                )
            },
            colors = TextFieldDefaults.outlinedTextFieldColors(
                textColor = Color.LightGray,
                unfocusedBorderColor = pink,
                focusedBorderColor = lightPink,
                focusedLabelColor = Color.LightGray,
                unfocusedLabelColor = Color.LightGray,
                cursorColor = Color.White
            )
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = onDismissRequest,
            modifier = Modifier.width(with(LocalDensity.current) { fieldSize.width.toDp() })
                .background(color = darkerGray, shape = RoundedCornerShape(0.dp)),
        ) {
            content.invoke(this)
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun composeSkidfuscatorErrorAlert(
    state: Boolean, onSelect: () -> Unit
) {
    if (state.not()) return

    AlertDialog(title = { Text("Skidfuscator not found") }, text = {
        Text(
            "Lolifuscator could not find Skifuscator Enterprise in current directory.\n" + "Move Skidfuscator jar to current directory or choose path manually."
        )
    }, modifier = Modifier.width(600.dp), shape = RoundedCornerShape(0.dp), onDismissRequest = { }, buttons = {
        Button(
            modifier = Modifier.width(100.dp).height(50.dp).padding(5.dp).offset(x = 250.dp),
            onClick = onSelect,
            colors = ButtonDefaults.buttonColors(
                backgroundColor = pink,
                disabledBackgroundColor = darkPink,
                contentColor = Color.White,
                disabledContentColor = Color.LightGray
            )
        ) {
            Text("Select")
        }
    }, backgroundColor = darkerGray, contentColor = Color.LightGray
    )
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun composeErrorAlert(
    state: Boolean, title: String, message: String, onDismiss: () -> Unit, onClick: (() -> Unit)? = null
) {
    if (state.not())
        return

    val size = message.lines().stream().mapToInt(String::length).max().orElse(0)
    if (size > 0) {
        AlertDialog(
            title = { Text(title) },
            text = { Text(message) },
            modifier = Modifier.width((size * 10).dp),
            shape = RoundedCornerShape(0.dp),
            onDismissRequest = onDismiss,
            buttons = {
                Button(
                    modifier = Modifier.offset(y = (-10).dp, x = ((size * 5) - 32).dp).width(64.dp),
                    onClick = onClick ?: onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = pink,
                        disabledBackgroundColor = darkPink,
                        contentColor = Color.White,
                        disabledContentColor = Color.LightGray
                    )
                ) {
                    Text("Ok")
                }
            },
            backgroundColor = darkerGray,
            contentColor = Color.LightGray
        )
    }
}

private const val warningText =
    "This tab will show you loli hentai(nsfw) images from gelbooru.com. You can continue, get back or switch rating of images:" +
            "\n  - Safe: Safe posts are images \"suitable\" for public viewing. Nudes, exposed nipples, pubic hair, cameltoe. Swimsuits and lingerie can be borderline cases." +
            "\n  - Questionable: Anything that isn't safe or explicit. This is the middle ground, so don't expect anything specific when browsing questionable posts. (Might be sexually suggestive)" +
            "\n" +
            "\nPlease take note: Occasionally explicit images will be marked safe, and vice versa. (https://gelbooru.com/index.php?page=help&topic=rating)"

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun composeNsfwWarning(
    state: Boolean, onContinue: () -> Unit, onDismiss: () -> Unit,
    onSafe: () -> Unit, onQuestionable: () -> Unit
) {
    if (state.not())
        return

    val size = warningText.lines().stream().mapToInt(String::length).max().orElse(0)
    AlertDialog(
        title = { Text("Loli Hentai(NSFW) ahead!") },
        text = { Text(warningText) },
        modifier = Modifier.width((size * 10).dp),
        shape = RoundedCornerShape(0.dp),
        onDismissRequest = onDismiss,
        buttons = {
            Row(
                modifier = Modifier.offset(y = (-10).dp).padding(start = 20.dp)
            ) {
                Button(
                    modifier = Modifier.width(140.dp).padding(end = 10.dp),
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = pink,
                        disabledBackgroundColor = darkPink,
                        contentColor = Color.White,
                        disabledContentColor = Color.LightGray
                    )
                ) {
                    Text("Get back")
                }

                Button(
                    modifier = Modifier.width(140.dp).padding(start = 10.dp, end = 10.dp),
                    onClick = onContinue,
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = pink,
                        disabledBackgroundColor = darkPink,
                        contentColor = Color.White,
                        disabledContentColor = Color.LightGray
                    )
                ) {
                    Text("Continue")
                }

                Button(
                    modifier = Modifier.width(140.dp).padding(start = 10.dp, end = 10.dp),
                    onClick = onSafe,
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = pink,
                        disabledBackgroundColor = darkPink,
                        contentColor = Color.White,
                        disabledContentColor = Color.LightGray
                    )
                ) {
                    Text("Safe")
                }

                Button(
                    modifier = Modifier.width(140.dp).padding(start = 10.dp),
                    onClick = onQuestionable,
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = pink,
                        disabledBackgroundColor = darkPink,
                        contentColor = Color.White,
                        disabledContentColor = Color.LightGray
                    )
                ) {
                    Text("Questionable")
                }
            }
        },
        backgroundColor = darkerGray,
        contentColor = Color.LightGray
    )
}