package ir.alamoot.plus;

import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.rtsp.RtspMediaSource;
import androidx.media3.datasource.DefaultDataSource;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;

public class PlayerFragment extends Fragment {

    private ExoPlayer player;
    private SurfaceView surface;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inf, @Nullable ViewGroup c, @Nullable Bundle b) {
        return inf.inflate(R.layout.player_fragment, c, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle b) {
        surface = v.findViewById(R.id.surface_view);
        ensurePlayer();
    }

    /** PATCH: lazily (re)create the player so playback works after lifecycle changes. */
    private void ensurePlayer() {
        if (player != null) return;
        player = new ExoPlayer.Builder(requireContext())
                .setMediaSourceFactory(new DefaultMediaSourceFactory(new DefaultDataSource.Factory(requireContext())))
                .build();
        player.setVideoSurfaceView(surface);
        player.addListener(new Player.Listener() {
            @Override public void onPlayerError(PlaybackException e) { /* surfaced to UI */ }
        });
    }

    public void play(String url) {
        if (!isAdded()) return; // PATCH: guard against use after detach
        mainHandler.post(() -> {
            if (!isAdded()) return;
            ensurePlayer();
            if (player == null || surface == null) return;
            MediaItem item = MediaItem.fromUri(Uri.parse(url));
            if (url.startsWith("rtsp://")) {
                player.setMediaSource(new RtspMediaSource.Factory().createMediaSource(item));
            } else {
                player.setMediaItem(item);
            }
            player.prepare();
            player.setPlayWhenReady(true);
        });
    }

    public void stop() {
        // PATCH: full stop/release pattern to avoid black screen
        if (player != null) {
            player.stop();
            player.clearVideoSurfaceView(surface);
            player.release();
            player = null;
        }
    }

    @Override public void onStop() {
        super.onStop();
        // PATCH: release on stop to avoid stale surface / black screen; player is rebuilt lazily.
        if (player != null) {
            player.stop();
            player.clearVideoSurfaceView(surface);
            player.release();
            player = null;
        }
    }

    @Override public void onDestroyView() {
        super.onDestroyView();
        if (player != null) {
            player.stop();
            player.clearVideoSurfaceView(surface);
            player.release();
            player = null;
        }
        surface = null;
    }
}
