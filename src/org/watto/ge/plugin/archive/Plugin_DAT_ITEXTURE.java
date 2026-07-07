/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2026 wattostudios
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
public class Plugin_DAT_ITEXTURE extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_DAT_ITEXTURE() {

    super("DAT_ITEXTURE", "DAT_ITEXTURE");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Ice and Fire");
    setExtensions("dat"); // MUST BE LOWER CASE
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
    setFileTypes(new FileType("itexture", "Texture Image", FileType.TYPE_IMAGE));

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
      if (fm.readNullString(16).equals("ITexture")) {
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

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      int numFiles = Archive.getMaxFiles();
      int realNumFiles = 0;

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(arcSize);

      // Loop through directory
      while (fm.getOffset() < arcSize) {

        // 16 - File Type (null terminated, filled with nulls)
        String type = fm.readNullString(16);

        // 16 - Filename (null terminated, filled with nulls or junk)
        String filename = fm.readNullString(16);

        long offset = fm.getOffset();

        //System.out.println(type + "\t" + (offset - 32));

        if (type.equals("ITexture")) {
          // 2 - Number of Mipmaps
          short numMipmaps = fm.readShort();

          // 2 - Unknown (0/1)
          fm.skip(2);

          // 2 - Image Width [1<<value]
          int width = (1 << fm.readShort());
          FieldValidator.checkWidth(width);

          // 2 - Image Height [1<<value]
          int height = (1 << fm.readShort());
          FieldValidator.checkWidth(height);

          // for each mipmap
          //   X - Image (8-bit paletted)
          int length = -4; // -4 as we've already read the first mipmap width/height
          for (int m = 0; m < numMipmaps; m++) {
            length += 4 + (width * height);

            width /= 2;
            height /= 2;
          }
          fm.skip(length);
        }
        else if (type.equals("ISprite")) {
          // 1 - Unknown
          fm.skip(1);

          // 1 - Number of Entries
          int numEntries = ByteConverter.unsign(fm.readByte());

          // 2 - Sprite Data Length
          int length = fm.readShort();
          FieldValidator.checkLength(length);

          // 2 - null

          // for each entry
          //   4 - Relative Offset?
          //   1 - Unknown

          // X - Sprite Data
          fm.skip(2 + numEntries * 5 + length);
        }
        else if (type.equals("ITextureMat")) {
          // 4 - null
          // 16 - Filename (null terminated, filled with nulls or junk)
          fm.skip(20);
        }
        else if (type.equals("IAnimSwitchMat")) {
          // 2 - Unknown (2)
          fm.skip(2);

          // 2 - Number of Entries
          int numEntries = fm.readShort();

          // for each entry
          // 16 - Filename (null terminated, filled with nulls or junk)
          fm.skip(numEntries * 16);
        }
        else if (type.equals("ICondSwitchMat")) {
          // 2 - Unknown (2)
          fm.skip(2);

          // 2 - Number of Entries
          int numEntries = fm.readShort();

          // for each entry
          // 16 - Filename (null terminated, filled with nulls or junk)
          fm.skip(numEntries * 16);
        }
        else if (type.equals("ITVScreenMat")) {
          // 2 - null
          fm.skip(2);

          // 2 - Number of Entries
          int numEntries = fm.readShort();

          // for each entry
          // 16 - Filename (null terminated, filled with nulls or junk)
          fm.skip(numEntries * 16);
        }
        else if (type.equals("IPortalMat")) {
          // 2 - null
          fm.skip(2);
        }
        else if (type.equals("IMonoMat")) {
          // 2 - null
          // 1 - Unknown
          fm.skip(3);
        }
        else if (type.equals("IMirrorMat")) {
          // 2 - null
          // 1 - Unknown
          fm.skip(3);
        }
        else if (type.equals("IReservedMat")) {
          // 2 - null
          fm.skip(2);
        }
        else if (type.equals("IWindowMat")) {
          // 2 - null
          fm.skip(2);
        }
        else if (type.equals("IPolySetKind")) {
          // 2 - Number of Entries in Loop 1
          int numEntries1 = fm.readShort();

          // 2 - Number of Entries in Loop 2
          int numEntries2 = fm.readShort();

          // 2 - Unknown
          // 2 - null

          // for each entry in loop 1
          // 12 - Unknown

          // for each entry in loop 2
          // 6 - Unknown

          fm.skip(4 + numEntries1 * 12 + numEntries2 * 6);
        }
        else if (type.equals("IPortalPoly")) {
          // 2 - Number of Entries
          int numEntries = fm.readShort();

          // 2 - Unknown
          // 4 - null

          // for each entry
          //   2 - Unknown

          // 16 - Filename (null terminated, filled with nulls or junk)
          fm.skip(6 + numEntries * 2 + 16);
        }
        else if (type.equals("IPoly")) {
          // 2 - Number of Entries
          int numEntries = fm.readShort();

          // 2 - Unknown
          // 4 - null

          // for each entry
          //   2 - Unknown

          fm.skip(6 + numEntries * 2);
        }
        else if (type.equals("IMapPoly")) {
          // 2 - Number of Entries
          int numEntries = fm.readShort();

          // 2 - Unknown
          // 4 - null
          // 2 - Unknown
          // 2 - Unknown
          // 2 - Unknown
          // 2 - Unknown
          // 2 - Unknown
          // 2 - Unknown

          // for each entry
          //   10 - Unknown
          fm.skip(18 + numEntries * 10);
        }
        else if (type.equals("ISwitchPoly")) {
          // 2 - Number of Entries
          int numEntries = fm.readShort();

          // 2 - Unknown
          // 4 - null

          // for each entry
          //   2 - Unknown

          // 2 - Unknown

          fm.skip(6 + numEntries * 2 + 2);
        }
        else if (type.equals("IAnimKind")) {
          // 2 - Number of Entries
          int numEntries = fm.readShort();

          // for each entry
          // 16 - Filename (null terminated, filled with nulls or junk)
          fm.skip(numEntries * 16);
        }
        else if (type.equals("IBlindKind")) {
          // 2 - null
          // 2 - Unknown
          // 1 - null
          // 2 - Unknown
          // 2 - Unknown
          // 2 - Unknown
          // 16 - Filename (null terminated, filled with nulls or junk)
          fm.skip(27);
        }
        else if (type.equals("ISpriteKind")) {
          // 2 - Number of entries? [1<<value]
          int numEntries = (1 << fm.readShort());
          // 2 - Unknown
          // 4 - Unknown
          // 2 - Unknown

          // for each entry
          // 1 - Unknown
          // 16 - Filename (null terminated, filled with nulls or junk)
          fm.skip(8 + numEntries * 17);
        }
        else if (type.equals("ICellObj")) {
          // 16 - Filename (null terminated, filled with nulls or junk)
          // 32 - Unknown
          // 16 - Filename (null terminated, filled with nulls or junk)
          fm.skip(64);
        }
        else if (type.equals("ICameraObj")) {
          // 70 - Unknown
          fm.skip(70);
        }
        else if (type.equals("IAniBlindObj")) {
          // 64 - Unknown
          fm.skip(64);
        }
        else if (type.equals("IAniSpriteObj")) {
          // 64 - Unknown
          fm.skip(64);
        }
        else if (type.equals("IAniPolySetObj")) {
          // 64 - Unknown
          fm.skip(64);
        }
        else if (type.equals("IPolySetObj")) {
          // 64 - Unknown
          fm.skip(64);
        }
        else if (type.equals("ISpriteObj")) {
          // 64 - Unknown
          fm.skip(64);
        }
        else if (type.equals("IBlindObj")) {
          // 64 - Unknown
          fm.skip(64);
        }
        else {
          ErrorLogger.log("[DAT_ITEXTURE] Unknown entry type: " + type);
          return null;
        }

        int length = (int) (fm.getOffset() - offset);

        filename += "." + type;

        //path,name,offset,length,decompLength,exporter
        resources[realNumFiles] = new Resource(path, filename, offset, length);
        realNumFiles++;

        TaskProgressManager.setValue(offset);
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
