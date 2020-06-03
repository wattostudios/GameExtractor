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
import org.watto.component.WSPluginManager;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.exporter.Exporter_ZLib;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_PAK_52 extends ArchivePlugin {

  int currentFile = 0;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_PAK_52() {

    super("PAK_52", "PAK_52");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Rebel Galaxy");
    setExtensions("pak"); // MUST BE LOWER CASE
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

      // 2 - Unknown (1)
      if (fm.readShort() == 1) {
        rating += 5;
      }

      // 4 - null
      if (fm.readInt() == 0) {
        rating += 5;
      }

      long arcSize = fm.getLength();

      // Directory Offset
      if (FieldValidator.checkOffset(fm.readInt(), arcSize)) {
        rating += 5;
      }

      fm.skip(8);

      // 4 - Compressed File Length
      if (FieldValidator.checkLength(fm.readInt(), arcSize)) {
        rating += 5;
      }

      // 1 - ZLib compression header
      if (fm.readString(1).equals("x")) {
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

      ExporterPlugin exporter = Exporter_ZLib.getInstance();

      // RESETTING GLOBAL VARIABLES
      currentFile = 0;

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 2 - Unknown (1)
      // 4 - null
      fm.skip(6);

      // 4 - Directory Offset
      int dirOffset = fm.readInt();
      FieldValidator.checkOffset(dirOffset, arcSize);

      fm.seek(dirOffset);

      // 4 - Number Of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 4 - Number of Folders?
      int numFolders = fm.readInt();
      FieldValidator.checkNumFiles(numFolders);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      //for (int i = 0; i < numFolders; i++) {
      readDirectory(path, arcSize, fm, resources, "");
      //}

      numFiles = currentFile;
      resources = resizeResources(resources, numFiles);

      // Go through the archive and get the file lengths
      fm.getBuffer().setBufferSize(8);

      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];
        fm.seek(resource.getOffset());

        // 4 - Decompressed File Length
        int decompLength = fm.readInt();
        FieldValidator.checkLength(decompLength);

        // 4 - Compressed File Length
        int length = fm.readInt();
        FieldValidator.checkOffset(length, arcSize);

        // X - File Data (ZLib Compression)
        long offset = fm.getOffset();

        resource.setOffset(offset);
        resource.setLength(length);
        resource.setDecompressedLength(decompLength);
        resource.setExporter(exporter);

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
   * 
   **********************************************************************************************
   **/
  @SuppressWarnings("unused")
  public void readDirectory(File path, long arcSize, FileManipulator fm, Resource[] resources, String parentDirName) {
    try {

      // 2 - Folder Name Length [*2 for unicode]
      short folderNameLength = fm.readShort();
      FieldValidator.checkFilenameLength(folderNameLength);

      // X - Folder Name Length (unicode) (including the / at the end)
      //String folderName = parentDirName + fm.readUnicodeString(folderNameLength);
      String folderName = fm.readUnicodeString(folderNameLength);

      //System.out.println("Folder: " + folderName);

      // 4 - Number of Entries in this Folder?
      int numFiles = fm.readInt();
      FieldValidator.checkRange(numFiles, 0, resources.length);

      // Loop through directory
      int numSubFolders = 0;
      for (int i = 0; i < numFiles; i++) {

        // 4 - Hash?
        fm.skip(4);

        //     1 - Entry Type (8=sub-directory, 15=file)
        int entryType = fm.readByte();

        if (entryType == 8) {
          // 2 - Folder Name Length [*2 for unicode]
          short subFolderNameLength = fm.readShort();
          FieldValidator.checkFilenameLength(subFolderNameLength);

          // X - Folder Name Length (unicode) (including the / at the end)
          String subFolderName = fm.readUnicodeString(subFolderNameLength);

          //System.out.println("\tSubFolder: " + subFolderName);

          // 8 - null
          fm.skip(8);

          numSubFolders++;
        }
        else {

          // 2 - Filename Length [*2 for unicode]
          short filenameLength = fm.readShort();
          FieldValidator.checkFilenameLength(filenameLength);

          // X - Filename Length (unicode)
          String filename = folderName + fm.readUnicodeString(filenameLength);

          //System.out.println("\tFilename: " + filename);

          // 4 - File Offset
          int offset = fm.readInt();
          FieldValidator.checkOffset(offset, arcSize);

          // 4 - Unknown
          fm.skip(4);

          //path,name,offset,length,decompLength,exporter
          resources[currentFile] = new Resource(path, filename, offset);

          TaskProgressManager.setValue(currentFile);
          currentFile++;

        }

      }

      // for each sub-directory in this folder
      for (int i = 0; i < numSubFolders; i++) {
        readDirectory(path, arcSize, fm, resources, folderName);
      }

    }
    catch (Throwable t) {
      logError(t);
    }
  }

  /**
   **********************************************************************************************
   Provide hints to the previewer so that certain document types are displayed appropriately
   **********************************************************************************************
   **/
  @Override
  public ViewerPlugin previewHint(Resource resource) {
    String extension = resource.getExtension();
    if (extension.equalsIgnoreCase("anno") || extension.equalsIgnoreCase("compositor") || extension.equalsIgnoreCase("fontdef") || extension.equalsIgnoreCase("frag") || extension.equalsIgnoreCase("fx") || extension.equalsIgnoreCase("hlsl") || extension.equalsIgnoreCase("material") || extension.equalsIgnoreCase("program") || extension.equalsIgnoreCase("pu") || extension.equalsIgnoreCase("vert")) {
      return (ViewerPlugin) WSPluginManager.getPlugin("Viewer", "TXT");
    }
    return null;
  }

}
