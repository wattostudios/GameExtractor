/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2026 wattostudios
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
import org.watto.datatype.FileType;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.Exporter_LZ11;
import org.watto.io.FileManipulator;
import org.watto.io.converter.ByteConverter;
import org.watto.io.converter.IntConverter;
import org.watto.io.converter.ShortConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_XPF extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_XPF() {

    super("XPF", "XPF");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Wizards of Waverly Place");
    setExtensions("xpf"); // MUST BE LOWER CASE
    setPlatforms("Nintendo DS");

    // MUST BE LOWER CASE !!!
    setFileTypes(new FileType("rgcn", "Nintendo Character Graphic Resource", FileType.TYPE_IMAGE),
        new FileType("rlcn", "Nintendo Color Resource", FileType.TYPE_PALETTE),
        new FileType("recn", "Nintendo Cell Resource", FileType.TYPE_PALETTE),
        new FileType("rnan", "Nintendo Animation Resource", FileType.TYPE_OTHER),
        new FileType("rtfn", "Nintendo Font Resource", FileType.TYPE_OTHER),
        new FileType("bmd0", "Nintendo Binary Model", FileType.TYPE_OTHER),
        new FileType("bca0", "Nintendo Character Animation", FileType.TYPE_OTHER),
        new FileType("bva0", "Nintendo Vis Animation", FileType.TYPE_OTHER));

    //setTextPreviewExtensions("colours", "rat", "screen", "styles"); // LOWER CASE

    setCanScanForFileTypes(true);

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
      if (fm.readInt() == -1159991567) {
        rating += 50;
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

      ExporterPlugin exporterLZ11 = Exporter_LZ11.getInstance();

      // RESETTING GLOBAL VARIABLES
      currentPalette = -1;

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 4 - Header (241,238,219,186)
      fm.skip(4);

      // 1 - Number of Names
      int numNames = ByteConverter.unsign(fm.readByte());

      int version = 0;

      for (int i = 0; i < numNames; i++) {
        // 2 - Name Length [*2 for Unicode]
        int nameLength = fm.readShort();
        FieldValidator.checkFilenameLength(nameLength);

        // X - Name (unicode)
        String name = fm.readUnicodeString(nameLength);

        // 4 - Version
        if (version == 0 && name.equals("Engine")) {
          version = fm.readInt();
        }
        else {
          fm.skip(4);
        }

        // 4 - Unknown
        fm.skip(4);
      }

      long relativeOffset = fm.getOffset();

      Resource[] resources = null;
      boolean[] compression = null;
      int numFiles = 0;

      if (version >= 0x20) {
        // 4 - Unknown
        fm.skip(4);

        // 2 - Number of Filenames
        int numFilenames = ShortConverter.unsign(fm.readShort());
        FieldValidator.checkNumFiles(numFilenames);

        for (int i = 0; i < numFilenames; i++) {
          // 2 - File ID?
          // 4 - Unknown
          fm.skip(6);

          // 2 - Filename Length [*2 for Unicode]
          int filenameLength = fm.readShort();
          FieldValidator.checkFilenameLength(filenameLength);

          // X - Filename (unicode)
          fm.skip(filenameLength * 2);
          //System.out.println(fm.readUnicodeString(filenameLength));
        }

        // 2 - Number of Files
        numFiles = ShortConverter.unsign(fm.readShort());
        FieldValidator.checkNumFiles(numFiles);

        resources = new Resource[numFiles];
        compression = new boolean[numFiles];
        TaskProgressManager.setMaximum(numFiles);

        // Loop through directory
        for (int i = 0; i < numFiles; i++) {

          // 2 - Unknown
          fm.skip(2);

          // 2 - Compression Flags (0=uncompressed, <0=lz11)
          short flags = fm.readShort();
          compression[i] = (flags < 0);

          // 4 - File Offset (relative to the start of the Details Directory)
          long offset = fm.readInt() + relativeOffset;
          FieldValidator.checkOffset(offset, arcSize);

          String filename = Resource.generateFilename(i);

          //path,name,offset,length,decompLength,exporter
          resources[i] = new Resource(path, filename, offset);

          TaskProgressManager.setValue(i);
        }

      }
      else if (version >= 0x10) {
        // 4 - Unknown
        fm.skip(4);

        // 2 - Number of Filenames
        int numFilenames = ShortConverter.unsign(fm.readShort());
        FieldValidator.checkNumFiles(numFilenames);

        for (int i = 0; i < numFilenames; i++) {
          // 2 - File ID?
          // 4 - Unknown
          fm.skip(6);

          // 2 - Filename Length [*2 for Unicode]
          int filenameLength = fm.readShort();
          FieldValidator.checkFilenameLength(filenameLength);

          // X - Filename (unicode)
          fm.skip(filenameLength * 2);
        }

        // 2 - Number of Files
        numFiles = ShortConverter.unsign(fm.readShort());
        FieldValidator.checkNumFiles(numFiles);

        // 2 - Number of Entries
        int numEntries = ShortConverter.unsign(fm.readShort());
        FieldValidator.checkNumFiles(numEntries);

        // for each entry
        //   2 - Unknown
        //   2 - Offset ID
        //   2 - Unknown
        //   2 - Unknown
        //   2 - Unknown
        fm.skip(numEntries * 10);

        resources = new Resource[numFiles];
        compression = new boolean[numFiles];
        TaskProgressManager.setMaximum(numFiles);

        // Loop through directory
        for (int i = 0; i < numFiles; i++) {

          // 2 - Unknown
          fm.skip(2);

          // 2 - Compression Flags (0=uncompressed, <0=lz11)
          short flags = fm.readShort();
          compression[i] = (flags < 0);

          // 4 - File Offset (relative to the start of the Details Directory)
          long offset = fm.readInt() + relativeOffset;
          FieldValidator.checkOffset(offset, arcSize);

          String filename = Resource.generateFilename(i);

          //path,name,offset,length,decompLength,exporter
          resources[i] = new Resource(path, filename, offset);

          TaskProgressManager.setValue(i);
        }

      }
      else {

        // 2 - Number of Filenames
        int numFilenames = ShortConverter.unsign(fm.readShort());
        FieldValidator.checkNumFiles(numFilenames);

        for (int i = 0; i < numFilenames; i++) {
          // 2 - File ID?
          fm.skip(2);

          // 2 - Filename Length [*2 for Unicode]
          int filenameLength = fm.readShort();
          FieldValidator.checkFilenameLength(filenameLength);

          // X - Filename (unicode)
          fm.skip(filenameLength * 2);
        }

        // 2 - Number of Files
        numFiles = ShortConverter.unsign(fm.readShort());
        FieldValidator.checkNumFiles(numFiles);

        // for each file
        //   2 - Unknown
        fm.skip(numFiles * 2);

        resources = new Resource[numFiles];
        compression = new boolean[numFiles];
        TaskProgressManager.setMaximum(numFiles);

        // Loop through directory
        for (int i = 0; i < numFiles; i++) {

          // 2 - Unknown
          fm.skip(2);

          // 2 - Compression Flags (0=uncompressed, <0=lz11)
          short flags = fm.readShort();
          compression[i] = (flags < 0);

          // 4 - File Offset (relative to the start of the Details Directory)
          long offset = fm.readInt() + relativeOffset;
          FieldValidator.checkOffset(offset, arcSize);

          String filename = Resource.generateFilename(i);

          //path,name,offset,length,decompLength,exporter
          resources[i] = new Resource(path, filename, offset);

          TaskProgressManager.setValue(i);
        }

      }

      calculateFileSizes(resources, arcSize);

      // Now go through and read the compression details
      fm.getBuffer().setBufferSize(6);

      for (int i = 0; i < numFiles; i++) {
        if (compression[i]) {
          Resource resource = resources[i];

          long offset = resource.getOffset();
          fm.seek(offset);

          long length = resource.getLength();

          // 1 - Compression Type (17)
          int compressionType = fm.readByte();
          if (compressionType != 17) {
            ErrorLogger.log("[XPF] Unknown compression Type: " + compressionType);
          }

          /*
          // 1 (Optional) - Flags (128/192)
          int flags = ByteConverter.unsign(fm.readByte());
          if (flags == 128 || flags == 192) {
            // there were flags
            offset += 6;
            length -= 6;
          }
          else {
            // there were no flags, go back a byte
            fm.relativeSeek(offset + 1);
            offset += 5;
            length -= 5;
          }
          
          
          // 4 - Decompressed Length
          int decompLength = fm.readInt();
          //FieldValidator.checkLength(decompLength);
           
           */

          // 3 - Decompressed Length
          int decompLength = IntConverter.convertLittle(new byte[] { fm.readByte(), fm.readByte(), fm.readByte(), 0 });
          FieldValidator.checkLength(decompLength);

          offset += 4;
          length -= 4;

          // X - Compressed File Data (LZ11 Compression)
          resource.setOffset(offset);
          resource.setLength(length);
          resource.setDecompressedLength(decompLength);
          resource.setExporter(exporterLZ11);

        }
        else {
          continue;
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

  int currentPalette = -1;

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

    if (headerInt1 == 1313231954) {
      return "rtfn";
    }
    else if (headerInt1 == 1313033298) {
      // Palette file. Remember how many palettes we have, so we can join them to the RGCN resources as defaults.

      currentPalette++;

      return "rlcn";
    }
    else if (headerInt1 == 1313032018) {
      // Graphic file. Attach a default palette to each one

      int paletteID = currentPalette;
      if (paletteID == -1) {
        paletteID = 0; // don't have a palette loaded yet, use the first palette by default 
      }
      resource.addProperty("PaletteID", paletteID);

      return "rgcn";
    }
    else if (headerInt1 == 1313031506) {
      return "recn";
    }
    else if (headerInt1 == 1312902738) {
      return "rnan";
    }
    else if (headerInt1 == 809782594) {
      return "bmd0";
    }
    else if (headerInt1 == 809583426) {
      return "bca0";
    }
    else if (headerInt1 == 809588290) {
      return "bva0";
    }

    return null;
  }

}
