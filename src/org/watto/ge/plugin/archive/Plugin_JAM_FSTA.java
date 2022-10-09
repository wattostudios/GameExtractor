/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2021 wattostudios
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
import org.watto.datatype.Archive;
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
public class Plugin_JAM_FSTA extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_JAM_FSTA() {

    super("JAM_FSTA", "JAM_FSTA");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Codename Kids Next Door - Operation: V.I.D.E.O.G.A.M.E.");
    setExtensions("jam"); // MUST BE LOWER CASE
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
    setFileTypes(new FileType("add", "Component Database", FileType.TYPE_DOCUMENT),
        new FileType("adr", "Component", FileType.TYPE_DOCUMENT),
        new FileType("aee", "Event", FileType.TYPE_DOCUMENT),
        new FileType("aes", "Event Subscriber", FileType.TYPE_DOCUMENT),
        new FileType("aet", "Event Trigger", FileType.TYPE_DOCUMENT),
        new FileType("akc", "Co-ordinates", FileType.TYPE_DOCUMENT),
        new FileType("agd", "Material Database", FileType.TYPE_DOCUMENT),
        new FileType("agl", "Material Lighting", FileType.TYPE_DOCUMENT),
        new FileType("agm", "Material", FileType.TYPE_DOCUMENT),
        new FileType("agf", "Material Font", FileType.TYPE_DOCUMENT),
        new FileType("agn", "Material Assignment", FileType.TYPE_DOCUMENT),
        new FileType("agt", "Material Texture", FileType.TYPE_DOCUMENT),
        new FileType("agi", "Material Image", FileType.TYPE_DOCUMENT),
        new FileType("ags", "Material Shader", FileType.TYPE_DOCUMENT),
        new FileType("anl", "Animation Set", FileType.TYPE_DOCUMENT),
        new FileType("anm", "Abstract Mapper", FileType.TYPE_DOCUMENT),
        new FileType("aob", "Audio Bank", FileType.TYPE_DOCUMENT),
        new FileType("aod", "Audio Database", FileType.TYPE_DOCUMENT),
        new FileType("aoe", "Audio Emitter", FileType.TYPE_DOCUMENT),
        new FileType("aos", "Audio Segment", FileType.TYPE_DOCUMENT),
        new FileType("asb", "Space Box", FileType.TYPE_DOCUMENT),
        new FileType("asd", "Space Database", FileType.TYPE_DOCUMENT),
        new FileType("ase", "Space Emitter", FileType.TYPE_DOCUMENT),
        new FileType("asg", "Space Group", FileType.TYPE_DOCUMENT),
        new FileType("asn", "Space Node", FileType.TYPE_DOCUMENT),
        new FileType("asm", "Space Modifier", FileType.TYPE_DOCUMENT),
        new FileType("aua", "Style Asset", FileType.TYPE_DOCUMENT),
        new FileType("aud", "Style Database", FileType.TYPE_DOCUMENT),
        new FileType("aus", "Style", FileType.TYPE_DOCUMENT),
        new FileType("auw", "Style Widget", FileType.TYPE_DOCUMENT),
        new FileType("gbp", "Path", FileType.TYPE_DOCUMENT),
        new FileType("gob", "Sound Block", FileType.TYPE_DOCUMENT),
        new FileType("god", "Sound Block Database", FileType.TYPE_DOCUMENT),
        new FileType("gon", "Sound Node", FileType.TYPE_DOCUMENT),
        new FileType("goo", "Sound Nodes", FileType.TYPE_DOCUMENT),
        new FileType("gom", "Sound Playlist", FileType.TYPE_DOCUMENT),
        new FileType("gov", "Sound Volume", FileType.TYPE_DOCUMENT),
        new FileType("pka", "Animation", FileType.TYPE_OTHER),
        new FileType("pnt", "PS2 Image", FileType.TYPE_IMAGE),
        new FileType("rmb", "Rumble", FileType.TYPE_DOCUMENT),
        new FileType("sap", "Actor", FileType.TYPE_DOCUMENT),
        new FileType("scm", "Mapping", FileType.TYPE_DOCUMENT),
        new FileType("sdl", "Sound Set", FileType.TYPE_DOCUMENT),
        new FileType("ssm", "Motion State", FileType.TYPE_DOCUMENT),
        new FileType("vfx", "VFX", FileType.TYPE_DOCUMENT));

    //setTextPreviewExtensions("aob", "aos", "aod", "agn", "agm", "agd", "mng"); // LOWER CASE

    //setCanScanForFileTypes(true);

  }

  String[] textPreviewExtensions = new String[] { "aob", "agl", "aos", "aod", "gom", "gbp", "ase", "ags", "asm", "agn", "agm", "agd", "agt", "mng", "aee", "aet", "aes", "add", "adr", "aus", "aud", "auw", "aua", "asb", "asd", "asg", "asn", "scm", "rmb", "agi", "akc", "anm", "aoe", "gob", "god", "gon", "goo", "gov", "sap", "sdl", "ssm", "vfx", "anl" };

  /**
   **********************************************************************************************
   Provide hints to the previewer so that certain document types are displayed appropriately
   **********************************************************************************************
   **/
  @Override
  public ViewerPlugin previewHint(Resource resource) {
    String extension = resource.getExtension().toLowerCase();

    int numExtensions = textPreviewExtensions.length;
    for (int i = 0; i < numExtensions; i++) {
      if (extension.equals(textPreviewExtensions[i])) {
        return (ViewerPlugin) WSPluginManager.getPlugin("Viewer", "TXT");
      }
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
      if (fm.readString(4).equals("FSTA")) {
        rating += 50;
      }

      fm.skip(4);

      long arcSize = fm.getLength();

      // Directory Offset
      if (FieldValidator.checkOffset(fm.readInt(), arcSize)) {
        rating += 5;
      }

      // 16 - Description? ("none" + nulls to fill)
      if (fm.readNullString(16).equals("none")) {
        rating += 10;
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

      // 4 - Header (FSTA)
      // 4 - Unknown
      fm.skip(8);

      // 4 - End of Directory Offset (Offset to UNKNOWN DATA)
      int endOfDirOffset = fm.readInt();
      FieldValidator.checkOffset(endOfDirOffset, arcSize);

      // 16 - Description? ("none" + nulls to fill)
      fm.skip(16);

      // 2 - Number of Names
      short numNames = fm.readShort();
      FieldValidator.checkNumFiles(numNames);

      // 2 - Number of Extensions
      short numExtensions = fm.readShort();
      FieldValidator.checkNumFiles(numExtensions);

      // read the names and extensions
      String[] names = new String[numNames];
      for (int i = 0; i < numNames; i++) {
        // 8 - Name (null terminated, filled with nulls)
        names[i] = fm.readNullString(8);
      }

      String[] extensions = new String[numExtensions];
      for (int i = 0; i < numExtensions; i++) {
        // 4 - Extension (null terminated, filled with nulls) (can be null)
        String extension = fm.readNullString(4);
        if (extension == null || extension.length() <= 0) {
          extensions[i] = extension;
        }
        else {
          extensions[i] = "." + extension;
        }
      }

      int numFiles = Archive.getMaxFiles();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(endOfDirOffset);

      // Loop through directory
      int realNumFiles = 0;
      while (fm.getOffset() < endOfDirOffset) {
        // 2 - Name ID
        short nameID = fm.readShort();

        // 2 - Extension ID
        short extensionID = fm.readShort();

        if (extensionID > numExtensions || nameID > numNames) {
          // 4 - Unknown
          fm.skip(4);
        }
        else {
          // 4 - File Offset
          int offset = fm.readInt();

          // 4 - File Length
          int length = fm.readInt();

          try {
            FieldValidator.checkOffset(offset, arcSize);
            FieldValidator.checkLength(length, arcSize);

            String filename = names[nameID] + extensions[extensionID];

            //path,name,offset,length,decompLength,exporter
            resources[realNumFiles] = new Resource(path, filename, offset, length);
            realNumFiles++;
          }
          catch (Throwable t) {
            // don't worry about this, try the next entry
          }
        }

        TaskProgressManager.setValue(fm.getOffset());
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

    /*
    if (headerInt1 == 2037149520) {
      return "js";
    }
    */

    return null;
  }

}
