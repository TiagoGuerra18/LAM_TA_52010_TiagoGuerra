package pt.upt.studenthub;

import android.content.Intent;
import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioGroup;
import pt.upt.studenthub.ExplorerModels.Settings;

/** Validates preferences locally and returns a result only when all fields are valid. */
public class SettingsActivity extends ExplorerScreen {
    @Override protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        setContentView(R.layout.activity_settings);
        Settings initial = ExplorerContract.settings(getIntent().getExtras());
        if (saved == null) {
            ((RadioGroup) findViewById(R.id.settingsUnits)).check(initial.fahrenheit ? R.id.unitF : R.id.unitC);
            ((RadioGroup) findViewById(R.id.settingsDays)).check(initial.days == 3 ? R.id.days3
                    : initial.days == 5 ? R.id.days5 : R.id.days7);
            ((CheckBox) findViewById(R.id.settingsHighlight)).setChecked(initial.highlightRain);
            ((EditText) findViewById(R.id.settingsThreshold)).setText(String.valueOf(initial.rainThreshold));
            ((EditText) findViewById(R.id.settingsInterval)).setText(String.valueOf(initial.refreshSeconds));
        }
        findViewById(R.id.settingsSave).setOnClickListener(v -> save());
        findViewById(R.id.settingsCancel).setOnClickListener(v -> finish());
    }

    private void save() {
        EditText threshold = findViewById(R.id.settingsThreshold);
        EditText interval = findViewById(R.id.settingsInterval);
        Integer rain = integer(threshold, 0, 100, R.string.ex_invalid_threshold);
        Integer seconds = integer(interval, 15, 3600, R.string.ex_invalid_interval);
        if (rain == null || seconds == null) return;
        int dayChoice = ((RadioGroup) findViewById(R.id.settingsDays)).getCheckedRadioButtonId();
        int days = dayChoice == R.id.days3 ? 3 : dayChoice == R.id.days5 ? 5 : 7;
        Settings settings = new Settings(((RadioGroup) findViewById(R.id.settingsUnits))
                .getCheckedRadioButtonId() == R.id.unitF, days, rain, seconds,
                ((CheckBox) findViewById(R.id.settingsHighlight)).isChecked());
        Bundle extras = new Bundle();
        ExplorerContract.write(extras, ExplorerContract.place(getIntent().getExtras()), settings);
        setResult(RESULT_OK, new Intent().putExtras(extras));
        finish();
    }

    private Integer integer(EditText field, int minimum, int maximum, int error) {
        field.setError(null);
        try {
            int value = Integer.parseInt(field.getText().toString().trim());
            if (value >= minimum && value <= maximum) return value;
        } catch (NumberFormatException ignored) {
            // Empty, fractional or overflowing values share the field-specific explanation below.
        }
        field.setError(getString(error));
        field.requestFocus();
        return null;
    }
}
