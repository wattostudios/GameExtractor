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
import org.watto.datatype.FileType;
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
public class Plugin_DAT_CMP extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_DAT_CMP() {

    super("DAT_CMP", "DAT_CMP");

    //         read write replace rename
    setProperties(true, true, false, false);

    setGames("Black Matrix");
    setExtensions("dat"); // MUST BE LOWER CASE
    setPlatforms("PS1");

    // MUST BE LOWER CASE !!!
    setFileTypes(new FileType("pac", "PAC Archive", FileType.TYPE_ARCHIVE));

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
      if (fm.readString(4).equals("CMP ")) {
        rating += 50;
      }

      if (ShortConverter.changeFormat(fm.readShort()) == 49) {
        rating += 5;
      }

      if (ShortConverter.changeFormat(fm.readShort()) == 2048) {
        rating += 5;
      }

      // Number Of Files
      if (FieldValidator.checkNumFiles(ShortConverter.changeFormat(fm.readShort()))) {
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

      // 4 - Header ("CMP ")
      // 2 - Unknown (49)
      // 2 - Padding Multiple (2048)
      fm.skip(8);

      // 2 - Number of Files
      int numFiles = ShortConverter.changeFormat(fm.readShort());
      FieldValidator.checkNumFiles(numFiles);

      // 2 - Unknown (1)
      // 4 - null
      fm.skip(6);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {

        // 2 - File Offset [*2048]
        int offset = ((int) (ShortConverter.changeFormat(fm.readShort()))) * 2048;
        FieldValidator.checkOffset(offset, arcSize);

        // 2 - File Length [*2048] (including padding)
        int length = ((int) (ShortConverter.changeFormat(fm.readShort()))) * 2048;
        FieldValidator.checkLength(length, arcSize);

        String filename = Resource.generateFilename(i);

        //path,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset, length);

        TaskProgressManager.setValue(i);
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
   * Writes an [archive] File with the contents of the Resources
   **********************************************************************************************
   **/
  @Override
  public void write(Resource[] resources, File path) {
    try {

      FileManipulator fm = new FileManipulator(path, true);
      int numFiles = resources.length;
      TaskProgressManager.setMaximum(numFiles);

      // Calculations
      TaskProgressManager.setMessage(Language.get("Progress_PerformingCalculations"));

      long dirSize = 16 + (numFiles * 4);
      long dirPadding = calculatePadding(dirSize, 2048);
      dirSize += dirPadding;

      // Write Header Data

      // 4 - Header ("CMP ")
      fm.writeString("CMP ");

      // 2 - Unknown (49)
      fm.writeShort(ShortConverter.changeFormat((short) 49));

      // 2 - Padding Multiple (2048)
      fm.writeShort(ShortConverter.changeFormat((short) 2048));

      // 2 - Number of Files
      fm.writeShort(ShortConverter.changeFormat((short) numFiles));

      // 2 - Unknown (1)
      fm.writeShort(ShortConverter.changeFormat((short) 1));

      // 4 - null
      fm.writeInt(0);

      // Write Directory
      TaskProgressManager.setMessage(Language.get("Progress_WritingDirectory"));
      long offset = dirSize;
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];
        long decompLength = resource.getDecompressedLength();
        decompLength += calculatePadding(decompLength, 2048);

        // 2 - File Offset [*2048]
        fm.writeShort(ShortConverter.changeFormat((short) (offset / 2048)));

        // 2 - File Length [*2048] (including padding)
        fm.writeShort(ShortConverter.changeFormat((short) (decompLength / 2048)));

        offset += decompLength;
      }

      // X - null Padding to a multiple of 2048 bytes
      for (int p = 0; p < dirPadding; p++) {
        fm.writeByte(0);
      }

      // Write Files
      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));
      for (int i = 0; i < resources.length; i++) {
        Resource resource = resources[i];

        // X - File Data
        write(resource, fm);

        // X - null Padding to a multiple of 2048 bytes
        int paddingSize = calculatePadding(resource.getDecompressedLength(), 2048);
        for (int p = 0; p < paddingSize; p++) {
          fm.writeByte(0);
        }

        TaskProgressManager.setValue(i);
      }

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

    if (headerInt1 == 541478977) {
      return "apf";
    }
    else if (headerInt1 == 541278544) {
      return "pac";
    }
    else if (headerInt1 == 1095189843 && headerInt2 == 1413567264 && headerInt3 == 542003797) {
      return "scr";
    }
    else if (headerInt1 == 5262913) {
      return "anp";
    }
    else if (headerInt1 == 541676368) {
      return "psi";
    }
    else if (headerInt1 == 877413203) {
      return "scl4";
    }
    else if (headerInt1 == 541346117) {
      return "eid";
    }
    else if (headerInt1 == 541346131) {
      return "sid";
    }
    else if (headerInt1 == 541346889) {
      return "ild";
    }
    else if (headerInt1 == 541345869) {
      return "mhd";
    }
    else if (headerInt1 == 541150275) {
      return "cla";
    }
    else if (headerInt1 == 542135107) {
      return "csp";
    }
    else if (headerInt1 == 1296913732) {
      return "dummy";
    }
    else if (headerInt1 == 541347393) {
      return "and";
    }
    else if (headerInt1 == 541934419) {
      return "scm";
    }
    else if (headerInt1 == 542327635) {
      return "scs";
    }
    else if (headerInt1 == 541348674) {
      return "bsd";
    }
    else if (headerInt1 == 876757843) {
      return "scb4";
    }

    return null;
  }

}
