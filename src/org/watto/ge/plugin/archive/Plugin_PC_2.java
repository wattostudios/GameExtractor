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
import org.watto.ge.helper.ResourceSorter_Offset;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.PluginFinder;
import org.watto.ge.plugin.RatedPlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.viewer.Viewer_PC_2_BMP;
import org.watto.io.FileManipulator;
import org.watto.io.buffer.ByteBuffer;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_PC_2 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_PC_2() {

    super("PC_2", "PC_2");

    //         read write replace rename
    setProperties(true, false, true, false);

    setGames("Bad Boys 2");
    setExtensions("pc"); // MUST BE LOWER CASE
    setPlatforms("PC");

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

      fm.skip(4);

      if (fm.readInt() == 16) {
        rating += 5;
      }

      if (fm.readInt() == 0) {
        rating += 5;
      }

      // Number Of Files
      if (FieldValidator.checkNumFiles(fm.readInt())) {
        rating += 5;
      }

      long arcSize = fm.getLength();

      // Directory Offset
      if (FieldValidator.checkOffset(fm.readInt() * 16, arcSize)) {
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

      // 4 - Unknown
      fm.skip(4);

      // 4 - Block Size (16)
      int blockSize = fm.readInt();

      // 4 - null
      fm.skip(4);

      // 4 - Number of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 4 - Details Directory Offset [*16]
      int dirOffset = fm.readInt() * blockSize;
      FieldValidator.checkOffset(dirOffset, arcSize);

      // 4 - Description Offset [*16]
      int descriptionOffset = fm.readInt() * blockSize;
      FieldValidator.checkOffset(descriptionOffset, arcSize);

      // 4 - null
      // 4 - Unknown (2/3)
      // 4 - Footer Offset [*16]
      // 4 - Unknown (1)
      fm.skip(16);

      // 4 - Filename Directory Offset [*16]
      int filenameDirOffset = fm.readInt() * blockSize;
      FieldValidator.checkOffset(filenameDirOffset, arcSize);

      // 4 - Filename Directory Length
      int filenameDirLength = fm.readInt();
      FieldValidator.checkLength(filenameDirLength, arcSize);

      // 4 - Details Directory Length
      // 4 - null
      // 4 - Unknown (90)
      // 4 - null
      // 1984 - null to offset 2048

      fm.seek(filenameDirOffset);

      byte[] filenameBytes = fm.readBytes(filenameDirLength);
      FileManipulator nameFM = new FileManipulator(new ByteBuffer(filenameBytes));

      fm.seek(dirOffset);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      ResourceSorter_Offset[] sorter = new ResourceSorter_Offset[numFiles];
      for (int i = 0; i < numFiles; i++) {
        // 4 - File Offset [*16]
        int offset = fm.readInt() * blockSize;
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - Hash?
        fm.skip(4);

        // 4 - File Length
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - Filename Offset (relative to the start of the filename directory)
        int filenameOffset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - Unknown (0/1)
        // 4 - Unknown (0/4)
        // 8 - Hash?
        fm.skip(16);

        // X - Filename (null)
        nameFM.seek(filenameOffset);
        String filename = nameFM.readNullString();
        FieldValidator.checkFilename(filename);

        //path,name,offset,length,decompLength,exporter
        Resource resource = new Resource(path, filename, offset, length);
        resources[i] = resource;

        sorter[i] = new ResourceSorter_Offset(resource);

        TaskProgressManager.setValue(i);
      }

      nameFM.close();

      fm.close();

      // sort the resources in offset order (important for when we do Replace());
      Arrays.sort(sorter);

      for (int i = 0; i < numFiles; i++) {
        resources[i] = sorter[i].getResource();
      }

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

      // 
      //
      // NOTE: For this one, due to weird paddings and FilenameTable files and things like that, we're just
      // keeping all files the same size as in the original archive, and so we can just copy-paste the header
      // and directory and other content from the source to the target.
      //
      //

      // Write Header Data

      // 4 - Unknown
      // 4 - Block Size (16)
      // 4 - null
      // 4 - Number of Files
      fm.writeBytes(src.readBytes(16));

      // 4 - Details Directory Offset [*16]
      int dirOffset = src.readInt();
      fm.writeInt(dirOffset);
      dirOffset *= 16;

      // 4 - Description Offset [*16]
      // 4 - null
      // 4 - Unknown (2/3)
      // 4 - Footer Offset [*16]
      // 4 - Unknown (1)
      // 4 - Filename Directory Offset [*16]
      // 4 - Filename Directory Length
      // 4 - Details Directory Length
      // 4 - null
      // 4 - Unknown (90)
      // 4 - null
      // 1984 - null to offset 2048
      fm.writeBytes(src.readBytes(2028));

      // Write Files
      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));
      for (int i = 0; i < resources.length; i++) {
        Resource resource = resources[i];
        write(resource, fm);
        TaskProgressManager.setValue(i);

        int padding = calculatePadding(resource.getDecompressedLength(), 16);
        for (int p = 0; p < padding; p++) {
          fm.writeByte(0);
        }

      }

      // Write Directory
      src.seek(dirOffset);
      TaskProgressManager.setMessage(Language.get("Progress_WritingDirectory"));
      int remainingLength = (int) (src.getLength() - dirOffset);
      fm.writeBytes(src.readBytes(remainingLength));

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

    /*
    if (headerInt1 == 2037149520) {
      return "js";
    }
    */

    return null;
  }

  /**
   **********************************************************************************************
   When replacing dtx images, if the fileToReplaceWith is a different format image (eg DDS, PNG, ...)
   it can be converted into a dtx image. All other files are replaced without conversion
   @param resourceBeingReplaced the Resource in the archive that is being replaced
   @param fileToReplaceWith the file on your PC that will replace the Resource. This file is the
          one that will be converted into a different format, if applicable.
   @return the converted file, if conversion was applicable/successful, else the original fileToReplaceWith
   **********************************************************************************************
   **/
  @Override
  public File convertOnReplace(Resource resourceBeingReplaced, File fileToReplaceWith) {

    String beingReplacedExtension = resourceBeingReplaced.getName();
    beingReplacedExtension = beingReplacedExtension.substring(0, beingReplacedExtension.length() - 1);
    if (beingReplacedExtension.endsWith(".bmp.")) {
      // try to convert

      String toReplaceWithExtension = fileToReplaceWith.getName();
      toReplaceWithExtension = toReplaceWithExtension.substring(0, beingReplacedExtension.length() - 1);
      if (toReplaceWithExtension.endsWith(".bmp.")) {
        // if the fileToReplace already has a dtx extension, assume it's already a compatible dtx file and doesn't need to be converted
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
      // If we're here, we have a rendered image, so we want to convert it into WIM
      //
      //
      Viewer_PC_2_BMP converterPlugin = new Viewer_PC_2_BMP();

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

}
