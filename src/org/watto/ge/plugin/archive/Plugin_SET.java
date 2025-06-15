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
import java.util.Arrays;

import org.watto.Language;
import org.watto.Settings;
import org.watto.component.PreviewPanel;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.PluginFinder;
import org.watto.ge.plugin.RatedPlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.viewer.Viewer_ARC_14_TXFL_TXFL;
import org.watto.io.FileManipulator;
import org.watto.io.FilenameSplitter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_SET extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_SET() {

    super("SET", "SET");

    //         read write replace rename
    setProperties(true, false, true, false);

    setGames("The Urbz: Sims in the City");
    setExtensions("set"); // MUST BE LOWER CASE
    setPlatforms("XBox");

    // MUST BE LOWER CASE !!!
    //setFileTypes(new FileType("txt", "Text Document", FileType.TYPE_DOCUMENT),
    //             new FileType("bmp", "Bitmap Image", FileType.TYPE_IMAGE)
    //             );

    //setTextPreviewExtensions("colours", "rat", "screen", "styles"); // LOWER CASE

    //setCanScanForFileTypes(true);

    setCanConvertOnReplace(true);

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

      if (fm.readInt() == 9) {
        rating += 5;
      }

      fm.skip(60);

      if (fm.readInt() == 0) {
        rating += 5;
      }

      // Number Of Files
      if (FieldValidator.checkNumFiles(fm.readInt())) {
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

      FileManipulator fm = new FileManipulator(path, false, 44); // small quick reads

      long arcSize = fm.getLength();

      // 4 - Unknown (9)
      // 64 - Archive Name (no extension, null terminated, filled with nulls)
      fm.skip(68);

      // 4 - Number of Files?
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        // 32 - Type Name (null terminated, filled with nulls) (eg Textures, Flashes, Shaders, ...)
        String type = fm.readNullString(32);

        // 4 - Hash?
        fm.skip(4);

        // 4 - File Length
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - null
        fm.skip(4);

        // X - File Data
        long offset = fm.getOffset();
        fm.skip(length);

        String extension = "";
        if (type.equals("Animations")) {
          extension = ".ani";
        }
        else if (type.equals("Ambients")) {
          extension = ".amb";
        }
        else if (type.equals("Characters")) {
          extension = ".chr";
        }
        else if (type.equals("EdithTreeSets")) {
          extension = ".edt";
        }
        else if (type.equals("Emitters")) {
          extension = ".emt";
        }
        else if (type.equals("Flashes")) {
          extension = ".big";
        }
        else if (type.equals("Fonts")) {
          extension = ".fnt";
        }
        else if (type.equals("Levels")) {
          extension = ".lvl";
        }
        else if (type.equals("Models")) {
          extension = ".mdl";
        }
        else if (type.equals("QuickDatas")) {
          extension = ".dat";
        }
        else if (type.equals("Samples")) {
          extension = ".wav";
        }
        else if (type.equals("Shaders")) {
          extension = ".shd";
        }
        else if (type.equals("Textures")) {
          extension = ".txfl";
        }

        String filename = type + "\\" + Resource.generateFilename(i) + extension;

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
  If an archive doesn't have filenames stored in it, the scanner can come here to try to work out
  what kind of file a Resource is. This method allows the plugin to provide additional plugin-specific
  extensions, which will be tried before any standard extensions.
  @return null if no extension can be determined, or the extension if one can be found
  **********************************************************************************************
  **/
  @Override
  public String guessFileExtension(Resource resource, byte[] headerBytes, int headerInt1, int headerInt2, int headerInt3, short headerShort1, short headerShort2, short headerShort3, short headerShort4, short headerShort5, short headerShort6) {

    if (headerInt1 == 1279678548) {
      return "txfl";
    }
    else if (headerInt2 == 22050 || headerInt2 == 11025 || headerInt2 == 44100) {
      return "wav_raw";
    }
    else if (headerInt1 == 1179076930) {
      return "big";
    }

    // THE BELOW ARE FROM THE SIMS 2 [XBOX]
    else if (headerInt2 == 1396920148) {
      return "amb"; // TOCS in the Ambients files
    }
    else if (headerInt2 == 1095649613) {
      return "ani"; // MINA in the Animatio files
    }
    else if (headerInt2 == 1128813123) {
      return "chr"; // CRHC in the Characte files
    }
    else if (headerInt2 == 1146377044) {
      return "chr"; // TSTD in the Datasets files
    }
    else if (headerInt2 == 1162105928) {
      return "edt"; // HTDE in the Edithtre files
    }
    else if (headerInt2 == 1161909315) {
      return "efa"; // CTAE in the Effectsa files
    }
    else if (headerInt2 == 1162169684) {
      return "efe"; // TMEE in the Effectse files
    }
    else if (headerInt2 == 1346458196) {
      return "emt"; // TRAP in the Emitters files
    }
    else if (headerInt2 == 1179407176) {
      return "fls"; // HSLF in the Flashes files
    }
    else if (headerInt2 == 1179602516) {
      return "fnt"; // TNOF in the Fonts files
    }
    else if (headerInt2 == 1297040460) {
      return "mdl"; // LDOM in the Models files
    }
    else if (headerInt2 == 1397245010) {
      return "shd"; // RDHS in the Shaders files
    }
    else if (headerInt2 == 1397053012) {
      return "sev"; // TVES in the Soundeve files
    }
    else if (headerInt2 == 1398035012) {
      return "str"; // DRTS in the Soundtra files
    }
    else if (headerInt2 == 1415071308) {
      return "txfl"; // LFXT in the Textures files
    }

    return null;
  }

  /**
   **********************************************************************************************
   When replacing txfl images, if the fileToReplaceWith is a different format image (eg DDS, PNG, ...)
   it can be converted into a txfl image. All other files are replaced without conversion
   @param resourceBeingReplaced the Resource in the archive that is being replaced
   @param fileToReplaceWith the file on your PC that will replace the Resource. This file is the
          one that will be converted into a different format, if applicable.
   @return the converted file, if conversion was applicable/successful, else the original fileToReplaceWith
   **********************************************************************************************
   **/
  @Override
  public File convertOnReplace(Resource resourceBeingReplaced, File fileToReplaceWith) {

    String beingReplacedExtension = resourceBeingReplaced.getExtension();
    if (beingReplacedExtension.equalsIgnoreCase("txfl")) {
      // try to convert

      String toReplaceWithExtension = FilenameSplitter.getExtension(fileToReplaceWith);
      if (toReplaceWithExtension.equalsIgnoreCase("txfl")) {
        // if the fileToReplace already has a txfl extension, assume it's already a compatible dtx file and doesn't need to be converted
        return fileToReplaceWith;
      }

      //
      //
      // if we're here, we want to scan to see if we can find an Image ViewerPlugin that can read the file into an ImageResource,
      // which we can then convert into a dtx using plugin Viewer_REZ_REZMGR_DTX
      //
      //

      // 1. Open the file
      FileManipulator fm = new FileManipulator(fileToReplaceWith, false);

      // 2. Get all the ViewerPlugins that can read this file type
      RatedPlugin[] plugins = PluginFinder.findPlugins(fm, ViewerPlugin.class); // NOTE: This closes the fm pointer!!!
      if (plugins == null || plugins.length == 0) {
        // no viewer plugins found that will accept this file
        return fileToReplaceWith;
      }

      Arrays.sort(plugins);

      // re-open the file - it was closed at the end of findPlugins();
      fm = new FileManipulator(fileToReplaceWith, false);

      // 3. Try each plugin until we find one that can render the file as an ImageResource
      PreviewPanel imagePreviewPanel = null;
      for (int i = 0; i < plugins.length; i++) {
        fm.seek(0); // go back to the start of the file

        imagePreviewPanel = ((ViewerPlugin) plugins[i].getPlugin()).read(fm);
        if (imagePreviewPanel != null) {
          // found a previewer
          break;
        }
      }

      fm.close();

      if (imagePreviewPanel == null) {
        // no plugins were able to open this file successfully
        return fileToReplaceWith;
      }

      //
      //
      // If we're here, we have a rendered image, so we want to convert it into TXFL
      //
      //
      Viewer_ARC_14_TXFL_TXFL converterPlugin = new Viewer_ARC_14_TXFL_TXFL();

      File destination = new File(fileToReplaceWith.getAbsolutePath() + "." + beingReplacedExtension);
      if (destination.exists()) {
        destination.delete();
      }

      FileManipulator fmOut = new FileManipulator(destination, true);
      converterPlugin.replace(resourceBeingReplaced, imagePreviewPanel, fmOut);
      fmOut.close();

      return destination;

    }
    else {
      return fileToReplaceWith;
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
      FileManipulator src = new FileManipulator(new File(Settings.getString("CurrentArchive")), false, 44); // small quick reads

      int numFiles = resources.length;
      TaskProgressManager.setMaximum(numFiles);

      // Calculations
      TaskProgressManager.setMessage(Language.get("Progress_PerformingCalculations"));

      // Write Header Data

      // 4 - Unknown (9)
      // 64 - Archive Name (no extension, null terminated, filled with nulls)
      // 4 - Number of Files
      fm.writeBytes(src.readBytes(72));

      // Write Directory
      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];
        long length = resource.getDecompressedLength();

        // 32 - Type Name (null terminated, filled with nulls) (eg Textures, Flashes, Shaders, ...)
        // 4 - Hash?
        fm.writeBytes(src.readBytes(36));

        // 4 - File Length
        int srcLength = src.readInt();
        fm.writeInt(length);

        // 4 - null
        fm.writeBytes(src.readBytes(4));

        // X - File Data
        write(resource, fm);
        src.skip(srcLength);

        TaskProgressManager.setValue(i);
      }

      src.close();
      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

}
