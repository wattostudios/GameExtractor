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
import org.watto.datatype.FileType;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.io.FileManipulator;
import org.watto.io.buffer.ByteBuffer;
import org.watto.io.converter.ByteConverter;
import org.watto.io.converter.IntConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_ARC_ARCC extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_ARC_ARCC() {

    super("ARC_ARCC", "ARC_ARCC");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Street Racing Syndicate");
    setExtensions("arc"); // MUST BE LOWER CASE
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
    setFileTypes(new FileType("arc_tex", "Texture Image", FileType.TYPE_IMAGE),
        new FileType("arc_faces", "3D Mesh Face Indices", FileType.TYPE_MODEL),
        new FileType("arc_verts", "3D Mesh Vertices", FileType.TYPE_MODEL));

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
      if (fm.readString(4).equals("ARCC")) {
        rating += 50;
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

      // 4 - Header (ARCC)
      fm.skip(4);

      // 4 - Number Of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // for each group (60)
      //  2 - Unknown ID (usually null)
      fm.skip(120);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      int dataOffset = (int) fm.getOffset() + (numFiles * 16);

      // Grab the filenames directory offset and length
      fm.skip((numFiles - 1) * 16);

      // 4 - null
      fm.skip(4);

      // 4 - Filename Directory Offset
      int nameDirOffset = fm.readInt() + dataOffset;
      FieldValidator.checkOffset(nameDirOffset, arcSize);

      int nameDirLength = (int) (arcSize - nameDirOffset);

      fm.relativeSeek(nameDirOffset);

      byte[] nameDirBytes = fm.readBytes(nameDirLength);
      FileManipulator nameFM = new FileManipulator(new ByteBuffer(nameDirBytes));

      // Loop through directory
      fm.relativeSeek(128);

      int realNumFiles = 0;
      String namePrefix = "";
      int facesCount = 1;
      int vertsCount = 1;
      for (int i = 0; i < numFiles; i++) {

        // 4 - Unknown
        fm.skip(4);

        // 4 - File Offset (relative to First File Offset) (exclude all =0 except for the first one)
        int offset = fm.readInt() + dataOffset;
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - Name Offset (relative to the start of the Names Directory)
        int nameOffset = fm.readInt();

        String filename = Resource.generateFilename(realNumFiles);

        if (nameOffset != -1) {
          FieldValidator.checkOffset(nameOffset, nameDirLength);

          nameFM.seek(nameOffset);
          // X - Filename
          // 1 - null Filename Terminator
          filename = nameFM.readNullString();
          FieldValidator.checkFilename(filename);

          filename = namePrefix + filename;
        }
        else {
          namePrefix = "";
        }

        // 1 - File Type ID? (1=Image, 15=3D Faces, 16=3D Vertices, 253=Filenames, 255=Empty File)
        // 3 - File Length (BIG)
        byte[] lengthBytes = fm.readBytes(4);
        int fileType = ByteConverter.unsign(lengthBytes[0]);
        lengthBytes[0] = 0;

        int length = IntConverter.convertBig(lengthBytes);
        FieldValidator.checkLength(length, arcSize);

        if (fileType == 255) {
          namePrefix = filename + "\\";
        }

        if (length == 0) {
          continue;
        }

        if (fileType == 1) {
          filename += ".arc_tex";
        }
        else if (fileType == 15) {
          filename = "Faces " + facesCount + ".arc_faces";
          facesCount++;
        }
        else if (fileType == 16) {
          filename = "Verts " + vertsCount + ".arc_verts";
          vertsCount++;
        }
        else if (fileType == 253) {
          filename = "Filenames.names";
        }

        //path,name,offset,length,decompLength,exporter
        resources[realNumFiles] = new Resource(path, filename, offset, length);

        TaskProgressManager.setValue(i);
        realNumFiles++;
      }

      resources = resizeResources(resources, realNumFiles);

      nameFM.close();

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

    if ((headerInt3 >= 1 || headerInt3 <= 10) && (headerInt1 > 0 && headerInt1 <= 2048) && (headerInt2 > 0 && headerInt2 <= 2048)) {
      return "arc_tex";
    }

    int length = (int) resource.getLength();

    if (headerInt1 == (length - 4) / 2) {
      return "arc_faces";
    }
    else if (headerInt2 == 36 && headerInt1 == (length - 12) / 36) {
      return "arc_verts";
    }

    return null;
  }

}
