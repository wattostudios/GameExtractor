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
import org.watto.component.WSPluginManager;
import org.watto.datatype.FileType;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_BIN_LIBSYS extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_BIN_LIBSYS() {

    super("BIN_LIBSYS", "BIN_LIBSYS");

    //         read write replace rename
    setProperties(true, true, true, true);

    setGames("Ultimate Race Pro");
    setExtensions("bin");
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
    setFileTypes(new FileType("mat", "Material Sprite Mapping", FileType.TYPE_OTHER),
        new FileType("atm", "Atom Model", FileType.TYPE_OTHER),
        new FileType("pix", "Preview Image", FileType.TYPE_IMAGE),
        new FileType("asc", "Text Document", FileType.TYPE_DOCUMENT),
        new FileType("bak", "Backup File", FileType.TYPE_DOCUMENT),
        new FileType("bat", "Batch Script", FileType.TYPE_DOCUMENT),
        new FileType("bin", "Track Definition", FileType.TYPE_OTHER),
        new FileType("cfg", "Vehicle Grouping", FileType.TYPE_OTHER),
        new FileType("cpp", "Programming Script", FileType.TYPE_DOCUMENT),
        new FileType("def", "Programming Script", FileType.TYPE_DOCUMENT),
        new FileType("int", "Programming Script", FileType.TYPE_DOCUMENT),
        new FileType("crd", "Co-ordinates Path", FileType.TYPE_OTHER),
        new FileType("dlg", "Dialog Image Mapping", FileType.TYPE_OTHER),
        new FileType("ion", "Description File", FileType.TYPE_OTHER),
        new FileType("msk", "Image Mask", FileType.TYPE_IMAGE),
        new FileType("tmr", "Timer", FileType.TYPE_OTHER),
        new FileType("rep", "Replay", FileType.TYPE_OTHER));

  }

  /**
   **********************************************************************************************
   Provide hints to the previewer so that certain document types are displayed appropriately
   **********************************************************************************************
   **/
  @Override
  public ViewerPlugin previewHint(Resource resource) {
    String extension = resource.getExtension();
    if (extension.equalsIgnoreCase("asc") || extension.equalsIgnoreCase("bak") || extension.equalsIgnoreCase("bat") || extension.equalsIgnoreCase("cpp") || extension.equalsIgnoreCase("def") || extension.equalsIgnoreCase("int") || extension.equalsIgnoreCase("crd") || extension.equalsIgnoreCase("ion") || extension.equalsIgnoreCase("dlg")) {
      return (ViewerPlugin) WSPluginManager.getPlugin("Viewer", "TXT");
    }
    return null;
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
      if (fm.readString(16).equals("LIBSYSHEADER1.0" + (byte) 0)) {
        rating += 50;
      }

      // Number Of Files
      int numFiles = fm.readInt();
      if (FieldValidator.checkNumFiles(numFiles)) {
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
  
  **********************************************************************************************
  **/
  @Override
  public Resource[] read(File path) {
    try {

      addFileTypes();

      FileManipulator fm = new FileManipulator(path, false);

      // 16 - Header (LIBSYSHEADER1.0 + null)
      fm.skip(16);

      // 4 - Number Of Files [-1]
      int numFiles = fm.readInt() - 1;
      FieldValidator.checkNumFiles(numFiles);

      long arcSize = fm.getLength();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        // 120 - Filename
        String filename = fm.readNullString(120);

        // 4 - File Offset
        long offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - null
        fm.skip(4);

        //path,id,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset);

        TaskProgressManager.setValue(i);
      }

      fm.close();

      calculateFileSizes(resources, arcSize);

      return resources;

    }
    catch (Throwable t) {
      logError(t);
      return null;
    }
  }

  /**
  **********************************************************************************************
  
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
      int archiveSize = 20 + ((numFiles + 1) * 128);
      for (int i = 0; i < numFiles; i++) {
        archiveSize += resources[i].getDecompressedLength();
      }

      // Write Header Data

      // 4 - Header (LIBSYSHEADER1.0 + null)
      fm.writeString("LIBSYSHEADER1.0");
      fm.writeByte(0);

      // 4 - Number Of Files
      fm.writeInt(numFiles + 1);

      // Write Directory
      TaskProgressManager.setMessage(Language.get("Progress_WritingDirectory"));
      long offset = 20 + ((numFiles + 1) * 128);
      for (int i = 0; i < numFiles; i++) {
        // 120 - Filename (null)
        fm.writeNullString(resources[i].getName(), 120);

        // 4 - Data Offset
        fm.writeInt((int) offset);

        // 4 - null
        fm.writeInt((int) 0);

        offset += resources[i].getDecompressedLength();
      }

      // 120 - null
      for (int i = 0; i < 120; i++) {
        fm.writeByte(0);
      }

      // 4 - Archive Size
      fm.writeInt((int) archiveSize);

      // 4 - null
      fm.writeInt((int) 0);

      // Write Files
      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));
      write(resources, fm);

      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

}
