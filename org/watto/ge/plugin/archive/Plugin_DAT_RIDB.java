/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2020 wattostudios
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
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.io.FileManipulator;
import org.watto.io.FilenameSplitter;
import org.watto.io.converter.ByteConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_DAT_RIDB extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_DAT_RIDB() {

    super("DAT_RIDB", "DAT_RIDB");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Duke Nukem Forever");
    setExtensions("dat"); // MUST BE LOWER CASE
    setPlatforms("PC");

    //setFileTypes(new FileType("txt", "Text Document", FileType.TYPE_DOCUMENT),
    //             new FileType("bmp", "Bitmap Image", FileType.TYPE_IMAGE)
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

      if (FilenameSplitter.getFilename(fm.getFile()).contains("BumpMaps")) {
        rating += 25;
      }
      else {
        rating = 0;
      }

      File dirFile = new File(fm.getFile().getAbsoluteFile().getParent() + File.separatorChar + "BumpMapDirectory.dat");
      if (dirFile.exists()) {
        rating += 24;
      }
      else {
        rating = 0;
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

      long arcSize = (int) path.length();

      String basePath = path.getAbsoluteFile().getParent();
      File sourcePath = new File(basePath + File.separatorChar + "BumpMapDirectory.dat");
      FileManipulator fm = new FileManipulator(sourcePath, false);

      // 4 - Header (RIDB)
      // 4 - Unknown
      // 4 - Unknown
      // 4 - Unknown
      fm.skip(16);

      // 1-5 - Number of Archives
      int numArchives = (int) PluginGroup_U.readIndex(fm);
      FieldValidator.checkNumFiles(numArchives);

      File[] archiveFiles = new File[numArchives];
      for (int i = 0; i < numArchives; i++) {
        // 1-5 - Filename Length (including null terminator)
        int nameLength = (int) PluginGroup_U.readIndex(fm);

        // X - Filename
        // 1 - null Filename Terminator
        String name = fm.readNullString(nameLength);

        // 4 - Unknown
        fm.skip(4);

        File archiveFile = new File(basePath + File.separatorChar + name + ".dat");
        if (!archiveFile.exists()) {
          return null;
        }
        archiveFiles[i] = archiveFile;
      }

      // 1-5 - Number of Files
      int numFiles = (int) PluginGroup_U.readIndex(fm);
      FieldValidator.checkNumFiles(numFiles);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      int[][] nameIDs = new int[numFiles][0];
      int[] indexCount = new int[numFiles];
      for (int i = 0; i < numFiles; i++) {

        // 4 - Number of Filename Indexes
        int numIndexes = fm.readInt();
        indexCount[i] = numIndexes;

        int[] indexes = new int[numIndexes];
        for (int j = 0; j < numIndexes; j++) {
          // 1-5 - Filename Index
          int nameIndex = (int) PluginGroup_U.readIndex(fm);
          indexes[j] = nameIndex;
        }
        nameIDs[i] = indexes;

        // 4 - Archive ID Number
        int archiveID = fm.readInt();
        FieldValidator.checkRange(archiveID, 0, numArchives);

        File archiveFile = archiveFiles[archiveID];

        // 4 - File Offset
        long offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 9 - Unknown
        fm.skip(9);

        //path,name,offset,length,decompLength,exporter
        resources[i] = new Resource(archiveFile, "", offset);

        TaskProgressManager.setValue(i);
      }

      // 1-5 - Number of Names
      //int numNames = (int) PluginGroup_U.readIndex(fm);
      //FieldValidator.checkNumFiles(numNames);
      //int numNames = ShortConverter.unsign(fm.readShort());
      int numNames = fm.readInt();
      FieldValidator.checkNumFiles(numNames);

      String[] names = new String[numNames];
      for (int i = 0; i < numNames; i++) {
        // 1-5 - Filename Length (including null terminator)
        int nameLength = (int) PluginGroup_U.readIndex(fm);

        // X - Filename
        // 1 - null Filename Terminator
        String name = fm.readNullString(nameLength);
        names[i] = name;
      }

      // Set the filenames
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];

        String filename = null;
        for (int j = 0; j < indexCount[i]; j++) {
          int nameID = nameIDs[i][j];
          if (nameID != 0) {
            FieldValidator.checkRange(nameID, 0, numNames);
            String namePiece = names[nameID];
            if (namePiece.equals("")) {
              continue;
            }
            if (filename == null) {
              filename = namePiece;
            }
            else {
              filename += "\\" + namePiece;
            }
          }
        }

        if (filename != null) {
          filename += ".dxtmipmap";
          resource.setName(filename);
          resource.setOriginalName(filename);
          resource.forceNotAdded(true);
        }
      }

      //calculateFileSizes(resources, arcSize);

      // Now we need to go to the offset in each archive file, to read the real offset and image width/height
      fm.close();

      if (numFiles == 0) {
        return null;
      }

      File currentFile = resources[0].getSource();
      fm = new FileManipulator(currentFile, false);
      arcSize = fm.getLength();

      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];

        File sourceFile = resource.getSource();
        if (!currentFile.equals(sourceFile)) {
          fm.close();
          currentFile = sourceFile;
          fm = new FileManipulator(currentFile, false);
          arcSize = fm.getLength();

          //System.out.println(currentFile.getName());
        }

        long offset = resource.getOffset();
        fm.seek(offset);

        //System.out.println(offset);

        // 4 - Image Width
        int width = fm.readInt();
        FieldValidator.checkWidth(width);

        // 4 - Image Height
        int height = fm.readInt();
        FieldValidator.checkHeight(height);

        // 36 - Unknown
        fm.skip(36);

        // 1 - size flag
        int sizeFlag = ByteConverter.unsign(fm.readByte());

        if ((sizeFlag & 1) == 1) {
          // 3 - Unknown
          fm.skip(3);
        }
        else {
          // 1 - Unknown
          fm.skip(1);
        }

        // 1 - Number of Mipmaps
        int numMipmaps = fm.readByte();

        // 4 - Texture Data Offset
        offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        int length = width * height;
        length += 4; // to cater for the 1-5 unreal header
        FieldValidator.checkLength(length, arcSize);

        resource.setOffset(offset);
        resource.setLength(length);
        resource.setDecompressedLength(length);
        resource.addProperty("Width", width);
        resource.addProperty("Height", height);
        resource.addProperty("MipmapCount", numMipmaps);
        resource.addProperty("ImageFormat", "DXT5");
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
