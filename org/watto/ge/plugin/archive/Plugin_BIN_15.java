
package org.watto.ge.plugin.archive;

import java.io.File;
import org.watto.task.TaskProgressManager;
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

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_BIN_15 extends ArchivePlugin {

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Plugin_BIN_15() {

    super("BIN_15", "BIN_15");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Apache Havoc");
    setExtensions("bin");
    setPlatforms("PC");

    setFileTypes("image", "Image (Indexed Color)",
        "palette", "Color Palette");

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

      if (fm.getFile().getName().equals("Textures.bin")) {
        rating += 25;
      }

      // Number Of Palettes
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

      // 4 - Number Of Palettes
      int numPalettes = fm.readInt();
      FieldValidator.checkNumFiles(numPalettes);

      // skip over the palettes
      int paletteSize = 256 * 4;
      long dirOffset = numPalettes * paletteSize + 4;
      FieldValidator.checkOffset(dirOffset, arcSize);

      fm.seek(dirOffset);

      // 4 - Number Of Images
      int numImages = fm.readInt();
      FieldValidator.checkNumFiles(numImages);

      int numFiles = numPalettes + numImages;
      FieldValidator.checkNumFiles(numFiles);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      int realNumFiles = 0;

      // Loop through the palettes
      long offset = 4;
      for (int i = 0; i < numPalettes; i++) {
        String filename = "Palette " + (i + 1) + ".palette";

        //path,name,offset,length,decompLength,exporter
        resources[realNumFiles] = new Resource(path, filename, offset, paletteSize);

        TaskProgressManager.setValue(realNumFiles);
        realNumFiles++;

        offset += paletteSize;
      }

      // Loop through the images
      for (int i = 0; i < numImages; i++) {
        // 128 - Filename (null) (no extension)
        String filename = fm.readNullString(128);
        FieldValidator.checkFilename(filename);
        filename += ".image";

        // 4 - Unknown (1)
        // 4 - Color Palette Number?
        fm.skip(8);

        // 4 - Image Width
        int imageWidth = fm.readInt();
        FieldValidator.checkPositive(imageWidth);

        // 4 - Image Height
        int imageHeight = fm.readInt();
        FieldValidator.checkPositive(imageHeight);

        // 4 - null
        fm.skip(4);

        // 4 - Number Of Mipmaps
        int numMipmaps = fm.readInt();
        FieldValidator.checkPositive(numMipmaps);

        // for each mipmap
        //   w*h - Pixel Data (Palette Index)
        offset = fm.getOffset();
        FieldValidator.checkOffset(offset);

        long length = 0;
        while (imageWidth > 1 && imageHeight > 1) {
          length += (imageWidth * imageHeight);

          imageWidth /= 2;
          imageHeight /= 2;
        }

        // add 1 for the last mipmap (1x1)
        length += 1;

        FieldValidator.checkLength(length, arcSize);
        fm.skip(length);

        //path,name,offset,length,decompLength,exporter
        resources[realNumFiles] = new Resource(path, filename, offset, length);

        TaskProgressManager.setValue(realNumFiles);
        realNumFiles++;
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
