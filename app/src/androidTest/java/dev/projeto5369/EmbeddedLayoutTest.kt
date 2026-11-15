package dev.projeto5369

import android.annotation.SuppressLint
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.ByteArrayInputStream
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Real Android WebView layout, with a local replacement stub. Does NOT validate YouTube playback. */
@RunWith(AndroidJUnit4::class)
class EmbeddedLayoutTest {
    @get:Rule val compose = createComposeRule()
    class Probe(val result: AtomicReference<String>) {
        @JavascriptInterface fun log(value: String) { result.set(value) }
        @JavascriptInterface fun event(value: Int) = Unit
        @JavascriptInterface fun failure(value: Int) = Unit
    }
    @SuppressLint("SetJavaScriptEnabled")
    @Test fun replacingPlayerDivWithIframeKeepsNonzeroHeight() {
        val result = AtomicReference("")
        var webView: WebView? = null
        compose.setContent {
            AndroidView(modifier=Modifier.size(320.dp,180.dp),factory={ context ->
                WebView(context).apply {
                    webView=this
                    settings.javaScriptEnabled=true
                    addJavascriptInterface(Probe(result),"P5369")
                    webViewClient=object : WebViewClient() {
                        override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse {
                            val body = if (request.url.path == "/iframe_api") """
                                window.YT={Player:function(id,opts){
                                  var f=document.createElement('iframe'); f.id='widget2';
                                  f.style.width=opts.width; f.style.height=opts.height;
                                  document.getElementById(id).replaceWith(f);
                                  setTimeout(function(){opts.events.onReady();},0);
                                }};
                                setTimeout(function(){onYouTubeIframeAPIReady();},0);
                            """ else ""
                            return WebResourceResponse("text/javascript","UTF-8",ByteArrayInputStream(body.toByteArray()))
                        }
                    }
                    loadDataWithBaseURL("https://dev.projeto5369/",embeddedHtml("M7lc1UVf-VE"),"text/html","UTF-8",null)
                }
            })
        }
        try {
            compose.waitUntil(15_000) { result.get().startsWith("ready iframe=") }
            val size=Regex("iframe=([0-9]+)x([0-9]+)").find(result.get())!!
            assertTrue("Iframe width: ${result.get()}",size.groupValues[1].toInt()>0)
            assertTrue("Iframe height: ${result.get()}",size.groupValues[2].toInt()>0)
        } finally { compose.runOnIdle { webView?.removeJavascriptInterface("P5369"); webView?.destroy() } }
    }
}
