package pt.upt.studenthub;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import java.util.List;

/** Displays the original asynchronous timetable module. */
public class TimetableActivity extends AppCompatActivity {

  // Chave partilhada pelas duas Activities para transportar o nome.
  public static final String EXTRA_STUDENT_NAME = "pt.upt.studenthub.STUDENT_NAME";

  // Chave utilizada para guardar o contador no Bundle.
  private static final String KEY_TAPS = "key_taps";

  private TimetableViewModel viewModel;
  private TextView tvStatus;
  private ProgressBar progressBar;
  private Button btnSincronizar;
  private Button btnCancelar;
  private Button btnTap;
  private LinearLayout sessionsContainer;
  private int taps = 0;

  // Prepara o ecrã do horário e os observadores.
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_timetable);
    if (getSupportActionBar() != null) {
      getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }
    // Recebe o nome enviado pela MainActivity através do Intent.
    String name = getIntent().getStringExtra(EXTRA_STUDENT_NAME);

    // Usa texto vazio caso a Activity seja aberta sem receber um nome.
    if (name == null) {
      name = "";
    }

    TextView tvTitle = findViewById(R.id.tvTitle);

    // Substitui o %1$s da string pelo nome recebido.
    tvTitle.setText(getString(R.string.timetable_title, name));

    tvStatus = findViewById(R.id.tvStatus);
    progressBar = findViewById(R.id.progressBar);
    btnSincronizar = findViewById(R.id.btnSincronizar);
    btnCancelar = findViewById(R.id.btnCancelar);
    btnTap = findViewById(R.id.btnTap);
    sessionsContainer = findViewById(R.id.sessionsContainer);

    // Recupera o contador depois de uma recriação da Activity.
    if (savedInstanceState != null) {
      taps = savedInstanceState.getInt(KEY_TAPS, 0);
    }

    updateTapButton();

    // Obtém a mesma instância do ViewModel depois da rotação.
    viewModel = new ViewModelProvider(this).get(TimetableViewModel.class);

    // Obtém as aulas guardadas no recurso string-arra e pede ao ViewModel para as carregar em
    // segundo plano.
    btnSincronizar.setOnClickListener(
        v -> viewModel.startSync(getResources().getStringArray(R.array.sessions)));

    // Pede o cancelamento da sincronização.
    btnCancelar.setOnClickListener(v -> viewModel.cancelSync());

    // Incrementa o contador e atualiza o texto do botão.
    btnTap.setOnClickListener(
        v -> {
          taps++;
          updateTapButton();
        });

    // Observa o progresso e atualiza a interface na thread principal.
    viewModel
        .getProgress()
        .observe(
            this,
            p -> {
              progressBar.setProgress(p);

              if (viewModel.getState().getValue() == TimetableViewModel.State.RUNNING) {
                tvStatus.setText(getString(R.string.status_running, p));
              }
            });

    // Apresenta as aulas recebidas dentro do ScrollView.
    viewModel.getSessions().observe(this, this::renderSessions);

    // Atualiza os botões e o texto quando o estado muda.
    viewModel.getState().observe(this, this::renderState);
  }

  // Coloca o número de toques no texto do botão.
  private void updateTapButton() {
    btnTap.setText(getString(R.string.btnTap, taps));
  }

  // Atualiza a interface de acordo com o estado da sincronização.
  private void renderState(TimetableViewModel.State s) {
    boolean running = s == TimetableViewModel.State.RUNNING;

    // Sincronizar fica desativado durante a execução.
    // Cancelar fica ativo apenas durante a execução.
    btnSincronizar.setEnabled(!running);
    btnCancelar.setEnabled(running);

    switch (s) {
      case IDLE:
        tvStatus.setText(R.string.status_idle);
        break;

      case RUNNING:
        // Mostra a percentagem atual, incluindo após uma rotação.
        Integer p = viewModel.getProgress().getValue();

        tvStatus.setText(getString(R.string.status_running, p == null ? 0 : p));
        break;

      case DONE:
        // Mostra o total de aulas carregadas.
        List<String> items = viewModel.getSessions().getValue();

        tvStatus.setText(getString(R.string.status_done, items == null ? 0 : items.size()));
        break;

      case CANCELLED:
        tvStatus.setText(R.string.status_cancelled);
        break;
    }

    // Solicita a atualização da opção Limpar no menu.
    invalidateOptionsMenu();
  }

  // Acrescenta apenas as aulas novas, com uma animação de entrada.
  private void renderSessions(List<String> items) {

    // Remove as linhas antigas se a lista foi limpa ou reiniciada.
    if (items.size() < sessionsContainer.getChildCount()) {
      sessionsContainer.removeAllViews();
    }

    // Obtém o espaçamento definido nos recursos.
    int pad = getResources().getDimensionPixelSize(R.dimen.spacing_s);

    for (int i = sessionsContainer.getChildCount(); i < items.size(); i++) {

      // Cria um TextView com o texto da aula.
      TextView tv = new TextView(this);
      tv.setText(items.get(i));

      // Define a aparência, altura mínima e espaçamento da linha.
      tv.setTextAppearance(android.R.style.TextAppearance_Material_Medium);
      tv.setMinHeight(getResources().getDimensionPixelSize(R.dimen.touch_target));
      tv.setPadding(0, pad, 0, pad);

      // Adiciona a aula ao contentor dentro do ScrollView.
      sessionsContainer.addView(tv);

      // Executa a animação XML de aparecimento e deslizamento.
      tv.startAnimation(AnimationUtils.loadAnimation(this, R.anim.item_enter));
    }
  }

  // Guarda o contador antes de a Activity ser recriada.
  @Override
  protected void onSaveInstanceState(@NonNull Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putInt(KEY_TAPS, taps);
  }

  // Cria o menu da barra superior usando menu_timetable.xml.
  @Override
  /** Creates the timetable menu. */
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.menu_timetable, menu);
    return true;
  }

  // Desativa Limpar enquanto está a sincronizar.
  @Override
  /** Disables clearing while synchronization is running. */
  public boolean onPrepareOptionsMenu(Menu menu) {
    MenuItem clear = menu.findItem(R.id.action_clear);

    clear.setEnabled(viewModel.getState().getValue() != TimetableViewModel.State.RUNNING);

    return super.onPrepareOptionsMenu(menu);
  }

  // Executa a ação correspondente à opção selecionada.
  @Override
  /** Handles timetable actions and About. */
  public boolean onOptionsItemSelected(@NonNull MenuItem item) {
    int id = item.getItemId();

    if (id == R.id.action_clear) {

      // Limpa os dados e repõe o estado inicial.
      viewModel.clear();
      return true;

    } else if (id == R.id.action_about) {

      // Abre a janela Sobre através da classe que criámos.
      AboutDialog.show(this);
      return true;
    }

    // Deixa a Activity tratar as restantes opções, incluindo a seta Up.
    return super.onOptionsItemSelected(item);
  }

}
