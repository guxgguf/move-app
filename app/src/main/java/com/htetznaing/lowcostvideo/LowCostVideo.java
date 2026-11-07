package com.htetznaing.lowcostvideo;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.htetznaing.lowcostvideo.Model.XModel;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Local lightweight replacement for com.github.KhunHtetzNaing:xGetter (not available on JitPack anymore).
 * Best effort: accepts direct media links, otherwise downloads the page and looks for a media URL in it.
 * Hosts that hide their links behind obfuscated scripts will report onError().
 */
public class LowCostVideo {

    public interface OnTaskCompleted {
        void onTaskCompleted(ArrayList<XModel> vidURL, boolean multiple_quality);
        void onError();
    }

    private static final String UA =
            "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Mobile Safari/537.36";
    private static final Pattern MEDIA = Pattern.compile(
            "https?:\\\\?/\\\\?/[^\"'\\s<>\\\\]+?\\.(?:mp4|m3u8|mkv|webm)(?:\\?[^\"'\\s<>\\\\]*)?",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern FB = Pattern.compile("\"(?:hd_src|sd_src)(?:_no_ratelimit)?\":\"([^\"]+)\"");

    private final Context context;
    private final Handler main = new Handler(Looper.getMainLooper());
    private OnTaskCompleted callback;

    public LowCostVideo(Context context) { this.context = context; }

    public void onFinish(OnTaskCompleted callback) { this.callback = callback; }

    public void find(final String input) {
        new Thread(() -> {
            try {
                ArrayList<XModel> list = resolve(input);
                if (list.isEmpty()) postError(); else postOk(list);
            } catch (Throwable t) {
                postError();
            }
        }).start();
    }

    private void postOk(final ArrayList<XModel> list) {
        main.post(() -> { if (callback != null) callback.onTaskCompleted(list, list.size() > 1); });
    }

    private void postError() {
        main.post(() -> { if (callback != null) callback.onError(); });
    }

    private ArrayList<XModel> resolve(String input) throws Exception {
        ArrayList<XModel> out = new ArrayList<>();
        if (input == null || input.trim().isEmpty()) return out;
        input = input.trim();

        // Facebook video id
        if (input.matches("\\d{5,}")) {
            String html = get("https://m.facebook.com/watch/?v=" + input);
            Matcher m = FB.matcher(html);
            int i = 0;
            Set<String> seen = new LinkedHashSet<>();
            while (m.find()) {
                String u = clean(m.group(1));
                if (seen.add(u)) add(out, u, i++ == 0 ? "SD/HD" : "Quality " + (i));
            }
            return out;
        }

        if (!input.startsWith("http")) return out;
        if (MEDIA.matcher(input).find()) { add(out, input, "Default"); return out; }

        String html = get(input);
        Matcher m = MEDIA.matcher(html);
        Set<String> seen = new LinkedHashSet<>();
        while (m.find()) seen.add(clean(m.group()));
        int i = 1;
        for (String u : seen) add(out, u, qualityOf(u, i++));
        return out;
    }

    private static void add(ArrayList<XModel> out, String url, String quality) {
        XModel x = new XModel();
        x.setUrl(url);
        x.setQuality(quality);
        out.add(x);
    }

    private static String qualityOf(String url, int idx) {
        Matcher m = Pattern.compile("(2160|1440|1080|720|480|360|240)p?").matcher(url);
        return m.find() ? m.group(1) + "p" : "Link " + idx;
    }

    private static String clean(String u) {
        return u.replace("\\/", "/").replace("\\u0025", "%").replace("\\u0026", "&").replace("&amp;", "&");
    }

    private String get(String link) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(link).openConnection();
        c.setInstanceFollowRedirects(true);
        c.setConnectTimeout(15000);
        c.setReadTimeout(20000);
        c.setRequestProperty("User-Agent", UA);
        InputStream in = c.getInputStream();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n, total = 0;
        while ((n = in.read(buf)) > 0 && total < 3_000_000) { bos.write(buf, 0, n); total += n; }
        in.close();
        c.disconnect();
        return bos.toString("UTF-8");
    }
}
