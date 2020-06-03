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

package org.watto.ge.plugin.exporter;

import java.io.File;
import org.watto.Language;
import org.watto.Settings;
import org.watto.datatype.Resource;
import org.watto.ge.helper.QuickBMSHelper;
import org.watto.io.FileManipulator;
import org.watto.io.FilenameChecker;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************
A file, which is compressed in blocks (optionally with different compression algorithms for
each block). The decompression needs to use QuickBMS. There are methods for running this
exporter directly (for decompressing a single file) or to generate scripts which can be
combined with other scripts to decompress multiple files in a bulk lot.
Effectively, the way this exporter runs is similar to Exporter_Default, because all we do is
decompress the file to a (temporary) location, and then read that decompressed file normally.
**********************************************************************************************
**/
public class BlockQuickBMSExporterWrapper extends Exporter_Default {

  /** the compression algorithm for each block **/
  String[] compressionTypes = null;

  /** the offset to each compressed block **/
  long[] blockOffsets;

  /** the length of each compressed block **/
  long[] blockLengths;

  /** the decompressed length of each compressed block **/
  long[] decompLengths;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public BlockQuickBMSExporterWrapper() {
    setName("Wrapper for exporting a file compressed in blocks, where each block can use a different compression algorithm, and where it needs to use QuickBMS for decompression.");
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public BlockQuickBMSExporterWrapper(String[] compressionTypes, long[] blockOffsets, long[] blockLengths, long[] decompLengths) {
    this.compressionTypes = compressionTypes;
    this.blockOffsets = blockOffsets;
    this.blockLengths = blockLengths;
    this.decompLengths = decompLengths;

    setName("Block Compression using QuickBMS");
  }

  public String[] getCompressionTypes() {
    return compressionTypes;
  }

  public void setCompressionTypes(String[] compressionTypes) {
    this.compressionTypes = compressionTypes;
  }

  public long[] getBlockLengths() {
    return blockLengths;
  }

  public long[] getBlockOffsets() {
    return blockOffsets;
  }

  public long[] getDecompLengths() {
    return decompLengths;
  }

  public void setBlockLengths(long[] blockLengths) {
    this.blockLengths = blockLengths;
  }

  public void setBlockOffsets(long[] blockOffsets) {
    this.blockOffsets = blockOffsets;
  }

  public void setDecompLengths(long[] decompLengths) {
    this.decompLengths = decompLengths;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public String buildScriptFile(Resource sourceFile) {
    try {

      // BECAUSE THE SCRIPT FILE ITSELF IS TOO LARGE (more than 1024 variables), WE ACTUALLY HAVE TO TRICK QuickBMS.
      // 1. Create the whole script file as normal. (EXCEPT WE NEED A SPACE CHARACTER AT THE END OF EACH LINE!)
      // 2. Create a "processing" script which will read each line of the script and convert it into instructions for QuickBMS.

      // Create a temporary file for the "real" BMS script
      String tempDirectory = new File(Settings.get("TempDirectory")).getAbsolutePath();

      //String scriptFilePath = tempDirectory + File.separator + "quickbmsscript_decompress_" + System.currentTimeMillis() + ".bms";
      String scriptFilePath = tempDirectory + File.separator + "quickbmsscript_decompress_" + System.currentTimeMillis() + ".txt";
      File scriptFile = new File(scriptFilePath);
      scriptFile = FilenameChecker.correctFilename(scriptFile); // removes funny characters etc.

      // write the script
      FileManipulator tempFM = new FileManipulator(scriptFile, true);
      String script = buildScript(sourceFile);
      if (script != null) {
        tempFM.writeString(script);
      }
      tempFM.close();

      // Now create a temporary file for the "processing" BMS script

      String processingFilePath = tempDirectory + File.separator + "quickbmsscript_processing_" + System.currentTimeMillis() + ".bms";
      File processingFile = new File(processingFilePath);
      processingFile = FilenameChecker.correctFilename(processingFile); // removes funny characters etc.

      // write the script
      tempFM = new FileManipulator(processingFile, true);

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
      return processingFile.getAbsolutePath();

    }
    catch (Throwable t) {
      return null;
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public String buildScript(Resource sourceFile) {
    try {

      String script = "append \n"; // turn on APPEND mode, so we write all the blocks to the same file

      int numBlocks = blockOffsets.length;
      String sourceName = sourceFile.getName();

      script += "set NAME " + sourceName + " \n"; // build a single Variable for the filename, and re-use that Variable in each clog call, so we don't end up with too many of them

      String compressionType = "";
      for (int i = 0; i < numBlocks; i++) {
        String thisCompressionType = compressionTypes[i];
        if (!thisCompressionType.equals(compressionType)) {
          script += "comtype " + thisCompressionType + " \n";
          compressionType = thisCompressionType;
        }
        script += "clog NAME " + blockOffsets[i] + " " + blockLengths[i] + " " + decompLengths[i] + " \n";
      }

      script += "append \n"; // turn off APPEND mode, so the next file can start (if bulk exporting)

      return script;

    }
    catch (Throwable t) {
      return null;
    }
  }

  /**
  **********************************************************************************************
  Uses QuickBMS to decompress a file
  **********************************************************************************************
  **/
  public File decompress(Resource resource, File tempDirectory) {
    try {

      // get the QuickBMS Executable
      String quickbmsPath = QuickBMSHelper.checkAndShowPopup();
      if (quickbmsPath == null) {
        return null;
      }

      // Build a script for doing the decompression
      String scriptPath = buildScriptFile(resource);
      if (scriptPath == null) {
        // problem generating the script file
        return null;
      }

      String sourceFile = resource.getSource().getAbsolutePath();

      //ProcessBuilder pb = new ProcessBuilder(quickbmsPath, "-o", "-O", destFile.getAbsolutePath(), "-Q", scriptPath, sourceFile);
      ProcessBuilder pb = new ProcessBuilder(quickbmsPath, "-o", scriptPath, sourceFile, tempDirectory.getAbsolutePath());

      // Progress dialog
      TaskProgressManager.show(1, 0, Language.get("Progress_DecompressingFiles"));
      TaskProgressManager.setIndeterminate(true);

      // Start the task
      TaskProgressManager.startTask();

      Process convertProcess = pb.start();
      int returnCode = convertProcess.waitFor(); // wait for QuickBMS to finish

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
      logError(t);
      return null;
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void open(Resource source) {
    try {

      File tempPath = new File(new File(Settings.get("TempDirectory")).getAbsolutePath());
      tempPath = FilenameChecker.correctFilename(tempPath); // removes funny characters etc.

      File outputFile = new File(tempPath.getAbsolutePath() + File.separatorChar + source.getName());
      outputFile = FilenameChecker.correctFilename(outputFile); // removes funny characters etc.

      if (outputFile.exists() && outputFile.length() <= 0) {
        // It can get here if the QuickBMS extract file and the temporary file are the same.
        if (this.exportDestination != null && outputFile.equals(this.exportDestination.getFile())) {
          // If this happens, close the (empty) temporary file, and tell QuickBMS to extract over it.
          this.exportDestination.close();
          this.exportDestination = null;
          tempPath = decompress(source, tempPath); // tempPath will return null if there was an error in QuickBMS
        }
        // already extracted - don't extract again
      }
      else {
        // run the extraction
        tempPath = decompress(source, tempPath); // tempPath will return null if there was an error in QuickBMS
      }

      //
      // Now open this decompressed file for reading normally.
      //
      if (tempPath == null || outputFile == null || !outputFile.exists()) {
        // some kind of problem running the extraction - a QuickBMS issue
        readLength = 0; // force the file to "not exist"
        return;
      }

      readLength = outputFile.length();

      // try to get the whole file in a single go, if it isn't too large (set to 200KB otherwise)
      int bufferSize = (int) readLength;
      if (bufferSize > 204800) {
        bufferSize = 204800;
      }

      readSource = new FileManipulator(outputFile, false, bufferSize);
      readSource.seek(0); // just in case, restart at the beginning of the decompressed file

    }
    catch (Throwable t) {
    }
  }

  /**
  **********************************************************************************************
  From ExporterPlugin, to do an actual extraction to a file. Special case for this plugin only!
  We normally don't overwrite this method, but just overwrite open() instead.
  **********************************************************************************************
  **/
  @Override
  public void extract(Resource source, FileManipulator destination) {
    try {
      this.exportDestination = destination;

      open(source);
      if (this.exportDestination == null) {
        // This occurs if QuickBMS has already written straight out to this file.
        // In this case, just close it all off and return, rather than duplicating the file
      }
      else {
        // normal case, want to copy the file from the QuickBMS temp location to the destination
        while (available()) {
          destination.writeByte(read());
        }
      }
      close();

      this.exportDestination = null;
    }
    catch (Throwable t) {
      logError(t);
    }
  }

}