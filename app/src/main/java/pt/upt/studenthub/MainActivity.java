package pt.upt.studenthub;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import android.os.StrictMode;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
  private EditText etName;

  @Override
  // Prepara o ecrã e os observadores.
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    enableStrictModeInDebug();
    setContentView(R.layout.activity_main);
    etName = findViewById(R.id.etName);
    Button btnOpenTimetable = findViewById(R.id.btnOpenTimetable);
    Button btnOpenWeather = findViewById(R.id.btnOpenWeather);
    btnOpenTimetable.setOnClickListener(v -> openWith(TimetableActivity.class));
    btnOpenWeather.setOnClickListener(v -> openWith(WeatherActivity.class));
    findViewById(R.id.btnOpenExplorer).setOnClickListener(v -> openWith(ExplorerActivity.class));
  }

  // Valida o nome e abre o ecrã escolhido.
  private void openWith(Class<?> target) {
    String name = readValidName();
    if (name == null) {
      return;
    }
    Intent intent = new Intent(this, target);
    intent.putExtra(TimetableActivity.EXTRA_STUDENT_NAME, name);
    startActivity(intent);
  }

  @Nullable
  // Devolve o nome válido ou assinala o erro.
  private String readValidName() {
    String name = etName.getText().toString().trim();
    if (name.isEmpty()) {
      etName.setError(getString(R.string.error_name_required));
      return null;
    }
    return name;
  }

  // Ativa a deteção de operações lentas em debug.
  private void enableStrictModeInDebug() {
    boolean debuggable = (getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
    if (debuggable) {
      StrictMode.setThreadPolicy(
          new StrictMode.ThreadPolicy.Builder()
              .detectDiskReads()
              .detectDiskWrites()
              .detectNetwork()
              .detectCustomSlowCalls()
              .penaltyLog()
              .penaltyDeathOnNetwork()
              .build());
    }
  }

  @Override
  // Cria o menu do ecrã.
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.menu_main, menu);
    return true;
  }

  @Override
  // Executa a opção selecionada.
  public boolean onOptionsItemSelected(@NonNull MenuItem item) {
    if (item.getItemId() == R.id.action_about) {
      AboutDialog.show(this);
      return true;
    }
    return super.onOptionsItemSelected(item);
  }
}
