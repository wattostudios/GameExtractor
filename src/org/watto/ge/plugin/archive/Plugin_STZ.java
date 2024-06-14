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
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.Exporter_Default;
import org.watto.ge.plugin.exporter.Exporter_ZLib;
import org.watto.io.FileManipulator;
import org.watto.io.converter.IntConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_STZ extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_STZ() {

    super("STZ", "STZ");

    //         read write replace rename
    setProperties(true, false, true, false);

    setGames("Sonic and SEGA All Stars Racing");
    setExtensions("stz"); // MUST BE LOWER CASE
    setPlatforms("Wii");

    // MUST BE LOWER CASE !!!
    //setFileTypes(new FileType("txt", "Text Document", FileType.TYPE_DOCUMENT),
    //             new FileType("bmp", "Bitmap Image", FileType.TYPE_IMAGE)
    //             );

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

      long arcSize = fm.getLength();

      // File Data Offset
      if (FieldValidator.checkOffset(IntConverter.changeFormat(fm.readInt()), arcSize)) {
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

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 4 - File Data Offset (for the first file)
      int numFiles = IntConverter.changeFormat(fm.readInt()) / 12;
      FieldValidator.checkNumFiles(numFiles);

      fm.relativeSeek(0);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      int realNumFiles = 0;
      for (int i = 0; i < numFiles; i++) {

        // 4 - File Offset
        int offset = IntConverter.changeFormat(fm.readInt());
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - Decompressed File Length
        int decompLength = IntConverter.changeFormat(fm.readInt());
        FieldValidator.checkLength(decompLength);

        // 4 - Compressed File Length
        int length = IntConverter.changeFormat(fm.readInt());
        FieldValidator.checkLength(length, arcSize);

        if (length > 0) {
          String filename = Resource.generateFilename(realNumFiles);

          //path,name,offset,length,decompLength,exporter
          resources[realNumFiles] = new Resource(path, filename, offset, length, decompLength, exporter);
          realNumFiles++;
        }

        TaskProgressManager.setValue(i);
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

    if (headerInt1 == 1330398546) {
      return "relo";
    }
    else if (headerInt1 == 1480938576) {
      return "ptex";
    }

    return null;
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

      // read the first file offset, so we know where to stop the directory
      int dataOffset = IntConverter.changeFormat(src.readInt());
      src.relativeSeek(0);

      // skip the directory, lets write the file data first
      fm.setLength(dataOffset);
      fm.seek(dataOffset);

      // Write Files
      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));

      ExporterPlugin exporterZLib = new Exporter_ZLib();
      ExporterPlugin exporterDefault = new Exporter_Default();

      long[] compressedLengths = new long[numFiles];
      int[] paddingSizes = new int[numFiles];
      for (int i = 0; i < resources.length; i++) {
        Resource resource = resources[i];
        long compLength = resource.getLength();
        if (resource.isReplaced()) {
          // need to compress the new file
          compLength = write(exporterZLib, resource, fm);
        }
        else {
          // original file, already compressed, just copy it

          ExporterPlugin originalExporter = resource.getExporter();
          resource.setExporter(exporterDefault);
          write(resource, fm);
          resource.setExporter(originalExporter);
        }

        compressedLengths[i] = compLength;

        int padding = calculatePadding(compLength, 64);
        for (int p = 0; p < padding; p++) {
          fm.writeByte(0);
        }

        paddingSizes[i] = padding;

        TaskProgressManager.setValue(i);
      }

      // write the footer
      int padding = calculatePadding(fm.getOffset(), 32);
      for (int p = 0; p < padding; p++) {
        fm.writeByte(0);
      }

      // now go back and write the directory
      fm.seek(0);

      // Write Directory
      TaskProgressManager.setMessage(Language.get("Progress_WritingDirectory"));
      long offset = dataOffset;

      int numEntries = dataOffset / 12;
      int realNumFiles = 0;

      for (int i = 0; i < numEntries; i++) {

        // 4 - File Offset
        src.skip(4);
        fm.writeInt(IntConverter.changeFormat((int) offset));

        // 4 - Decompressed File Length
        // 4 - Compressed File Length
        int srcDecompLength = IntConverter.changeFormat(src.readInt());
        int srcCompLength = IntConverter.changeFormat(src.readInt());

        if (srcCompLength <= 0) {
          // blank entry
          fm.writeInt(IntConverter.changeFormat((int) srcDecompLength));
          fm.writeInt(IntConverter.changeFormat((int) srcCompLength));
        }
        else {
          // real entry

          Resource resource = resources[realNumFiles];
          long decompLength = resource.getDecompressedLength();
          long length = compressedLengths[realNumFiles];

          fm.writeInt(IntConverter.changeFormat((int) decompLength));
          fm.writeInt(IntConverter.changeFormat((int) length));

          offset += length + paddingSizes[realNumFiles];
          realNumFiles++;
        }

      }

      src.close();
      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

}
