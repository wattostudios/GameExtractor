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
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.Exporter_LZMA;
import org.watto.ge.plugin.exporter.Exporter_ZLib_CompressedSizeOnly;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_DZ_DTRZ extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_DZ_DTRZ() {

    super("DZ_DTRZ", "DZ_DTRZ");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Doomdarks Revenge",
        "Talisman",
        "The Lords of Midnight");
    setExtensions("dz"); // MUST BE LOWER CASE
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
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

      // Header
      if (fm.readString(4).equals("DTRZ")) {
        rating += 50;
      }

      // Number Of Files
      if (FieldValidator.checkNumFiles(fm.readShort())) {
        rating += 5;
      }

      // Number Of Files
      if (FieldValidator.checkNumFiles(fm.readShort())) {
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

      ExporterPlugin exporterLZMA = Exporter_LZMA.getInstance();
      ExporterPlugin exporterZLib = Exporter_ZLib_CompressedSizeOnly.getInstance();

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 4 - Header (DTRZ)
      fm.skip(4);

      // 2 - Number of Files
      short numFiles = fm.readShort();
      FieldValidator.checkNumFiles(numFiles);

      // 2 - Number of Folders
      short numFolders = fm.readShort();
      FieldValidator.checkNumFiles(numFolders);

      // 1 - null
      fm.skip(1);

      String[] filenames = new String[numFiles];
      for (int i = 0; i < numFiles; i++) {
        // X - Filename
        // 1 - null Filename Terminator
        String filename = fm.readNullString();
        FieldValidator.checkFilename(filename);
        filenames[i] = filename;

        //System.out.println(filename);
      }

      //System.out.println("---BREAK---");

      String[] folderNames = new String[numFolders];
      for (int i = 0; i < numFolders - 1; i++) {
        // X - Folder Name
        // 1 - null Folder Name Terminator
        String filename = fm.readNullString();
        FieldValidator.checkFilename(filename);
        folderNames[i] = filename;

        //System.out.println(filename);
      }

      folderNames[numFolders - 1] = "";

      //System.out.println("---BREAK---");
      //System.out.println(fm.getOffset());

      int maxNumFiles = numFiles + 50;

      int[] fileIDs = new int[maxNumFiles];
      int[] folderIDs = new int[maxNumFiles];
      for (int i = 0; i < numFiles; i++) {
        // 2 - Folder ID
        short folderID = fm.readShort();
        FieldValidator.checkRange(folderID, 0, numFolders);

        // 2 - File ID
        short fileID = fm.readShort();
        FieldValidator.checkRange(fileID, 0, maxNumFiles);
        fileIDs[fileID] = i;
        folderIDs[fileID] = folderID;

        while (fileID != -1) {
          // 2 - File ID
          fileID = fm.readShort();

          if (fileID == -1) {
            break;
          }

          FieldValidator.checkRange(fileID, 0, numFiles);
          fileIDs[fileID] = i;
          folderIDs[fileID] = folderID;
        }

      }

      //System.out.println(fm.getOffset());

      // 2 - Unknown (1)
      fm.skip(2);

      // 2 - Number of Files
      numFiles = fm.readShort();
      FieldValidator.checkNumFiles(numFiles);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        // 4 - File Offset
        int offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        //System.out.println("--" + offset);

        // 4 - Compressed Length (NOT RIGHT - IN SOME ARCHIVES THIS IS ALSO THE DECOMP LENGTH)
        int length = fm.readInt();
        //FieldValidator.checkLength(length, arcSize);
        FieldValidator.checkLength(length);

        // 4 - Decompressed Length
        int decompLength = fm.readInt();
        FieldValidator.checkLength(decompLength);

        // 4 - Unknown (256)
        int compressionType = fm.readInt();

        String filename = filenames[fileIDs[i]];
        int folderID = folderIDs[i] - 1;
        if (folderID >= 0 && folderID < numFolders) {
          filename = folderNames[folderID] + "\\" + filename;
        }

        if (compressionType == 512) {
          // LZMA86HEAD Compression

          //path,name,offset,length,decompLength,exporter
          resources[i] = new Resource(path, filename, offset, length, decompLength, exporterLZMA);
        }
        else if (compressionType == 8) {
          // ZLib Compression

          //path,name,offset,length,decompLength,exporter
          resources[i] = new Resource(path, filename, offset, length, decompLength, exporterZLib);
        }
        else {
          // No Compression

          //path,name,offset,length,decompLength,exporter
          resources[i] = new Resource(path, filename, offset, length, decompLength);
        }

        TaskProgressManager.setValue(i);
      }

      // now work out the real compressed lengths
      for (int i = 0; i < numFiles - 1; i++) {
        Resource resource = resources[i];
        long compLength = resources[i + 1].getOffset() - resource.getOffset();
        FieldValidator.checkLength(compLength, arcSize);
        resource.setLength(compLength);
      }
      Resource lastResource = resources[numFiles - 1];
      long lastLength = arcSize - lastResource.getOffset();
      lastResource.setLength(lastLength);

      fm.close();

      return resources;

    }
    catch (Throwable t) {
      logError(t);
      return null;
    }
  }

}
