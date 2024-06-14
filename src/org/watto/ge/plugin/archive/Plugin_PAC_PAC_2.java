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
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.io.FileManipulator;
import org.watto.io.converter.IntConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_PAC_PAC_2 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_PAC_PAC_2() {

    super("PAC_PAC_2", "PAC_PAC_2");

    //         read write replace rename
    setProperties(true, false, true, false);

    setGames("Black Matrix");
    setExtensions("pac"); // MUST BE LOWER CASE
    setPlatforms("PS1");

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

      // Header
      if (fm.readString(4).equals("PAC ")) {
        rating += 50;
      }

      if (IntConverter.changeFormat(fm.readInt()) == 2) {
        rating += 5;
      }

      // Number Of Files
      if (FieldValidator.checkNumFiles(IntConverter.changeFormat(fm.readInt()))) {
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

      // 4 - Header ("PAC ")
      // 4 - Unknown (2)
      fm.skip(8);

      // 4 - Number of Files
      int numFiles = IntConverter.changeFormat(fm.readInt());
      FieldValidator.checkNumFiles(numFiles);

      // 4 - null
      fm.skip(4);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {

        // 4 - File Offset
        int offset = IntConverter.changeFormat(fm.readInt());
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - Unknown
        fm.skip(4);

        String filename = Resource.generateFilename(i);

        //path,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset);

        TaskProgressManager.setValue(i);
      }

      calculateFileSizes(resources, arcSize);

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

      // 4 - Header ("PAC ")
      // 4 - Unknown (2)
      // 4 - Number of Files
      // 4 - null
      fm.writeBytes(src.readBytes(16));

      // Write Directory
      TaskProgressManager.setMessage(Language.get("Progress_WritingDirectory"));
      long offset = 16 + (numFiles * 8);
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];
        long length = resource.getDecompressedLength();

        // 4 - File Offset
        fm.writeInt(IntConverter.changeFormat((int) offset));
        src.skip(4);

        // 4 - Unknown
        fm.writeBytes(src.readBytes(4));

        offset += length;
      }

      // Write Files
      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));
      write(resources, fm);

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
