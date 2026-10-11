package ir.farimod.app.ui

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/**
 * Port of `app/components/ImageInputList.tsx`.
 *
 * RN renders a `ScrollView` (contentContainerStyle: row-reverse, wrap, padding 10) inside a plain `View`.
 * In RN the inner ScrollView takes its content height when nested in another vertical ScrollView (the only usage,
 * GalleryItemScreen), so here the grid is a plain wrapping flow (no nested scroll — a nested vertical scroll
 * with unbounded height would crash Compose). Put the call inside the parent's `verticalScroll`.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ImageInputList(
    imageUris: List<String> = emptyList(),
    onRemoveImage: (String?) -> Unit,
    onAddImage: (String?) -> Unit,
    onImagePress: ((String) -> Unit)? = null,
    isReadOnly: Boolean = false
) {
    // Dimensions.get("window").width / 3 - 13
    val itemSize: Dp = windowSize().first / 3 - 13.dp

    Column {
        // flexDirection: "row-reverse", flexWrap: "wrap", padding: 10
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            FlowRow(Modifier.fillMaxWidth().padding(10.dp)) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    // key={uri}
                    imageUris.forEach { uri ->
                        androidx.compose.runtime.key(uri) {
                            // itemContainer: margin 3, size = width/3 - 13
                            Box(Modifier.padding(3.dp).size(itemSize)) {
                                if (isReadOnly) {
                                    // readOnlyImageWrapper
                                    Box(
                                        Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(15.dp))
                                            .background(Color(0xFFF0F0F0))
                                            .touchableOpacity(activeOpacity = 0.8f) { onImagePress?.invoke(uri) }
                                    ) {
                                        NetworkImage(uri, Modifier.fillMaxSize(), resizeMode = ResizeMode.cover)
                                    }
                                } else {
                                    ImageInput(
                                        imageUri = uri,
                                        onChangeImage = { onRemoveImage(uri) },
                                        onImagePress = onImagePress
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
