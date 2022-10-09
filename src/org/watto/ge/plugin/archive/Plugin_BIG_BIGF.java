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
import java.util.Arrays;
import org.watto.ErrorLogger;
import org.watto.Language;
import org.watto.component.PreviewPanel;
import org.watto.component.PreviewPanel_Image;
import org.watto.component.WSPluginManager;
import org.watto.datatype.FileType;
import org.watto.datatype.ImageResource;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.PluginFinder;
import org.watto.ge.plugin.RatedPlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.exporter.Exporter_Default;
import org.watto.ge.plugin.exporter.Exporter_REFPACK;
import org.watto.ge.plugin.viewer.Viewer_BIG_BIGF_CMB;
import org.watto.ge.plugin.viewer.Viewer_BIG_BIGF_FSH_SHPI;
import org.watto.io.FileManipulator;
import org.watto.io.FilenameSplitter;
import org.watto.io.converter.ByteConverter;
import org.watto.io.converter.IntConverter;
import org.watto.io.converter.StringConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_BIG_BIGF extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_BIG_BIGF() {

    super("BIG_BIGF", "Electronic Arts BIG/VIV [BIG_BIGF]");

    //         read write replace rename
    setProperties(true, true, true, true);

    setGames("Ajax Club Football 2005",
        "Battlefield 2 (PS2)",
        "Command And Conquer: Generals",
        "Command And Conquer 3",
        "Create",
        "Def Jam - Fight For NY",
        "EA Cricket 2005",
        "Euro 2000",
        "FIFA 99",
        "FIFA 2000",
        "FIFA 2001",
        "FIFA 2002",
        "FIFA 2003",
        "FIFA 2004",
        "FIFA 2005",
        "FIFA 06",
        "FIFA 07",
        "FIFA 08",
        "FIFA 09",
        "FIFA 10",
        "FIFA Manager 06",
        "FIFA Manager 08",
        "FIFA Manager 09",
        "FIFA Manager 10",
        "FIFA Manager 11",
        "FIFA World Cup 2006",
        "Harry Potter And The Goblet Of Fire",
        "Harry Potter And The Half-Blood Prince",
        "Harry Potter And The Order Of The Phoenix",
        "NBA 2003",
        "NBA 2004",
        "NBA 2005",
        "NHL 2001",
        "NHL 2002",
        "NHL 2003",
        "NHL 2004",
        "NHL 2005",
        "NHL 06",
        "Need For Speed",
        "Need For Speed 2",
        "Need For Speed 3: Hot Pursuit",
        "Need For Speed: High Stakes",
        "Need For Speed: Hot Pursuit 2",
        "Need For Speed: Porsche Unleashed",
        "Red Alert 3",
        "Starlancer",
        "UEFA Champions League 2004",
        "UEFA Champions League 2005",
        "UEFA Euro 2004");

    setExtensions("big", "viv");

    setPlatforms("PC", "PS2", "PS1");

    // MUST BE LOWER CASE !!!
    setFileTypes(new FileType("shd", "Shader", FileType.TYPE_OTHER),
        new FileType("wak", "Map Settings", FileType.TYPE_OTHER),
        new FileType("pso", "Polygon Shader", FileType.TYPE_OTHER),
        new FileType("vso", "Vertex Shader", FileType.TYPE_OTHER),
        new FileType("w3d", "3D Object", FileType.TYPE_OTHER),
        new FileType("wnd", "Window Settings", FileType.TYPE_OTHER),
        new FileType("fsh", "FSH Image", FileType.TYPE_IMAGE),
        new FileType("ssh", "SSH Image", FileType.TYPE_IMAGE),
        new FileType("psh", "PSH Image", FileType.TYPE_IMAGE),
        new FileType("cmb", "CMB Image", FileType.TYPE_IMAGE));

    setCanConvertOnReplace(true);

  }

  /**
   **********************************************************************************************
   Decompressed an archive, where the whole archive is compressed.
   Reads the compressed block information first, then processes the compressed blocks themselves.
   Writes the output to a file with the same name, but with "_ge_decompressed" at the end of it.
   The decompressed file contains the same header as the compressed file, so you can open
   the decompressed file in GE directly, without needing to re-decompress anything.
   If the decompressed file already exists, we use that, we don't re-decompress.
   **********************************************************************************************
   **/
  public FileManipulator decompressArchive(FileManipulator fm) {
    try {
      // Build a new "_ge_decompressed" archive file in the current directory
      File origFile = fm.getFile();

      String pathOnly = FilenameSplitter.getDirectory(origFile);
      String filenameOnly = FilenameSplitter.getFilename(origFile);
      String extensionOnly = FilenameSplitter.getExtension(origFile);

      File decompFile = new File(pathOnly + File.separatorChar + filenameOnly + "_ge_decompressed" + "." + extensionOnly);
      if (decompFile.exists()) {
        // we've already decompressed this file before - open and return it
        return new FileManipulator(decompFile, false);
      }

      FileManipulator decompFM = new FileManipulator(decompFile, true);

      long currentOffset = fm.getOffset();
      long arcSize = fm.getLength();

      fm.seek(0); // to fill the buffer from the start of the file, for efficient reading
      fm.skip(2); // skip the 2-byte compression header, so we can grab the decompressed size

      // 3 bytes - Decompressed Size
      byte[] decompBytes = new byte[] { 0, fm.readByte(), fm.readByte(), fm.readByte() };
      int decompLength = IntConverter.convertBig(decompBytes);

      fm.seek(0); // return to the start, ready for decompression

      int compLength = (int) fm.getLength();

      // Now decompress the block into the decompressed file
      TaskProgressManager.setMessage(Language.get("Progress_DecompressingArchive")); // progress bar
      TaskProgressManager.setMaximum(arcSize); // progress bar
      TaskProgressManager.setIndeterminate(true);

      Exporter_REFPACK exporter = Exporter_REFPACK.getInstance();
      exporter.open(fm, compLength, decompLength);

      while (exporter.available()) {
        decompFM.writeByte(exporter.read());
      }

      // Force-write out the decompressed file to write it to disk, then change the buffer to read-only.
      decompFM.close();
      decompFM = new FileManipulator(decompFile, false);

      TaskProgressManager.setMessage(Language.get("Progress_ReadingArchive")); // progress bar
      TaskProgressManager.setIndeterminate(false);

      // Return the file pointer to the beginning, and return the decompressed file
      decompFM.seek(currentOffset);
      return decompFM;
    }
    catch (Throwable t) {
      ErrorLogger.log(t);
      return null;
    }
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

      byte[] headerBytes = fm.readBytes(4);

      // Header
      if (StringConverter.convertLittle(headerBytes).equals("BIGF")) {
        rating += 50;
      }
      else if (headerBytes[0] == 16 && ByteConverter.unsign(headerBytes[1]) == 251) {
        rating += 50; // the whole file is compressed using RefPack
        return rating; // exit early to avoid the remaining checks
      }

      // Archive Size
      if (fm.readInt() == fm.getLength()) {
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
   Provide hints to the previewer so that certain document types are displayed appropriately
   **********************************************************************************************
   **/
  @Override
  public ViewerPlugin previewHint(Resource resource) {
    String extension = resource.getExtension();
    if (extension.equalsIgnoreCase("shd") || extension.equalsIgnoreCase("loc") || extension.equalsIgnoreCase("skn") || extension.equalsIgnoreCase("irr")) {
      return (ViewerPlugin) WSPluginManager.getPlugin("Viewer", "TXT");
    }
    else if (extension.equalsIgnoreCase("cdata")) { // Red Alert 3 Audio Files
      return (ViewerPlugin) WSPluginManager.getPlugin("Viewer", "FFMPEG_Audio_EA_SCHl");
    }
    return null;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public Resource[] read(File path) {
    try {

      addFileTypes();

      // NOTE - All fields are big endian EXCEPT for the archive size field

      FileManipulator fm = new FileManipulator(path, false);

      // 4 - Header (BIGF)
      int headerByte1 = ByteConverter.unsign(fm.readByte());
      int headerByte2 = ByteConverter.unsign(fm.readByte());

      if (headerByte1 == 16 && headerByte2 == 251) {
        // the whole file is compressed using RefPack - decompress it first

        FileManipulator decompFM = decompressArchive(fm);
        if (decompFM != null) {
          fm.close(); // close the original archive
          fm = decompFM; // now we're going to read from the decompressed file instead
          fm.seek(0); // go to the same point in the decompressed file as in the compressed file

          path = fm.getFile(); // So the resources are stored against the decompressed file
          fm.skip(2); // skip the 2-byte header we checked at the beginning
        }
      }

      // 4 - Archive Size (LITTLE)
      fm.skip(6);

      // 4 - Number Of Files
      int numFiles = IntConverter.changeFormat(fm.readInt());
      FieldValidator.checkNumFiles(numFiles);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // 4 - Directory Size
      fm.skip(4);

      long arcSize = fm.getLength();

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        // 4 Bytes - Data Offset
        int offset = IntConverter.changeFormat(fm.readInt());

        // 4 Bytes - File Size
        int length = IntConverter.changeFormat(fm.readInt());

        if (offset == arcSize && length == -1) {
          length = 0;
        }
        else {
          FieldValidator.checkOffset(offset, arcSize);
          FieldValidator.checkLength(length, arcSize);
        }

        // X Bytes - Filename (null)
        String filename = fm.readNullString();
        if (filename.length() == 0) {
          filename = Resource.generateFilename(i);
        }
        else {
          FieldValidator.checkFilename(filename);
        }

        //path,id,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset, length);

        TaskProgressManager.setValue(i);
      }

      // Now go through the files and work out if they're compressed or not. If so, set the exporter appropriately
      ExporterPlugin exporter = Exporter_REFPACK.getInstance();
      fm.getBuffer().setBufferSize(10); // teeny tiny buffer for quick reads
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];
        fm.seek(resource.getOffset());

        // 2 - Compression Header
        if (fm.readByte() == 16 && ByteConverter.unsign(fm.readByte()) == 251) {
          // Compressed

          // 3 - Decompressed Length
          byte[] decompBytes = new byte[] { 0, fm.readByte(), fm.readByte(), fm.readByte() };
          int decompLength = IntConverter.convertBig(decompBytes);

          resource.setDecompressedLength(decompLength);
          resource.setExporter(exporter);
        }

        // force the "not added" icon, in case the original archive was decompressed
        resource.forceNotAdded(true);

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
  
  **********************************************************************************************
  **/
  @Override
  public void write(Resource[] resources, File path) {
    try {

      FileManipulator fm = new FileManipulator(path, true);
      int numFiles = resources.length;

      TaskProgressManager.setMaximum(numFiles);

      int paddingMultiple = 4; // PC
      String extension = FilenameSplitter.getExtension(path);
      boolean ps2Format = false;
      if (extension.equalsIgnoreCase("viv")) {
        // PS2
        paddingMultiple = 64;
        ps2Format = true;
      }

      // Calculations
      TaskProgressManager.setMessage(Language.get("Progress_PerformingCalculations"));
      int directorySize = 16 + 8;
      int filesSize = 0;
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];

        // If the file hasn't changed, keep it compressed (if it's compressed), otherwise for replaced files we need to just store uncompressed
        long length = resource.getLength();
        if (resource.isReplaced()) {
          length = resource.getDecompressedLength();

          // work out the compressed length (5-byte header + 113 bytes for every 112 bytes in the file + a 1-4 stop block at the end)
          int numBlocks = (int) (length / 112);
          int lastBlock = (int) (length - (numBlocks * 112));

          int compLength = 5 + (numBlocks * 113);
          if (lastBlock < 4) {
            // last bytes written out as the stop block
            compLength += lastBlock + 1;
          }
          else {
            // write out the last block, then write an empty stop block
            compLength += (lastBlock + 1) + 1;// (lastBlock+header)+emptyStopBlock
          }

          length = compLength;
        }

        // add padding to the file
        if (i != numFiles - 1) {
          // No padding on the last file
          long paddingSize = paddingMultiple - (length % paddingMultiple);
          if (paddingSize < paddingMultiple) {
            length += paddingSize;
          }
        }
        filesSize += length;

        directorySize += 8 + resource.getNameLength() + 1;
      }

      int dirPaddingSize = paddingMultiple - (directorySize % paddingMultiple);
      if (dirPaddingSize < paddingMultiple) {
      }
      else {
        dirPaddingSize = 0;
      }

      int archiveSize = filesSize + directorySize + dirPaddingSize;

      // Write Header Data

      // 4 - Header (BIGF)
      fm.writeString("BIGF");

      // 4 - Archive Size (LITTLE)
      fm.writeInt(archiveSize);

      // 4 - Number Of Files
      fm.writeInt(IntConverter.changeFormat(numFiles));

      // 4 - Directory Size
      fm.writeInt(IntConverter.changeFormat(directorySize));

      // Write Directory
      TaskProgressManager.setMessage(Language.get("Progress_WritingDirectory"));
      int offset = directorySize + dirPaddingSize;
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];

        // If the file hasn't changed, keep it compressed (if it's compressed), otherwise for replaced files we need to just store uncompressed
        long length = resource.getLength();
        if (resource.isReplaced()) {
          length = resource.getDecompressedLength();

          // work out the compressed length (5-byte header + 113 bytes for every 112 bytes in the file + a 1-4 stop block at the end)
          int numBlocks = (int) (length / 112);
          int lastBlock = (int) (length - (numBlocks * 112));

          int compLength = 5 + (numBlocks * 113);
          if (lastBlock < 4) {
            // last bytes written out as the stop block
            compLength += lastBlock + 1;
          }
          else {
            // write out the last block, then write an empty stop block
            compLength += (lastBlock + 1) + 1;// (lastBlock+header)+emptyStopBlock
          }

          length = compLength;
        }

        // 4 Bytes - Data Offset
        fm.writeInt(IntConverter.changeFormat(offset));

        // 4 Bytes - File Size
        fm.writeInt(IntConverter.changeFormat((int) length));

        // X Bytes - Filename (null)
        fm.writeNullString(resource.getName());
        offset += length;

        long paddingSize = paddingMultiple - (length % paddingMultiple);
        if (paddingSize < paddingMultiple) {
          offset += paddingSize;
        }
      }

      // 8 - Unknown
      if (ps2Format) {
        // PS2
        fm.writeInt(842150476);
        fm.writeInt(16777216);
      }
      else {
        // PC
        fm.writeInt(1278358324);
        fm.writeInt(0);
      }

      // Dir Padding
      for (int i = 0; i < dirPaddingSize; i++) {
        fm.writeByte(0);
      }

      // Write Files
      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));

      ExporterPlugin defaultExporter = Exporter_Default.getInstance();
      ExporterPlugin refpackExporter = Exporter_REFPACK.getInstance();

      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];

        // If the file hasn't changed, keep it compressed (if it's compressed), otherwise for replaced files we need to just store uncompressed
        long length = resource.getLength();
        if (resource.isReplaced()) {
          // Write it using RefPack
          //length = resource.getDecompressedLength();
          //write(resource, fm);
          length = write(refpackExporter, resource, fm); // returns the compressed length, for calculating the padding down further
        }
        else {
          // trick it to use the Default exporter, copy verbatim, so that it stays compressed
          ExporterPlugin origExporter = resource.getExporter();
          resource.setExporter(defaultExporter);
          write(resource, fm);
          resource.setExporter(origExporter);
        }

        if (i != numFiles - 1) {
          // padding on all files EXCEPT the last one

          long paddingSize = paddingMultiple - (length % paddingMultiple);
          if (paddingSize < paddingMultiple) {
            for (int p = 0; p < paddingSize; p++) {
              fm.writeByte(0);
            }
          }
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
   When replacing SSH/FSH images, if the fileToReplaceWith is a different format image (eg DDS, PNG, ...)
   it can be converted into a SSH/FSH image. All other files are replaced without conversion
   @param resourceBeingReplaced the Resource in the archive that is being replaced
   @param fileToReplaceWith the file on your PC that will replace the Resource. This file is the
          one that will be converted into a different format, if applicable.
   @return the converted file, if conversion was applicable/successful, else the original fileToReplaceWith
   **********************************************************************************************
   **/
  @Override
  public File convertOnReplace(Resource resourceBeingReplaced, File fileToReplaceWith) {
    // Only tested a little bit, on NHL2003 PS2 8-bit paletted images (format 2, palette format 33)

    String beingReplacedExtension = resourceBeingReplaced.getExtension();
    if (beingReplacedExtension.equalsIgnoreCase("ssh") || beingReplacedExtension.equalsIgnoreCase("fsh") || beingReplacedExtension.equalsIgnoreCase("psh") || beingReplacedExtension.equalsIgnoreCase("cmb")) {
      // try to convert

      String toReplaceWithExtension = FilenameSplitter.getExtension(fileToReplaceWith);
      if (toReplaceWithExtension.equalsIgnoreCase("ssh") || toReplaceWithExtension.equalsIgnoreCase("fsh") || toReplaceWithExtension.equalsIgnoreCase("psh") || toReplaceWithExtension.equalsIgnoreCase("cmb")) {
        // if the fileToReplace already has a SSH/FSH extension, assume it's already a compatible SSH/FSH file and doesn't need to be converted
        return fileToReplaceWith;
      }

      //
      //
      // if we're here, we want to scan to see if we can find an Image ViewerPlugin that can read the file into an ImageResource,
      // which we can then convert into a SSH/FSH using plugin Viewer_BIG_BIGF_FSH_SHPI
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
          // 4. We have found a plugin that was able to render the image

          // 5. Need to see if there are multiple frames, and if so, try to render them all
          try {
            if (fileToReplaceWith.getName().contains("_ge_frame_")) {
              // probably multiple frames

              // find all the frame files (max 1000)
              String baseName = fileToReplaceWith.getAbsolutePath();
              String extension = "";
              int numberPos = baseName.lastIndexOf("_ge_frame_");
              int dotPos = baseName.lastIndexOf('.');
              if (numberPos > 0) {
                if (dotPos > numberPos) {
                  extension = baseName.substring(dotPos);
                  baseName = baseName.substring(0, numberPos + 10);
                }
              }
              File[] imageFiles = new File[1000];
              int numImages = 0;
              for (int f = 0; f < 1000; f++) {
                File imageFile = new File(baseName + f + extension);
                if (imageFile.exists()) {
                  imageFiles[f] = imageFile;
                  numImages++;
                }
                else {
                  // no more images
                  break;
                }
              }

              // now have all the image files, read each of them into an ImageResource
              ViewerPlugin plugin = (ViewerPlugin) plugins[i].getPlugin();
              ImageResource[] images = new ImageResource[numImages];
              for (int f = 0; f < numImages; f++) {
                FileManipulator singleFM = new FileManipulator(imageFiles[f], false);
                PreviewPanel previewPanel = plugin.read(singleFM);
                singleFM.close();

                if (previewPanel != null && previewPanel instanceof PreviewPanel_Image) {
                  images[f] = ((PreviewPanel_Image) previewPanel).getImageResource();
                }
              }

              // now set next/previous frames
              for (int f = 0; f < numImages; f++) {
                ImageResource image = images[f];
                if (f == numImages - 1) {
                  image.setNextFrame(images[0]);
                }
                else {
                  image.setNextFrame(images[f + 1]);
                }

                if (f == 0) {
                  image.setPreviousFrame(images[numImages - 1]);
                }
                else {
                  image.setPreviousFrame(images[f - 1]);
                }
              }

              // put the first frame onto the original previewpanel
              ImageResource firstImage = images[0];
              firstImage.setManualFrameTransition(true);

              ((PreviewPanel_Image) imagePreviewPanel).setImageResource(firstImage);

              // now the imagePreviewPanel contains the first frame, with links to all the subsequent frames, all loaded from files.
            }
          }
          catch (Throwable t) {
            ErrorLogger.log(t);
          }
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
      // If we're here, we have a rendered image, so we want to convert it into STX using Viewer_XAF_XAF_STX
      //
      //
      if (beingReplacedExtension.equalsIgnoreCase("cmb")) {
        Viewer_BIG_BIGF_CMB converterPlugin = new Viewer_BIG_BIGF_CMB();

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
        Viewer_BIG_BIGF_FSH_SHPI converterPlugin = new Viewer_BIG_BIGF_FSH_SHPI();

        File destination = new File(fileToReplaceWith.getAbsolutePath() + "." + beingReplacedExtension);
        if (destination.exists()) {
          destination.delete();
        }

        FileManipulator fmOut = new FileManipulator(destination, true);
        converterPlugin.replace(resourceBeingReplaced, imagePreviewPanel, fmOut);
        fmOut.close();

        return destination;
      }

    }
    else {
      return fileToReplaceWith;
    }
  }

}
