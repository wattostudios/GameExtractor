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
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_BIN_TIMX extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_BIN_TIMX() {

    super("BIN_TIMX", "BIN_TIMX");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Dynasty Warriors 5");
    setExtensions("bin"); // MUST BE LOWER CASE
    setPlatforms("XBox");

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
      if (fm.readString(4).equals("TIMX")) {
        rating += 50;
      }

      fm.skip(8);

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

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      int numFiles = Archive.getMaxFiles();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(arcSize);

      int realNumFiles = 0;
      while (fm.getOffset() < arcSize) {
        long offset = fm.getOffset();

        // 4 - Header
        String header = fm.readString(4);

        if (header.equals("TIMX")) {
          // 4 - Unknown
          // 4 - Unknown
          fm.skip(8);

          // 4 - File Length
          int length = fm.readInt();
          FieldValidator.checkLength(length, arcSize);

          fm.skip(length - 16);

          String filename = Resource.generateFilename(realNumFiles) + "." + header.toLowerCase();

          //path,name,offset,length,decompLength,exporter
          resources[realNumFiles] = new Resource(path, filename, offset, length);
          realNumFiles++;

          TaskProgressManager.setValue(offset);
        }
        else if (header.equals("MESC")) {
          // 4 - Unknown
          fm.skip(4);

          // 4 - File Length
          int length = fm.readInt();
          FieldValidator.checkLength(length, arcSize);

          fm.skip(length - 12);

          String filename = Resource.generateFilename(realNumFiles) + "." + header.toLowerCase();

          //path,name,offset,length,decompLength,exporter
          resources[realNumFiles] = new Resource(path, filename, offset, length);
          realNumFiles++;

          TaskProgressManager.setValue(offset);
        }
        else if (header.equals("SWX ")) {
          // keep skipping blocks of 2048 until we find the next one

          boolean found = false;

          long nextOffset = offset;

          int paddingSize = calculatePadding(nextOffset, 2048);
          if (paddingSize == 0) {
            nextOffset += 2048;
          }
          else {
            nextOffset += paddingSize;
          }

          while (nextOffset < arcSize) {
            fm.seek(nextOffset);
            String headerCheck = fm.readString(4);
            //System.out.println(offset + "\t" + headerCheck);
            if (headerCheck.equals("TIMX") || headerCheck.equals("SWX ") || headerCheck.equals("MESC")) {
              fm.relativeSeek(nextOffset);
              found = true;
              break;
            }
            else {
              nextOffset += 2048;
            }
          }

          if (!found) { // we're lost
            //return null;
            break;
          }

          long length = nextOffset - offset;
          FieldValidator.checkLength(length, arcSize);

          String filename = Resource.generateFilename(realNumFiles) + "." + header.toLowerCase();

          //path,name,offset,length,decompLength,exporter
          resources[realNumFiles] = new Resource(path, filename, offset, length);
          realNumFiles++;

          TaskProgressManager.setValue(offset);
        }
        else {

          //System.out.println(offset + "\t" + header);

          // if the offset is not a multiple of 2048, skip to that multiple
          int paddingSize = calculatePadding(offset, 2048);
          if (paddingSize > 4) {
            fm.skip(paddingSize - 4); // -4 because we've already read 4 bytes for the header
            continue;
          }
          else {
            // otherwise try to find the next TIMX header within the next 2048 bytes
            boolean found = false;
            int byte1 = fm.readByte();
            int byte2 = fm.readByte();
            int byte3 = fm.readByte();
            int byte4 = fm.readByte();
            for (int b = 3; b < 2048; b++) {
              if (byte1 == 84 && byte2 == 73 && byte3 == 77 && byte4 == 88) {
                // found the header
                fm.relativeSeek(fm.getOffset() - 4);
                found = true;
                break;
              }
              else {
                byte1 = byte2;
                byte2 = byte3;
                byte3 = byte4;
                byte4 = fm.readByte();
              }
            }

            if (!found) {
              // just keep skipping over blocks of 2048 bytes until we find the next TIMX

              while (offset < arcSize) {
                fm.seek(offset);
                String headerCheck = fm.readString(4);
                //System.out.println(offset + "\t" + headerCheck);
                if (headerCheck.equals("TIMX") || headerCheck.equals("SWX ") || headerCheck.equals("MESC")) {
                  fm.relativeSeek(offset);
                  found = true;
                  break;
                }
                else {
                  offset += 2048;
                }
              }
            }

            if (found) {
              continue;
            }

            // couldn't find anything, so we're lost.
            //return null;
            break;
          }
        }

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
