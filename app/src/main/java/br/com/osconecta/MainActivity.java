package br.com.osconecta;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebResourceError;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class MainActivity extends Activity {
    private static final String APP_URL = "https://raw.githubusercontent.com/ernestogodoi4-ship-it/OsConecta/main/app/src/main/assets/index.html";
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
        s.setCacheMode(WebSettings.LOAD_NO_CACHE);
        s.setAllowFileAccess(false); // arquivos em /android_asset continuam acessíveis
        s.setAllowContentAccess(false);
        web.addJavascriptInterface(new Bridge(), "Android");
        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
                Uri u = r.getUrl();
                if ("https".equals(u.getScheme()) && "raw.githubusercontent.com".equals(u.getHost()) && u.getPath().startsWith("/ernestogodoi4-ship-it/OsConecta/")) return false;
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
        if (b == null) loadLatestInterface();
        else web.restoreState(b);
    }

    private void loadLatestInterface() {
        new Thread(() -> {
            String html = null;
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) new URL(APP_URL).openConnection();
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(8000);
                connection.setUseCaches(false);
                connection.setRequestProperty("Cache-Control", "no-cache");
                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    StringBuilder content = new StringBuilder();
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = reader.readLine()) != null) content.append(line).append('\n');
                    }
                    String candidate = content.toString();
                    if (candidate.toLowerCase().contains("<html") && candidate.contains("id=\"catalog\"")) html = candidate;
                }
            } catch (Exception ignored) {
                // Sem internet ou arquivo remoto indisponível: abre a interface embutida no APK.
            } finally {
                if (connection != null) connection.disconnect();
            }
            final String latest = html;
            new Handler(Looper.getMainLooper()).post(() -> {
                if (isFinishing()) return;
                if (latest != null) web.loadDataWithBaseURL("https://raw.githubusercontent.com/ernestogodoi4-ship-it/OsConecta/main/", latest, "text/html", "UTF-8", null);
                else {
                    offlineFallback = true;
                    web.loadUrl("file:///android_asset/index.html");
                }
            });
        }).start();
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

