
package org.watto.ge.plugin.archive;

import java.io.File;
import org.watto.ErrorLogger;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
////////////////////////////////////////////////////////////////////////////////////////////////
//                                                                                            //
//                                       GAME EXTRACTOR                                       //
//                               Extensible Game Archive Editor                               //
//                                http://www.watto.org/extract                                //
//                                                                                            //
//                           Copyright (C) 2002-2009  WATTO Studios                           //
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
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_TRC_TEXTUREPACK4 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_TRC_TEXTUREPACK4() {

    super("TRC_TEXTUREPACK4", "TRC_TEXTUREPACK4");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("X Motor Racing");
    setExtensions("trc", "pnt", "shd"); // MUST BE LOWER CASE
    setPlatforms("PC");

    //setFileTypes("","",
    //             "",""
    //             );

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
      if (fm.readString(11).equals("TexturePack")) {
        rating += 50;
      }

      String header2 = fm.readString(1);
      if (header2.equals("4")) {
        fm.skip(9);
      }
      else if (header2.equals("D")) {
        fm.skip(12);
      }

      // Number Of Files
      if (FieldValidator.checkNumFiles(fm.readInt())) {
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

      // X - Header (TexturePack4/TexturePackDDS2)
      // 1 - null Header Terminator
      fm.skip(11);
      String header = fm.readNullString();

      // 8 - null
      fm.skip(8);

      // 4 - Number Of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      if (header.equals("DDS2")) {
        for (int i = 0; i < numFiles; i++) {
          // 2 - Filename Length (including null terminator)
          fm.skip(2);

          // X - Filename
          // 1 - null Filename Terminator
          String filename = fm.readNullString();
          FieldValidator.checkFilename(filename);
          filename += ".dds";

          // 4 - Unknown (1)
          // 4 - Unknown
          // 2 - Image Height/Width
          // 2 - Image Height/Width
          fm.skip(12);

          // 4 - File Offset
          int offset = fm.readInt();
          FieldValidator.checkOffset(offset, arcSize);

          // 4 - File Length
          int length = fm.readInt();
          FieldValidator.checkLength(length, arcSize);

          // 11 - null
          fm.skip(11);

          //path,name,offset,length,decompLength,exporter
          resources[i] = new Resource(path, filename, offset, length);

          TaskProgressManager.setValue(i);
        }

      }
      else if (header.equals("4")) {
        for (int i = 0; i < numFiles; i++) {
          // 2 - Filename Length (including null terminator)
          fm.skip(2);

          // X - Filename
          // 1 - null Filename Terminator
          String filename = fm.readNullString();
          FieldValidator.checkFilename(filename);

          // 4 - Unknown (1)
          // 2 - Unknown
          fm.skip(6);

          // 2 - Bits Per Pixel (24=RGB/32=RGBA)
          short bitsPerPixel = fm.readShort();
          FieldValidator.checkRange(bitsPerPixel, 8, 32);

          // 2 - Image Width
          short width = fm.readShort();
          FieldValidator.checkWidth(width);

          // 2 - Image Height
          short height = fm.readShort();
          FieldValidator.checkHeight(height);

          String imageFormat = "";
          if (bitsPerPixel == 24) {
            imageFormat = "RGB";
          }
          else if (bitsPerPixel == 32) {
            imageFormat = "RGBA";
          }
          else if (bitsPerPixel == 8) {
            imageFormat = "8BitPaletted";
          }

          // 4 - File Offset
          int offset = fm.readInt();
          FieldValidator.checkOffset(offset, arcSize);

          //path,name,offset,length,decompLength,exporter
          Resource resource = new Resource(path, filename, offset);
          resource.addProperty("ImageFormat", imageFormat);
          resource.addProperty("Width", width);
          resource.addProperty("Height", height);

          resources[i] = resource;

          TaskProgressManager.setValue(i);
        }

        calculateFileSizes(resources, arcSize);
      }
      else {
        ErrorLogger.log("[TRC_TEXTUREPACK4] Unknown Directory Format: " + header);
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
