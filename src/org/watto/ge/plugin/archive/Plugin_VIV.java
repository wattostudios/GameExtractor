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
import org.watto.Language;
import org.watto.Settings;
import org.watto.datatype.FileType;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.io.FileManipulator;
import org.watto.io.converter.ByteArrayConverter;
import org.watto.io.converter.IntConverter;
import org.watto.io.converter.ShortConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_VIV extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_VIV() {

    super("VIV", "VIV");

    //         read write replace rename
    setProperties(true, false, true, false);

    setGames("Triple Play 98: Home Run Derby",
        "Triple Play 2000",
        "NHL 98",
        "NHL 2001");
    setExtensions("big", "viv");
    setPlatforms("PC", "PS1");

    // MUST BE LOWER CASE !!!
    setFileTypes(new FileType("shd", "Shader", FileType.TYPE_OTHER),
        new FileType("wak", "Map Settings", FileType.TYPE_OTHER),
        new FileType("pso", "Polygon Shader", FileType.TYPE_OTHER),
        new FileType("vso", "Vertex Shader", FileType.TYPE_OTHER),
        new FileType("w3d", "3D Object", FileType.TYPE_OTHER),
        new FileType("wnd", "Window Settings", FileType.TYPE_OTHER),
        new FileType("fsh", "FSH Image", FileType.TYPE_IMAGE),
        new FileType("ssh", "SSH Image", FileType.TYPE_IMAGE),
        new FileType("psh", "PSH Image", FileType.TYPE_IMAGE));

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

      // Header
      if (fm.readString(2).equals(new String(new byte[] { (byte) 192, (byte) 251 }))) {
        rating += 50;
      }

      fm.skip(2);

      // Number Of Files
      int numFiles = ShortConverter.changeFormat(fm.readShort());
      if (FieldValidator.checkNumFiles(numFiles)) {
        rating += 5;
      }

      // null
      if (fm.readByte() == 0) {
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

      // BIG ENDIAN FORMAT!

      FileManipulator fm = new FileManipulator(path, false);

      // 2 - Header (192,251)
      // 2 - Directory Size [+ 0-7]
      fm.skip(4);

      // 2 - Number Of Files
      int numFiles = ShortConverter.changeFormat(fm.readShort());
      FieldValidator.checkNumFiles(numFiles);

      long arcSize = fm.getLength();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {

        // 4 - Offset
        //long offset = IntConverter.changeFormat(fm.readInt());
        byte[] offsetData = new byte[4];
        offsetData[0] = 0;
        offsetData[1] = fm.readByte();
        offsetData[2] = fm.readByte();
        offsetData[3] = fm.readByte();
        long offset = IntConverter.convertBig(offsetData);
        FieldValidator.checkOffset(offset, arcSize);

        // 1 - Unknown
        fm.skip(1);

        // 2 - Length
        //long length = ShortConverter.changeFormat(fm.readShort());
        //FieldValidator.checkLength(length,arcSize);
        fm.skip(2);

        // X - Filename (null)
        String filename = fm.readNullString();

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

      long offset = 6;
      for (int i = 0; i < numFiles; i++) {
        offset += 6 + resources[i].getNameLength() + 1;
      }

      int headerPaddingSize = calculatePadding(offset, 4);
      offset += headerPaddingSize;

      // Write Header Data

      // 2 - Header (192,251)
      // 2 - Directory Size (not including padding)
      // 2 - Number Of Files
      fm.writeBytes(src.readBytes(6));

      // Write Directory
      TaskProgressManager.setMessage(Language.get("Progress_WritingDirectory"));
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];
        long length = resource.getDecompressedLength();

        // 3 - File Offset
        byte[] offsetBytes = ByteArrayConverter.convertBig((int) offset);
        fm.writeByte(offsetBytes[1]);
        fm.writeByte(offsetBytes[2]);
        fm.writeByte(offsetBytes[3]);

        // 3 - File Length
        byte[] lengthBytes = ByteArrayConverter.convertBig((int) length);
        fm.writeByte(lengthBytes[1]);
        fm.writeByte(lengthBytes[2]);
        fm.writeByte(lengthBytes[3]);

        // X - Filename
        // 1 - null Filename Terminator
        fm.writeString(resource.getName());
        fm.writeByte(0);

        offset += length;
        offset += calculatePadding(offset, 4);
      }

      for (int p = 0; p < headerPaddingSize; p++) {
        fm.writeByte(0);
      }

      // Write Files
      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));
      for (int i = 0; i < resources.length; i++) {
        Resource resource = resources[i];
        write(resource, fm);

        int paddingSize = calculatePadding(resource.getDecompressedLength(), 4);
        for (int p = 0; p < paddingSize; p++) {
          fm.writeByte(0);
        }

        TaskProgressManager.setValue(i);
      }

      src.close();
      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

  /**
   **********************************************************************************************
   Uses the conversion routine of Plugin_BIG_BIGF as it's all the same
   **********************************************************************************************
   **/
  public File convertOnReplace(Resource resourceBeingReplaced, File fileToReplaceWith) {
    return new Plugin_BIG_BIGF().convertOnReplace(resourceBeingReplaced, fileToReplaceWith);
  }

}
