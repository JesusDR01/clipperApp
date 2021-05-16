module clipperApp {

  requires transitive javafx.graphics;
  requires javafx.controls;
  requires javafx.fxml;
  requires javafx.base;
  opens application to javafx.fxml;
  opens application.vistas to javafx.fxml;
  exports application;
  exports application.vistas;
}

