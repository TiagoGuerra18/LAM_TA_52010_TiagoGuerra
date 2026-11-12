package pt.upt.studenthub;

import static org.junit.Assert.*;
import android.net.Uri;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import pt.upt.studenthub.ExplorerModels.Place;
import pt.upt.studenthub.ExplorerModels.Settings;

/** Checks externally meaningful query parameters for the V1 API contract. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34, manifest = Config.NONE)
public class ExplorerApiTest {
    @Test public void listUsesConfiguredLocationUnitDaysAndTimezone() {
        Uri uri = Uri.parse(ExplorerApi.daily(Place.LISBON, new Settings(true, 3, 40, 120, true)));
        assertEquals("https", uri.getScheme());
        assertEquals("38.72", uri.getQueryParameter("latitude"));
        assertEquals("-9.14", uri.getQueryParameter("longitude"));
        assertEquals("3", uri.getQueryParameter("forecast_days"));
        assertEquals("fahrenheit", uri.getQueryParameter("temperature_unit"));
        assertEquals("Europe/Lisbon", uri.getQueryParameter("timezone"));
    }
    @Test public void detailUsesSelectedDateInsteadOfAnExampleDate() {
        Uri uri = Uri.parse(ExplorerApi.hourly(Place.FARO, Settings.defaults(), "2026-12-09"));
        assertEquals("2026-12-09", uri.getQueryParameter("start_date"));
        assertEquals("2026-12-09", uri.getQueryParameter("end_date"));
        assertNotNull(uri.getQueryParameter("hourly"));
        assertNull(uri.getQueryParameter("daily"));
    }
    @Test public void comparisonRequestsTodayWithOneDay() {
        Uri uri = Uri.parse(ExplorerApi.comparison(Place.PORTO, Settings.defaults(), false));
        assertEquals("1", uri.getQueryParameter("forecast_days"));
        assertNull(uri.getQueryParameter("start_date"));
    }
}
