package ir.farimod.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import ir.farimod.app.network.get
import ir.farimod.app.network.int
import ir.farimod.app.network.long
import ir.farimod.app.network.str
import kotlinx.serialization.json.JsonElement

/**
 * A screen instance on a stack (React Navigation `route`).
 *
 * `params` mirrors RN `route.params`:
 *  - JS objects are passed as [JsonElement] (same keys as the API JSON / RN code),
 *  - primitives as String / Int / Long / Double / Boolean,
 *  - callbacks as Kotlin lambdas.
 */
class Route(
    val name: String,
    params: Map<String, Any?> = emptyMap(),
    val key: Long = nextKey()
) {
    var params: Map<String, Any?> by mutableStateOf(params)

    fun str(k: String): String? = when (val v = params[k]) {
        null -> null
        is JsonElement -> v.str
        else -> v.toString()
    }

    fun int(k: String): Int? = when (val v = params[k]) {
        null -> null
        is Int -> v
        is Long -> v.toInt()
        is Double -> v.toInt()
        is String -> v.trim().toIntOrNull()
        is JsonElement -> v.int
        else -> null
    }

    fun long(k: String): Long? = when (val v = params[k]) {
        null -> null
        is Int -> v.toLong()
        is Long -> v
        is Double -> v.toLong()
        is String -> v.trim().toLongOrNull()
        is JsonElement -> v.long
        else -> null
    }

    fun bool(k: String): Boolean? = when (val v = params[k]) {
        null -> null
        is Boolean -> v
        is String -> v.equals("true", true)
        else -> null
    }

    /** JSON object/array param (RN: a plain JS object passed through `navigate`). */
    fun json(k: String): JsonElement? = params[k] as? JsonElement

    @Suppress("UNCHECKED_CAST")
    fun <T> callback(k: String): T? = params[k] as? T

    companion object {
        private var counter = 0L
        fun nextKey(): Long = ++counter
    }
}

/** Tab names exactly as in RN `AppNavigator.tsx`. */
object Tabs {
    const val Profile = "پروفایل"
    const val Home = "خانه"
    const val Mag = "وبلاگ"
    val all = listOf(Profile, Home, Mag)
}

/** Route names that live in the HomeStack nested navigator (tab bar stays visible). */
val HomeStackRoutes = setOf(
    "AllMembers", "AllProducts", "AllCourses", "AllMemberGroups", "PortfolioDetail",
    "ProductDetails", "CourseDetails", "UserProfile", "MagDetailes", "GalleryItem"
)

/** Routes with `gestureEnabled: false` in RN AppNavigator (iOS swipe-back disabled). */
val GestureDisabledRoutes = setOf(
    "AddPortfolio", "AddProduct", "CourseStudents", "CourseRegistration", "AddNewPost",
    "PortfolioDetail", "PortfolioList", "AddNewCourse", "CourseDetails", "MyRegistrations",
    "MyTeachingCourses", "ProductDetails", "AllProducts", "AllMembers", "Subscription", "UserProfile"
)

/**
 * React Navigation-like API handed to every screen through [LocalNavigation].
 *
 *   navigation.navigate("CourseDetails", mapOf("courseData" to json))
 *   navigation.goBack()
 */
interface Navigation {
    fun navigate(name: String, params: Map<String, Any?> = emptyMap())
    fun push(name: String, params: Map<String, Any?> = emptyMap()) = navigate(name, params)
    fun goBack()
    fun canGoBack(): Boolean
    fun replace(name: String, params: Map<String, Any?> = emptyMap())
    fun popToTop()
    /** Replace the whole stack (RN `navigation.reset({ index: 0, routes: [{ name }] })`). */
    fun reset(name: String, params: Map<String, Any?> = emptyMap())
    /** Update params of the current route (RN `navigation.setParams`). */
    fun setParams(params: Map<String, Any?>)
}

/** The navigation object of the nearest navigator (home stack or app stack / auth stack). */
val LocalNavigation = compositionLocalOf<Navigation> { error("No Navigation provided") }

/** The route the current screen was created for (RN `useRoute()`). */
val LocalRoute = compositionLocalOf<Route> { error("No Route provided") }

@Composable
fun useNavigation(): Navigation = LocalNavigation.current

@Composable
fun useRoute(): Route = LocalRoute.current

/**
 * Holds every navigator of the app:
 *  - [root]: top level stack (IntroFlow / Auth when logged out; MainTabs + app screens when logged in)
 *  - [homeStack]: nested stack inside the "خانه" tab
 *  - tab state with `backBehavior = "history"`
 */
class NavigationState(initialRoot: List<Route>) {
    val root = mutableStateListOf<Route>().apply { addAll(initialRoot) }
    val homeStack = mutableStateListOf(Route("HomeScreen"))

    var currentTab by mutableStateOf(Tabs.Home)
        private set
    private val tabHistory = mutableListOf(Tabs.Home)

    fun selectTab(tab: String) {
        if (tab == currentTab) return
        tabHistory.remove(tab)
        tabHistory.add(tab)
        currentTab = tab
    }

    /** Called by Android back button / swipe back. Returns true when something was popped. */
    fun handleBack(): Boolean {
        val top = root.lastOrNull()
        if (top != null && top.name == "MainTabs") {
            if (currentTab == Tabs.Home && homeStack.size > 1) {
                homeStack.removeAt(homeStack.lastIndex); return true
            }
            if (tabHistory.size > 1) {
                tabHistory.removeAt(tabHistory.lastIndex)
                currentTab = tabHistory.last()
                return true
            }
        }
        if (root.size > 1) {
            root.removeAt(root.lastIndex); return true
        }
        return false
    }

    fun resetRoot(routes: List<Route>) {
        root.clear(); root.addAll(routes)
        homeStack.clear(); homeStack.add(Route("HomeScreen"))
        tabHistory.clear(); tabHistory.add(Tabs.Home); currentTab = Tabs.Home
    }

    /** Navigation used by screens that are direct children of the root stack. */
    val rootNavigation: Navigation = object : Navigation {
        override fun navigate(name: String, params: Map<String, Any?>) {
            if (name in Tabs.all) {
                popToMainTabs(); selectTab(name); return
            }
            if (name == "MainTabs") {
                popToMainTabs()
                (params["screen"] as? String)?.let { if (it in Tabs.all) selectTab(it) }
                return
            }
            // Nested navigators of the logged-out flow: navigate("Auth") opens its initial route (Login),
            // navigate("IntroFlow") opens its initial route (WelcomeIntro).
            if (name == "Auth") { root.add(Route("Login", params)); return }
            if (name == "IntroFlow") { root.add(Route("WelcomeIntro", params)); return }
            root.add(Route(name, params))
        }

        override fun goBack() { handleBack() }
        override fun canGoBack(): Boolean = root.size > 1
        override fun replace(name: String, params: Map<String, Any?>) {
            if (root.isNotEmpty()) root.removeAt(root.lastIndex)
            root.add(Route(name, params))
        }

        override fun popToTop() { while (root.size > 1) root.removeAt(root.lastIndex) }
        override fun reset(name: String, params: Map<String, Any?>) {
            resetRoot(listOf(Route(name, params)))
        }

        override fun setParams(params: Map<String, Any?>) {
            root.lastOrNull()?.let { it.params = it.params + params }
        }
    }

    private fun popToMainTabs() {
        while (root.size > 1 && root.last().name != "MainTabs") root.removeAt(root.lastIndex)
    }

    /** Navigation used by screens rendered inside the Home tab's nested stack. */
    val homeNavigation: Navigation = object : Navigation {
        override fun navigate(name: String, params: Map<String, Any?>) {
            when {
                name in Tabs.all -> { selectTab(name) }
                name == "MainTabs" -> {
                    (params["screen"] as? String)?.let { if (it in Tabs.all) selectTab(it) }
                }
                name in HomeStackRoutes || name == "HomeScreen" -> homeStack.add(Route(name, params))
                else -> rootNavigation.navigate(name, params)
            }
        }

        override fun goBack() {
            if (homeStack.size > 1) homeStack.removeAt(homeStack.lastIndex) else rootNavigation.goBack()
        }

        override fun canGoBack(): Boolean = homeStack.size > 1 || root.size > 1
        override fun replace(name: String, params: Map<String, Any?>) {
            if (homeStack.size > 1) homeStack.removeAt(homeStack.lastIndex)
            homeStack.add(Route(name, params))
        }

        override fun popToTop() { while (homeStack.size > 1) homeStack.removeAt(homeStack.lastIndex) }
        override fun reset(name: String, params: Map<String, Any?>) = rootNavigation.reset(name, params)
        override fun setParams(params: Map<String, Any?>) {
            homeStack.lastOrNull()?.let { it.params = it.params + params }
        }
    }
}
