////////////////////////////////////////////////////////////////////////////////////////////////
//                                                                                            //
//                                       WATTO STUDIOS                                        //
//                             Java Code, Programs, and Software                              //
//                                    http://www.watto.org                                    //
//                                                                                            //
//                           Copyright (C) 2004-2010  WATTO Studios                           //
//                                                                                            //
// This program is free software; you can redistribute it and/or modify it under the terms of //
// the GNU General Public License published by the Free Software Foundation; either version 2 //
// of the License, or (at your option) any later versions. This program is distributed in the //
// hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranties //
// of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License //
// at http://www.gnu.org for more details. For updates and information about this program, go //
// to the WATTO Studios website at http://www.watto.org or email watto@watto.org . Thanks! :) //
//                                                                                            //
////////////////////////////////////////////////////////////////////////////////////////////////

package org.watto;

import java.io.File;
import java.io.FilenameFilter;
import org.watto.io.FileExtensionFilter;
import org.watto.io.FileManipulator;

/***********************************************************************************************
A singleton class that logs errors to the command prompt and an external file. Clears all logs
except for the 5 most recent files.
***********************************************************************************************/
public final class ErrorLogger {

  /**
   The level of debugging to provide<br>
   0 = No logging<br>
   1 = All messages<br>
   2 = All messages, and stack traces for <code>Throwable</code><br>
   3 = All messages, and stack traces for all messages and <code>Throwable</code>
    **/
  static int debugLevel = 2;

  /** The file to which the errors are written **/
  static FileManipulator errorLog;

  /***********************************************************************************************
  Deletes any old logs from the <code>logDirectory</code>. Only deletes the oldest logs with
  extension *.log if there are more than <code>numToKeep</code> logs in the directory.
  @param logDirectory the directory that contains the log files
  @param numToKeep the maximum number of logs to keep in the directory
  ***********************************************************************************************/
  public static void clearLogs(File logDirectory, int numToKeep) {
    if (!logDirectory.exists()) {
      return;
    }

    File[] logs = logDirectory.listFiles((FilenameFilter) new FileExtensionFilter("log"));

    if (logs == null || logs.length < numToKeep) {
      return;
    }

    int logCount = logs.length;
    long[][] logDates = new long[logCount][2];

    // get the last modified dates for the log files
    for (int i = 0; i < logCount; i++) {
      logDates[i] = new long[] { i, logs[i].lastModified() };
    }

    // sort the files by their last modification date
    for (int i = 0; i < logCount; i++) {
      for (int j = i + 1; j < logCount; j++) {
        if (logDates[i][1] > logDates[j][1]) {
          long[] temp = logDates[i];
          logDates[i] = logDates[j];
          logDates[j] = temp;
        }
      }
    }

    // remove any extra log files
    int numToRemove = logCount - numToKeep;
    for (int i = 0; i < numToRemove; i++) {
      logs[(int) logDates[i][0]].delete();
    }

  }

  /***********************************************************************************************
  Closes the current log file
  ***********************************************************************************************/
  public static void closeLog() {
    try {
      if (errorLog != null && errorLog.isOpen()) {
        errorLog.close();
      }
    }
    catch (Throwable t) {
    }
  }

  /***********************************************************************************************
  Gets the level of debugging<br>
   0 = No logging<br>
   1 = Write all messages to the Log File only<br>
   2 = Write all messages to the Log File and the Command Prompt<br>
   3 = Write all messages and a stack trace to the Log File<br>
   4 = Write all messages and a stack trace to the Log File and the Command Prompt<br>
  @return the debugging level
  ***********************************************************************************************/
  public static int getDebugLevel() {
    return debugLevel;
  }

  /***********************************************************************************************
  Records an information <code>message</code> in the log
  @param message the information message
  ***********************************************************************************************/
  public static void log(String message) {
    log("INFO", message);
  }

  /***********************************************************************************************
  Records an information <code>message</code> in the log, with a <code>heading</code>
  @param heading the heading for the message
  @param message the information message
  ***********************************************************************************************/
  public static void log(String heading, String message) {
    try {

      // command prompt
      if (debugLevel == 1 || debugLevel == 2) {
        System.out.println(heading + ": " + message);
      }
      else if (debugLevel == 3) {
        System.out.println(heading + ": " + message);
        // trigger a dummy exception at this spot, to generate a stack trace
        try {
          throw new Exception();
        }
        catch (Throwable t) {
          t.printStackTrace();
        }
      }

      // Check that the errorLog is open!!!
      if (errorLog == null || !errorLog.isOpen()) {
        return;
      }

      // error log
      errorLog.writeLine(heading + ": " + message);

      if (debugLevel == 3) {
        // trigger a dummy exception at this spot, to generate a stack trace
        try {
          throw new Exception();
        }
        catch (Throwable t) {
          StackTraceElement[] errorStack = t.getStackTrace();
          for (int i = 0; i < errorStack.length; i++) {
            errorLog.writeLine(errorStack[i].toString());
          }
        }
      }

      //errorLog.writeLine("===== END OF ERROR =====");
    }
    catch (Throwable t) {
      // Hopefully this never happens :)
    }
  }

  /***********************************************************************************************
  Records a <code>Throwable</code> error in the log, with a <code>heading</code>
  @param heading the heading for the message
  @param message the <code>Throwable</code> error
  ***********************************************************************************************/
  public static void log(String heading, Throwable message) {
    try {

      // command prompt
      if (debugLevel == 1) {
        System.out.println(heading + ": " + message);
      }
      else if (debugLevel == 2 || debugLevel == 3) {
        System.out.println(heading + ": " + message);
        message.printStackTrace();
      }

      // Check that the errorLog is open!!!
      if (errorLog == null || !errorLog.isOpen()) {
        return;
      }

      // error log
      errorLog.writeLine(heading + ": " + message);

      if (debugLevel == 2 || debugLevel == 3) {
        StackTraceElement[] errorStack = message.getStackTrace();
        for (int i = 0; i < errorStack.length; i++) {
          errorLog.writeLine(errorStack[i].toString());
        }
      }

      errorLog.writeLine("===== END OF ERROR =====");
    }
    catch (Throwable t) {
      // Hopefully this never happens :)
    }
  }

  /***********************************************************************************************
  Records a <code>Throwable</code> error in the log
  @param message the <code>Throwable</code> error
  ***********************************************************************************************/
  public static void log(Throwable message) {
    log("ERROR", message);
  }

  /***********************************************************************************************
  Opens a log file for writing
  @param logFile the file to open
  ***********************************************************************************************/
  public static void openLog(File logFile) {
    if (errorLog != null && errorLog.isOpen()) {
      errorLog.close();
    }
    errorLog = new FileManipulator(logFile, true);
  }

  /***********************************************************************************************
  Sets the level of debugging<br>
   0 = No logging<br>
   1 = Write all messages to the Log File only<br>
   2 = Write all messages to the Log File and the Command Prompt<br>
   3 = Write all messages and a stack trace to the Log File<br>
   4 = Write all messages and a stack trace to the Log File and the Command Prompt<br>
  param level the debugging level
  ***********************************************************************************************/
  public static void setDebugLevel(int level) {
    if (level > 4) {
      level = 4;
    }
    else if (level < 0) {
      level = 0;
    }

    debugLevel = level;
  }

  /***********************************************************************************************
  Opens a new log in the <i>logs</i> directory
  ***********************************************************************************************/
  public ErrorLogger() {
    this(new File("logs"));
  }

  /***********************************************************************************************
  Opens a new log in the <code>logDirectory</code>. If there are more than 5 logs in this
  directory, the old logs are removed.
  @param logDirectory the directory to store the log files
  ***********************************************************************************************/
  public ErrorLogger(File logDirectory) {
    clearLogs(logDirectory, 5);
    File logFile = new File(logDirectory.getAbsolutePath() + java.io.File.separator + "errors-" + java.util.Calendar.getInstance().getTime().toString().replaceAll(":", "_") + ".log");
    openLog(logFile);
  }
}