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
import org.watto.ErrorLogger;
import org.watto.Language;
import org.watto.component.WSPluginManager;
import org.watto.datatype.FileType;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.exporter.Exporter_REFPACK;
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

    setPlatforms("PC", "PS2");

    // MUST BE LOWER CASE !!!
    setFileTypes(new FileType("shd", "Shader", FileType.TYPE_OTHER),
        new FileType("wak", "Map Settings", FileType.TYPE_OTHER),
        new FileType("pso", "Polygon Shader", FileType.TYPE_OTHER),
        new FileType("vso", "Vertex Shader", FileType.TYPE_OTHER),
        new FileType("w3d", "3D Object", FileType.TYPE_OTHER),
        new FileType("wnd", "Window Settings", FileType.TYPE_OTHER),
        new FileType("fsh", "FSH Image", FileType.TYPE_IMAGE));

  }

  /**
   **********************************************************************************************
   Decompressed an archive, where the whole archive is compressed.
   Reads the compressed block information first, then processes the compressed blocks themselves.
   Writes the output to a file with the same name, but with "_ge_decompressed" at the end of it.
   The decompressed file contains the same Unreal header as the compressed file, so you can open
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

      // Calculations
      TaskProgressManager.setMessage(Language.get("Progress_PerformingCalculations"));
      int directorySize = 16 + 8;
      int filesSize = 0;
      for (int i = 0; i < numFiles; i++) {
        filesSize += resources[i].getDecompressedLength();
        directorySize += 8 + resources[i].getNameLength() + 1;
      }

      int dirPaddingSize = 4 - (directorySize % 4);
      if (dirPaddingSize < 4) {
        directorySize += dirPaddingSize;
      }

      int archiveSize = filesSize + directorySize;

      // Write Header Data

      // 4 - Header (BIGF)
      fm.writeString("BIGF");

      // 4 - Archive Size (LITTLE)
      fm.writeInt(archiveSize);

      // 4 - Number Of Files
      fm.writeInt(IntConverter.changeFormat(numFiles));

      // 4 - Directory Size
      fm.writeInt(IntConverter.changeFormat(directorySize - 1));

      // Write Directory
      TaskProgressManager.setMessage(Language.get("Progress_WritingDirectory"));
      int offset = directorySize;
      for (int i = 0; i < numFiles; i++) {
        long length = resources[i].getDecompressedLength();

        // 4 Bytes - Data Offset
        fm.writeInt(IntConverter.changeFormat(offset));

        // 4 Bytes - File Size
        fm.writeInt(IntConverter.changeFormat((int) length));

        // X Bytes - Filename (null)
        fm.writeNullString(resources[i].getName());
        offset += length;

        long paddingSize = 4 - (resources[i].getDecompressedLength() % 4);
        if (paddingSize < 4) {
          offset += paddingSize;
        }
      }

      fm.writeInt(1278358324);
      fm.writeInt(0);

      for (int i = 0; i < dirPaddingSize; i++) {
        fm.writeByte(0);
      }

      // Write Files
      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));

      for (int i = 0; i < numFiles; i++) {
        write(resources[i], fm);

        long paddingSize = 4 - (resources[i].getDecompressedLength() % 4);
        if (paddingSize < 4) {
          for (int p = 0; p < paddingSize; p++) {
            fm.writeByte(0);
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

}
