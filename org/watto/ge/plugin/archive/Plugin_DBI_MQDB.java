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
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_DBI_MQDB extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_DBI_MQDB() {

    super("DBI_MQDB", "DBI_MQDB");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Disciples: Sacred Lands");
    setExtensions("dbi", "ff"); // MUST BE LOWER CASE
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
      if (fm.readString(4).equals("MQDB")) {
        rating += 50;
      }

      fm.skip(4);

      // 4 - Unknown (9)
      if (fm.readInt() == 9) {
        rating += 5;
      }

      fm.skip(12);

      long arcSize = fm.getLength();

      // Directory Offset
      if (FieldValidator.checkOffset(fm.readInt(), arcSize)) {
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

      // 4 - Header (MQDB)
      // 4 - null
      // 4 - Unknown (9)
      // 12 - null
      fm.skip(24);

      // 4 - Details Directory Offset
      int dirOffset = fm.readInt();
      FieldValidator.checkOffset(dirOffset, arcSize);

      fm.seek(dirOffset);

      // 4 - Number Of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      int[] ids = new int[numFiles];
      for (int i = 0; i < numFiles; i++) {
        // 4 - File ID? (mostly-incremental from 0)
        ids[i] = fm.readInt();

        // 4 - File Length
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - File Length
        int decompLength = fm.readInt();
        FieldValidator.checkLength(decompLength);

        // 4 - File Offset
        int offset = fm.readInt() + 28;
        FieldValidator.checkOffset(offset, arcSize);

        if (length == decompLength) {
          length -= 28;
          decompLength -= 28;
        }
        else {
          // only change the compLength
          length -= 28;
        }

        String filename = Resource.generateFilename(i);

        //path,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset, length, decompLength);

        TaskProgressManager.setValue(i);
      }

      /*
      // read the filenames
      fm.getBuffer().setBufferSize(8); // small quick reads
      
      for (int i = 4; i < numFiles; i++) {
        Resource resource = resources[i];
      
        long offset = resource.getOffset();
        fm.seek(offset);
      
        // 8 - Filename (filled with nulls)
        String filename = fm.readNullString(8);
      
        resource.setOffset(offset + 20);
        if (resource.isCompressed()) {
          resource.setLength(resource.getLength() - 20);
        }
        else {
          resource.setLength(resource.getLength() - 20);
          resource.setDecompressedLength(resource.getDecompressedLength() - 20);
        }
      
        resource.setName(filename);
        resource.setOriginalName(filename);
      }
      */

      String[] names = null;
      int[] nameIDs = null;
      if (numFiles > 4) {
        for (int i = 0; i < 4; i++) {
          // Find the file with the filenames in it
          Resource resource = resources[i];
          long offset = resource.getOffset();
          if (offset == 92) {
            offset += 4; // this one (FF) has an additional 4-byte field
          }

          int length = (int) resource.getLength() + 28; // as we subtract 28 in the original loop
          if (length <= 32) {
            continue; // nope, try again
          }

          // work out the size of the name fields (256 in FF files, 8 in DMI files)

          int numNames = length / 16;
          int difference = numFiles - numNames;
          if (difference >= 0 && difference < 5) {
            fm.seek(offset);

            // yep - 16-byte entries
            names = new String[numNames];
            nameIDs = new int[numNames];

            for (int f = 0; f < numNames; f++) {

              // 12 - Filename
              String filename = fm.readNullString(12);
              names[f] = filename;

              // 4 - File ID
              int fileID = fm.readInt();
              nameIDs[f] = fileID;
            }

            break;
          }
          else {
            numNames = length / 260;
            difference = numFiles - numNames;
            if (difference >= 0 && difference < 5) {
              fm.seek(offset);

              // yep - 260-byte entries
              names = new String[numNames];
              nameIDs = new int[numNames];

              for (int f = 0; f < numNames; f++) {

                // 256 - Filename
                String filename = fm.readNullString(256);
                names[f] = filename;

                // 4 - File ID
                int fileID = fm.readInt();
                nameIDs[f] = fileID;
              }

              break;

            }
            else {
              // this file isn't the right one
            }
          }

        }
      }

      if (names != null) {
        int numNames = names.length;

        for (int i = 0; i < numFiles; i++) {
          int id = ids[i];

          boolean found = false;

          for (int f = i; f < numNames; f++) {
            // look from the current position onwards, which should be pretty close
            if (nameIDs[f] == id) {
              // found it
              Resource resourceID = resources[i];
              String filename = names[f];
              if (filename != null) {
                resourceID.setName(filename);
                resourceID.setOriginalName(filename);

                found = true;
                break;
              }
            }
          }

          if (!found) {
            for (int f = 0; f < i; f++) {
              // look from the current position onwards, which should be pretty close
              if (nameIDs[f] == id) {
                // found it
                Resource resourceID = resources[i];
                String filename = names[f];
                if (filename != null) {
                  resourceID.setName(filename);
                  resourceID.setOriginalName(filename);

                  found = true;
                  break;
                }
              }
            }
          }

        }
      }

      fm.close();

      return resources;

    }
    catch (

    Throwable t) {
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
