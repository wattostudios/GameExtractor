/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2024 wattostudios
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
import org.watto.Language;
import org.watto.Settings;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_XBR extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_XBR() {

    super("XBR", "XBR");

    //         read write replace rename
    setProperties(true, false, true, false);

    setGames("Cars");
    setExtensions("xbr"); // MUST BE LOWER CASE
    setPlatforms("XBox");

    //setFileTypes(new FileType("txt", "Text Document", FileType.TYPE_DOCUMENT),
    //             new FileType("bmp", "Bitmap Image", FileType.TYPE_IMAGE)
    //             );

    setTextPreviewExtensions("ani", "as", "asz", "aty", "aud", "cmt", "dat", "dxm", "emt", "juk", "lod", "lyr", "lyt", "mcf", "mst", "mtl", "pag", "pb", "pck", "pco", "phy", "pls", "poi", "prt", "psh", "rpt", "scn", "spl", "sty", "tcs", "toy", "trk", "vsh"); // LOWER CASE

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

      if (fm.readInt() == 2) {
        rating += 5;
      }

      // Number Of Files
      if (FieldValidator.checkNumFiles(fm.readInt())) {
        rating += 5;
      }

      fm.skip(8);

      long arcSize = fm.getLength();

      // Directory Length
      if (FieldValidator.checkLength(fm.readInt(), arcSize)) {
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

      // 4 - Unknown (2)
      fm.skip(4);

      // 4 - Number of Files?
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 4 - Unknown
      // 4 - Unknown
      fm.skip(8);

      // 4 - Details Directory Length
      int dirLength = fm.readInt();
      FieldValidator.checkLength(dirLength, arcSize);

      // 4 - Unknown
      fm.skip(4);

      dirLength += 24; // so now it points to the end of the directory offset (we added the 24-byte header)

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        //int i = 0;
        //while (fm.getOffset() < dirLength) {

        // 4 - Filename Length
        int filenameLength = fm.readInt();
        FieldValidator.checkFilenameLength(filenameLength);

        // X - Filename
        String filename = fm.readString(filenameLength);

        // 4 - File Length (not including padding)
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - File Offset [*2048]
        long offset = fm.readInt() * 2048;
        FieldValidator.checkOffset(offset, arcSize);

        //path,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset, length);

        TaskProgressManager.setValue(i);
        //i++;
      }

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

      // Calculations
      TaskProgressManager.setMessage(Language.get("Progress_PerformingCalculations"));

      // Write Header Data

      // 4 - Unknown (2)
      // 4 - Number of Files
      // 4 - Unknown
      // 4 - Unknown
      fm.writeBytes(src.readBytes(16));

      // 4 - Details Directory Length
      int dirLength = src.readInt();
      fm.writeInt(dirLength);

      // 4 - Unknown
      fm.writeBytes(src.readBytes(4));

      long dirPadding = calculatePadding(dirLength + 24, 2048);

      // Write Directory
      TaskProgressManager.setMessage(Language.get("Progress_WritingDirectory"));
      long offset = (dirLength + 24) + dirPadding;

      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];
        long length = resource.getDecompressedLength();

        // 4 - Filename Length
        int filenameLength = src.readInt();
        fm.writeInt(filenameLength);

        // X - Filename
        fm.writeBytes(src.readBytes(filenameLength));

        // 4 - File Length (not including padding)
        fm.writeInt(length);
        src.skip(4);

        // 4 - File Offset [*2048]
        fm.writeInt(offset / 2048);
        src.skip(4);

        offset += length;
        offset += calculatePadding(offset, 2048);
      }

      // X - Padding to a multiple of 2048 bytes
      for (int p = 0; p < dirPadding; p++) {
        fm.writeByte(0);
      }

      // Write Files
      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));
      for (int i = 0; i < resources.length; i++) {
        Resource resource = resources[i];
        write(resource, fm);

        int length = (int) resource.getDecompressedLength();
        int padding = calculatePadding(length, 2048);
        for (int p = 0; p < padding; p++) {
          fm.writeByte(0);
        }

        TaskProgressManager.setValue(i);
      }

      src.close();
      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

}
