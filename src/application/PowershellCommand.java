package application;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

public class PowershellCommand {
  
  public static String getOutput(String command) throws IOException {
    command = "powershell.exe ".concat(command);
    // Getting the version
    // Executing the command
    Process powerShellProcess = Runtime.getRuntime().exec(command);
    // Getting the results
    powerShellProcess.getOutputStream().close();
    BufferedReader stdout = new BufferedReader(new InputStreamReader(powerShellProcess.getInputStream()));
    String output = stdout.lines().collect(Collectors.joining("\n"));
    stdout.close();
    return output;
  }
  public static void exec(String command) throws IOException {
    command = "powershell.exe ".concat(command);
    Process powerShellProcess = Runtime.getRuntime().exec(command);
  }
  

}
