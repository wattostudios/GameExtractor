
package org.watto.ge.plugin.archive;

import java.io.File;
import org.watto.task.TaskProgressManager;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
////////////////////////////////////////////////////////////////////////////////////////////////
//                                                                                            //
//                                       GAME EXTRACTOR                                       //
//                               Extensible Game Archive Editor                               //
//                                http://www.watto.org/extract                                //
//                                                                                            //
//                           Copyright (C) 2002-2009  WATTO Studios                           //
//                                                                                            //
// This program is free software; you can redistribute it and/or modify it under the terms of //
// the GNU General Public License published by the Free Software Foundation; either version 2 //
// of the License, or (at your option) any later versions. This program is distributed in the //
// hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranties //
// of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License //
// at http://www.gnu.org for more details. For updates and information about this program, go //
// to the WATTO Studios website at http://www.watto.org or email watto@watto.org . Thanks! :) //
//                                                                                            //
////////////////////////////////////////////////////////////////////////////////////////////////
import org.watto.io.FileManipulator;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_3DF extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_3DF() {

    super("3DF", "3DF");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Carnivores",
        "Carnivores 2",
        "Carnivores: Ice Age");
    setExtensions("3df");
    setPlatforms("PC");

    setFileTypes("tri", "3D Triangle",
        "pnt", "3D Point",
        "bne", "3D Bone",
        "pic", "Texture Image");

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

      // Number Of Files
      if (FieldValidator.checkNumFiles(fm.readInt() + fm.readInt() + fm.readInt())) {
        rating += 5;
      }

      long arcSize = fm.getLength();

      // Texture Size
      if (FieldValidator.checkLength(fm.readInt(), arcSize)) {
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

      long arcSize = fm.getLength();

      // 4 - Number Of Points
      int numPoints = fm.readInt();

      // 4 - Number Of Triangles
      int numTriangles = fm.readInt();

      // 4 - Number Of Bones
      int numBones = fm.readInt();

      // 4 - Texture Length
      int textureLength = fm.readInt();
      FieldValidator.checkLength(textureLength, arcSize);

      int numFiles = numBones + numPoints + numTriangles + 1;
      FieldValidator.checkNumFiles(numFiles);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      int realNumFiles = 0;

      // Loop through triangle directory
      for (int i = 0; i < numTriangles; i++) {
        // 4 - Point 1
        // 4 - Point 2
        // 4 - Point 3
        // 4 - Point 1 X
        // 4 - Point 2 X
        // 4 - Point 3 X
        // 4 - Point 1 Y
        // 4 - Point 2 Y
        // 4 - Point 3 Y
        // 4 - Flags
        // 4 - Unknown
        // 4 - Parent Triangle Index
        // 4 - Unknown
        // 4 - Unknown
        // 4 - Unknown
        // 4 - Unknown
        long offset = (int) fm.getOffset();
        long length = 64;
        fm.skip(64);

        String filename = Resource.generateFilename(i) + ".tri";

        //path,id,name,offset,length,decompLength,exporter
        resources[realNumFiles] = new Resource(path, filename, offset, length);

        TaskProgressManager.setValue(realNumFiles);
        realNumFiles++;
      }

      // Loop through point directory
      for (int i = 0; i < numPoints; i++) {
        // 4 - X Coordinate
        // 4 - Y Coordinate
        // 4 - Z Coordinate
        // 4 - Attached Bone Index
        long offset = (int) fm.getOffset();
        long length = 16;
        fm.skip(16);

        String filename = Resource.generateFilename(i) + ".pnt";

        //path,id,name,offset,length,decompLength,exporter
        resources[realNumFiles] = new Resource(path, filename, offset, length);

        TaskProgressManager.setValue(realNumFiles);
        realNumFiles++;
      }

      // Loop through bones directory
      for (int i = 0; i < numBones; i++) {
        long offset = (int) fm.getOffset();

        // 32 - Bone Name
        String filename = fm.readNullString(32);
        FieldValidator.checkFilename(filename);
        filename += ".bne";

        // 4 - Bone X
        // 4 - Bone Y
        // 4 - Bone Z
        // 2 - Parent
        // 2 - Unknown
        fm.skip(4);

        long length = 48;

        //path,id,name,offset,length,decompLength,exporter
        resources[realNumFiles] = new Resource(path, filename, offset, length);

        TaskProgressManager.setValue(realNumFiles);
        realNumFiles++;
      }

      // texture image
      int textureOffset = (int) fm.getOffset();
      String textureFilename = "Texture.pic";
      fm.skip(textureLength);

      resources[realNumFiles] = new Resource(path, textureFilename, textureOffset, textureLength);
      realNumFiles++;

      fm.close();

      return resources;

    }
    catch (Throwable t) {
      logError(t);
      return null;
    }
  }

}
