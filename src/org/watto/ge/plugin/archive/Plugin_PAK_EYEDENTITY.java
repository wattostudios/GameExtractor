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
import org.watto.Language;
import org.watto.Settings;
import org.watto.component.WSPopup;
import org.watto.datatype.Archive;
import org.watto.datatype.FileType;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.Exporter_Custom_LZ77EA_970;
import org.watto.ge.plugin.exporter.Exporter_ZLib;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************
Description of compression...

https://pastebin.com/rGpBFwAV

        The compression is an LZ77 variant. It requires 3 parameters:
                Copy offset: Move backwards by this amount of bytes and start copying a certain number of bytes following that position.
                Copy length: How many bytes to copy. If the length is larger than the offset, start at the offset again and copy the same values again.
                Proceed length: The number of bytes that were not compressed and can be read directly.
       
        Note that the offset is defined in regards to the already decompressed data which e.g. does not contain any compression metadata.
       
        The three values are split up however; while the copy length and proceed length are
        stated together in a single byte, before an uncompressed section, the relevant offset
        is given after the uncompressed section:
                Use the proceed length to read the uncompressed data, at which point you arrive at the start of the offset value.
                Read this value, then move to the offset and copy a number of bytes (given by copy length)
                to the decompressed data. Afterwards, the next copy and proceed length are given and the process starts anew.
       
        The offset has a constant size of 2 bytes, in little endian.
       
        The two lengths share the same byte. The first half of the byte belongs to the proceed length,
        whereas the second half belongs to the copy length.
       
        When the half-byte of the proceed length is f, then the length is extended by another byte,
        which is placed directly after the byte that contains both lengths. The value of that byte
        is added to the value of the proceed length (i.e. f). However, if the extra byte is ff, one more
        byte is read (and so on) and all values are added together.
       
        The copy length can be extended in the same manner. However, the possible extra bytes are
        located at the end, right after the offset.
        Additionally, a constant value of 4 is added to obtain the actual copy length.
       
        Finally, it is possible that a file ends without specifying an offset (as the last few bytes
        in the file were not compressed). The proceed length is not affected by that (and the copy
        length is of no relevance).
**********************************************************************************************
**/
public class Plugin_PAK_EYEDENTITY extends ArchivePlugin {

  String compressionType = "LZ77EA_970";

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_PAK_EYEDENTITY() {

    super("PAK_EYEDENTITY", "PAK_EYEDENTITY");

    //         read write replace rename
    setProperties(true, false, true, false);

    setGames("Dragon Nest");
    setExtensions("pak", "rfs"); // MUST BE LOWER CASE
    setPlatforms("PC");

    setFileTypes(new FileType("act", "Action", FileType.TYPE_OTHER),
        new FileType("ani", "Animation", FileType.TYPE_OTHER),
        new FileType("cam", "Camera", FileType.TYPE_OTHER),
        new FileType("eff", "Effect", FileType.TYPE_OTHER),
        new FileType("lua", "LUA Script", FileType.TYPE_DOCUMENT),
        new FileType("msh", "Mesh", FileType.TYPE_OTHER),
        new FileType("ptc", "Particle", FileType.TYPE_OTHER),
        new FileType("skn", "Skin", FileType.TYPE_OTHER),
        new FileType("ui", "User Interface", FileType.TYPE_OTHER));

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
      if (fm.readString(32).equals("EyedentityGames Packing File 0.1")) {
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

      ExporterPlugin exporterZLib = Exporter_ZLib.getInstance();
      //ExporterPlugin exporterLZ77 = new Exporter_QuickBMS_Decompression("LZ77EA_970");
      ExporterPlugin exporterLZ77 = Exporter_Custom_LZ77EA_970.getInstance();

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 32 - Header (EyedentityGames Packing File 0.1)
      // 224 - null Padding to offset 256
      // 4 - Unknown (11/13)
      fm.skip(260);

      // 4 - Number of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 4 - Directory Offset
      int dirOffset = fm.readInt();
      FieldValidator.checkOffset(dirOffset, arcSize);

      fm.seek(dirOffset);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {

        // 256 - Filename (filled with nulls)
        fm.skip(1); // skip the "\" at the beginning, or the "null" byte for a file in the root directory 
        String filename = fm.readNullString(255);
        FieldValidator.checkFilename(filename);
        if (filename.charAt(filename.length() - 1) == (char) 2) { // funny characters at the end of 1 file, which trip up QuickBMS
          filename = filename.substring(0, filename.length() - 1);
        }

        // 4 - Compressed Length
        int firstLength = fm.readInt();
        FieldValidator.checkLength(firstLength, arcSize);

        // 4 - Decompressed Length
        int decompLength = fm.readInt();
        FieldValidator.checkLength(decompLength);

        // 4 - Compressed Length (again)
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);
        length = firstLength; // Use the First Length - the Second Length seems to trip up the compression

        // 4 - File Offset
        int offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - Unknown
        // 40 - null
        fm.skip(44);

        if (length == decompLength) {
          // no compression

          //path,name,offset,length,decompLength,exporter
          resources[i] = new Resource(path, filename, offset, length);
        }
        else {
          // zlib compression

          //path,name,offset,length,decompLength,exporter
          resources[i] = new Resource(path, filename, offset, length, decompLength, exporterLZ77);
        }

        TaskProgressManager.setValue(i);
      }

      // now go through - some archives use LZ77EA_970 compression, others use ZLib, so detect the right type here...
      fm.getBuffer().setBufferSize(1);
      compressionType = "LZ77EA_970";
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];

        fm.seek(resource.getOffset());
        if (fm.readString(1).equals("x")) {
          compressionType = "ZLib";
          // actually using ZLib compression
          resource.setExporter(exporterZLib);
        }
        // otherwise, leave as LZ77EA_970 compression, as set in the original loop
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

      // Ask what compression algorithm they want to use.
      String[] compressionOptions = new String[] { "LZ77EA_970 (RFS)", "ZLib (PAK)" };
      String defaultOption = "LZ77EA_970 (RFS)";
      if (compressionType.equals("ZLib")) {
        defaultOption = "ZLib (PAK)";
      }

      String selectedValue = WSPopup.showOption("ChooseCompressionType", compressionOptions, defaultOption);

      ExporterPlugin exporter = new Exporter_Custom_LZ77EA_970();
      if (selectedValue.equals("ZLib (PAK)")) {
        exporter = new Exporter_ZLib();
      }

      // show the progressbar, as it would have been hidden by the option popup above.
      TaskProgressManager.show(2, 0, Language.get("Progress_WritingArchive"));
      TaskProgressManager.setIndeterminate(true, 0); // first 1 is indeterminate
      TaskProgressManager.setMaximum(Archive.getNumFiles(), 1); // second one shows how many files are done
      TaskProgressManager.startTask();

      // Calculations
      TaskProgressManager.setMessage(Language.get("Progress_PerformingCalculations"));

      // Write Header Data

      // 32 - Header (EyedentityGames Packing File 0.1)
      // 224 - null Padding to offset 256
      // 4 - Unknown (11)
      // 4 - Number of Files
      fm.writeBytes(src.readBytes(264));

      // 4 - Directory Offset
      int srcDirOffset = src.readInt();
      fm.writeInt(0); // we'll write this later

      // 756 - null Padding to offset 1024
      fm.writeBytes(src.readBytes(756));

      // Write Files
      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));
      long[] compLengths = write(exporter, resources, fm);

      long dirOffset = fm.getOffset();
      src.relativeSeek(srcDirOffset);

      // Write Directory
      TaskProgressManager.setMessage(Language.get("Progress_WritingDirectory"));
      long offset = 1024;
      for (int i = 0; i < numFiles; i++) {
        Resource fd = resources[i];
        int compLength = (int) compLengths[i];

        // 256 - Filename (filled with nulls)
        fm.writeBytes(src.readBytes(256));

        // 4 - Compressed Length
        // 4 - Decompressed Length
        // 4 - Compressed Length (again)
        // 4 - File Offset
        fm.writeInt(compLength);
        fm.writeInt((int) fd.getDecompressedLength());
        fm.writeInt(compLength);
        fm.writeInt(offset);

        src.skip(16);

        // 4 - Unknown
        // 40 - null
        fm.writeBytes(src.readBytes(44));

        offset += compLength;
      }

      // go back and write the dirOffset
      fm.seek(264);
      fm.writeInt(dirOffset);

      src.close();
      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

}
