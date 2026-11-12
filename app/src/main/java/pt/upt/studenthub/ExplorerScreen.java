package pt.upt.studenthub;

import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;

/** Shared Up navigation, transitions, debug policy and lifecycle logging for Explorer screens. */
public abstract class ExplorerScreen extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        log("onCreate");
        if ((getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                    .detectNetwork().penaltyLog().penaltyDeathOnNetwork().build());
        }
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        overridePendingTransition(R.anim.ex_screen_enter, R.anim.ex_fade_out);
    }

    @Override
    protected void onStart() { super.onStart(); log("onStart"); }
    @Override
    protected void onResume() { super.onResume(); log("onResume"); }
    @Override
    protected void onPause() { log("onPause"); super.onPause(); }
    @Override
    protected void onStop() { log("onStop"); super.onStop(); }
    @Override
    protected void onDestroy() { log("onDestroy"); super.onDestroy(); }

    /** Up returns to the existing parent, retaining its ViewModel and name field. */
    @Override
    public boolean onSupportNavigateUp() { finish(); return true; }

    /** Applies the exit animation for both Up and the system Back action. */
    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.ex_fade_in, R.anim.ex_screen_exit);
    }

    private void log(String event) { Log.d("ExplorerLifecycle", getClass().getSimpleName() + " " + event); }
}
