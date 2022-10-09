/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2022 wattostudios
 *
 * License Information:
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License
 * published by the Free Software Foundation; either version 2 of the License, or (at your option) any later versions. This
 * program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranties
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License at http://www.gnu.org for more
 * details. For further information on this application, refer to the authors' website.
 */

package org.watto.ge.plugin.archive;

import java.io.File;
import org.watto.ErrorLogger;
import org.watto.Language;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.io.FileManipulator;
import org.watto.io.FilenameSplitter;
import org.watto.io.converter.ByteConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************
Ref: Zenhax --> rdzig2big.bms
**********************************************************************************************
**/
public class Plugin_ZIG extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_ZIG() {

    super("ZIG", "ZIG");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Race Driver 3");
    setExtensions("zig"); // MUST BE LOWER CASE
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
    //setFileTypes(new FileType("txt", "Text Document", FileType.TYPE_DOCUMENT),
    //             new FileType("bmp", "Bitmap Image", FileType.TYPE_IMAGE)
    //             );

    //setTextPreviewExtensions("colours", "rat", "screen", "styles"); // LOWER CASE

    //setCanScanForFileTypes(true);

  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public int getMatchRating(FileManipulator fm) {
    try {

      int rating = 0;

      if (FieldValidator.checkExtension(fm, extensions)) {
        rating += 25;
      }

      // Header
      if (fm.readInt() == 1584358631) { // encrypted BIGF header
        rating += 50;
      }

      return rating;

    }
    catch (Throwable t) {
      return 0;
    }
  }

  /**
   **********************************************************************************************
   Decrypts the archive
   **********************************************************************************************
   **/
  public FileManipulator decryptArchive(FileManipulator fm) {
    try {
      // Build a new "_ge_decrypted" archive file in the current directory
      File origFile = fm.getFile();

      String pathOnly = FilenameSplitter.getDirectory(origFile);
      String filenameOnly = FilenameSplitter.getFilename(origFile);
      String extensionOnly = FilenameSplitter.getExtension(origFile);

      File decompFile = new File(pathOnly + File.separatorChar + filenameOnly + "_ge_decrypted" + "." + extensionOnly);
      if (decompFile.exists()) {
        // we've already decrypted this file before - open and return it
        return new FileManipulator(decompFile, false);
      }

      FileManipulator decompFM = new FileManipulator(decompFile, true);

      long arcSize = fm.getLength();

      // Now decrypt the block into the decrypted file
      TaskProgressManager.setMessage(Language.get("Progress_DecompressingArchive")); // progress bar
      TaskProgressManager.setMaximum(arcSize); // progress bar
      TaskProgressManager.setIndeterminate(true);

      long addNumber = 7;
      byte[] key = "This file is (C) Codemasters Software Company ltd. It took a lot of time and effort to produce. Leave it alone.".getBytes();
      int keyLength = key.length;

      int j = 0;
      for (int i = 0; i < arcSize; i++) {
        int currentByte = ByteConverter.unsign(fm.readByte());
        int keyByte = key[j];

        currentByte += keyByte + addNumber;

        addNumber *= 0x5b;
        addNumber &= 0xff;

        decompFM.writeByte(currentByte);

        j++;
        if (j >= keyLength) {
          j = 0;
        }

      }

      // Force-write out the decrypted file to write it to disk, then change the buffer to read-only.
      decompFM.close();
      decompFM = new FileManipulator(decompFile, false);

      TaskProgressManager.setMessage(Language.get("Progress_ReadingArchive")); // progress bar
      TaskProgressManager.setIndeterminate(false);

      decompFM.seek(0);
      return decompFM;
    }
    catch (Throwable t) {
      ErrorLogger.log(t);
      return null;
    }
  }

  /**
   **********************************************************************************************
   * Reads an [archive] File into the Resources
   **********************************************************************************************
   **/
  @Override
  public Resource[] read(File path) {
    try {

      // NOTE - Compressed files MUST know their DECOMPRESSED LENGTH
      //      - Uncompressed files MUST know their LENGTH

      addFileTypes();

      //ExporterPlugin exporter = Exporter_ZLib.getInstance();

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      //if (fm.readInt() == 1584358631) {
      if (fm.readInt() != 1179076930) { // for anything other than BIGF, assume it needs to be decrypted
        fm.relativeSeek(0);

        FileManipulator decompFM = decryptArchive(fm);

        if (decompFM != null) {
          fm.close(); // close the original archive
          fm = decompFM; // now we're going to read from the decompressed file instead
          fm.relativeSeek(0); // go to the same point in the decompressed file as in the compressed file

          path = fm.getFile(); // So the resources are stored against the decompressed file
        }
      }

      fm.close();

      Resource[] resources = new Plugin_BIG_BIGF_2().read(path);

      if (resources != null) {
        int numFiles = resources.length;
        for (int i = 0; i < numFiles; i++) {
          resources[i].forceNotAdded(true);
        }
      }

      return resources;

    }
    catch (Throwable t) {
      logError(t);
      return null;
    }
  }

}
