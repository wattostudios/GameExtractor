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
import org.watto.Language;
import org.watto.datatype.Archive;
import org.watto.datatype.FileType;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.io.FileManipulator;
import org.watto.io.converter.ByteConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_TIM extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_TIM() {

    super("TIM", "TIM");

    //         read write replace rename
    setProperties(true, true, false, false);

    setGames("Digimon Rumble Arena");
    setExtensions("tim"); // MUST BE LOWER CASE
    setPlatforms("PS1");

    // MUST BE LOWER CASE !!!
    setFileTypes(new FileType("tim", "Single TIM Image", FileType.TYPE_IMAGE));

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
      if (fm.readInt() == 16) {
        rating += 5;
      }

      if (FieldValidator.checkRange(fm.readInt(), 0, 9)) {
        rating += 5;
      }

      long arcSize = fm.getLength();

      // Data or Palette Length
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

      int numFiles = Archive.getMaxFiles();
      int realNumFiles = 0;

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(arcSize);

      // Loop through directory
      while (fm.getOffset() < arcSize) {
        long startOffset = fm.getOffset();

        // 1 - Header (16)
        // 1 - Version (0)
        // 2 - null
        fm.skip(4);

        // 1 - Flags (first 2 bits = color depth --> 0=4bpp, 1=8bpp, 2=16bpp, 3=24bpp)
        int flags = ByteConverter.unsign(fm.readByte());

        // 3 - null
        fm.skip(3);

        int bpp = (flags & 3);
        if (bpp == 3) {
          bpp = 24;
        }
        else if (bpp == 2) {
          bpp = 16;
        }
        else if (bpp == 1) {
          bpp = 8;
        }
        else if (bpp == 0) {
          bpp = 4;
        }

        boolean paletted = (((flags & 8) >> 3) == 1);

        if (paletted) {
          // 4 - Palette Data Length
          int paletteLength = fm.readInt() - 12;

          // 2 - Palette X
          // 2 - Palette Y
          fm.skip(4);

          // 2 - Number of Colors
          int numColors = fm.readShort();

          // 2 - Number of Palettes
          int numPalettes = fm.readShort();

          if (bpp == 4) {
            // use the numColors in the fields above
          }
          else {
            numColors = paletteLength / 2;
            numPalettes = 1;
          }
          FieldValidator.checkNumColors(numColors);

          for (int p = 0; p < numPalettes; p++) {
            fm.skip(numColors * 2);
          }

        }

        // 4 - Image Data Length
        int imageDataLength = fm.readInt();
        FieldValidator.checkLength(imageDataLength, arcSize);

        // 2 - Image X
        // 2 - Image Y
        fm.skip(4);

        // 2 - Image Width
        short width = fm.readShort();
        FieldValidator.checkWidth(width);

        // 2 - Image Height
        short height = fm.readShort();
        FieldValidator.checkHeight(height);

        // X - Pixels
        if (paletted) {

          imageDataLength -= 12;
          /*
          if (numPalettes == 1 && bpp == 4 && (imageDataLength == width * height * 2)) {
            // force it
            bpp = 8;
          }
          */

          if (bpp == 4) {
            width *= 4;
            fm.skip(width * height / 2);
          }
          else if (bpp == 8) {
            width *= 2;
            fm.skip(width * height);
          }
        }
        else {
          if (bpp == 16) {
            fm.skip(width * height * 2);
          }
          else if (bpp == 24) {
            fm.skip(width * height * 3);
          }
        }

        long endOffset = fm.getOffset();

        int length = (int) (endOffset - startOffset);

        String filename = Resource.generateFilename(realNumFiles) + ".tim";

        //path,name,offset,length,decompLength,exporter
        resources[realNumFiles] = new Resource(path, filename, startOffset, length);
        realNumFiles++;

        TaskProgressManager.setValue(endOffset);
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
   * Writes an [archive] File with the contents of the Resources
   **********************************************************************************************
   **/
  @Override
  public void write(Resource[] resources, File path) {
    try {

      FileManipulator fm = new FileManipulator(path, true);
      int numFiles = resources.length;
      TaskProgressManager.setMaximum(numFiles);

      // Write Files
      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));
      write(resources, fm);

      fm.close();

    }
    catch (Throwable t) {
      logError(t);
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
