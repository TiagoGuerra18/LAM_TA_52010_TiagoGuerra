package pt.upt.studenthub;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

/** Displays current campus weather while respecting the Activity lifecycle. */
public class WeatherActivity extends AppCompatActivity {
  private WeatherViewModel viewModel;
  private ConnectivityManager connectivityManager;
  private TextView tvOffline;
  private TextView tvStatus;
  private TextView tvTemperature;
  private TextView tvDescription;
  private TextView tvHumidity;
  private TextView tvWind;
  private View weatherCard;
  private ProgressBar progressLoading;
  private Button btnRefresh;
  private CheckBox cbAutoRefresh;
  private boolean online = true;
  private final BroadcastReceiver airplaneModeReceiver =
      new BroadcastReceiver() {
        @Override
        /** Reports the airplane-mode broadcast. */
        public void onReceive(Context context, Intent intent) {
          boolean enabled = intent.getBooleanExtra("state", false);
          Toast.makeText(
                  context,
                  enabled ? R.string.toast_airplane_on : R.string.toast_airplane_off,
                  Toast.LENGTH_SHORT)
              .show();
        }
      };
  private final ConnectivityManager.NetworkCallback networkCallback =
      new ConnectivityManager.NetworkCallback() {
        @Override
        /** Delivers network availability to the main thread. */
        public void onAvailable(@NonNull Network network) {
          runOnUiThread(() -> setOnline(true));
        }

        @Override
        /** Delivers network loss to the main thread. */
        public void onLost(@NonNull Network network) {
          runOnUiThread(() -> setOnline(false));
        }
      };

  @Override
  // Prepara o ecrã e os observadores.
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_weather);
    if (getSupportActionBar() != null) {
      getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }
    String name = getIntent().getStringExtra(TimetableActivity.EXTRA_STUDENT_NAME);
    if (name == null) {
      name = "";
    }
    TextView tvGreeting = findViewById(R.id.tvGreeting);
    tvGreeting.setText(getString(R.string.weather_title, name));
    tvOffline = findViewById(R.id.tvOffline);
    tvStatus = findViewById(R.id.tvWeatherStatus);
    tvTemperature = findViewById(R.id.tvTemperature);
    tvDescription = findViewById(R.id.tvDescription);
    tvHumidity = findViewById(R.id.tvHumidity);
    tvWind = findViewById(R.id.tvWind);
    weatherCard = findViewById(R.id.weatherCard);
    progressLoading = findViewById(R.id.progressLoading);
    btnRefresh = findViewById(R.id.btnRefresh);
    cbAutoRefresh = findViewById(R.id.cbAutoRefresh);
    connectivityManager = ContextCompat.getSystemService(this, ConnectivityManager.class);
    viewModel = new ViewModelProvider(this).get(WeatherViewModel.class);
    btnRefresh.setOnClickListener(v -> viewModel.load(WeatherApi.currentWeatherUrl()));
    viewModel.getReport().observe(this, this::renderReport);
    viewModel.getStatus().observe(this, this::renderStatus);
    if (viewModel.getStatus().getValue() == WeatherViewModel.Status.IDLE) {
      viewModel.load(WeatherApi.currentWeatherUrl());
    }
  }

  @Override
  // Regista o receiver e a monitorização da rede.
  protected void onStart() {
    super.onStart();
    ContextCompat.registerReceiver(
        this,
        airplaneModeReceiver,
        new IntentFilter(Intent.ACTION_AIRPLANE_MODE_CHANGED),
        ContextCompat.RECEIVER_NOT_EXPORTED);
    setOnline(isOnlineNow());
    connectivityManager.registerDefaultNetworkCallback(networkCallback);
  }

  @Override
  // Remove o receiver e a monitorização da rede.
  protected void onStop() {
    unregisterReceiver(airplaneModeReceiver);
    connectivityManager.unregisterNetworkCallback(networkCallback);
    super.onStop();
  }

  // Verifica se existe uma rede com acesso à Internet.
  private boolean isOnlineNow() {
    Network active = connectivityManager.getActiveNetwork();
    if (active == null) {
      return false;
    }
    NetworkCapabilities caps = connectivityManager.getNetworkCapabilities(active);
    return caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
  }

  // Atualiza o banner e tenta novamente após um erro.
  private void setOnline(boolean nowOnline) {
    boolean cameBack = !online && nowOnline;
    online = nowOnline;
    tvOffline.setVisibility(online ? View.GONE : View.VISIBLE);
    if (cameBack
        && cbAutoRefresh.isChecked()
        && viewModel.getStatus().getValue() == WeatherViewModel.Status.ERROR) {
      Toast.makeText(this, R.string.toast_back_online, Toast.LENGTH_SHORT).show();
      viewModel.load(WeatherApi.currentWeatherUrl());
    }
  }

  // Atualiza o progresso, os botões e a mensagem.
  private void renderStatus(WeatherViewModel.Status s) {
    boolean loading = s == WeatherViewModel.Status.LOADING;
    progressLoading.setVisibility(loading ? View.VISIBLE : View.GONE);
    btnRefresh.setEnabled(!loading);
    switch (s) {
      case IDLE:
        tvStatus.setText(R.string.status_weather_idle);
        break;
      case LOADING:
        tvStatus.setText(R.string.status_loading);
        break;
      case SUCCESS:
        WeatherReport r = viewModel.getReport().getValue();
        tvStatus.setText(getString(R.string.status_success, r == null ? "" : r.hour()));
        break;
      case ERROR:
        tvStatus.setText(errorMessage(viewModel.getError().getValue()));
        break;
    }
    invalidateOptionsMenu();
  }

  // Converte o tipo de erro numa mensagem.
  private String errorMessage(WeatherViewModel.ErrorType type) {
    if (type == null) {
      return getString(R.string.err_no_connection);
    }
    switch (type) {
      case TIMEOUT:
        return getString(R.string.err_timeout);
      case HTTP:
        return getString(R.string.err_http, viewModel.getLastHttpCode());
      case PARSE:
        return getString(R.string.err_parse);
      default:
        return getString(R.string.err_no_connection);
    }
  }

  // Mostra os valores e anima o cartão.
  private void renderReport(WeatherReport r) {
    if (r == null) {
      weatherCard.setVisibility(View.GONE);
      return;
    }
    tvTemperature.setText(getString(R.string.temp_value, r.temperature));
    tvDescription.setText(WeatherCodes.describe(r.weatherCode));
    tvHumidity.setText(getString(R.string.humidity_value, r.humidity));
    tvWind.setText(getString(R.string.wind_value, r.windSpeed));
    weatherCard.setVisibility(View.VISIBLE);
    weatherCard.startAnimation(AnimationUtils.loadAnimation(this, R.anim.item_enter));
  }

  @Override
  // Cria o menu do ecrã.
  /** Creates the campus weather menu. */
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.menu_weather, menu);
    return true;
  }

  @Override
  // Desativa as ações durante o carregamento.
  /** Disables request commands during loading. */
  public boolean onPrepareOptionsMenu(Menu menu) {
    boolean loading = viewModel.getStatus().getValue() == WeatherViewModel.Status.LOADING;
    menu.findItem(R.id.action_refresh).setEnabled(!loading);
    menu.findItem(R.id.action_server_error).setEnabled(!loading);
    return super.onPrepareOptionsMenu(menu);
  }

  @Override
  // Executa a opção selecionada.
  /** Handles campus refresh, HTTP error simulation and About. */
  public boolean onOptionsItemSelected(@NonNull MenuItem item) {
    int id = item.getItemId();
    if (id == R.id.action_refresh) {
      viewModel.load(WeatherApi.currentWeatherUrl());
      return true;
    } else if (id == R.id.action_server_error) {
      viewModel.load(WeatherApi.brokenUrl());
      return true;
    } else if (id == R.id.action_about) {
      AboutDialog.show(this);
      return true;
    }
    return super.onOptionsItemSelected(item);
  }

}
