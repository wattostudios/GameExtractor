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
import org.watto.component.WSPluginManager;
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
public class Plugin_JAM_JAM2 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_JAM_JAM2() {

    super("JAM_JAM2", "JAM_JAM2");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Leisure Suit Larry: Manga Cum Laude");
    setExtensions("jam");
    setPlatforms("PC");

    setFileTypes("acx", "Sound Audio File",
        "add", "Data Registry",
        "adr", "Animation Database Component",
        "aee", "Event Activation",
        "aes", "Event Subscriber",
        "aet", "Event Trigger",
        "afw", "Menu Text",
        "aga", "Animation Descriptor",
        "agd", "Animation Graphic Database",
        "agf", "Font Graphics",
        "agi", "Alpha Material Mapping",
        "agl", "Lighting Effects",
        "agm", "Animation Material",
        "agn", "Animation Material Mapping",
        "ags", "Animation Shader",
        "agt", "Animation Texture",
        "aid", "Descriptions",
        "ais", "Information Messages",
        "aiv", "Player Inventory",
        "akc", "Animation Positioning",
        "akw", "Animation Bone Weights",
        "ana", "Abstract Controller",
        "anl", "Animation Lists",
        "anm", "Button Mapping",
        "aob", "Sound Segment Bank",
        "aod", "Sound Segment Database",
        "aoe", "Aound Emitter",
        "aos", "Sound Segment",
        "aot", "Sound Stream",
        "asb", "Animation Bounds",
        "asd", "Animation Database",
        "ase", "Visual Effects Emitter",
        "asg", "Animation Space",
        "asl", "Animation Motion",
        "asm", "Animation Modifiers",
        "asn", "Animation Node",
        "aua", "Image Assets",
        "aud", "Image Database",
        "aus", "Image Style",
        "auw", "Image Widget",
        "dac", "Dynamic Nodes",
        "gbp", "Path Motion",
        "ler", "Icon Group",
        "lqf", "Camera Views",
        "lwm", "Minigame Events",
        "mng", "Minigame Loader",
        "moo", "Moods",
        "nod", "Sound Node",
        "not", "Script Notes",
        "pev", "Photo Evaluator",
        "rhy", "Cinematic Items",
        "rmb", "Rumble Information",
        "scm", "Button Mapping",
        "sif", "Cinematics",
        "sdl", "Sound List",
        "slt", "Sound Target",
        "snc", "Cinematic Sequence",
        "ssm", "Motion States",
        "vfx", "Visual Effects Group",
        "wgg", "WGG File",
        "wka", "WKA File",
        "wps", "Action Group");

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
      if (fm.readString(4).equals("JAM2")) {
        rating += 50;
      }

      fm.skip(4);

      long arcSize = fm.getLength();

      // First File Offset
      if (FieldValidator.checkOffset(fm.readInt(), arcSize)) {
        rating += 5;
      }

      // Header 2
      if (fm.readString(4).equals("none")) {
        rating += 5;
      }

      // null
      if (fm.readInt() == 0) {
        rating += 5;
      }

      // null
      if (fm.readInt() == 0) {
        rating += 5;
      }

      // null
      if (fm.readInt() == 0) {
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
  @SuppressWarnings("unused")
  @Override
  public Resource[] read(File path) {
    try {

      addFileTypes();

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 4 - Header (JAM2)
      // 4 - Unknown
      fm.skip(8);

      // 4 - First File Offset
      int firstFileOffset = fm.readInt();
      FieldValidator.checkOffset(firstFileOffset, arcSize);

      // 4 - Header 2 (none)
      // 12 - null
      fm.skip(16);

      // 2 - Number Of Filenames
      int numFilenames = fm.readShort();
      FieldValidator.checkNumFiles(numFilenames);

      // 2 - Number Of Extensions
      int numExtensions = fm.readShort();
      FieldValidator.checkNumFiles(numFilenames);

      String[] names = new String[numFilenames];
      for (int i = 0; i < numFilenames; i++) {
        // 8 - Filename (null)
        names[i] = fm.readNullString(8);
      }

      String[] extensions = new String[numExtensions];
      for (int i = 0; i < numExtensions; i++) {
        // 4 - Extension Name (null)
        extensions[i] = fm.readNullString(4);
      }

      // approx only - some file entries are not valid
      int numFiles = (firstFileOffset - (int) fm.getOffset()) / 8;

      FieldValidator.checkNumFiles(numFiles);

      // 4 - Unknown
      fm.skip(4);

      String[] filenames = new String[numFiles];
      long[] offsets = new long[numFiles];

      int realNumFiles = 0;
      for (int i = 0; i < numFiles; i++) {
        // 2 - Filename ID
        int filenameID = fm.readShort();

        // 2 - File Extension ID
        int fileExtID = fm.readShort();

        // 4 - Offset
        long offset = fm.readInt();

        if (offset >= firstFileOffset) {
          if (filenameID < numFilenames && fileExtID < numExtensions) {
            filenames[realNumFiles] = names[filenameID] + "." + extensions[fileExtID];
          }
          else {
            filenames[realNumFiles] = Resource.generateFilename(realNumFiles);
          }

          offsets[realNumFiles] = offset;

          realNumFiles++;
        }

      }

      Resource[] resources = new Resource[realNumFiles];
      TaskProgressManager.setMaximum(realNumFiles);

      // Loop through directory
      for (int i = 0; i < realNumFiles; i++) {

        long offset = offsets[i];
        fm.seek(offset);

        // 4 - File Size
        long length = fm.readInt();

        // 4 - File Size
        int decompLength = fm.readInt();

        // 4 - Unknown
        // 4 - Unknown
        // 4 - Unknown
        // 4 - Unknown
        // 4 - Unknown
        // 4 - Unknown
        // X - File Data
        offset += 32;

        // 0-3 - Padding to multiple of 4 bytes

        //String filename = Resource.generateFilename(realNumFiles);
        String filename = filenames[i];

        //path,id,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset, length);

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
   Provide hints to the previewer so that certain document types are displayed appropriately
   **********************************************************************************************
   **/
  @Override
  public ViewerPlugin previewHint(Resource resource) {
    String extension = resource.getExtension();
    if (extension.charAt(0) == 'A' || extension.charAt(0) == 'a') {
      return (ViewerPlugin) WSPluginManager.getPlugin("Viewer", "TXT");
    }
    return null;
  }

}
