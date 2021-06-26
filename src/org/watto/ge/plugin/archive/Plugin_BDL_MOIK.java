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
import org.watto.datatype.Archive;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.io.FileManipulator;
import org.watto.io.buffer.ByteBuffer;
import org.watto.io.converter.ByteConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_BDL_MOIK extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_BDL_MOIK() {

    super("BDL_MOIK", "BDL_MOIK");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Worms Revolution");
    setExtensions("bdl", "xom"); // MUST BE LOWER CASE
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
      if (fm.readString(4).equals("MOIK")) {
        rating += 50;
      }

      fm.skip(20);

      //long arcSize = fm.getLength();

      // 4 - Number of Types
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

      // 4 - Header (MOIK)
      // 4 - Version? (2) (BIG)
      // 16 - null
      fm.skip(24);

      // 4 - Number of Types
      int numTypes = fm.readInt();
      FieldValidator.checkNumFiles(numTypes);

      // 4 - Unknown
      // 4 - Unknown
      // 28 - null
      fm.skip(36);

      String[] types = new String[numTypes];
      for (int i = 0; i < numTypes; i++) {
        // 4 - Header (TYPE)
        // 4 - Unknown
        // 4 - Unknown
        // 4 - null
        // 16 - Unknown
        fm.skip(32);

        // 32 - Type Name (null terminated, filled with nulls)
        String type = fm.readNullString(32);
        FieldValidator.checkFilename(type);
        types[i] = type;
      }

      // 4 - Header (GUID)
      // 12 - null
      fm.skip(16);

      // 4 - Header (SCHM)
      // 4 - Unknown (1)
      // 8 - null
      fm.skip(16);

      // 4 - Header (STRS)
      fm.skip(4);

      // 4 - Number of Filenames
      int numNames = fm.readInt();
      FieldValidator.checkNumFiles(numNames);

      // 4 - Filename Directory Length
      int nameDirLength = fm.readInt();
      FieldValidator.checkLength(nameDirLength, arcSize);

      int[] nameOffsets = new int[numNames];
      for (int i = 0; i < numNames; i++) {
        // 4 - Filename Offset (relative to the start of the Filename Directory)
        int nameOffset = fm.readInt();
        FieldValidator.checkOffset(nameOffset, nameDirLength);
        nameOffsets[i] = nameOffset;
      }

      byte[] nameBytes = fm.readBytes(nameDirLength);
      FileManipulator nameFM = new FileManipulator(new ByteBuffer(nameBytes));

      String[] names = new String[numNames];
      for (int i = 0; i < numNames; i++) {
        nameFM.seek(nameOffsets[i]);

        // X - Filename
        // 1 - null Filename Terminator
        String name = nameFM.readNullString();

        names[i] = name;
      }

      nameFM.close();

      int numFiles = Archive.getMaxFiles();

      TaskProgressManager.setMaximum(arcSize);
      TaskProgressManager.setIndeterminate(true);

      // Loop through directory
      int realNumFiles = 0;
      long[] offsets = new long[numFiles];
      int[] nameIDs = new int[numFiles];
      //int[] typeIDs = new int[numFiles];

      while (fm.getOffset() < arcSize) {
        if (fm.readByte() == 67) {
          if (fm.readByte() == 84) {
            if (fm.readByte() == 78) {
              if (fm.readByte() == 82) {
                // found the next file
                offsets[realNumFiles] = fm.getOffset() - 4;

                int checkVal = fm.readByte();
                fm.skip(2);

                //if (image){
                // 4 - Header (CTNR)
                // 3 - null
                // 1 or 2 - Image Name
                // 2 - Image Width
                // 2 - Image Height
                // 2 - Number of Mipmaps
                // 2 - Image Format (0=DXT1, 2=DXT5)

                // 1 - Number of Mipmaps
                // for each mipmap
                //   4 - Length of Mipmap Image Data

                // 1 - Number of Mipmaps
                // for each mipmap
                //   4 - Offset to Mipmap Image Data (relative to the start of the Image Data)

                // 4 - Unknown (9=DXT1 / 11=DXT5)
                // 4 - Unknown (1)
                // 4 - Unknown
                // }

                /*
                if (numNames < 256) {
                  nameIDs[realNumFiles] = ByteConverter.unsign(fm.readByte());// - 1;
                }
                else {
                  nameIDs[realNumFiles] = ShortConverter.unsign(fm.readShort());// - 1;
                }
                */
                if (checkVal == 0) {
                  int nameID = ByteConverter.unsign(fm.readByte());
                  if (nameID >= 128) {
                    nameID = ((ByteConverter.unsign(fm.readByte())) << 7) | (nameID & 127);
                  }
                  nameIDs[realNumFiles] = nameID;
                }
                else {
                  nameIDs[realNumFiles] = -1;
                }

                /*
                fm.skip(4);
                typeIDs[realNumFiles] = ByteConverter.unsign(fm.readByte());
                */

                realNumFiles++;
              }
            }
          }
        }
      }

      fm.getBuffer().setBufferSize(64);

      TaskProgressManager.setIndeterminate(false);

      offsets[realNumFiles] = arcSize;

      numFiles = realNumFiles;
      Resource[] resources = new Resource[numFiles];

      TaskProgressManager.setMaximum(numFiles);

      for (int i = 0; i < numFiles; i++) {
        long offset = offsets[i];
        long length = offsets[i + 1] - offset;

        /*
        // read through all the properties
        fm.seek(offset);
        
        int readLength = 0;
        while (readLength < length) {
          // 1 - Type
          int type = ByteConverter.unsign(fm.readByte());
          String typeName = types[type];
          readLength++;
        
          // X - Details
          if (typeName.equals("XPsVertexDataSet")) {
            // 2 - Image Width
            // 2 - Image Height
            fm.skip(4);
        
            readLength += 4;
          }
          else if (typeName.equals("XImage")) {
            // 3 - null
            // 4 - Image Format? (1=DXT1)
            // 4 - Unknown
            // X - Pixels
            readLength = (int) length; // skip to the end
          }
          else if (typeName.equals("XBaseResourceDescriptor")) {
            // 3 - null
            fm.skip(3);
        
            // 1 - Number of Descriptors?
            int numDescriptors = fm.readByte();
        
            // 4 - Image Data Length
            fm.skip(numDescriptors * 4);
        
            readLength += 4 + (numDescriptors * 4);
          }
        }
        */

        String filename;
        int nameID = nameIDs[i];
        if (nameID >= numNames || nameID <= 0) { // name 0 is empty
          filename = Resource.generateFilename(i);
        }
        else {
          filename = names[nameID];
        }

        if (length < 200 && filename.endsWith("tga")) {
          filename = Resource.generateFilename(i);
        }

        /*
        int typeID = typeIDs[i];
        if (typeID >= numTypes) {
          //
        }
        else {
          filename += "." + types[typeID];
        }
        */

        //path,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset, length);

        TaskProgressManager.setValue(i);
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
