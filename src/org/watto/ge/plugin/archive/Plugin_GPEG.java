/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2021 wattostudios
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
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.io.FileManipulator;
import org.watto.io.FilenameSplitter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_GPEG extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_GPEG() {

    super("GPEG", "GPEG");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Saints Row: The Third: Remastered");
    setExtensions("gpeg", "cpeg", "gvbh", "cvbh", "gvbm", "cvbm"); // MUST BE LOWER CASE
    setPlatforms("PC");

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

      if (fm.readString(4).equals("GEKV")) {
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

      String openedFile = path.getAbsolutePath();
      String openedExtension = FilenameSplitter.getExtension(openedFile);

      File dataFile = null;
      File headerFile = null;

      if (openedExtension.startsWith("g")) {
        dataFile = path;

        String headerExtension = "c" + openedExtension.substring(1);
        headerFile = new File(openedFile.replace(openedExtension, headerExtension));
      }
      else if (openedExtension.startsWith("c")) {
        headerFile = path;

        String dataExtension = "g" + openedExtension.substring(1);
        dataFile = new File(openedFile.replace(openedExtension, dataExtension));
      }
      else {
        return null;
      }

      if (!dataFile.exists() || !headerFile.exists()) {
        return null;
      }

      long arcSize = (int) dataFile.length();

      FileManipulator fm = new FileManipulator(headerFile, false);

      // 4 - Header (GEKV)
      // 4 - Unknown (13)
      // 4 - CPEG File Length
      // 4 - GPEG File Length
      fm.skip(16);

      // 4 - Number of Images
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 2 - Number of Images
      // 2 - Unknown (16)
      fm.skip(4);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        // 8 - Image Data Offset
        long offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);
        fm.skip(4);

        // 2 - Image Width
        short width = fm.readShort();
        FieldValidator.checkWidth(width);

        // 2 - Image Height
        short height = fm.readShort();
        FieldValidator.checkHeight(height);

        // 2 - Image Format
        int format = fm.readShort();

        // 2 - Unknown (256)
        // 2 - Unknown (1)
        // 2 - Unknown (1)
        // 2 - Unknown (1)
        // 2 - Unknown
        // 8 - null
        // 2 - null
        // 1 - Unknown
        fm.skip(21);
        //System.out.println(offset + "\t" + unk1 + "\t" + unk2 + "\t" + unk3 + "\t" + unk4 + "\t" + unk5 + "\t" + unk6 + "\t" + unk7);

        // 1 - Number of Mipmaps
        int mipmapCount = fm.readByte();
        FieldValidator.checkRange(mipmapCount, 1, 20);

        // 4 - Image Data Length
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 32 - null
        fm.skip(32);

        String imageFormat = "";
        if (format == 400) {
          imageFormat = "DXT1";
        }
        else if (format == 402) {
          imageFormat = "DXT5";
        }
        else if (format == 407) {
          imageFormat = "BGRA";
        }
        else if (format == 411) {
          imageFormat = "BC5";
        }
        else {
          ErrorLogger.log("[GPEG] Unknown Image Format: " + format);
        }

        //path,name,offset,length,decompLength,exporter
        Resource resource = new Resource(dataFile, "", offset, length);
        resource.addProperty("Width", width);
        resource.addProperty("Height", height);
        resource.addProperty("MipmapCount", mipmapCount);
        resource.addProperty("ImageFormat", imageFormat);
        resource.forceNotAdded(true);
        resources[i] = resource;

        TaskProgressManager.setValue(i);
      }

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        // X - Filename
        // 1 - null Filename Terminator
        String filename = fm.readNullString();

        Resource resource = resources[i];
        resource.setName(filename);
        resource.setOriginalName(filename);
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
  If an archive doesn't have filenames stored in it, the scanner can come here to try to work out
  what kind of file a Resource is. This method allows the plugin to provide additional plugin-specific
  extensions, which will be tried before any standard extensions.
  @return null if no extension can be determined, or the extension if one can be found
  **********************************************************************************************
  **/
  @Override
  public String guessFileExtension(Resource resource, byte[] headerBytes, int headerInt1, int headerInt2, int headerInt3, short headerShort1, short headerShort2, short headerShort3, short headerShort4, short headerShort5, short headerShort6) {

    /*
    if (headerInt1 == 2037149520) {
      return "js";
    }
    */

    return null;
  }

}
