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
import org.watto.component.WSPluginManager;
import org.watto.datatype.FileType;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.io.FileManipulator;
import org.watto.io.converter.IntConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_RESOURCES extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_RESOURCES() {

    super("RESOURCES", "RESOURCES");

    //         read write replace rename
    setProperties(true, true, false, false);

    setGames("Doom 3");
    setExtensions("resources"); // MUST BE LOWER CASE
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
    setFileTypes(new FileType("bmd5mesh", "bmd5mesh Model", FileType.TYPE_MODEL),
        new FileType("blwo", "blwo Model", FileType.TYPE_MODEL),
        new FileType("bimage", "Bitmap Image", FileType.TYPE_IMAGE));

    //setTextPreviewExtensions("aas48", "aas96", "glsl", "gui", "map", "md5camera", "af", "cg", "def", "fx", "mtr", "pda", "prt", "script", "skin", "sndshd", "uniforms", "vfp", "vp"); // LOWER CASE

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
      if (fm.readInt() == 218104016) {
        rating += 50;
      }

      long arcSize = fm.getLength();

      // Directory Offset
      if (FieldValidator.checkOffset(IntConverter.changeFormat(fm.readInt()), arcSize)) {
        rating += 5;
      }

      // Directory Length
      if (FieldValidator.checkLength(IntConverter.changeFormat(fm.readInt()), arcSize)) {
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

      // 4 - Unknown (218104016)
      fm.skip(4);

      // 4 - Directory Offset
      long dirOffset = IntConverter.changeFormat(fm.readInt());
      FieldValidator.checkOffset(dirOffset, arcSize);

      // 4 - Directory Length
      fm.seek(dirOffset);

      // 4 - Number Of Files
      int numFiles = IntConverter.changeFormat(fm.readInt());
      FieldValidator.checkNumFiles(numFiles);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {

        // 4 - Filename Length (LITTLE ENDIAN)
        int filenameLength = fm.readInt();
        FieldValidator.checkFilenameLength(filenameLength);

        // X - Filename
        String filename = fm.readString(filenameLength);

        // 4 - File Offset
        int offset = IntConverter.changeFormat(fm.readInt());
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - File Length
        int length = IntConverter.changeFormat(fm.readInt());
        FieldValidator.checkLength(length, arcSize);

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

      long directoryOffset = 12;
      long directoryLength = 0;
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];
        directoryOffset += resource.getDecompressedLength();
        directoryLength += 12 + resource.getNameLength();
      }

      // Write Header Data

      // 4 - Header (218104016)
      fm.writeInt(218104016);

      // 4 - Directory Offset
      fm.writeInt(IntConverter.changeFormat((int) directoryOffset));

      // 4 - Directory Length
      fm.writeInt(IntConverter.changeFormat((int) directoryLength));

      // Write Files
      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));
      write(resources, fm);

      // 4 - Number of Files
      fm.writeInt(IntConverter.changeFormat((int) numFiles));

      // Write Directory
      TaskProgressManager.setMessage(Language.get("Progress_WritingDirectory"));
      long offset = 12;
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];
        long decompLength = resource.getDecompressedLength();

        String filename = resource.getName();

        // 4 - Filename Length (LITTLE ENDIAN)
        fm.writeInt((int) filename.length());

        // X - Filename
        fm.writeString(filename);

        // 4 - File Offset
        fm.writeInt(IntConverter.changeFormat((int) offset));

        // 4 - File Length
        fm.writeInt(IntConverter.changeFormat((int) decompLength));

        offset += decompLength;
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

    /*
    if (headerInt1 == 2037149520) {
      return "js";
    }
    */

    return null;
  }

  /**
   **********************************************************************************************
   Provide hints to the previewer so that certain document types are displayed appropriately
   **********************************************************************************************
   **/
  @Override
  public ViewerPlugin previewHint(Resource resource) {
    // NOTE: THIS IS REQUIRED BECAUSE IT DOESN'T WORK WITH setFileTypes AND setTextPreviews
    String extension = resource.getExtension();

    if (extension.equalsIgnoreCase("aas48") || extension.equalsIgnoreCase("aas96") || extension.equalsIgnoreCase("glsl") || extension.equalsIgnoreCase("gui") || extension.equalsIgnoreCase("map") || extension.equalsIgnoreCase("md5camera") || extension.equalsIgnoreCase("af") || extension.equalsIgnoreCase("cg") || extension.equalsIgnoreCase("def") || extension.equalsIgnoreCase("fx") || extension.equalsIgnoreCase("mtr") || extension.equalsIgnoreCase("pda") || extension.equalsIgnoreCase("prt") || extension.equalsIgnoreCase("script") || extension.equalsIgnoreCase("skin") || extension.equalsIgnoreCase("sndshd") || extension.equalsIgnoreCase("uniforms") || extension.equalsIgnoreCase("vfp") || extension.equalsIgnoreCase("vp")) {
      return (ViewerPlugin) WSPluginManager.getPlugin("Viewer", "TXT");
    }
    return null;
  }

}
