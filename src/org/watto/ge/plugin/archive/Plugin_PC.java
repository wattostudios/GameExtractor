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
import org.watto.Language;
import org.watto.Settings;
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
public class Plugin_PC extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_PC() {

    super("PC", "PC");

    //         read write replace rename
    setProperties(true, false, true, false);

    setGames("Evil Dead");
    setExtensions("pc"); // MUST BE LOWER CASE
    setPlatforms("PC");

    setFileTypes(new FileType("pc_wav_arc", "Audio Archive", FileType.TYPE_ARCHIVE),
        new FileType("pc_mesh", "3D Mesh", FileType.TYPE_MODEL));

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

      fm.skip(12);

      if (fm.readInt() == 20) {
        rating += 5;
      }

      if (fm.readInt() + 20 == fm.getLength()) {
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

      // 4 - Unknown
      // 4 - Unknown
      // 4 - Unknown
      // 4 - Header Length (20)
      // 4 - Archive Length [+20]
      fm.skip(20);

      // 4 - Number Of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      int[] fileTypes = new int[numFiles];
      for (int i = 0; i < numFiles; i++) {
        // 3 - Flags?
        fm.skip(3);

        // 1 - File Type
        int fileType = ByteConverter.unsign(fm.readByte());
        fileTypes[i] = fileType;

        // 4 - File Header Type? (0/1)
        fm.skip(4);

        // 4 - File Offset
        long offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - File Length (including file header fields)
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        String extension = "." + fileType;
        if (fileType == 8) {
          extension = ".jpg"; // or dds, or bmp
        }
        else if (fileType == 14) {
          extension = ".pc_wav_arc";
        }
        else if (fileType == 13) {
          extension = ".pc_wav_arc";
        }
        else if (fileType == 11) {
          extension = ".pc_mesh";
        }
        else if (fileType == 17 || fileType == 49 || fileType == 81 || fileType == 113 || fileType == 177 || fileType == 145 || fileType == 209 || fileType == 241) {
          extension = ".txt";
        }

        String filename = Resource.generateFilename(i) + extension;

        //path,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset, length);

        TaskProgressManager.setValue(i);
      }

      fm.getBuffer().setBufferSize(128);

      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];
        long offset = resource.getOffset();

        fm.relativeSeek(offset);

        int fileType = fileTypes[i];

        if (fileType == 8 || fileType == 11) {
          int length = (int) resource.getLength();

          // 128 - Filename (null terminated, filled with nulls)
          String filename = fm.readNullString(128);

          if (filename.startsWith("\\\\")) {
            filename = filename.substring(2);

            // 4 - Unknown
            offset += 132;
            length -= 132;
          }
          else {
            fm.relativeSeek(offset);

            fm.skip(4);

            filename = fm.readNullString(128);

            if (filename.contains(".dds")) {
              // ok

              // 4 - Unknown
              offset += 136;
              length -= 136;
            }
            else if (filename.contains(".bmp")) {
              // ok

              offset += 132;
              length -= 132;
            }
            else {
              // not a filename
              continue;
            }

          }

          resource.setOffset(offset);
          resource.setLength(length);
          resource.setDecompressedLength(length);

          resource.setName(filename);
          resource.setOriginalName(filename);
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

      // Write Header Data

      // 4 - Unknown
      // 4 - Unknown
      // 4 - Unknown
      // 4 - Header Length (20)
      fm.writeBytes(src.readBytes(16));

      // 4 - Archive Length [+20]
      fm.writeBytes(src.readBytes(4)); // we'll replace this with the real value later

      // 4 - Number of Files
      fm.writeBytes(src.readBytes(4));

      // Write Directory
      TaskProgressManager.setMessage(Language.get("Progress_WritingDirectory"));
      long offset = 16024; //20 + 4 + (numFiles * 16);
      int[] srcOffsets = new int[numFiles];
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];
        long length = resource.getDecompressedLength();

        // 3 - Flags?
        // 1 - File Type
        fm.writeBytes(src.readBytes(4));

        // 4 - File Header Type? (0/1)
        fm.writeBytes(src.readBytes(4));

        // 4 - File Offset
        srcOffsets[i] = src.readInt();
        fm.writeInt(offset);

        // 4 - File Length (including file header fields)
        String extension = resource.getExtension();
        if (extension.equals("jpg")) {
          length += 132;
        }
        else if (extension.equals("dds")) {
          length += 136;
        }

        fm.writeInt(length);
        src.skip(4);

        offset += length;
      }

      // X - Padding (byte 255) to offset 16024
      int paddingSize = 16024 - (20 + 4 + (numFiles * 16));
      for (int p = 0; p < paddingSize; p++) {
        fm.writeByte(255);
      }

      // Write Files
      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];

        src.relativeSeek(srcOffsets[i]);

        String extension = resource.getExtension();
        if (extension.equals("jpg")) {
          // 128 - Filename (null terminated, filled with nulls)
          // 4 - Unknown
          fm.writeBytes(src.readBytes(132));
        }
        else if (extension.equals("bmp")) {
          // 4 - Unknown
          // 128 - Filename (null terminated, filled with nulls)
          fm.writeBytes(src.readBytes(132));
        }
        else if (extension.equals("dds")) {
          // 4 - Unknown
          // 128 - Filename (null terminated, filled with nulls)
          // 4 - Unknown
          fm.writeBytes(src.readBytes(136));
        }

        write(resource, fm);
        TaskProgressManager.setValue(i);
      }

      // go back and write the archive length into the header
      fm.seek(16);
      fm.writeInt(fm.getLength() - 20);

      src.close();
      fm.close();

    }
    catch (Throwable t) {
      logError(t);
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

    String extension = resource.getExtension();
    if (extension != null && !extension.equals("")) {
      return extension;
    }

    if (headerInt1 == 1 && headerInt2 == 8) {
      return "pc_mesh";
    }

    return null;
  }

}
