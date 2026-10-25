package br.com.osconecta;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebResourceError;

public class MainActivity extends Activity {
    private static final String APP_URL = "https://ernestogodoi4-ship-it.github.io/OsConecta/";
    private WebView web;
    private boolean offlineFallback = false;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(Color.parseColor("#061A2B"));
        web = new WebView(this);
        setContentView(web);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setCacheMode(WebSettings.LOAD_NO_CACHE); // busca a publicação mais recente ao iniciar
        s.setAllowFileAccess(false); // arquivos em /android_asset continuam acessíveis
        s.setAllowContentAccess(false);
        web.addJavascriptInterface(new Bridge(), "Android");
        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
                Uri u = r.getUrl();
                if ("https".equals(u.getScheme()) && "ernestogodoi4-ship-it.github.io".equals(u.getHost()) && u.getPath().startsWith("/OsConecta/")) return false;
                if ("http".equals(u.getScheme()) || "https".equals(u.getScheme())) {
                    startActivity(new Intent(Intent.ACTION_VIEW, u));
                    return true;
                }
                return false;
            }

            @Override
            public void onReceivedError(WebView v, WebResourceRequest r, WebResourceError e) {
                super.onReceivedError(v, r, e);
                if (r.isForMainFrame() && !offlineFallback) {
                    offlineFallback = true;
                    v.loadUrl("file:///android_asset/index.html");
                }
            }
        });
        if (b == null) web.loadUrl(APP_URL);
        else web.restoreState(b);
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        web.saveState(out);
    }

    class Bridge {
        @JavascriptInterface
        public void copy(final String t) {
            runOnUiThread(() -> {
                ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                cm.setPrimaryClip(ClipData.newPlainText("Encerramento OS", t));
            });
        }

        @JavascriptInterface
        public void share(final String t) {
            runOnUiThread(() -> {
                Intent i = new Intent(Intent.ACTION_SEND);
                i.setType("text/plain");
                i.putExtra(Intent.EXTRA_TEXT, t);
                startActivity(Intent.createChooser(i, "Compartilhar encerramento"));
            });
        }
    }
}

