package pt.upt.studenthub;

import android.os.Bundle;
import pt.upt.studenthub.ExplorerModels.Place;
import pt.upt.studenthub.ExplorerModels.Settings;

/** Primitive Intent/Bundle protocol: no Activity or mutable model is passed between screens. */
public final class ExplorerContract {
    public static final String DATE = "explorer.date";
    public static final String FAVORITE = "explorer.favorite";
    public static final String FAVORITE_KEY = "explorer.favorite_key";
    private ExplorerContract() { }

    /** Writes the current location and preferences into an Intent's extras or saved state. */
    public static void write(Bundle bundle, Place place, Settings settings) {
        bundle.putInt("explorer.place", place.ordinal());
        bundle.putBoolean("explorer.fahrenheit", settings.fahrenheit);
        bundle.putInt("explorer.days", settings.days);
        bundle.putInt("explorer.threshold", settings.rainThreshold);
        bundle.putInt("explorer.interval", settings.refreshSeconds);
        bundle.putBoolean("explorer.highlight", settings.highlightRain);
    }

    /** Reads a location with bounds checking. */
    public static Place place(Bundle bundle) {
        return Place.fromIndex(bundle == null ? 0 : bundle.getInt("explorer.place", 0));
    }

    /** Reads and revalidates settings, falling back safely for malformed extras. */
    public static Settings settings(Bundle bundle) {
        if (bundle == null) return Settings.defaults();
        try {
            return new Settings(bundle.getBoolean("explorer.fahrenheit", false),
                    bundle.getInt("explorer.days", 7), bundle.getInt("explorer.threshold", 50),
                    bundle.getInt("explorer.interval", 60), bundle.getBoolean("explorer.highlight", true));
        } catch (IllegalArgumentException exception) { return Settings.defaults(); }
    }
}
