package ir.alamoot.plus;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Loads camera brand presets from assets/brand_presets.json. */
public class BrandPresets {

    public static class Preset {
        public final String name, rtspPort, httpPort, streamPath, sampleUrl;
        public Preset(String n, String r, String h, String p, String s) {
            name = n; rtspPort = r; httpPort = h; streamPath = p; sampleUrl = s;
        }
        public String rtspUrl(String host) {
            return "rtsp://" + host + ":" + rtspPort + streamPath;
        }
    }

    private final List<Preset> presets = new ArrayList<>();

    public BrandPresets(Context ctx) {
        try (InputStream is = ctx.getAssets().open("brand_presets.json")) {
            byte[] buf = new byte[is.available()];
            is.read(buf);
            JSONArray arr = new JSONArray(new String(buf, StandardCharsets.UTF_8));
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                presets.add(new Preset(o.getString("name"), o.getString("rtspPort"),
                        o.getString("httpPort"), o.getString("streamPath"), o.optString("sampleUrl")));
            }
        } catch (Exception ignored) {}
    }

    public List<String> names() {
        List<String> n = new ArrayList<>();
        for (Preset p : presets) n.add(p.name);
        return n;
    }

    public Preset get(int i) { return presets.get(i); }
    public int count() { return presets.size(); }
}
