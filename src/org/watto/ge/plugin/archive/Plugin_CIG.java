/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2024 wattostudios
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

import org.watto.Language;
import org.watto.Settings;
import org.watto.datatype.Archive;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.io.FileManipulator;
import org.watto.io.converter.ShortConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_CIG extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_CIG() {

    super("CIG", "CIG");

    //         read write replace rename
    setProperties(true, false, true, false);

    setGames("Growlanser Generations");
    setExtensions("cig"); // MUST BE LOWER CASE
    setPlatforms("PS2");

    //setFileTypes(new FileType("txt", "Text Document", FileType.TYPE_DOCUMENT),
    //             new FileType("bmp", "Bitmap Image", FileType.TYPE_IMAGE)
    //             );

    setEnabled(false);

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

      int numFiles = Archive.getMaxFiles();
      int realNumFiles = 0;

      Resource[] resources = new Resource[numFiles];

      while (fm.getOffset() < arcSize) {
        long relativeOffset = fm.getOffset();

        // 4 - Number Of Files
        int localNumFiles = fm.readInt();

        try {

          if (localNumFiles == 1141178368 || localNumFiles == -2080047104) {
            // D-block
            //System.out.println(relativeOffset + "\tD-block");

            // 4 - Unknown
            // 4 - Unknown
            // 4 - Unknown
            fm.skip(12);

            // 4 - File Data Length (excluding footer data)
            int length = fm.readInt();
            FieldValidator.checkLength(length, arcSize);

            // X - File Data
            // X - Footer Data
            String filename = Resource.generateFilename(realNumFiles) + ".d";

            //path,name,offset,length,decompLength,exporter
            resources[realNumFiles] = new Resource(path, filename, relativeOffset, length);

            long nextOffset = relativeOffset + length;
            nextOffset += calculatePadding(nextOffset, 2048);
            fm.seek(nextOffset);

            realNumFiles++;
            continue;
          }
          else if (localNumFiles == 2) {
            // Texture Block
            //System.out.println(relativeOffset + "\tTexture");

            // 4 - Texture Length (including these header fields)
            int length = fm.readInt();
            FieldValidator.checkLength(length, arcSize);

            // 4 - Texture Header (TMX0)
            String textureHeader = fm.readString(4);
            if (!textureHeader.equals("TMX0")) {
              // not a texture
              //System.out.println(relativeOffset + "\t" + localNumFiles);
              fm.seek(relativeOffset + 2048); //  move to the next 2048 and try again
              continue;
            }
            String filename = Resource.generateFilename(realNumFiles) + ".tmx0";

            //path,name,offset,length,decompLength,exporter
            resources[realNumFiles] = new Resource(path, filename, relativeOffset, length);

            long nextOffset = relativeOffset + length;
            nextOffset += calculatePadding(nextOffset, 2048);
            fm.seek(nextOffset);

            realNumFiles++;
            continue;
          }

          //FieldValidator.checkNumFiles(localNumFiles);
          FieldValidator.checkRange(localNumFiles, 1, 1000);

          TaskProgressManager.setMaximum(localNumFiles);

          // Loop through directory
          boolean readBigDirectory = false;
          for (int i = 0; i < localNumFiles; i++) {

            // 2 - File Offset [*2048]
            int offsetShort = ShortConverter.unsign(fm.readShort());
            if (i == 0 && offsetShort != 1) {
              // not a short directory

              if (offsetShort == (localNumFiles * 8) + 8) {
                // a big directory
                fm.relativeSeek(relativeOffset);
                fm.skip(4);
                readBigDirectory = true;
                break;
              }

              //System.out.println(relativeOffset + "\t" + localNumFiles);
              fm.seek(relativeOffset + 2048); //  move to the next 2048 and try again
              continue;
            }
            long offset = (offsetShort * 2048) + relativeOffset;
            FieldValidator.checkOffset(offset, arcSize);

            // 2 - File Length [*2048] (including padding)
            int length = (ShortConverter.unsign(fm.readShort()) * 2048);
            FieldValidator.checkLength(length, arcSize);

            String filename = Resource.generateFilename(realNumFiles) + ".short";

            //path,name,offset,length,decompLength,exporter
            resources[realNumFiles] = new Resource(path, filename, offset, length);

            TaskProgressManager.setValue(i);
            realNumFiles++;
          }

          if (readBigDirectory) {
            for (int i = 0; i < localNumFiles; i++) {

              // 4 - File Offset
              long offset = fm.readInt() + relativeOffset;
              FieldValidator.checkOffset(offset, arcSize);

              // 4 - File Length
              int length = fm.readInt();
              FieldValidator.checkLength(length, arcSize);

              String filename = Resource.generateFilename(realNumFiles) + ".long";

              //path,name,offset,length,decompLength,exporter
              resources[realNumFiles] = new Resource(path, filename, offset, length);

              TaskProgressManager.setValue(i);
              realNumFiles++;
            }
            //System.out.println(relativeOffset + "\tLong Directory");

            // get the last offset + length, so we know where the next block starts
            Resource lastResource = resources[realNumFiles - 1];
            long nextOffset = lastResource.getOffset() + lastResource.getLength();
            nextOffset += calculatePadding(nextOffset, 2048);

            if (nextOffset < relativeOffset) {
              nextOffset = relativeOffset + 2048;
            }

            fm.seek(nextOffset);
          }
          else {
            //System.out.println(relativeOffset + "\tShort Directory");

            // get the last offset + length, so we know where the next block starts
            Resource lastResource = resources[realNumFiles - 1];
            long nextOffset = lastResource.getOffset() + lastResource.getLength();
            if (nextOffset < relativeOffset) {
              nextOffset = relativeOffset + 2048;
            }
            fm.seek(nextOffset);
          }
        }
        catch (Throwable t) {
          //System.out.println(relativeOffset + "\t" + localNumFiles);
          fm.seek(relativeOffset + 2048); //  move to the next 2048 and try again
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
   * Writes an [archive] File with the contents of the Resources. The archive is written using
   * data from the initial archive - it isn't written from scratch.
   **********************************************************************************************
   **/
  @Override
  public void replace(Resource[] resources, File path) {
    try {

      FileManipulator fm = new FileManipulator(path, true);
      FileManipulator src = new FileManipulator(new File(Settings.getString("CurrentArchive")), false);

      int numFiles = resources.length;
      TaskProgressManager.setMaximum(numFiles);

      // Calculations
      TaskProgressManager.setMessage(Language.get("Progress_PerformingCalculations"));

      long archiveSize = 16;
      long directorySize = 0;
      long filesSize = 0;
      for (int i = 0; i < numFiles; i++) {
        filesSize += resources[i].getDecompressedLength();
        directorySize += 8 + resources[i].getNameLength() + 1;
      }
      archiveSize += filesSize + directorySize;

      // Write Header Data

      // 4 - file header (GABA)
      // 4 - Version (4)
      // 4 - numFiles
      // 4 - Entry Size (64)
      fm.writeBytes(src.readBytes(16));

      // 4 - Header (BIGF)
      fm.writeString("BIGF");
      src.skip(4);

      // 4 - Archive Size
      fm.writeInt(archiveSize);
      src.skip(4);

      // 4 - Number Of Files
      fm.writeInt(numFiles);
      src.skip(4);

      // 4 - Directory Size
      fm.writeInt(directorySize);
      src.skip(4);

      // Write Directory
      TaskProgressManager.setMessage(Language.get("Progress_WritingDirectory"));
      long offset = 16 + (64 * numFiles);
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];
        long length = resource.getDecompressedLength();

        // X - Filename (null)
        fm.writeNullString(resource.getName());

        // 4 - File Offset
        fm.writeInt(offset);

        // 4 - File Length
        fm.writeInt(length);

        src.skip(40);

        // 4 - Audio Quality (22050)
        // 4 - File Type? (6,28,37)
        // 4 - Unknown
        // 12 - null
        fm.writeBytes(src.readBytes(24));

        offset += length;
      }

      // Write Files
      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));
      write(resources, fm);

      //ExporterPlugin exporter = new Exporter_ZLib();
      //long[] compressedLengths = write(exporter,resources,fm);

      src.close();
      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

}
