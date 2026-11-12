package pt.upt.studenthub;

import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;
import android.app.Activity;
import android.widget.EditText;
import android.widget.RadioGroup;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;

/** Verifies validation and the Activity result protocol rather than a duplicate validation function. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class SettingsActivityTest {
    @Test public void invalidValuesStayOnScreenWithoutAResult() {
        try (ActivityController<SettingsActivity> controller = Robolectric.buildActivity(SettingsActivity.class).setup()) {
            SettingsActivity activity = controller.get();
            EditText threshold = activity.findViewById(R.id.settingsThreshold);
            EditText interval = activity.findViewById(R.id.settingsInterval);
            threshold.setText("101");
            interval.setText("0");
            activity.findViewById(R.id.settingsSave).performClick();
            assertNotNull(threshold.getError());
            assertNotNull(interval.getError());
            assertFalse(activity.isFinishing());
            assertEquals(Activity.RESULT_CANCELED, shadowOf(activity).getResultCode());
        }
    }
    @Test public void validValuesAreReturnedAsAResult() {
        try (ActivityController<SettingsActivity> controller = Robolectric.buildActivity(SettingsActivity.class).setup()) {
            SettingsActivity activity = controller.get();
            ((RadioGroup) activity.findViewById(R.id.settingsUnits)).check(R.id.unitF);
            ((RadioGroup) activity.findViewById(R.id.settingsDays)).check(R.id.days3);
            ((EditText) activity.findViewById(R.id.settingsThreshold)).setText("25");
            ((EditText) activity.findViewById(R.id.settingsInterval)).setText("120");
            activity.findViewById(R.id.settingsSave).performClick();
            assertEquals(Activity.RESULT_OK, shadowOf(activity).getResultCode());
            ExplorerModels.Settings result = ExplorerContract.settings(shadowOf(activity).getResultIntent().getExtras());
            assertTrue(result.fahrenheit);
            assertEquals(3, result.days);
            assertEquals(25, result.rainThreshold);
            assertEquals(120, result.refreshSeconds);
        }
    }
    @Test public void unsavedFormSurvivesRecreation() {
        try (ActivityController<SettingsActivity> controller = Robolectric.buildActivity(SettingsActivity.class).setup()) {
            ((EditText) controller.get().findViewById(R.id.settingsThreshold)).setText("42");
            controller.recreate();
            assertEquals("42", ((EditText) controller.get().findViewById(R.id.settingsThreshold)).getText().toString());
        }
    }
}
