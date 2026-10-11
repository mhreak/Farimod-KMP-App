package ir.farimod.app.screens.magdetailes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.LinkInteractionListener
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import ir.farimod.app.platform.openUrl
import ir.farimod.app.ui.AppText
import ir.farimod.app.ui.NetworkImage
import ir.farimod.app.ui.RNFonts
import ir.farimod.app.ui.ResizeMode

/**
 * Port of `react-native-render-html` usage in MagDetailesScreen (baseStyle + tagsStyles for
 * p, div, ul, ol, li, strong, em, a, img).
 */
@Composable
internal fun MagHtmlContent(html: String, modifier: Modifier = Modifier) {
    val nodes = remember(html) { parseMagHtml(html) }
    SelectionContainer {
        Column(modifier.fillMaxWidth()) {
            MagHtmlNodes(nodes)
        }
    }
}

private class MagHNode(
    val tag: String,
    val text: String = "",
    val attrs: Map<String, String> = emptyMap(),
    val children: MutableList<MagHNode> = mutableListOf()
)

private val magVoidTags = setOf("br", "img", "hr", "meta", "link", "input", "source", "wbr")
private val magInlineTags = setOf(
    "#text", "br", "strong", "b", "em", "i", "u", "a", "span", "small", "sub", "sup", "font", "label"
)
private val magTagRe = Regex("<(/?)([a-zA-Z][a-zA-Z0-9]*)\\b([^>]*)>", RegexOption.IGNORE_CASE)
private val magAttrRe = Regex("([a-zA-Z_:][-a-zA-Z0-9_:.]*)\\s*=\\s*(\"[^\"]*\"|'[^']*'|[^\\s\"'=<>`]+)")
private val magEntityRe = Regex("&(#x[0-9a-fA-F]+|#\\d+|[a-zA-Z]+);")

private fun MagHNode.isInline(): Boolean = tag in magInlineTags

private fun parseMagHtml(src: String): List<MagHNode> {
    val cleaned = src.replace(Regex("<!--.*?-->", setOf(RegexOption.DOT_MATCHES_ALL)), "")
    val root = MagHNode("root")
    val stack = ArrayDeque<MagHNode>()
    stack.addLast(root)
    var i = 0
    while (i < cleaned.length) {
        val lt = cleaned.indexOf('<', i)
        if (lt < 0) {
            appendMagText(stack.last(), cleaned.substring(i))
            break
        }
        if (lt > i) appendMagText(stack.last(), cleaned.substring(i, lt))
        if (cleaned.startsWith("<!", lt) || cleaned.startsWith("<?", lt)) {
            val end = cleaned.indexOf('>', lt)
            if (end < 0) break
            i = end + 1
            continue
        }
        val match = magTagRe.find(cleaned, lt)
        if (match == null || match.range.first != lt) {
            appendMagText(stack.last(), "<")
            i = lt + 1
            continue
        }
        val closing = match.groupValues[1] == "/"
        val name = match.groupValues[2].lowercase()
        val attrSrc = match.groupValues[3]
        val selfClose = name in magVoidTags || attrSrc.trimEnd().endsWith("/")
        if (closing) {
            while (stack.size > 1 && stack.last().tag != name) stack.removeLast()
            if (stack.size > 1 && stack.last().tag == name) stack.removeLast()
        } else {
            val node = MagHNode(name, attrs = parseMagAttrs(attrSrc))
            stack.last().children.add(node)
            if (!selfClose) stack.addLast(node)
        }
        i = match.range.last + 1
    }
    return unwrapMagDocument(root.children)
}

private fun unwrapMagDocument(nodes: List<MagHNode>): List<MagHNode> {
    if (nodes.size == 1 && nodes[0].tag == "html") {
        val body = nodes[0].children.firstOrNull { it.tag == "body" }
        if (body != null) return body.children
        return nodes[0].children.filter { it.tag != "head" }
    }
    return nodes
}

private fun appendMagText(parent: MagHNode, raw: String) {
    val decoded = decodeMagEntities(raw).replace('\u00A0', ' ')
    if (decoded.isBlank()) return
    parent.children.add(MagHNode("#text", text = decoded))
}

private fun decodeMagEntities(s: String): String {
    if (!s.contains('&')) return s
    return magEntityRe.replace(s) { m ->
        val body = m.groupValues[1]
        when (body) {
            "amp" -> "&"
            "lt" -> "<"
            "gt" -> ">"
            "quot" -> "\""
            "apos" -> "'"
            "nbsp" -> " "
            "zwnj" -> "\u200C"
            else -> when {
                body.startsWith("#x", ignoreCase = true) ->
                    body.drop(2).toIntOrNull(16)?.takeIf { it in 0..0xFFFF }?.let { it.toChar().toString() } ?: m.value
                body.startsWith("#") ->
                    body.drop(1).toIntOrNull()?.takeIf { it in 0..0xFFFF }?.let { it.toChar().toString() } ?: m.value
                else -> m.value
            }
        }
    }
}

private fun parseMagAttrs(src: String): Map<String, String> {
    val out = LinkedHashMap<String, String>()
    for (m in magAttrRe.findAll(src)) {
        val key = m.groupValues[1].lowercase()
        var value = m.groupValues[2]
        if (value.length >= 2 && (value.first() == '"' || value.first() == '\'')) {
            value = value.substring(1, value.length - 1)
        }
        out[key] = decodeMagEntities(value)
    }
    return out
}

@Composable
private fun MagHtmlNodes(nodes: List<MagHNode>) {
    var index = 0
    while (index < nodes.size) {
        val node = nodes[index]
        if (node.isInline()) {
            val run = ArrayList<MagHNode>()
            while (index < nodes.size && nodes[index].isInline()) {
                run.add(nodes[index])
                index++
            }
            MagInlineText(run, bottom = 10.dp)
        } else {
            MagHtmlBlock(node)
            index++
        }
    }
}

@Composable
private fun MagHtmlBlock(node: MagHNode) {
    when (node.tag) {
        "head", "script", "style" -> Unit
        "html", "body" -> MagHtmlNodes(node.children)
        "p", "div", "blockquote", "figcaption", "section", "article" -> {
            Column(Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
                if (node.children.isEmpty() || node.children.all { it.isInline() }) {
                    MagInlineText(node.children, bottom = 0.dp)
                } else {
                    MagHtmlNodes(node.children)
                }
            }
        }
        "h1", "h2", "h3", "h4", "h5", "h6" -> {
            val size = when (node.tag) {
                "h1" -> 28
                "h2" -> 24
                "h3" -> 22
                "h4" -> 20
                "h5" -> 18
                else -> 16
            }
            MagInlineText(node.children, bottom = 10.dp, fontSize = size, forceBold = true)
        }
        "ul" -> MagListBlock(node, ordered = false)
        "ol" -> MagListBlock(node, ordered = true)
        "li" -> {
            Column(Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
                if (node.children.all { it.isInline() }) MagInlineText(node.children, bottom = 0.dp)
                else MagHtmlNodes(node.children)
            }
        }
        "img" -> MagHtmlImage(node.attrs)
        "br" -> Spacer(Modifier.height(8.dp))
        "hr" -> Box(
            Modifier
                .fillMaxWidth()
                .absolutePadding(top = 8.dp, bottom = 8.dp)
                .height(1.dp)
                .background(Color(0xFFE0E0E0))
        )
        else -> {
            if (node.children.all { it.isInline() }) MagInlineText(node.children, bottom = 10.dp)
            else MagHtmlNodes(node.children)
        }
    }
}

@Composable
private fun MagListBlock(node: MagHNode, ordered: Boolean) {
    val items = node.children.filter { it.tag == "li" }
    Column(
        Modifier
            .fillMaxWidth()
            .absolutePadding(right = 20.dp)
            .padding(bottom = 10.dp)
    ) {
        items.forEachIndexed { index, li ->
            val prefix = if (ordered) "${index + 1}. " else "• "
            val prefixed = listOf(MagHNode("#text", text = prefix)) + li.children
            Column(Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
                if (li.children.all { it.isInline() }) {
                    MagInlineText(prefixed, bottom = 0.dp)
                } else {
                    MagInlineText(listOf(MagHNode("#text", text = prefix)), bottom = 0.dp)
                    MagHtmlNodes(li.children)
                }
            }
        }
    }
}

@Composable
private fun MagHtmlImage(attrs: Map<String, String>) {
    val src = attrs["src"] ?: return
    val w = attrs["width"]?.toFloatOrNull()
    val h = attrs["height"]?.toFloatOrNull()
    val sizeMod = if (w != null && h != null && w > 0f && h > 0f) {
        Modifier.fillMaxWidth().aspectRatio(w / h)
    } else {
        Modifier.fillMaxWidth().height(200.dp)
    }
    NetworkImage(
        uri = src,
        modifier = Modifier
            .absolutePadding(top = 12.dp, bottom = 12.dp)
            .then(sizeMod)
            .clip(RoundedCornerShape(12.dp)),
        resizeMode = ResizeMode.contain
    )
}

private data class MagInl(val bold: Boolean, val italic: Boolean, val underline: Boolean, val link: String?)

@Composable
private fun MagInlineText(
    nodes: List<MagHNode>,
    bottom: Dp,
    fontSize: Int = 16,
    forceBold: Boolean = false
) {
    val regular = RNFonts.Yekan_Bakh_Regular
    val bold = RNFonts.Yekan_Bakh_Bold
    val annotated = buildAnnotatedString {
        fun appendNodes(list: List<MagHNode>, style: MagInl) {
            for (n in list) {
                when (n.tag) {
                    "#text" -> {
                        val span = SpanStyle(
                            color = if (style.link != null) Color(0xFF1976D2) else Color(0xFF34495E),
                            fontFamily = if (style.bold || forceBold) bold else regular,
                            fontStyle = if (style.italic) FontStyle.Italic else FontStyle.Normal,
                            textDecoration = when {
                                style.link != null || style.underline -> TextDecoration.Underline
                                else -> null
                            }
                        )
                        val link = style.link
                        if (link != null) {
                            pushLink(
                                LinkAnnotation.Clickable(
                                    tag = link,
                                    styles = TextLinkStyles(span),
                                    linkInteractionListener = LinkInteractionListener {
                                    openUrl(link)
                                    Unit
                                }
                                )
                            )
                            withStyle(span) { append(n.text) }
                            pop()
                        } else {
                            withStyle(span) { append(n.text) }
                        }
                    }
                    "br" -> append("\n")
                    "strong", "b" -> appendNodes(n.children, style.copy(bold = true))
                    "em", "i" -> appendNodes(n.children, style.copy(italic = true))
                    "u" -> appendNodes(n.children, style.copy(underline = true))
                    "a" -> appendNodes(n.children, style.copy(link = n.attrs["href"] ?: style.link))
                    else -> appendNodes(n.children, style)
                }
            }
        }
        appendNodes(nodes, MagInl(bold = forceBold, italic = false, underline = false, link = null))
    }
    if (annotated.text.isEmpty() && bottom == 0.dp) return
    AppText(
        text = annotated,
        modifier = Modifier.fillMaxWidth().padding(bottom = bottom),
        fontSize = fontSize.sp,
        color = Color(0xFF34495E),
        fontFamily = if (forceBold) bold else regular,
        textAlign = TextAlign.Right,
        lineHeight = if (fontSize == 16) 28.sp else (fontSize + 10).sp
    )
}
