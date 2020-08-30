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
import org.watto.ErrorLogger;
import org.watto.Language;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.exporter.Exporter_ZLib_CompressedSizeOnly;
import org.watto.io.FileManipulator;
import org.watto.io.FilenameSplitter;
import org.watto.io.converter.IntConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_WTD_RSC extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_WTD_RSC() {

    super("WTD_RSC", "WTD_RSC");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("GTA 4");
    setExtensions("wtd"); // MUST BE LOWER CASE
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
      if (fm.readString(4).equals("RSC" + (char) 5)) {
        rating += 50;
      }

      // 4 - Resource Type (8=WTD)
      if (fm.readInt() == 8) {
        rating += 5;
      }

      fm.skip(4);

      // X - compressed data
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
   Decompressed an archive, where the whole archive is compressed.
   Reads the compressed block information first, then processes the compressed blocks themselves.
   Writes the output to a file with the same name, but with "_ge_decompressed" at the end of it.
   The decompressed file contains the same Unreal header as the compressed file, so you can open
   the decompressed file in GE directly, without needing to re-decompress anything.
   If the decompressed file already exists, we use that, we don't re-decompress.
   **********************************************************************************************
   **/
  public FileManipulator decompressArchive(FileManipulator fm, int compLength) {
    try {
      // Build a new "_ge_decompressed" archive file in the current directory
      File origFile = fm.getFile();

      String pathOnly = FilenameSplitter.getDirectory(origFile);
      String filenameOnly = FilenameSplitter.getFilename(origFile);
      String extensionOnly = FilenameSplitter.getExtension(origFile);

      File decompFile = new File(pathOnly + File.separatorChar + filenameOnly + "_ge_decompressed" + "." + extensionOnly);
      if (decompFile.exists()) {
        // we've already decompressed this file before - open and return it
        return new FileManipulator(decompFile, false);
      }

      FileManipulator decompFM = new FileManipulator(decompFile, true);

      // Now decompress the block into the decompressed file
      TaskProgressManager.setMessage(Language.get("Progress_DecompressingArchive")); // progress bar
      TaskProgressManager.setMaximum(999); // progress bar
      TaskProgressManager.setIndeterminate(true);

      Exporter_ZLib_CompressedSizeOnly exporter = Exporter_ZLib_CompressedSizeOnly.getInstance();
      exporter.open(fm, compLength, compLength);

      while (exporter.available()) {
        decompFM.writeByte(exporter.read());
      }

      // Force-write out the decompressed file to write it to disk, then change the buffer to read-only.
      decompFM.close();
      decompFM = new FileManipulator(decompFile, false);

      TaskProgressManager.setMessage(Language.get("Progress_ReadingArchive")); // progress bar
      TaskProgressManager.setIndeterminate(false);

      // Return the file pointer to the beginning, and return the decompressed file
      return decompFM;
    }
    catch (Throwable t) {
      ErrorLogger.log(t);
      return null;
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

      // 4 - Header ("RSC" + (byte)5)
      // 4 - Resource Type (8=WTD)
      // 4 - Unknown
      fm.skip(12);

      // X - ZLib Compressed Image Data
      FileManipulator decompFM = decompressArchive(fm, (int) (arcSize - 12));
      if (decompFM != null) {
        fm.close(); // close the original archive
        fm = decompFM; // now we're going to read from the decompressed file instead
        fm.seek(0); // go to the same point in the decompressed file as in the compressed file

        path = fm.getFile(); // So the resources are stored against the decompressed file
        arcSize = path.length();
      }

      // 4 - Header? ((byte)132,83,105,0)
      // 4 - Header Length (32) (&= 0x0fffffff)
      // 4 - null
      // 4 - Unknown (1)
      // 4 - Hash Directory Offset (&= 0x0fffffff)
      fm.skip(20);

      // 2 - Number Of Images
      short numFiles = fm.readShort();
      FieldValidator.checkNumFiles(numFiles);

      // 2 - Number Of Images
      fm.skip(2);

      // 4 - Offsets Directory Offset (&= 0x0fffffff)
      int offsetsDirOffset = fm.readInt() & 0x0fffffff;
      FieldValidator.checkOffset(offsetsDirOffset, arcSize);

      // 2 - Number Of Images
      // 2 - Number Of Images
      // 4 - null
      // 524 - Padding (byte 205)
      fm.seek(offsetsDirOffset);

      int[] entryOffsets = new int[numFiles];
      int largestOffset = 0;
      for (int i = 0; i < numFiles; i++) {
        // 4 - Offset to an Entry in the Details Directory (&= 0x0fffffff)
        int entryOffset = fm.readInt() & 0x0fffffff;
        FieldValidator.checkOffset(offsetsDirOffset, arcSize);
        entryOffsets[i] = entryOffset;

        if (entryOffset + 80 > largestOffset) {
          largestOffset = entryOffset + 80;
        }
      }

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      int[] filenameOffsets = new int[numFiles];

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        fm.relativeSeek(entryOffsets[i]);
        // 4 - Unknown (7019924)
        // 4 - null
        // 4 - Unknown (65536)
        // 8 - null
        fm.skip(20);

        // 4 - Filename Offset (&= 0x0fffffff)
        int filenameOffset = fm.readInt() & 0x0fffffff;
        FieldValidator.checkOffset(filenameOffset, arcSize);
        filenameOffsets[i] = filenameOffset;

        // 4 - null
        fm.skip(4);

        // 2 - Image Width
        short width = fm.readShort();
        FieldValidator.checkWidth(width);

        // 2 - Image Height
        short height = fm.readShort();
        FieldValidator.checkHeight(height);

        // 4 - Image Format Header (DXT1/DXT5)
        String imageFormat = fm.readString(4);

        // 2 - Stride Size
        // 1 - Type
        fm.skip(3);

        // 1 - Mipmap Count
        int mipmapCount = fm.readByte();

        // 4 - Unknown
        // 4 - Unknown
        // 4 - Unknown
        // 4 - Unknown
        // 4 - Unknown
        // 4 - Unknown
        // 4 - Previous Texture Entry Offset (&= 0x0fffffff)
        // 4 - Next Texture Entry Offset (&= 0x0fffffff)
        fm.skip(32);

        // 4 - Image Data Offset (relative to the start of the IMAGE DATA) (&= 0x0fffffff)
        int offset = (fm.readInt() & 0x0fffffff);
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - null
        fm.skip(4);

        // work out the length
        int length = 0;
        if (imageFormat.equals("DXT1")) {
          length = width * height / 2;
        }
        else if (imageFormat.equals("DXT3") || imageFormat.equals("DXT5")) {
          length = width * height;
        }
        else {
          int imageFormatInt = IntConverter.convertLittle(imageFormat.getBytes());
          if (imageFormatInt == 21) {
            // RGBA
            length = width * height * 4;
            imageFormat = "RGBA";
          }
          else if (imageFormatInt == 50) {
            // 8BitPaletted
            length = width * height;
            imageFormat = "8BitPaletted";
          }
          else {
            ErrorLogger.log("[WTD_RSC] Unknown Image Format: " + imageFormat + "  " + imageFormatInt);
          }
        }

        int mipmapLength = length;
        for (int m = 1; m < mipmapCount; m++) {
          mipmapLength /= 4;
          length += mipmapLength;
          if (mipmapLength < 16) {
            if (mipmapLength < 8 && imageFormat.equals("DXT1")) {
              mipmapLength = 8;
            }
            else {
              mipmapLength = 16;
            }
          }

        }

        //path,name,offset,length,decompLength,exporter
        Resource resource = new Resource(path, "", offset, length);
        resource.addProperty("Width", width);
        resource.addProperty("Height", height);
        resource.addProperty("ImageFormat", imageFormat);
        resource.forceNotAdded(true);
        resources[i] = resource;

        TaskProgressManager.setValue(i);
      }

      // now get the filenames
      for (int i = 0; i < numFiles; i++) {
        fm.seek(filenameOffsets[i]);

        // X - Filename (null terminated)
        String filename = fm.readNullString();
        FieldValidator.checkFilename(filename);

        int currentOffset = (int) fm.getOffset();
        currentOffset += calculatePadding(currentOffset, 32);

        if (currentOffset > largestOffset) {
          largestOffset = currentOffset;
        }

        if (filename.startsWith("pack:/")) {
          filename = filename.substring(6);
        }

        //path,name,offset,length,decompLength,exporter
        Resource resource = resources[i];
        resource.setName(filename);
        resource.setOriginalName(filename);
        //resource.setOffset(resource.getOffset() + offsetDifference);

        TaskProgressManager.setValue(i);
      }

      largestOffset += calculatePadding(largestOffset, 4096);

      // now update the offsets
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];
        resource.setOffset(resource.getOffset() + largestOffset);
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
