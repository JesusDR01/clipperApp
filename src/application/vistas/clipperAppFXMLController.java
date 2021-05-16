package application.vistas;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import application.PowershellCommand;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;

public class clipperAppFXMLController {

  @FXML
  private TextField shortcutName;
  @FXML
  private TextArea shortcutText;
  @FXML
  private Button makeShortcut;
  @FXML
  private CheckBox rememberPath;
  @FXML
  private Button forgetPath;

  @FXML
  private Label clipperResponse;

  final File TEMP_PATH_FILE = new File(System.getProperty("java.io.tmpdir") + "/cliperAppPath.txt");

  // Event Listener on Button[#makeShortcut].onAction
  @FXML
  public void createShortcut(ActionEvent event) {
    boolean woxIsRunning = checkWoxRunning();
    if (woxIsRunning) {
      File selectedDirectory = getSelectedDirectory(TEMP_PATH_FILE);
      if (selectedDirectory != null) {
        createFiles(selectedDirectory);
        restartWox();
      } else {
        clipperResponse.setText("No has elegido carpeta");
      }
    } else {
      clipperResponse.setText("Wox no se está ejecutando...");
    }
  }

  private void displayWoxIsRunning() {
    boolean woxIsRunning = checkWoxRunning();
    if (!woxIsRunning) {
      clipperResponse.setVisible(true);
      clipperResponse.setText("Wox no se está ejecutando");
    }
  }

  private boolean checkWoxRunning() {
    boolean woxIsRunning = true;
    try {
      String woxPath =
          PowershellCommand.getOutput("Get-Process -Name wox | Select -ExpandProperty path");
      if (woxPath.isEmpty()) {
        woxIsRunning = false;
      }
    } catch (IOException e1) {
      e1.printStackTrace();
    }
    return woxIsRunning;
  }

  private void restartWox() {
    try {
      String woxPath =
          PowershellCommand.getOutput("Get-Process -Name wox | Select -ExpandProperty path");
      if (woxPath.isEmpty()) {
        clipperResponse.setText("Wox no se está ejecutando...");
      } else {
        clipperResponse.setText("Creando archivos necesarios...");
      }
      Runtime.getRuntime().exec("taskkill /F /IM wox.exe");
      PowershellCommand.exec("Start-Process ".concat(woxPath));
    } catch (IOException e1) {
      e1.printStackTrace();
    }
  }

  private File getSelectedDirectory(File tempPathFile) {

    File selectedDirectory;
    if (rememberPath.isSelected()) {
      if (tempPathFile.exists()) {// Load path from file
        selectedDirectory = loadSelectedDirectory(tempPathFile);
      } else { // save path in file
        selectedDirectory = getSelectedDirectoryUsingChooser();
        if (selectedDirectory != null) {
          tempPathFile = new File(System.getProperty("java.io.tmpdir") + "/cliperAppPath.txt");
          createDestinationFile(tempPathFile, selectedDirectory.getAbsolutePath());
        }

      }
    } else {
      selectedDirectory = getSelectedDirectoryUsingChooser();
    }
    return selectedDirectory;
  }

  private File loadSelectedDirectory(File tempPathFile) {
    File selectedDirectory;
    try {
      String path = Files.readString(Paths.get(tempPathFile.getPath()));
      selectedDirectory = new File(path);
    } catch (IOException e) {
      e.printStackTrace();
      selectedDirectory = getSelectedDirectoryUsingChooser();
    }
    return selectedDirectory;
  }

  private void createFiles(File selectedDirectory) {
    createDirectoriesNeeded(selectedDirectory);
    createBat(selectedDirectory);
    createTxt(selectedDirectory);
    createVbs(selectedDirectory);
  }

  private File getSelectedDirectoryUsingChooser() {
    DirectoryChooser directoryChooser = new DirectoryChooser();
    directoryChooser.setInitialDirectory(new File(System.getProperty("user.home") + "/Desktop"));
    File selectedDirectory = directoryChooser.showDialog(makeShortcut.getScene().getWindow());
    return selectedDirectory;
  }

  private void createVbs(File selectedDirectory) {
    File vbsFile = createFile(selectedDirectory, "/clipper/", ".vbs");
    String text =
        "set objshell = createobject(\"wscript.shell\")\r\n " + "objshell.run \"..\\commands\\"
            .concat(shortcutName.getText().concat(".bat\"")).concat(",vbhide");
    createDestinationFile(vbsFile, text);
  }

  private void createTxt(File selectedDirectory) {
    File txtFile = createFile(selectedDirectory, "/fileclips/", ".txt");
    createDestinationFile(txtFile, shortcutText.getText());
  }

  private void createDestinationFile(File txtFile, String text) {
    try (BufferedWriter destinationTxtFile = new BufferedWriter(new FileWriter(txtFile))) {
      destinationTxtFile.write(text);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private void createBat(File selectedDirectory) {
    File batFile = createFile(selectedDirectory, "/commands/", ".bat");
    String text = "clip < \"../fileclips/".concat(shortcutName.getText()).concat(".txt\"");
    createDestinationFile(batFile, text);
  }

  private File createFile(File selectedDirectory, String subdir, String extension) {
    File batFile = new File(selectedDirectory.getPath().concat(subdir)
        .concat(shortcutName.getText().concat(extension)));
    return batFile;
  }

  private void createDirectoriesNeeded(File selectedDirectory) {
    Set<String> directories = listFilesUsingJavaIO(selectedDirectory);
    Set<String> needed_directories =
        new HashSet<String>(Set.of("clipper", "commands", "fileclips"));
    // Get directories that we have
    directories.retainAll(needed_directories);
    // Check directories we need
    needed_directories = needed_directories.stream().filter(dir -> !directories.contains(dir))
        .collect(Collectors.toSet());
    // Create directories needed
    needed_directories.forEach(
        needed -> new File(selectedDirectory.getPath().concat("/").concat(needed)).mkdir());
  }

  public Set<String> listFilesUsingJavaIO(File selectedDirectory) {
    return Stream.of(selectedDirectory.listFiles()).filter(file -> !file.isFile())
        .map(File::getName).collect(Collectors.toSet());
  }

  @FXML
  public void initialize() {
    disableDefaultShortcutNameFocus();
    displayResponseVisibility();
    displayShortcutNameNotEmpty();
    displayWoxIsRunning();
  }

  private void disableDefaultShortcutNameFocus() {
    Platform.runLater(() -> clipperResponse.getParent().requestFocus());
  }

  private void displayResponseVisibility() {
    clipperResponse.textProperty().addListener((prop, oldVal, newVal) -> {
      changeResponseLabelVisibility();
    });
  }

  private void changeResponseLabelVisibility() {
    Task<Void> task = new Task<Void>() {
      @Override
      public Void call() {
        clipperResponse.setVisible(true);
        final int SECONDS_TO_WAIT = 5;
        for (int i = 1; i <= SECONDS_TO_WAIT; i++) {
          try {
            Thread.sleep(1000);
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
          updateProgress(i, SECONDS_TO_WAIT);
        }
        clipperResponse.setVisible(false); // Dissapear after 5 seconds
        if (clipperResponse.getText().equals("Creando archivos necesarios...")) {
          Platform.runLater(new Runnable() {
            @Override
            public void run() {
              clipperResponse.setText("Proceso completado!");
            }
          });
        }
        return null;
      }
    };
    new Thread(task).start();
  }

  private void displayShortcutNameNotEmpty() {
    shortcutName.textProperty().addListener((prop, oldVal, newVal) -> {
      if (shortcutName.getText().isBlank() || shortcutName.getText().isEmpty()) {
        makeShortcut.setDisable(true);
      } else {
        makeShortcut.setDisable(false);
      }
    });
  }

  // Event Listener on Button[#forgetPath].onAction
  @FXML
  void forget(ActionEvent event) {
    Path path = FileSystems.getDefault().getPath(TEMP_PATH_FILE.getAbsolutePath());
    try {
      Files.delete(path);
      clipperResponse.setText("Ruta olvidada");
    } catch (IOException e) {
      clipperResponse.setText("No existía una ruta guardada");
    }
  }

}
