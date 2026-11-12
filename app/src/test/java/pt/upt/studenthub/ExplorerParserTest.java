package pt.upt.studenthub;

import static org.junit.Assert.*;
import org.json.JSONException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/** Exercises real org.json parsing with representative V1 payloads. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34, manifest = Config.NONE)
public class ExplorerParserTest {
    static final String DAILY = "{\"daily\":{\"time\":[\"2026-11-12\"],"
            + "\"temperature_2m_max\":[20],\"temperature_2m_min\":[10],"
            + "\"precipitation_probability_max\":[70],\"weather_code\":[61]}}";
    private static final String HOURLY = "{\"hourly\":{\"time\":[\"2026-11-12T09:00\"],"
            + "\"temperature_2m\":[12.5],\"precipitation_probability\":[80],\"weather_code\":[3]}}";

    @Test public void dailyAlignsMeasurementsByIndex() throws Exception {
        ExplorerModels.DayForecast day = ExplorerParser.daily(DAILY).get(0);
        assertEquals("2026-11-12", day.date);
        assertEquals(20, day.maximum, 0.001);
        assertEquals(10, day.minimum, 0.001);
        assertEquals(70, day.rain, 0.001);
        assertEquals(61, day.weatherCode);
    }
    @Test public void hourlyPreservesLocalHourAndFractions() throws Exception {
        ExplorerModels.HourForecast hour = ExplorerParser.hourly(HOURLY).get(0);
        assertEquals("2026-11-12T09:00", hour.time);
        assertEquals(12.5, hour.temperature, 0.001);
    }
    @Test public void nullMeasurementsRemainUnavailable() throws Exception {
        ExplorerModels.DayForecast day = ExplorerParser.daily(DAILY.replace("[20]", "[null]")
                .replace("[61]", "[null]")).get(0);
        assertNull(day.maximum);
        assertEquals(-1, day.weatherCode);
    }
    @Test public void emptyArraysAreValidEmptyResult() throws Exception {
        assertTrue(ExplorerParser.daily(DAILY.replace("[\"2026-11-12\"]", "[]")
                .replace("[20]", "[]").replace("[10]", "[]")
                .replace("[70]", "[]").replace("[61]", "[]")).isEmpty());
    }
    @Test(expected = JSONException.class) public void malformedJsonIsRejected() throws Exception {
        ExplorerParser.daily("not json");
    }
    @Test(expected = JSONException.class) public void missingSectionIsRejected() throws Exception {
        ExplorerParser.daily("{}");
    }
    @Test(expected = JSONException.class) public void apiErrorIsRejected() throws Exception {
        ExplorerParser.daily("{\"error\":true,\"reason\":\"invalid request\"}");
    }
    @Test(expected = JSONException.class) public void mismatchedArraysAreRejected() throws Exception {
        ExplorerParser.daily(DAILY.replace("[20]", "[]"));
    }
    @Test(expected = JSONException.class) public void invalidCalendarDateIsRejected() throws Exception {
        ExplorerParser.daily(DAILY.replace("2026-11-12", "2026-02-30"));
    }
    @Test(expected = JSONException.class) public void nonNumericValuesAreRejected() throws Exception {
        ExplorerParser.daily(DAILY.replace("[20]", "[\"warm\"]"));
    }
    @Test(expected = JSONException.class) public void invalidProbabilityIsRejected() throws Exception {
        ExplorerParser.hourly(HOURLY.replace("[80]", "[101]"));
    }
    @Test(expected = UnsupportedOperationException.class) public void outputCannotBeModified() throws Exception {
        ExplorerParser.daily(DAILY).clear();
    }
}
