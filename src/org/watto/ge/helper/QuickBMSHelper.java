/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2020 wattostudios
 *
 * License Information:
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License
 * published by the Free Software Foundation; either version 2 of the License, or (at your option) any later versions. This
 * program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranties
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License at http://www.gnu.org for more
 * details. For further information on this application, refer to the authors' website.
 */

package org.watto.ge.helper;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.watto.ErrorLogger;
import org.watto.Language;
import org.watto.Settings;
import org.watto.SingletonManager;
import org.watto.io.FileManipulator;
import org.watto.task.Task;
import org.watto.task.TaskProgressManager;
import org.watto.task.Task_Popup_QuickBMSDownloader;

/**
**********************************************************************************************
Helper methods for conversing with QuickBMS
**********************************************************************************************
**/
public class QuickBMSHelper {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public QuickBMSHelper() {
  }

  /**
  **********************************************************************************************
  Gets the path to the external library or executable
  **********************************************************************************************
  **/
  public static String getExternalLibraryPath() {
    try {

      String quickbmsPath = Settings.getString("QuickBMS_Path");

      File quickbmsFile = new File(quickbmsPath);

      if (quickbmsFile.exists() && quickbmsFile.isDirectory()) {
        // Path is a directory, append the filename to it
        quickbmsPath = quickbmsPath + File.separatorChar + "quickbms.exe";
        quickbmsFile = new File(quickbmsPath);
      }

      if (!quickbmsFile.exists()) {
        // quickbms path is invalid
        ErrorLogger.log("[QuickBMSHelper] QuickBMS can't be found at the path " + quickbmsFile.getAbsolutePath());
        return null;
      }

      quickbmsPath = quickbmsFile.getAbsolutePath();
      return quickbmsPath;
    }
    catch (Throwable t) {
      ErrorLogger.log(t);
      return null;
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public static boolean isExternalLibraryAvailable() {

    String libPath = getExternalLibraryPath();

    if (libPath == null) {
      // Show a popup saying "QuickBMS is missing", AFTER the archive has been opened - ie when trying to preview or export an actual file.
      // See Task_PreviewFile.redo() and SidePanel_DirectoryList.exportFiles() for the implementation of this.
      SingletonManager.set("ShowMessageBeforeExport", "QuickBMSMissing");
    }

    return (libPath != null);
  }

  /**
  **********************************************************************************************
  Gets the path to QuickBMS. If it doesnt exist, it will show a popup asking to download. 
  @return the path to quickbms (if it was found and/or downloaded), or null otherwise
  **********************************************************************************************
  **/
  public static String checkAndShowPopup() {
    String quickbmsPath = getExternalLibraryPath();
    if (quickbmsPath == null) {
      // QuickBMS wasn't found - try to download it
      QuickBMSHelper.showMissingPopup();

      // now check again - if it was downloaded successfully, it should exist now...
      quickbmsPath = QuickBMSHelper.getExternalLibraryPath();
      if (quickbmsPath == null) {
        // not downloaded, or some other error
        return null;
      }
    }
    return quickbmsPath;
  }

  /**
  **********************************************************************************************
  BECAUSE SOME SCRIPT FILES ARE TOO LARGE (more than 1024 variables), PARTICULARLY WHEN WE NEED
  TO USE BULK EXPORTING WHERE EACH FILE IS COMPRESSED IN SMALL BLOCKS, WE ACTUALLY HAVE TO TRICK
  QuickBMS by writing a real script, and then creating this "processing" script which analyses 
  each of the script lines and calls the appropriate function. That's what this function does -
  it generates the "processing" script.
  
  *** INPUT SCRIPT MUST HAVE A SPACE CHARACTER AT THE END OF EACH LINE! ***
  
  @return the processing script
  **********************************************************************************************
  **/
  public static String buildProcessingScript(File scriptFileToProcess) {

    String script = "open \".\" \"" + scriptFileToProcess.getAbsolutePath() + "\" 1\n";
    script += "\n";
    script += "get ARCSIZE ASIZE 1\n";
    script += "set DELIMITER_BYTE long 0x20\n";
    script += "\n";
    script += "do\n";
    script += "  getct PROPERTY STRING DELIMITER_BYTE 1\n";
    script += "  if PROPERTY = append\n";
    script += "    get DUMMYLINE LINE 1\n";
    script += "    \n";
    script += "    append\n";
    script += "    \n";
    script += "  elif PROPERTY = set\n";
    script += "    getct DUMMY STRING DELIMITER_BYTE 1\n";
    script += "    getct INNAME STRING DELIMITER_BYTE 1\n";
    script += "    get DUMMYLINE LINE 1\n";
    script += "    \n";
    script += "    set NAME INNAME\n";
    script += "    \n";
    script += "  elif PROPERTY = comtype\n";
    script += "    getct INCOMTYPE STRING DELIMITER_BYTE 1\n";
    script += "    get DUMMYLINE LINE 1\n";
    script += "    \n";
    // TODO will need to add other compression types here, if they're going to be in the script
    script += "    if INCOMTYPE = LZ2K\n";
    script += "      comtype LZ2K\n";
    script += "    elif INCOMTYPE = DFLT\n";
    script += "      comtype DFLT\n";
    script += "    elif INCOMTYPE = JCALG\n";
    script += "      comtype JCALG\n";
    script += "    elif INCOMTYPE = RFPK\n";
    script += "      comtype RFPK\n";
    script += "    elif INCOMTYPE = RNC\n";
    script += "      comtype RNC\n";
    script += "    elif INCOMTYPE = ZIPX\n";
    script += "      comtype deflate\n";
    //script += "      encryption rc4 TMP \"\" 0 4\n"; // LEGO Compression of TXT files
    script += "    elif INCOMTYPE = LZMA\n";
    script += "      comtype LZMA\n";
    script += "    elif INCOMTYPE = ZLIB\n";
    script += "      comtype ZLIB\n";
    //script += "    elif INCOMTYPE = LZ77EA_970\n";
    //script += "      comtype LZ77EA_970\n";
    script += "    endif\n";
    script += "    \n";
    script += "  elif PROPERTY = clog\n";
    script += "    getct DUMMY STRING DELIMITER_BYTE 1\n";
    script += "    getct INOFFSET STRING DELIMITER_BYTE 1\n";
    script += "    getct INLENGTH STRING DELIMITER_BYTE 1\n";
    script += "    getct INDECOMPLENGTH STRING DELIMITER_BYTE 1\n";
    script += "    get DUMMYLINE LINE 1\n";
    script += "\n";
    script += "    clog NAME INOFFSET INLENGTH INDECOMPLENGTH\n";
    script += "\n";
    script += "  elif PROPERTY = log\n";
    script += "    getct DUMMY STRING DELIMITER_BYTE 1\n";
    script += "    getct INOFFSET STRING DELIMITER_BYTE 1\n";
    script += "    getct INLENGTH STRING DELIMITER_BYTE 1\n";
    script += "    get DUMMYLINE LINE 1\n";
    script += "\n";
    script += "    log NAME INOFFSET INLENGTH\n";
    script += "  else\n";
    script += "    get DUMMYLINE LINE 1\n";
    script += "  ENDIF\n";
    script += "savepos OFFSET 1\n";
    script += "while OFFSET < ARCSIZE\n";

    return script;

  }

  /**
  **********************************************************************************************
  Shows a popup saying that QuickBMS is missing, and asking whether to download it
  **********************************************************************************************
  **/
  public static void showMissingPopup() {
    /*
    // v3.10 if a QuickBMS script has been loaded, check that QuickBMS can be found. 
    // If not, show an error message to the user, and open the help information to the right place
    ((WSSidePanelHolder) ComponentRepository.get("SidePanelHolder")).loadPanel("SidePanel_Help");
    ((SidePanel_Help) WSPluginManager.getGroup("SidePanel").getPlugin("SidePanel_Help")).loadFile(new File("help/ExternalSoftware.html"));
    */

    Task_Popup_QuickBMSDownloader popupTask = new Task_Popup_QuickBMSDownloader("QuickBMSMissing", true);
    popupTask.setDirection(Task.DIRECTION_REDO);
    popupTask.redo();
  }

  /**
  **********************************************************************************************
  Downloads QuickBMS and extracts it into "external_bins\quickbms"
  **********************************************************************************************
  **/
  public static boolean downloadFromWebsite() {
    try {

      // Download the file
      URL url = new URL("http://aluigi.altervista.org/papers/quickbms.zip");
      File tempDownloadFile = new File(Settings.getString("TempDirectory") + File.separatorChar + "quickbms.zip");

      if (!tempDownloadFile.exists()) {

        TaskProgressManager.show(1, 0, Language.get("Progress_Downloading"));
        TaskProgressManager.setIndeterminate(true);
        TaskProgressManager.startTask();

        //URLConnection urlConnection = url.openConnection();
        //InputStream urlStream = urlConnection.getInputStream();
        //long downloadSize = urlConnection.getContentLengthLong();
        InputStream urlStream = url.openStream();
        ReadableByteChannel readableByteChannel = Channels.newChannel(urlStream);

        FileOutputStream fileOutputStream = new FileOutputStream(tempDownloadFile);
        FileChannel fileChannel = fileOutputStream.getChannel();
        fileChannel.transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
        fileOutputStream.close();

        TaskProgressManager.stopTask();
      }

      if (!tempDownloadFile.exists()) {
        return false;
      }

      // Extract it
      String quickBMSPath = Settings.getString("QuickBMS_Path");
      if (quickBMSPath == null || quickBMSPath.length() <= 0) {
        //quickBMSPath = "external_bins\\quickbms";
        quickBMSPath = "external_bins" + File.separatorChar + "quickbms";
        Settings.set("QuickBMS_Path", quickBMSPath);
      }

      File outputPath = new File(quickBMSPath + File.separatorChar + "quickbms.exe");
      if (!outputPath.exists()) {

        ZipFile zipFile = new ZipFile(tempDownloadFile);
        Enumeration<? extends ZipEntry> files = zipFile.entries();

        while (files.hasMoreElements()) {
          ZipEntry entry = files.nextElement();

          String name = entry.getName();
          if (name.equals("quickbms.exe")) {
            // found the file

            BufferedInputStream source = new BufferedInputStream(zipFile.getInputStream(entry));
            long length = entry.getSize();

            FileManipulator destination = new FileManipulator(outputPath, true);
            outputPath = destination.getFile();
            for (int i = 0; i < length; i++) {
              destination.writeByte(source.read());
            }
            destination.close();

            break;
          }
        }

        zipFile.close();
      }

      // Check that it's accessible now
      if (!outputPath.exists()) {
        return false;
      }

      return true;
    }
    catch (Throwable t) {
      return false;
    }
  }

}