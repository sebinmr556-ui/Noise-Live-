package com.example.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

data class ThemeColorsConfig(
    val primaryColor: String = "#D946EF",
    val secondaryColor: String = "#A855F7",
    val backgroundColor: String = "#090514",
    val surfaceColor: String = "#1B1033",
    val accentColor: String = "#EAB308",
    val textColor: String = "#FFFFFF",
    val allColors: Map<String, String> = emptyMap()
)

data class ModuleFlagsConfig(
    val homeEnabled: Boolean = true,
    val chatroomEnabled: Boolean = true,
    val feedEnabled: Boolean = true,
    val messagesEnabled: Boolean = true,
    val profileEnabled: Boolean = true,
    val storiesEnabled: Boolean = true,
    val allFlags: Map<String, Boolean> = emptyMap()
)

data class ScreenConfig(
    val screenKey: String,
    val title: String = "",
    val subtitle: String = "",
    val enabled: Boolean = true,
    val layoutType: String = "default",
    val backgroundColor: String = "",
    val accentColor: String = "",
    val features: Map<String, Boolean> = emptyMap(),
    val properties: Map<String, String> = emptyMap(),
    val rawJson: String = "{}"
) {
    fun getString(key: String, default: String = ""): String =
        properties[key] ?: default

    fun getBoolean(key: String, default: Boolean = true): Boolean =
        features[key] ?: properties[key]?.toBooleanStrictOrNull() ?: default

    fun getInt(key: String, default: Int = 0): Int =
        properties[key]?.toIntOrNull() ?: default
}

data class RemoteAppConfig(
    val appName: String = "Noise",
    val version: String = "1.0.0",
    val themeColors: ThemeColorsConfig = ThemeColorsConfig(),
    val moduleFlags: ModuleFlagsConfig = ModuleFlagsConfig(),
    val screens: Map<String, ScreenConfig> = emptyMap(),
    val rawConfigJson: String = "{}",
    val fetchedAtMillis: Long = 0L
) {
    fun getScreen(name: String): ScreenConfig? = screens[name]
}

sealed class ConfigLoadState {
    data object Idle : ConfigLoadState()
    data object Loading : ConfigLoadState()
    data class Success(val config: RemoteAppConfig) : ConfigLoadState()
    data class Error(
        val message: String,
        val fallbackConfig: RemoteAppConfig = RemoteAppConfig()
    ) : ConfigLoadState()
}

object AppConfigManager {

    const val BASE_RAW_URL = "https://raw.githubusercontent.com/sebinmr556-ui/Elements/main/"
    const val MAIN_CONFIG_PATH = "config.json"

    val SCREEN_PATHS = mapOf(
        "home" to "screen/home.json",
        "chatroom" to "screen/chatroom.json",
        "feed" to "screen/feed.json",
        "messages" to "screen/messages.json",
        "profile" to "screen/profile.json"
    )

    private val managerScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(12, TimeUnit.SECONDS)
            .writeTimeout(12, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    private val _loadState = MutableStateFlow<ConfigLoadState>(ConfigLoadState.Idle)
    val loadState: StateFlow<ConfigLoadState> = _loadState.asStateFlow()

    private val _config = MutableStateFlow(RemoteAppConfig())
    val config: StateFlow<RemoteAppConfig> = _config.asStateFlow()

    /**
     * Triggers an asynchronous background fetch of config.json and all screen JSON files.
     * Safe to call from MainActivity.onCreate() or a Compose LaunchedEffect.
     */
    fun loadConfigAsync(onResult: ((ConfigLoadState) -> Unit)? = null) {
        managerScope.launch {
            val result = loadAllConfigs()
            onResult?.invoke(result)
        }
    }

    /**
     * Fetches config.json and all screen JSON files concurrently on Dispatchers.IO.
     * Never throws; returns ConfigLoadState.Success or ConfigLoadState.Error with safe defaults.
     */
    suspend fun loadAllConfigs(): ConfigLoadState = withContext(Dispatchers.IO) {
        if (_loadState.value is ConfigLoadState.Loading) {
            return@withContext _loadState.value
        }
        _loadState.value = ConfigLoadState.Loading

        try {
            // 1. Fetch main config.json
            val mainConfigUrl = "$BASE_RAW_URL$MAIN_CONFIG_PATH"
            val mainConfigRaw = fetchUrlText(mainConfigUrl)
            val mainJson = JSONObject(mainConfigRaw)

            // 2. Fetch all screen JSON files in parallel (gracefully tolerating individual file errors)
            val deferredScreens = SCREEN_PATHS.map { (screenKey, relativePath) ->
                async {
                    val screenUrl = "$BASE_RAW_URL$relativePath"
                    val screenRaw = runCatching { fetchUrlText(screenUrl) }.getOrNull()
                    if (!screenRaw.isNullOrBlank()) {
                        screenKey to parseScreenConfig(screenKey, screenRaw)
                    } else {
                        screenKey to ScreenConfig(screenKey = screenKey)
                    }
                }
            }
            val loadedScreens = deferredScreens.awaitAll().toMap()

            // 3. Parse root config.json + merge loaded screen configs
            val parsedConfig = parseMainConfig(
                json = mainJson,
                rawConfigJson = mainConfigRaw,
                screens = loadedScreens
            )

            _config.value = parsedConfig
            val successState = ConfigLoadState.Success(parsedConfig)
            _loadState.value = successState
            successState
        } catch (e: Exception) {
            val errorMsg = e.localizedMessage ?: "Unable to load remote configuration"
            val errorState = ConfigLoadState.Error(
                message = errorMsg,
                fallbackConfig = _config.value
            )
            _loadState.value = errorState
            errorState
        }
    }

    @Throws(IOException::class)
    private fun fetchUrlText(url: String): String {
        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .get()
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("HTTP ${response.code} when fetching $url")
            }
            val bodyString = response.body?.string()
            if (bodyString.isNullOrBlank()) {
                throw IOException("Empty response body from $url")
            }
            return bodyString
        }
    }

    private fun parseMainConfig(
        json: JSONObject,
        rawConfigJson: String,
        screens: Map<String, ScreenConfig>
    ): RemoteAppConfig {
        val appName = json.optString("app_name")
            .ifBlank { json.optString("appName", "Noise") }

        val version = json.optString("version")
            .ifBlank { json.optString("app_version", "1.0.0") }

        // Parse theme colors (supports nested "theme" / "colors" object or flat keys)
        val themeObj = json.optJSONObject("theme")
            ?: json.optJSONObject("colors")
            ?: json.optJSONObject("theme_colors")
            ?: JSONObject()

        val allColors = jsonObjectToStringMap(themeObj)
        val themeColors = ThemeColorsConfig(
            primaryColor = themeObj.optString("primary", json.optString("primary_color", "#D946EF")),
            secondaryColor = themeObj.optString("secondary", json.optString("secondary_color", "#A855F7")),
            backgroundColor = themeObj.optString("background", json.optString("background_color", "#090514")),
            surfaceColor = themeObj.optString("surface", json.optString("surface_color", "#1B1033")),
            accentColor = themeObj.optString("accent", json.optString("accent_color", "#EAB308")),
            textColor = themeObj.optString("text", json.optString("text_color", "#FFFFFF")),
            allColors = allColors
        )

        // Parse module flags (supports nested "modules" / "features" / "flags" object)
        val modulesObj = json.optJSONObject("modules")
            ?: json.optJSONObject("features")
            ?: json.optJSONObject("flags")
            ?: JSONObject()

        val allFlags = jsonObjectToBooleanMap(modulesObj)
        val moduleFlags = ModuleFlagsConfig(
            homeEnabled = modulesObj.optBoolean("home", true),
            chatroomEnabled = modulesObj.optBoolean("chatroom", true),
            feedEnabled = modulesObj.optBoolean("feed", true),
            messagesEnabled = modulesObj.optBoolean("messages", true),
            profileEnabled = modulesObj.optBoolean("profile", true),
            storiesEnabled = modulesObj.optBoolean("stories", true),
            allFlags = allFlags
        )

        return RemoteAppConfig(
            appName = appName,
            version = version,
            themeColors = themeColors,
            moduleFlags = moduleFlags,
            screens = screens,
            rawConfigJson = rawConfigJson,
            fetchedAtMillis = System.currentTimeMillis()
        )
    }

    private fun parseScreenConfig(screenKey: String, rawJson: String): ScreenConfig {
        return try {
            val json = JSONObject(rawJson)
            val title = json.optString("title")
                .ifBlank { json.optString("screen_title", screenKey.replaceFirstChar { it.uppercase() }) }
            val subtitle = json.optString("subtitle", json.optString("description", ""))
            val enabled = json.optBoolean("enabled", json.optBoolean("is_enabled", true))
            val layoutType = json.optString("layout", json.optString("layout_type", "default"))
            val bgColor = json.optString("background_color", json.optString("backgroundColor", ""))
            val accentColor = json.optString("accent_color", json.optString("accentColor", ""))

            val featuresObj = json.optJSONObject("features")
                ?: json.optJSONObject("flags")
                ?: JSONObject()
            val featuresMap = jsonObjectToBooleanMap(featuresObj).toMutableMap()

            val propertiesMap = mutableMapOf<String, String>()
            val keys = json.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val value = json.opt(key)
                when (value) {
                    is Boolean -> featuresMap[key] = value
                    is String, is Number -> propertiesMap[key] = value.toString()
                    is JSONArray -> propertiesMap[key] = value.toString()
                }
            }

            ScreenConfig(
                screenKey = screenKey,
                title = title,
                subtitle = subtitle,
                enabled = enabled,
                layoutType = layoutType,
                backgroundColor = bgColor,
                accentColor = accentColor,
                features = featuresMap,
                properties = propertiesMap,
                rawJson = rawJson
            )
        } catch (_: Exception) {
            ScreenConfig(screenKey = screenKey, rawJson = rawJson)
        }
    }

    private fun jsonObjectToStringMap(jsonObject: JSONObject): Map<String, String> {
        val map = mutableMapOf<String, String>()
        val keys = jsonObject.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val value = jsonObject.opt(key)
            if (value != null && value !is JSONObject && value !is JSONArray) {
                map[key] = value.toString()
            }
        }
        return map
    }

    private fun jsonObjectToBooleanMap(jsonObject: JSONObject): Map<String, Boolean> {
        val map = mutableMapOf<String, Boolean>()
        val keys = jsonObject.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            when (val value = jsonObject.opt(key)) {
                is Boolean -> map[key] = value
                is JSONObject -> {
                    if (value.has("enabled")) {
                        map[key] = value.optBoolean("enabled", true)
                    }
                }
            }
        }
        return map
    }
}
