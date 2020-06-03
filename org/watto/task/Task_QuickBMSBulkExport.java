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

package org.watto.task;

import java.io.File;
import org.watto.ErrorLogger;
import org.watto.Language;
import org.watto.Settings;
import org.watto.SingletonManager;
import org.watto.datatype.Resource;
import org.watto.ge.helper.QuickBMSHelper;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.BlockQuickBMSExporterWrapper;
import org.watto.ge.plugin.exporter.Exporter_QuickBMSWrapper;
import org.watto.ge.plugin.exporter.Exporter_QuickBMS_Decompression;
import org.watto.io.FileManipulator;
import org.watto.io.FilenameChecker;

/**
**********************************************************************************************
This Task stores a list of Resources, and when run, it will send all these resource names
to QuickBMS to extract in a single bulk run, instead of being done individually. This means 
that we only actually call QuickBMS once, and so the archive is only loaded/processed once.
**********************************************************************************************
**/
public class Task_QuickBMSBulkExport extends AbstractTask {

  /** The direction to perform in the thread **/
  int direction = 1;

  Resource[] resources;

  int numResources = 0;

  File outputDirectory = null;

  /**
  **********************************************************************************************
  Used for ThumbnailExtraction, limited to 1000 resources
  **********************************************************************************************
  **/
  public Task_QuickBMSBulkExport(Resource resource) {
    resources = new Resource[1000]; // max 1000 resources
    resources[0] = resource;
    numResources = 1; // because we added one
    outputDirectory = null;
  }

  /**
  **********************************************************************************************
  Used for ThumbnailExtraction, limited to 1000 resources
  **********************************************************************************************
  **/
  public Task_QuickBMSBulkExport(Resource resource, File destDirectory) {
    resources = new Resource[1000]; // max 1000 resources
    resources[0] = resource;
    numResources = 1; // because we added one
    outputDirectory = destDirectory;
  }

  /**
  **********************************************************************************************
  Used for bulk extraction of files. Can't add any more resources to this list.
  **********************************************************************************************
  **/
  public Task_QuickBMSBulkExport(Resource[] resourcesIn) {
    resources = resourcesIn;
    numResources = resourcesIn.length;

    outputDirectory = null;
  }

  /**
  **********************************************************************************************
  Used for bulk extraction of files. Can't add any more resources to this list.
  **********************************************************************************************
  **/
  public Task_QuickBMSBulkExport(Resource[] resourcesIn, File destDirectory) {
    resources = resourcesIn;
    numResources = resourcesIn.length;

    outputDirectory = destDirectory;
  }

  /**
  **********************************************************************************************
  Uses QuickBMS to decompress the files
  **********************************************************************************************
  **/
  public File extractFiles(String nameList, File sourceFile, String scriptPath, File tempDirectory) {
    try {

      // get the QuickBMS Executable
      String quickbmsPath = getExternalLibraryPath();
      if (quickbmsPath == null) {
        // quickbms wasn't found
        return null;
      }

      // Build a script for doing the decompression
      if (scriptPath == null || !(new File(scriptPath).exists())) {
        // problem reading the script file
        return null;
      }

      ProcessBuilder pb = null;
      if (nameList == null) {
        // Exporter_QuickBMS_Decompression
        // The script was custom-built with only the files needed in it
        pb = new ProcessBuilder(quickbmsPath, "-o", "-Q", scriptPath, sourceFile.getAbsolutePath(), tempDirectory.getAbsolutePath());
        //pb = new ProcessBuilder(quickbmsPath, "-o", scriptPath, sourceFile.getAbsolutePath(), tempDirectory.getAbsolutePath());
      }
      else {
        // Exporter_QuickBMSWrapper
        // Using an existing script, so need to feed in the filename filter to only extract certain files
        pb = new ProcessBuilder(quickbmsPath, "-o", "-Q", "-f", nameList, scriptPath, sourceFile.getAbsolutePath(), tempDirectory.getAbsolutePath());
        //pb = new ProcessBuilder(quickbmsPath, "-o", "-f", nameList, scriptPath, sourceFile.getAbsolutePath(), tempDirectory.getAbsolutePath());
      }

      // Progress dialog
      TaskProgressManager.show(1, 0, Language.get("Progress_DecompressingFiles"));
      TaskProgressManager.setIndeterminate(true);

      // Start the task
      TaskProgressManager.startTask();

      Process convertProcess = pb.start();
      int returnCode = convertProcess.waitFor(); // wait for QuickBMS to finish

      /*
      int returnCode = 0;
      BufferedReader outputReader = new BufferedReader(new InputStreamReader(convertProcess.getInputStream()));
      BufferedReader errorReader = new BufferedReader(new InputStreamReader(convertProcess.getErrorStream()));
      
      String errorLine = errorReader.readLine();
      while (errorLine != null) {
        System.out.println(errorLine);
        errorLine = errorReader.readLine();
      }
      
      String outputLine = outputReader.readLine();
      while (outputLine != null) {
        System.out.println(outputLine);
        outputLine = outputReader.readLine();
      }
      */

      // Stop the task
      TaskProgressManager.stopTask();

      if (returnCode == 0) {
        // successful decompression
        //if (destFile.exists()) {
        return tempDirectory;
        //}
      }

      return null;

    }
    catch (Throwable t) {
      return null;
    }
  }

  /**
  **********************************************************************************************
  Gets the path to the external library or executable
  **********************************************************************************************
  **/
  public String getExternalLibraryPath() {
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
        ErrorLogger.log("quickbms can't be found at the path " + quickbmsFile.getAbsolutePath());
        return null;
      }

      quickbmsPath = quickbmsFile.getAbsolutePath();
      return quickbmsPath;
    }
    catch (Throwable t) {
      return null;
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void addResourceToExtract(Resource resource) {
    if (resources == null || numResources >= resources.length) {
      // couldn't add file, doesn't matter
      return;
    }
    resources[numResources] = resource;
    numResources++;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public boolean isExternalLibraryAvailable() {

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
  
  **********************************************************************************************
  **/
  @Override
  public void redo() {

    //System.out.println("Bulk Loading Thumbnails");

    if (resources == null || resources.length <= 0 || numResources <= 0) {
      return; // empty list of files to export
    }

    File tempPath = outputDirectory;
    if (tempPath == null) {
      tempPath = new File(new File(Settings.get("TempDirectory")).getAbsolutePath());
    }
    tempPath = FilenameChecker.correctFilename(tempPath); // removes funny characters etc.

    //
    // 1. Get some of the generic details from the first resource (they should all be the same).
    //
    Resource firstResource = resources[0];
    File sourceFile = firstResource.getSource();

    String scriptPath = null;
    String nameList = null;

    ExporterPlugin exporter = firstResource.getExporter();
    if (exporter instanceof Exporter_QuickBMSWrapper) {
      // QuickBMS wrapper for an existing BMS script - need to list all the files we want to extract 

      scriptPath = ((Exporter_QuickBMSWrapper) exporter).getScriptPath();

      // 
      // 2. Compile the list of names to export
      //
      File nameListFile = new File(new File(new File(Settings.get("TempDirectory")).getAbsolutePath()) + File.separator + "ge_quickbms_extract_filelist_" + System.currentTimeMillis() + ".txt");
      nameListFile = FilenameChecker.correctFilename(nameListFile); // removes funny characters etc.
      FileManipulator fm = new FileManipulator(nameListFile, true);
      for (int i = 0; i < numResources; i++) {
        fm.writeString(resources[i].getName());
        fm.writeByte(13);
        fm.writeByte(10);
      }
      fm.close();

      nameList = nameListFile.getAbsolutePath();
    }
    else if (exporter instanceof Exporter_QuickBMS_Decompression) {
      // QuickBMS for decompression only - need to create a temporary BMS script

      // 
      // 2. Compile a script with all the files listed in it
      //
      File scriptFile = new File(new File(new File(Settings.get("TempDirectory")).getAbsolutePath()) + File.separator + "ge_quickbms_extract_" + System.currentTimeMillis() + ".bms");
      scriptFile = FilenameChecker.correctFilename(scriptFile); // removes funny characters etc.
      FileManipulator fm = new FileManipulator(scriptFile, true);

      String compressionType = ((Exporter_QuickBMS_Decompression) exporter).getCompressionType();
      fm.writeString("comtype " + compressionType + "\n");

      for (int i = 0; i < numResources; i++) {
        Resource resource = resources[i];
        //System.out.println("Bulk for " + resource.getName());
        fm.writeString("clog \"" + resource.getName() + "\" " + resource.getOffset() + " " + resource.getLength() + " " + resource.getDecompressedLength() + "\n");
      }
      fm.close();

      scriptPath = scriptFile.getAbsolutePath();
    }
    else if (exporter instanceof BlockQuickBMSExporterWrapper) {
      // QuickBMS for decompression only - need to create a temporary BMS script
      // HOWEVER, the Resource is made up of lots of individual compressed blocks, so we need to do some special exporting
      // to append the blocks to the same single file. 

      // 
      // 2. Compile a script with all the files listed in it
      //

      //
      // BECAUSE THE SCRIPT FILE ITSELF IS TOO LARGE (more than 1024 variables), WE ACTUALLY HAVE TO TRICK QuickBMS.
      // 1. Create the whole script file as normal. (EXCEPT WE NEED A SPACE CHARACTER AT THE END OF EACH LINE!)
      // 2. Create a "processing" script which will read each line of the script and convert it into instructions for QuickBMS.
      //

      // Create a temporary file for the "real" BMS script
      String tempDirectory = new File(Settings.get("TempDirectory")).getAbsolutePath();
      //File scriptFile = new File(tempDirectory + File.separator + "ge_quickbms_extract_" + System.currentTimeMillis() + ".bms");
      File scriptFile = new File(tempDirectory + File.separator + "ge_quickbms_extract_" + System.currentTimeMillis() + ".txt");
      scriptFile = FilenameChecker.correctFilename(scriptFile); // removes funny characters etc.
      FileManipulator fm = new FileManipulator(scriptFile, true);

      for (int i = 0; i < numResources; i++) {
        Resource resource = resources[i];
        String script = ((BlockQuickBMSExporterWrapper) resource.getExporter()).buildScript(resource);
        if (script != null) {
          //System.out.println("Bulk for " + resource.getName());
          fm.writeString(script);
        }
      }
      fm.close();

      // Now create a temporary file for the "processing" BMS script

      String processingFilePath = tempDirectory + File.separator + "ge_quickbms_extract_processing_" + System.currentTimeMillis() + ".bms";
      File processingFile = new File(processingFilePath);
      processingFile = FilenameChecker.correctFilename(processingFile); // removes funny characters etc.

      // write the script
      FileManipulator tempFM = new FileManipulator(processingFile, true);

      /*      
      tempFM.writeString("open \".\" \"" + scriptFile.getAbsolutePath() + "\" 1\n");
      tempFM.writeString("\n");
      tempFM.writeString("get ARCSIZE ASIZE 1\n");
      tempFM.writeString("set DELIMITER_BYTE long 0x20\n");
      tempFM.writeString("\n");
      tempFM.writeString("do\n");
      tempFM.writeString("  getct PROPERTY STRING DELIMITER_BYTE 1\n");
      tempFM.writeString("  if PROPERTY = append\n");
      tempFM.writeString("    get DUMMYLINE LINE 1\n");
      tempFM.writeString("    \n");
      tempFM.writeString("    append\n");
      tempFM.writeString("    \n");
      tempFM.writeString("  elif PROPERTY = set\n");
      tempFM.writeString("    getct DUMMY STRING DELIMITER_BYTE 1\n");
      tempFM.writeString("    getct INNAME STRING DELIMITER_BYTE 1\n");
      tempFM.writeString("    get DUMMYLINE LINE 1\n");
      tempFM.writeString("    \n");
      tempFM.writeString("    set NAME INNAME\n");
      tempFM.writeString("    \n");
      tempFM.writeString("  elif PROPERTY = comtype\n");
      tempFM.writeString("    getct INCOMTYPE STRING DELIMITER_BYTE 1\n");
      tempFM.writeString("    get DUMMYLINE LINE 1\n");
      tempFM.writeString("    \n");
      tempFM.writeString("    if INCOMTYPE = LZ2K\n");
      tempFM.writeString("      comtype LZ2K\n");
      tempFM.writeString("    endif\n");
      tempFM.writeString("    \n");
      tempFM.writeString("  elif PROPERTY = clog\n");
      tempFM.writeString("    getct DUMMY STRING DELIMITER_BYTE 1\n");
      tempFM.writeString("    getct INOFFSET STRING DELIMITER_BYTE 1\n");
      tempFM.writeString("    getct INLENGTH STRING DELIMITER_BYTE 1\n");
      tempFM.writeString("    getct INDECOMPLENGTH STRING DELIMITER_BYTE 1\n");
      tempFM.writeString("    get DUMMYLINE LINE 1\n");
      tempFM.writeString("\n");
      tempFM.writeString("    clog NAME INOFFSET INLENGTH INDECOMPLENGTH\n");
      tempFM.writeString("\n");
      tempFM.writeString("  elif PROPERTY = log\n");
      tempFM.writeString("    getct DUMMY STRING DELIMITER_BYTE 1\n");
      tempFM.writeString("    getct INOFFSET STRING DELIMITER_BYTE 1\n");
      tempFM.writeString("    getct INLENGTH STRING DELIMITER_BYTE 1\n");
      tempFM.writeString("    get DUMMYLINE LINE 1\n");
      tempFM.writeString("\n");
      tempFM.writeString("    log NAME INOFFSET INLENGTH\n");
      tempFM.writeString("  else\n");
      tempFM.writeString("    get DUMMYLINE LINE 1\n");
      tempFM.writeString("  ENDIF\n");
      tempFM.writeString("savepos OFFSET 1\n");
      tempFM.writeString("while OFFSET < ARCSIZE\n");
      */

      String processingScript = QuickBMSHelper.buildProcessingScript(scriptFile);
      tempFM.writeString(processingScript);

      tempFM.close();

      // return the processing file --> it will be the one that calls the Real script
      //scriptPath = scriptFile.getAbsolutePath();
      scriptPath = processingFile.getAbsolutePath();

    }

    if (scriptPath == null) {
      return;
    }

    //
    // 3. Remove this task from the SingletonManager, so that you can't add any more Resources to it (we're about to run the actual export)
    //
    SingletonManager.remove("QuickBMSBulkExportTask");

    //
    // 4. Run the export for all the files
    //
    extractFiles(nameList, sourceFile, scriptPath, tempPath);

    //
    // 5. Update all the resources so that they know they've been extracted
    //
    String absoluteTempPath = tempPath.getAbsolutePath();
    for (int i = 0; i < numResources; i++) {
      Resource resource = resources[i];

      File outputFile = new File(absoluteTempPath + File.separator + resource.getName());
      if (outputFile.exists()) {
        // found the exported file, update the resource accordingly
        resource.setExportedPath(outputFile);
      }

    }

    //System.out.println("Bulk Extract Complete");

    //
    // 6. Clean up
    //
    resources = null;
    numResources = 0;
    outputDirectory = null;

    return;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  @SuppressWarnings("rawtypes")
  public String toString() {
    Class cl = getClass();
    String name = cl.getName();
    Package pack = cl.getPackage();

    if (pack != null) {
      name = name.substring(pack.getName().length() + 1);
    }

    return Language.get(name);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void undo() {
    if (!TaskProgressManager.canDoTask()) {
      return;
    }
  }

}
