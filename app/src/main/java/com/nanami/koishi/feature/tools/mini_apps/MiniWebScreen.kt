package com.nanami.koishi.feature.tools.mini_apps

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.os.Message
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.DesktopWindows
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.nanami.koishi.R
import com.nanami.koishi.feature.tools.mini_apps.engine.AdBlockRules
import com.nanami.koishi.feature.tools.mini_apps.engine.MiniAppEntry
import com.nanami.koishi.feature.tools.mini_apps.engine.MiniAppUrl
import com.nanami.koishi.feature.tools.mini_apps.engine.MiniAppUserAgent
import com.nanami.koishi.feature.tools.mini_apps.engine.WebViewDataCleaner
import kotlinx.coroutines.launch
import java.io.ByteArrayInputStream
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MiniWebScreen(
    entry: MiniAppEntry,
    onExit: () -> Unit,
    onToggleDesktopMode: () -> Unit,
    onPageTitleResolved: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val titleCallback by rememberUpdatedState(onPageTitleResolved)

    var pageTitle by remember(entry.id) { mutableStateOf(entry.title) }
    var progress by remember(entry.id) { mutableIntStateOf(0) }
    var hasError by remember(entry.id) { mutableStateOf(false) }
    var reloadToken by remember(entry.id) { mutableIntStateOf(0) }
    var appliedDesktopMode by remember(entry.id) { mutableStateOf(entry.desktopMode) }

    val desktopModeState = rememberUpdatedState(entry.desktopMode)
    val mobileUserAgent = remember { MiniAppUserAgent.sanitize(WebSettings.getDefaultUserAgent(context)) }
    val desktopUserAgent = remember(mobileUserAgent) { MiniAppUserAgent.desktopUserAgent(mobileUserAgent) }

    fun showToast(messageRes: Int) {
        Toast.makeText(context, context.getString(messageRes), Toast.LENGTH_SHORT).show()
    }

    val webView = remember(entry.id) {
        WebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            applyMiniAppSettings(if (entry.desktopMode) desktopUserAgent else mobileUserAgent)

            webViewClient = MiniAppWebViewClient(
                onPageStarted = {
                    hasError = false
                    progress = 5
                },
                onPageFinished = { progress = 100 },
                onPageFailed = {
                    progress = 100
                    hasError = true
                },
                onOpenExternal = { url ->
                    if (!openExternal(context, url)) {
                        showToast(R.string.mini_apps_web_external_failed)
                    }
                    true
                },
                applyDesktopViewport = { view ->
                    if (desktopModeState.value) {
                        view.evaluateJavascript(desktopViewportScriptFor(view), null)
                    }
                }
            )

            webChromeClient = MiniAppWebChromeClient(
                onProgressChange = { progress = it },
                onTitleReceived = { title ->
                    if (MiniAppUrl.isMeaningfulTitle(title, entry.url)) {
                        pageTitle = title.trim()
                        titleCallback(title)
                    }
                }
            )
        }
    }

    LaunchedEffect(entry.id, reloadToken) {
        progress = 0
        hasError = false
        webView.loadUrl(entry.url)
    }

    LaunchedEffect(entry.desktopMode) {
        val enabled = entry.desktopMode
        webView.settings.userAgentString = if (enabled) desktopUserAgent else mobileUserAgent
        if (appliedDesktopMode != enabled) {
            appliedDesktopMode = enabled
            // 直接带 no-cache 重新请求主文档，避免复用按旧 UA 缓存的页面
            webView.loadUrl(
                webView.url?.takeIf { it.isNotBlank() } ?: entry.url,
                mapOf("Cache-Control" to "no-cache")
            )
        }
    }

    DisposableEffect(entry.id) {
        onDispose {
            (webView.parent as? ViewGroup)?.removeView(webView)
            webView.stopLoading()
            webView.destroy()
        }
    }

    val navigateBack: () -> Unit = {
        if (webView.canGoBack()) webView.goBack() else onExit()
    }

    BackHandler(enabled = true, onBack = navigateBack)

    val isLoading = progress in 1..99

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column {
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = navigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = stringResource(R.string.mini_apps_web_back),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    title = {
                        Column {
                            Text(
                                text = pageTitle,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = when {
                                    isLoading -> stringResource(R.string.mini_apps_web_progress, progress)
                                    entry.desktopMode -> stringResource(R.string.mini_apps_web_desktop_mode)
                                    else -> MiniAppUrl.hostOf(entry.url) ?: entry.url
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            onToggleDesktopMode()
                            showToast(
                                if (entry.desktopMode) {
                                    R.string.mini_apps_web_mobile_enabled
                                } else {
                                    R.string.mini_apps_web_desktop_enabled
                                }
                            )
                        }) {
                            Icon(
                                imageVector = if (entry.desktopMode) {
                                    Icons.Rounded.Smartphone
                                } else {
                                    Icons.Rounded.DesktopWindows
                                },
                                contentDescription = stringResource(
                                    if (entry.desktopMode) {
                                        R.string.mini_apps_web_switch_to_mobile
                                    } else {
                                        R.string.mini_apps_web_switch_to_desktop
                                    }
                                ),
                                tint = if (entry.desktopMode) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }

                        IconButton(onClick = { webView.reload() }) {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = stringResource(R.string.mini_apps_web_refresh),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        IconButton(onClick = {
                            scope.launch {
                                val cleared = try {
                                    WebViewDataCleaner.clearForSite(
                                        context = context,
                                        webView = webView,
                                        url = webView.url ?: entry.url
                                    )
                                    true
                                } catch (error: Exception) {
                                    false
                                }
                                showToast(
                                    if (cleared) {
                                        R.string.mini_apps_web_clear_done
                                    } else {
                                        R.string.mini_apps_web_clear_failed
                                    }
                                )
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Rounded.CleaningServices,
                                contentDescription = stringResource(R.string.mini_apps_web_clear),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                ) {
                    if (isLoading) {
                        LinearProgressIndicator(
                            progress = { progress / 100f },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AndroidView(
                factory = { webView },
                modifier = Modifier.fillMaxSize()
            )

            if (hasError) {
                MiniWebErrorContent(
                    onRetry = { reloadToken++ },
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                )
            }
        }
    }
}

@Composable
private fun MiniWebErrorContent(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.ErrorOutline,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(56.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.mini_apps_web_error_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.mini_apps_web_error_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(onClick = onRetry) {
            Icon(
                imageVector = Icons.Rounded.Refresh,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.size(6.dp))
            Text(stringResource(R.string.mini_apps_web_retry))
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
private fun WebView.applyMiniAppSettings(userAgent: String) {
    settings.apply {
        javaScriptEnabled = true
        domStorageEnabled = true
        loadWithOverviewMode = true
        useWideViewPort = true
        setSupportZoom(true)
        builtInZoomControls = true
        displayZoomControls = false
        mediaPlaybackRequiresUserGesture = true
        cacheMode = WebSettings.LOAD_DEFAULT
        javaScriptCanOpenWindowsAutomatically = false
        setSupportMultipleWindows(false)
        userAgentString = userAgent
    }

    CookieManager.getInstance().setAcceptThirdPartyCookies(this, false)
}

private fun desktopViewportScriptFor(view: WebView): String {
    val density = view.resources.displayMetrics.density.takeIf { it > 0f } ?: 1f
    val cssWidth = if (view.width > 0) {
        view.width / density
    } else {
        view.resources.displayMetrics.widthPixels / density
    }
    return MiniAppUserAgent.desktopViewportScript(cssWidth)
}

private class MiniAppWebViewClient(
    private val onPageStarted: () -> Unit,
    private val onPageFinished: () -> Unit,
    private val onPageFailed: () -> Unit,
    private val onOpenExternal: (String) -> Boolean,
    private val applyDesktopViewport: (WebView) -> Unit
) : WebViewClient() {

    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        val scheme = request.url.scheme?.lowercase(Locale.US) ?: return false
        return when (scheme) {
            "http", "https" -> AdBlockRules.isBlockedHost(request.url.host)
            else -> onOpenExternal(request.url.toString())
        }
    }

    override fun shouldInterceptRequest(
        view: WebView,
        request: WebResourceRequest
    ): WebResourceResponse? {
        if (request.isForMainFrame) return null
        if (!AdBlockRules.shouldBlockRequest(request.url.toString())) return null
        return WebResourceResponse("text/plain", "utf-8", ByteArrayInputStream(ByteArray(0)))
    }

    override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
        onPageStarted()
    }

    override fun onPageCommitVisible(view: WebView, url: String) {
        applyDesktopViewport(view)
    }

    override fun onPageFinished(view: WebView, url: String) {
        applyDesktopViewport(view)
        onPageFinished()
    }

    override fun onReceivedError(
        view: WebView,
        request: WebResourceRequest,
        error: WebResourceError
    ) {
        if (request.isForMainFrame) onPageFailed()
    }

    override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
        handler.cancel()
        onPageFailed()
    }
}

private class MiniAppWebChromeClient(
    private val onProgressChange: (Int) -> Unit,
    private val onTitleReceived: (String) -> Unit
) : WebChromeClient() {

    override fun onProgressChanged(view: WebView, newProgress: Int) {
        onProgressChange(newProgress)
    }

    override fun onReceivedTitle(view: WebView, title: String?) {
        if (!title.isNullOrBlank()) onTitleReceived(title)
    }

    override fun onCreateWindow(
        view: WebView,
        isDialog: Boolean,
        isUserGesture: Boolean,
        resultMsg: Message?
    ): Boolean = false
}

private fun openExternal(context: Context, url: String): Boolean {
    val intent = if (url.startsWith("intent:", ignoreCase = true)) {
        runCatching { Intent.parseUri(url, Intent.URI_INTENT_SCHEME) }.getOrNull()
    } else {
        Intent(Intent.ACTION_VIEW, Uri.parse(url))
    } ?: return false

    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    return try {
        context.startActivity(intent)
        true
    } catch (error: Exception) {
        val fallback = intent.getStringExtra("browser_fallback_url")
        !fallback.isNullOrBlank() &&
                !fallback.startsWith("intent:", ignoreCase = true) &&
                openExternal(context, fallback)
    }
}
