package dev.projeto5369.nativeplayer;
import android.net.Uri;
import androidx.annotation.Nullable;
import java.util.List;
import androidx.media3.exoplayer.dash.manifest.DashManifest;
import androidx.media3.exoplayer.dash.manifest.DashManifestParser;
/** Adapted from NewTube v1.17.1 Media3SourceFactory, MIT; see bundled notices. VOD only. */
@androidx.annotation.OptIn(markerClass = androidx.media3.common.util.UnstableApi.class)
final class NativeDashParser extends DashManifestParser {
        @Override
        protected DashManifest buildMediaPresentationDescription(
                long availabilityStartTime,
                long durationMs,
                long minBufferTimeMs,
                boolean dynamic,
                long minUpdateTimeMs,
                long timeShiftBufferDepthMs,
                long suggestedPresentationDelayMs,
                long publishTimeMs,
                @Nullable androidx.media3.exoplayer.dash.manifest.ProgramInformation programInformation,
                @Nullable androidx.media3.exoplayer.dash.manifest.UtcTimingElement utcTiming,
                @Nullable androidx.media3.exoplayer.dash.manifest.ServiceDescriptionElement serviceDescription,
                @Nullable Uri location,
                List<androidx.media3.exoplayer.dash.manifest.Period> periods) {
            return super.buildMediaPresentationDescription(
                    availabilityStartTime,
                    durationMs,
                    minBufferTimeMs,
                    /* dynamic= */ false,
                    minUpdateTimeMs,
                    timeShiftBufferDepthMs,
                    suggestedPresentationDelayMs,
                    publishTimeMs,
                    programInformation,
                    utcTiming,
                    serviceDescription,
                    location,
                    periods);
        }
}
