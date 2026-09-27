package fr.shabbattv;

import android.content.Context;
import android.net.Uri;
import android.util.AtomicFile;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.zip.GZIPInputStream;

/** Bundled, verified H.264/AAC black clip. No remote URL or fabricated binary string. */
public final class BlackPlaybackAsset {
    private static final String SHA256 = "0b1a30c8ae4375f7d1639099ef8bd9a011773b9d75e04662964913bf8375e1f2";
    private static final int BYTES = 4885;
    private BlackPlaybackAsset() {}

    public static synchronized Uri uri(Context context) throws Exception {
        File file = new File(context.getCacheDir(), "shabbat-black-loop-v111.mp4");
        if (file.isFile() && file.length() == BYTES) {
            try (InputStream cached = new FileInputStream(file)) {
                if (valid(read(cached))) return Uri.fromFile(file);
            } catch (IOException ignored) { /* Rebuild a missing or damaged cache from the asset. */ }
        }
        byte[] data;
        try (InputStream asset = context.getAssets().open("black_loop.mp4.gz");
                InputStream decoded = new GZIPInputStream(asset)) {
            data = read(decoded);
        }
        if (!valid(data)) throw new IOException("Black-video integrity check failed");
        AtomicFile target = new AtomicFile(file);
        FileOutputStream output = null;
        try {
            output = target.startWrite();
            output.write(data);
            target.finishWrite(output);
        } catch (IOException error) {
            if (output != null) target.failWrite(output);
            throw error;
        }
        return Uri.fromFile(file);
    }
    private static byte[] read(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream(BYTES);
        byte[] buffer = new byte[4096];
        int count;
        while ((count = input.read(buffer)) != -1) {
            output.write(buffer, 0, count);
            if (output.size() > BYTES) throw new IOException("Unexpected black-video size");
        }
        return output.toByteArray();
    }
    private static boolean valid(byte[] data) throws Exception {
        if (data.length != BYTES) return false;
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(data);
        StringBuilder hex = new StringBuilder(64);
        for (byte b : digest) {
            hex.append(Character.forDigit((b >>> 4) & 15, 16));
            hex.append(Character.forDigit(b & 15, 16));
        }
        return SHA256.equals(hex.toString());
    }
}
