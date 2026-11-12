package pt.upt.studenthub;

import android.content.Context;
import androidx.appcompat.app.AlertDialog;

// Classe partilhada pelas duas Activities para mostrar a janela "Sobre".

public final class AboutDialog {

  private AboutDialog() {}

  // O parâmetro context recebe a Activity onde a janela será apresentada.

  public static void show(Context context) {

    // Cria e configura a janela de diálogo.

    new AlertDialog.Builder(context)

        // Define o título usando o texto guardado em strings.xml.

        .setTitle(R.string.about_title)

        // Define a mensagem usando o texto guardado em strings.xml.

        .setMessage(R.string.about_message)

        // Adiciona o botão "OK".

        // null significa que não há uma ação adicional:

        // ao tocar no botão, a janela fecha automaticamente.

        .setPositiveButton(R.string.ok, null)

        // Mostra a janela no ecrã.

        .show();
  }
}
