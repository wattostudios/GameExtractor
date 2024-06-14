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

import org.watto.ErrorLogger;
import org.watto.datatype.FileType;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.Exporter_LZO1Y;
import org.watto.ge.plugin.exporter.Exporter_LZO_MiniLZO;
import org.watto.io.FileManipulator;
import org.watto.io.converter.ShortConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_TBV_TBVOL extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_TBV_TBVOL() {

    super("TBV_TBVOL", "TBV_TBVOL");

    //         read write replace rename
    setProperties(true, false, false, false);

    setExtensions("tbv");
    setGames("3D Ultra Pinball: Thrill Ride",
        "3D Ultra Cool Pool",
        "3D Ultra Radio Control Racers",
        "3D Ultra NASCAR Pinball",
        "Return of the Incredible Machine: Contraptions",
        "The Incredible Machine: Even More Contraptions",
        "3D Ultra Lionel Traintown");
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
    setFileTypes(new FileType("pal", "Color Palette", FileType.TYPE_OTHER),
        new FileType("par", "Part", FileType.TYPE_OTHER),
        new FileType("lev", "Level", FileType.TYPE_OTHER),
        new FileType("tba", "Animation", FileType.TYPE_OTHER),
        new FileType("tbb", "Bitmap", FileType.TYPE_IMAGE),
        new FileType("tbi", "Interface", FileType.TYPE_OTHER),
        new FileType("tbf", "Font", FileType.TYPE_OTHER),
        new FileType("tbt", "TBT Document", FileType.TYPE_DOCUMENT),
        new FileType("rrx", "RRX Document", FileType.TYPE_DOCUMENT),
        new FileType("rrm", "RRM Document", FileType.TYPE_DOCUMENT),
        new FileType("rrt", "RRT Document", FileType.TYPE_DOCUMENT),
        new FileType("rr", "RR File", FileType.TYPE_OTHER),
        new FileType("bmx", "BMX Image Sprites", FileType.TYPE_IMAGE));

    setTextPreviewExtensions("tbt", "rrx", "rrm", "rrt", "h"); // LOWER CASE

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
      if (fm.readString(9).equals("TBVolume" + (char) 0)) {
        rating += 50;
      }

      fm.skip(2);

      // Number Of Files
      if (FieldValidator.checkNumFiles(fm.readShort())) {
        rating += 5;
      }

      fm.skip(4);

      // Description
      if (fm.readString(8).equals("RichRayl")) {
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
  
  **********************************************************************************************
  **/
  @Override
  public Resource[] read(File path) {
    try {

      addFileTypes();

      ExporterPlugin exporter = Exporter_LZO_MiniLZO.getInstance();

      Exporter_LZO1Y exporterLZO1Y = Exporter_LZO1Y.getInstance();
      exporterLZO1Y.setSwapHeaderFields(true);

      FileManipulator fm = new FileManipulator(path, false, 28); // short quick reads

      // 9 - Header (TBVolume + null)
      // 2 - Unknown
      fm.skip(11);

      // 2 - Number of Files
      int numFiles = ShortConverter.unsign(fm.readShort());
      FieldValidator.checkNumFiles(numFiles);

      long arcSize = fm.getLength();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // 2 - Unknown
      // 2 - Unknown
      // 12 - Description 1 (RichRayl@CUC)
      // 12 - Description 2 (null)
      // 4 - Unknown
      fm.skip(32);

      // 4 - First Data Offset
      int firstDataOffset = fm.readInt();
      FieldValidator.checkOffset(firstDataOffset, arcSize);

      // X - Unknown

      fm.seek(firstDataOffset);
      for (int i = 0; i < numFiles; i++) {

        // 24 - Filename
        String filename = fm.readNullString(24);
        FieldValidator.checkFilename(filename);

        // 4 - Raw File Length
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // X - File Data
        long offset = (int) fm.getOffset();
        fm.skip(length);

        //offset += 55;
        //length -= 55;

        //path,id,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset, length);

        TaskProgressManager.setValue(i);
      }

      // Go through and detect compressed files
      fm.getBuffer().setBufferSize(24);
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];
        fm.seek(resource.getOffset());

        // 4 - Compression Header (PKX:)
        if (fm.readString(4).equals("PKX:")) {
          // 4 - Unknown
          // 4 - Unknown
          fm.skip(8);

          // 4 - Compression Type (257=LZO1X, 258=LZO1Y)
          int compressionType = fm.readInt();

          int resourceLength = (int) resource.getDecompressedLength();

          // 4 - Compressed Data Length
          int length = fm.readInt();
          FieldValidator.checkLength(length, resourceLength);

          // 4 - Decompressed Data Length
          int decompLength = fm.readInt();
          FieldValidator.checkLength(decompLength);

          long offset = fm.getOffset();

          if (compressionType == 257) { // LZO1X
            resource.setOffset(offset);
            resource.setLength(length);
            resource.setDecompressedLength(decompLength);
            resource.setExporter(exporter);
          }
          else if (compressionType == 258) { // LZO1Y
            resource.setOffset(offset - 8); // this exporter needs to read the length fields
            resource.setLength(length + 8);
            resource.setDecompressedLength(decompLength);
            resource.setExporter(exporterLZO1Y);
          }
          else {
            // leave as Raw
            ErrorLogger.log("[TBV_TBVOL] Unknown compression type: " + compressionType + " at offset " + (offset - 24));
            resource.setOffset(offset);
            resource.setLength(length);
            resource.setDecompressedLength(length);
          }
        }
      }

      fm.close();

      return resources;

    }
    catch (Throwable t) {
      logError(t);
      return null;
    }
  }

}