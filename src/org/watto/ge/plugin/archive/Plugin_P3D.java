/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2023 wattostudios
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
import org.watto.Settings;
import org.watto.component.PreviewPanel;
import org.watto.datatype.Archive;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.viewer.Viewer_P3D_BMP;
import org.watto.io.FileManipulator;
import org.watto.io.converter.ByteConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_P3D extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_P3D() {

    super("P3D", "P3D");

    //         read write replace rename
    setProperties(true, false, true, false);

    setGames("NHL Championship 2000");
    setExtensions("p3d"); // MUST BE LOWER CASE
    setPlatforms("PC");

    //setFileTypes(new FileType("txt", "Text Document", FileType.TYPE_DOCUMENT),
    //             new FileType("bmp", "Bitmap Image", FileType.TYPE_IMAGE)
    //             );

    setCanConvertOnReplace(true);

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

      // 2 - Code
      if (fm.readShort() == -252) {
        rating += 5;
      }

      // 4 - File Length
      if (fm.readInt() == fm.getLength()) {
        rating += 5;
      }

      return rating;

    }
    catch (Throwable t) {
      return 0;
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

      long arcSize = fm.getLength();

      // 2 - Unknown (-252)
      // 4 - File Length
      fm.skip(6);

      int numFiles = Archive.getMaxFiles();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(arcSize);

      // Loop through directory
      int realNumFiles = 0;
      while (fm.getOffset() < arcSize) {

        // 2 - Code (12320)
        short code = fm.readShort();

        // 4 - Block Length (including these 6 bytes)
        int blockLength = fm.readInt() - 6;
        FieldValidator.checkLength(blockLength, arcSize);

        long offset = fm.getOffset();

        // X - Block Data
        if (code == 12353) {
          // IMAGE BLOCK

          // 1 - Name Length
          int nameLength = ByteConverter.unsign(fm.readByte());

          // X - Name
          String filename = fm.readString(nameLength);

          long length = blockLength;

          //path,name,offset,length,decompLength,exporter
          resources[realNumFiles] = new Resource(path, filename, offset - 6, length + 6);
          realNumFiles++;

          TaskProgressManager.setValue(offset);

          fm.relativeSeek(offset + blockLength);

        }
        else {
          fm.skip(blockLength);
        }
      }

      resources = resizeResources(resources, realNumFiles);

      fm.close();

      return resources;

    }
    catch (Throwable t) {
      logError(t);
      return null;
    }
  }

  /**
   **********************************************************************************************
   * Writes an [archive] File with the contents of the Resources. The archive is written using
   * data from the initial archive - it isn't written from scratch.
   **********************************************************************************************
   **/
  @Override
  public void replace(Resource[] resources, File path) {
    try {

      FileManipulator fm = new FileManipulator(path, true);
      FileManipulator src = new FileManipulator(new File(Settings.getString("CurrentArchive")), false);

      int numFiles = resources.length;
      TaskProgressManager.setMaximum(numFiles);

      long srcLength = src.getLength();

      // Write Header Data

      // 2 - Unknown (-252)
      // 4 - File Length
      fm.writeBytes(src.readBytes(6)); // Just copy for now, we'll replace with the real file length at the end

      int realNumFiles = 0;
      while (src.getOffset() < srcLength) {

        // 2 - Code (12320)
        short code = src.readShort();

        // 4 - Block Length (including these 6 bytes)
        int blockLength = src.readInt() - 6;
        FieldValidator.checkLength(blockLength, srcLength);

        // X - Block Data
        if (code == 12353) {
          // IMAGE BLOCK

          // write the full image, which includes the code and block length already
          write(resources[realNumFiles], fm);
          realNumFiles++;

          src.skip(blockLength);
        }
        else {
          fm.writeShort(code);
          fm.writeInt(blockLength + 6);

          fm.writeBytes(src.readBytes(blockLength));
        }

      }

      // Now that the full file is written, go back and write the file length in the header
      fm.seek(2);
      fm.writeInt(fm.getLength());

      src.close();
      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

  /**
   **********************************************************************************************
   When replacing files, if the file is of a certain type, it will be converted before replace
   @param resourceBeingReplaced the Resource in the archive that is being replaced
   @param fileToReplaceWith the file on your PC that will replace the Resource. This file is the
          one that will be converted into a different format, if applicable.
   @return the converted file, if conversion was applicable/successful, else the original fileToReplaceWith
   **********************************************************************************************
   **/
  @Override
  public File convertOnReplace(Resource resourceBeingReplaced, File fileToReplaceWith) {
    try {

      PreviewPanel imagePreviewPanel = loadFileForConversion(resourceBeingReplaced, fileToReplaceWith, "bmp");
      if (imagePreviewPanel == null) {
        // no conversion needed, or wasn't able to be converted
        return fileToReplaceWith;
      }

      // The plugin that will do the conversion
      Viewer_P3D_BMP converterPlugin = new Viewer_P3D_BMP();

      String beingReplacedExtension = resourceBeingReplaced.getExtension();
      File destination = new File(fileToReplaceWith.getAbsolutePath() + "." + beingReplacedExtension);
      if (destination.exists()) {
        destination.delete();
      }

      FileManipulator fmOut = new FileManipulator(destination, true);
      converterPlugin.replace(resourceBeingReplaced, imagePreviewPanel, fmOut);
      fmOut.close();

      return destination;

    }
    catch (Throwable t) {
      ErrorLogger.log(t);
      return fileToReplaceWith;
    }
  }

}
