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
import org.watto.datatype.Archive;
import org.watto.datatype.FileType;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.io.FileManipulator;
import org.watto.io.converter.StringConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_PPF_PPAK extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_PPF_PPAK() {

    super("PPF_PPAK", "PPF_PPAK");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Psychonauts");
    setExtensions("ppf"); // MUST BE LOWER CASE
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
    setFileTypes(new FileType("1tx", "Texture Image", FileType.TYPE_IMAGE));

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
      if (fm.readString(4).equals("PPAK")) {
        rating += 50;
      }

      if (fm.readInt() == 130557) {
        rating += 5;
      }

      // Number Of Files
      if (FieldValidator.checkNumFiles(fm.readShort() + 2)) { // +2 to allow -1 as a real value
        rating += 5;
      }

      fm.skip(4);

      long arcSize = fm.getLength();

      // First File Length
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

      FileManipulator fm = new FileManipulator(path, false, 128); // small and quick reads

      long arcSize = fm.getLength();

      // 4 - Header (PPAK)
      // 4 - Unknown (130557)
      fm.skip(8);

      // 2 - Number of Files
      int numFiles = fm.readShort();

      Resource[] resources = null;

      if (numFiles == -1) {
        // MULTIPLE BLOCKS
        fm.seek(8);

        numFiles = Archive.getMaxFiles();

        resources = new Resource[numFiles];
        TaskProgressManager.setMaximum(arcSize);

        int realNumFiles = 0;

        // Loop through directory
        while (fm.getOffset() < arcSize) {
          // 2 - Number of Files in the Block
          short numFilesInBlock = fm.readShort();
          if (numFilesInBlock == -1) {
            // 4 - Unknown
            // 4 - Unknown
            fm.skip(6);
            numFilesInBlock = fm.readShort();
          }
          else if (numFilesInBlock == 20557) {
            break;
          }

          FieldValidator.checkNumFiles(numFilesInBlock);

          for (int i = 0; i < numFilesInBlock; i++) {

            // 4 - File Type (reversed String)
            String extension = StringConverter.reverse(fm.readString(4)).trim();

            // 4 - File Length (not including these 2 header fields)
            int length = fm.readInt();
            FieldValidator.checkLength(length, arcSize);

            long offset = fm.getOffset();
            //System.out.println(offset);

            String filename = null;
            if (extension.equals("1TX")) {
              // 40 - Unknown
              fm.skip(40);

              // 2 - Filename Length (including Null Terminator)
              short filenameLength = fm.readShort();
              FieldValidator.checkFilenameLength(filenameLength);

              // X - Filename
              filename = fm.readNullString(filenameLength) + "." + extension;

              // 1 - null Filename Terminator
              // 4 - Unknown
              // 4 - Image Format? (11)
              // 4 - null
              // 4 - Unknown
              // 4 - Image Width
              // 4 - Image Height
              // 4 - Image Format?
              // 4 - Mipmap Count
              // 8 - Unknown
              // 8 - null
              // X - Image Data
              fm.skip(length - (fm.getOffset() - offset));
            }
            else {
              filename = Resource.generateFilename(i);
            }

            //path,name,offset,length,decompLength,exporter
            resources[realNumFiles] = new Resource(path, filename, offset, length);
            realNumFiles++;

            TaskProgressManager.setValue(offset);
          }
        }

        resources = resizeResources(resources, realNumFiles);
      }
      else {
        // A SINGLE BLOCK

        FieldValidator.checkNumFiles(numFiles);

        resources = new Resource[numFiles];
        TaskProgressManager.setMaximum(numFiles);

        // Loop through directory
        for (int i = 0; i < numFiles; i++) {

          // 4 - File Type (reversed String)
          String extension = StringConverter.reverse(fm.readString(4)).trim();

          // 4 - File Length (not including these 2 header fields)
          int length = fm.readInt();
          FieldValidator.checkLength(length, arcSize);

          long offset = fm.getOffset();
          //System.out.println(offset);

          String filename = null;
          if (extension.equals("1TX")) {
            // 40 - Unknown
            fm.skip(40);

            // 2 - Filename Length (including Null Terminator)
            short filenameLength = fm.readShort();
            FieldValidator.checkFilenameLength(filenameLength);

            // X - Filename
            filename = fm.readNullString(filenameLength) + "." + extension;

            // 1 - null Filename Terminator
            // 4 - Unknown
            // 4 - Image Format? (11)
            // 4 - null
            // 4 - Unknown
            // 4 - Image Width
            // 4 - Image Height
            // 4 - Image Format?
            // 4 - Mipmap Count
            // 8 - Unknown
            // 8 - null
            // X - Image Data
            fm.skip(length - (fm.getOffset() - offset));
          }
          else {
            filename = Resource.generateFilename(i);
          }

          //path,name,offset,length,decompLength,exporter
          resources[i] = new Resource(path, filename, offset, length);

          TaskProgressManager.setValue(i);
        }
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
