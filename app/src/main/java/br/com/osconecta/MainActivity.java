package br.com.osconecta;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.ContentValues;
import android.provider.MediaStore;
import android.util.Base64;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import android.content.IntentFilter;
import android.content.ActivityNotFoundException;
import android.graphics.Color;
import android.os.Build;
import android.os.Environment;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebResourceError;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import java.util.concurrent.TimeUnit;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import org.json.JSONArray;
import org.json.JSONObject;

public class MainActivity extends Activity {
    private static final String APP_URL = "https://raw.githubusercontent.com/ernestogodoi4-ship-it/OsConecta/main/app/src/main/assets/index.html";
    private static final String RELEASE_URL = "https://api.github.com/repos/ernestogodoi4-ship-it/OsConecta/releases/latest";
    private static final String UPDATE_PREFS = "osconecta_updates";
    private static final String DOWNLOAD_ID = "download_id";
    private WebView web;
    private boolean offlineFallback = false;
    private DownloadManager downloadManager;
    private BroadcastReceiver downloadReceiver;
    private boolean receiverRegistered = false;
    private boolean pendingInstall = false;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(Color.parseColor("#061A2B"));
        downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        registerDownloadReceiver();
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
        checkForAppUpdate();
        scheduleProtocolBackup();
    }

    private void scheduleProtocolBackup() {
        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(ProtocolBackupWorker.class, 12, TimeUnit.HOURS).build();
        WorkManager.getInstance(getApplicationContext()).enqueueUniquePeriodicWork(ProtocolBackupWorker.WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request);
    }
    private void checkForAppUpdate() {
        new Thread(() -> {
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) new URL(RELEASE_URL).openConnection();
                connection.setConnectTimeout(6000);
                connection.setReadTimeout(8000);
                connection.setRequestProperty("Accept", "application/vnd.github+json");
                connection.setRequestProperty("User-Agent", "OSConecta-Android");
                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) return;
                StringBuilder json = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) json.append(line);
                }
                JSONObject release = new JSONObject(json.toString());
                String tag = release.optString("tag_name", "").replaceFirst("^[vV]", "");
                String installed = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
                if (compareVersions(tag, installed) <= 0) return;
                JSONArray assets = release.optJSONArray("assets");
                if (assets == null) return;
                String apkUrl = null;
                for (int i = 0; i < assets.length(); i++) {
                    JSONObject asset = assets.optJSONObject(i);
                    if (asset != null && asset.optString("name", "").toLowerCase().endsWith(".apk")) {
                        apkUrl = asset.optString("browser_download_url", null);
                        break;
                    }
                }
                if (apkUrl == null || !apkUrl.startsWith("https://github.com/ernestogodoi4-ship-it/OsConecta/releases/download/")) return;
                final String latestVersion = tag;
                final String downloadUrl = apkUrl;
                new Handler(Looper.getMainLooper()).post(() -> showUpdateDialog(latestVersion, downloadUrl));
            } catch (Exception ignored) {
                // Falha de rede ou resposta inválida: o app continua usando a versão instalada.
            } finally {
                if (connection != null) connection.disconnect();
            }
        }).start();
    }

    private static int compareVersions(String latest, String installed) {
        String[] a = latest.split("[.+-]", 3);
        String[] b = (installed == null ? "0" : installed).replaceFirst("^[vV]", "").split("[.+-]", 3);
        int count = Math.max(a.length, b.length);
        for (int i = 0; i < count; i++) {
            long x = i < a.length ? parseVersionPart(a[i]) : 0;
            long y = i < b.length ? parseVersionPart(b[i]) : 0;
            if (x != y) return Long.compare(x, y);
        }
        return 0;
    }

    private static long parseVersionPart(String part) {
        try { return Long.parseLong(part.replaceAll("[^0-9].*$", "")); }
        catch (Exception ignored) { return 0; }
    }

    private void showUpdateDialog(String version, String apkUrl) {
        if (isFinishing() || isDestroyed()) return;
        new AlertDialog.Builder(this)
            .setTitle("Atualização disponível")
            .setMessage("OS Conecta " + version + " está disponível. Baixar o APK do GitHub? O Android solicitará confirmação para instalar.")
            .setNegativeButton("Depois", (dialog, which) -> dialog.dismiss())
            .setPositiveButton("Baixar atualização", (dialog, which) -> downloadUpdate(apkUrl, version))
            .show();
    }

    private void downloadUpdate(String apkUrl, String version) {
        try {
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(apkUrl));
            request.setTitle("OS Conecta " + version);
            request.setDescription("Baixando atualização do aplicativo");
            request.setMimeType("application/vnd.android.package-archive");
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalFilesDir(this, Environment.DIRECTORY_DOWNLOADS, "OSConecta-" + version + ".apk");
            long id = downloadManager.enqueue(request);
            getSharedPreferences(UPDATE_PREFS, MODE_PRIVATE).edit().putLong(DOWNLOAD_ID, id).apply();
        } catch (Exception e) {
            new AlertDialog.Builder(this).setMessage("Não foi possível iniciar o download. Verifique sua conexão e tente novamente.").setPositiveButton("OK", null).show();
        }
    }

    private void registerDownloadReceiver() {
        downloadReceiver = new BroadcastReceiver() {
            @Override public void onReceive(Context context, Intent intent) {
                long expected = getSharedPreferences(UPDATE_PREFS, MODE_PRIVATE).getLong(DOWNLOAD_ID, -1);
                if (intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -2) != expected) return;
                DownloadManager.Query query = new DownloadManager.Query().setFilterById(expected);
                try (android.database.Cursor cursor = downloadManager.query(query)) {
                    if (cursor != null && cursor.moveToFirst() && cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)) == DownloadManager.STATUS_SUCCESSFUL) {
                        if (hasWindowFocus()) openDownloadedApk(expected);
                    } else {
                        getSharedPreferences(UPDATE_PREFS, MODE_PRIVATE).edit().remove(DOWNLOAD_ID).apply();
                    }
                } catch (Exception ignored) { }
            }
        };
        IntentFilter filter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(downloadReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        else registerReceiver(downloadReceiver, filter);
        receiverRegistered = true;
    }

    private void openDownloadedApk(long id) {
        if (isFinishing() || isDestroyed()) return;
        if (Build.VERSION.SDK_INT >= 26 && !getPackageManager().canRequestPackageInstalls()) {
            pendingInstall = true;
            new AlertDialog.Builder(this)
                .setTitle("Permitir instalação da atualização")
                .setMessage("Para instalar o APK baixado do GitHub, permita que o OS Conecta instale esta atualização nas configurações do Android.")
                .setNegativeButton("Agora não", (d, w) -> pendingInstall = false)
                .setPositiveButton("Abrir configurações", (d, w) -> {
                    Intent settings = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + getPackageName()));
                    startActivityForResult(settings, 2301);
                }).show();
            return;
        }
        Uri apk = downloadManager.getUriForDownloadedFile(id);
        if (apk == null) return;
        try {
            Intent install = new Intent(Intent.ACTION_VIEW);
            install.setDataAndType(apk, "application/vnd.android.package-archive");
            install.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(install);
            getSharedPreferences(UPDATE_PREFS, MODE_PRIVATE).edit().remove(DOWNLOAD_ID).apply();
        } catch (ActivityNotFoundException ignored) {
            new AlertDialog.Builder(this).setMessage("O Android não encontrou o instalador de pacotes.").setPositiveButton("OK", null).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 2301 && pendingInstall) {
            pendingInstall = false;
            if (Build.VERSION.SDK_INT < 26 || getPackageManager().canRequestPackageInstalls()) {
                long id = getSharedPreferences(UPDATE_PREFS, MODE_PRIVATE).getLong(DOWNLOAD_ID, -1);
                if (id >= 0) openDownloadedApk(id);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        long id = getSharedPreferences(UPDATE_PREFS, MODE_PRIVATE).getLong(DOWNLOAD_ID, -1);
        if (id >= 0 && downloadManager != null && !pendingInstall) {
            try (android.database.Cursor cursor = downloadManager.query(new DownloadManager.Query().setFilterById(id))) {
                if (cursor != null && cursor.moveToFirst() && cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)) == DownloadManager.STATUS_SUCCESSFUL) openDownloadedApk(id);
            } catch (Exception ignored) { }
        }
    }

    @Override
    protected void onDestroy() {
        if (receiverRegistered) unregisterReceiver(downloadReceiver);
        super.onDestroy();
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
        public void saveProtocolData(final String json) {
            getSharedPreferences("osconecta_protocols", MODE_PRIVATE).edit().putString("records", json == null ? "[]" : json).apply();
        }

        @JavascriptInterface
        public void copy(final String t) {
            runOnUiThread(() -> {
                ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                cm.setPrimaryClip(ClipData.newPlainText("Encerramento OS", t));
            });
        }

        @JavascriptInterface
        public void saveBackupFile(final String fileName, final String encoded, final String backupMonth) {
            final String safeName = (fileName == null ? "OSConecta-Protocolos.xlsx" : fileName).replaceAll("[^A-Za-z0-9._-]", "_");
            final String safeMonth = backupMonth == null ? "" : backupMonth.replaceAll("[^0-9-]", "");
            new Thread(() -> {
                boolean saved = false;
                try {
                    byte[] bytes = Base64.decode(encoded, Base64.DEFAULT);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        ContentValues values = new ContentValues();
                        values.put(MediaStore.MediaColumns.DISPLAY_NAME, safeName);
                        values.put(MediaStore.MediaColumns.MIME_TYPE, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
                        values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/OS Conecta");
                        Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                        if (uri != null) {
                            try (OutputStream output = getContentResolver().openOutputStream(uri)) {
                                if (output != null) { output.write(bytes); output.flush(); saved = true; }
                            }
                        }
                    } else {
                        File directory = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
                        if (directory == null) directory = getFilesDir();
                        if (!directory.exists()) directory.mkdirs();
                        try (FileOutputStream output = new FileOutputStream(new File(directory, safeName))) {
                            output.write(bytes); output.flush(); saved = true;
                        }
                    }
                } catch (Exception ignored) { }
                final boolean result = saved;
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (web == null) return;
                    String callback = "window.onProtocolBackupSaved && window.onProtocolBackupSaved(" + JSONObject.quote(safeMonth) + "," + result + ")";
                    web.evaluateJavascript(callback, null);
                });
            }).start();
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
