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

package org.watto.ge.plugin.viewer;

import java.util.Arrays;
import org.watto.ErrorLogger;
import org.watto.component.PreviewPanel;
import org.watto.component.PreviewPanel_3DModel;
import org.watto.datatype.Archive;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.AllFilesPlugin;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.archive.Plugin_POD;
import org.watto.io.FileManipulator;
import javafx.scene.shape.TriangleMesh;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_POD_BIN extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_POD_BIN() {
    super("POD_BIN", "POD_BIN 3D Model");
    setExtensions("bin");
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public boolean canWrite(PreviewPanel panel) {
    return false;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public int getMatchRating(FileManipulator fm) {
    try {

      int rating = 0;

      ArchivePlugin plugin = Archive.getReadPlugin();
      if (plugin instanceof Plugin_POD) {
        rating += 50;
      }
      else if (!(plugin instanceof AllFilesPlugin)) {
        return 0;
      }

      if (FieldValidator.checkExtension(fm, extensions)) {
        rating += 25;
      }
      else {
        return 0;
      }

      return rating;

    }
    catch (

    Throwable t) {
      return 0;
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public PreviewPanel read(FileManipulator fm) {

    // Read in the model

    // 4 - Header
    fm.skip(4);

    // 4 - Scale
    int scale = fm.readInt();
    scale = 50;

    // 4 - Unknown
    // 4 - Unknown
    fm.skip(8);

    // 4 - Number of Points
    int numPoints = fm.readInt();

    int numPoints3 = numPoints * 3;
    float[] points = new float[numPoints3];

    for (int i = 0, j = 0; i < numPoints; i++, j += 3) {
      // 4 - X Point
      // 4 - Y Point
      // 4 - Z point
      float xPoint = ((float) fm.readInt()) / scale;
      float yPoint = ((float) fm.readInt()) / scale;
      float zPoint = ((float) fm.readInt()) / scale;

      points[j] = xPoint;
      points[j + 1] = yPoint;
      points[j + 2] = zPoint;
    }

    long arcSize = fm.getLength();

    // Set up the mesh
    TriangleMesh triangleMesh = new TriangleMesh();
    //triangleMesh.getTexCoords().addAll(0, 0); // empty texture

    float[] texCoords = new float[numPoints * 2];
    Arrays.fill(texCoords, 0);
    triangleMesh.getTexCoords().addAll(texCoords); // empty texture

    // Add the points
    triangleMesh.getPoints().addAll(points);

    while (fm.getOffset() < arcSize) {
      // 4 - Block Type
      int blockType = fm.readInt();
      //System.out.println("Block " + blockType + " at " + fm.getOffset());

      if (blockType == 3) {
        // Normal Vertex
        fm.skip(8);

        float[] normals = new float[numPoints3];

        for (int i = 0, j = 0; i < numPoints; i++, j += 3) {
          // 4 - X Point
          // 4 - Y Point
          // 4 - Z point
          float xNormal = ((float) fm.readInt()) / scale;
          float yNormal = ((float) fm.readInt()) / scale;
          float zNormal = ((float) fm.readInt()) / scale;

          normals[j] = xNormal;
          normals[j + 1] = yNormal;
          normals[j + 2] = zNormal;
        }

        triangleMesh.getNormals().addAll(normals);
      }
      else if (blockType == 23) {
        // Unknown
        fm.skip(8);
      }
      else if (blockType == 13) {
        // Texture

        // 4 - Unknown
        fm.skip(4);

        // 16 - Texture Filename
        //String textureName = fm.readNullString(16);
        //System.out.println(textureName);
        fm.skip(16);
      }
      else if (blockType == 29) {
        // Animated Texture

        // 4 - Unknown
        fm.skip(4);

        // 4 - Number of Textures
        int numTextures = fm.readInt();

        // 16 - Unknown
        fm.skip(16);

        // X - Image Filenames
        fm.skip(numTextures * 32);
      }
      else if (blockType == 10) {
        // Color
        fm.skip(4);
      }
      else if (blockType == 14 || blockType == 17 || blockType == 24 || blockType == 41 || blockType == 51 || blockType == 52) {
        // Face

        // 4 - Number of Points for the Face
        int numFacePoints = fm.readInt();

        // 16 - Unknown
        fm.skip(16);

        // X - Points
        int[] face = null;

        int[] rawPoints = new int[numFacePoints];
        for (int i = 0; i < numFacePoints; i++) {
          // 4 - Point Index
          rawPoints[i] = fm.readInt();

          // 8 - Unknown
          fm.skip(8);
        }

        if (numFacePoints == 3) {
          /*
          face = new int[3];
          face[0] = rawPoints[0];
          face[1] = rawPoints[1];
          face[2] = rawPoints[2];
          */
          face = new int[6];
          face[0] = rawPoints[0];
          face[1] = rawPoints[1];
          face[2] = rawPoints[2];

          face[3] = rawPoints[2]; // reverse faces
          face[4] = rawPoints[1];
          face[5] = rawPoints[0];
        }
        else if (numFacePoints == 4) {
          //System.out.println("4");
          /*
          face = new int[6];
          face[0] = rawPoints[0];
          face[1] = rawPoints[1];
          face[2] = rawPoints[2];
          face[3] = rawPoints[1];
          face[4] = rawPoints[2];
          face[5] = rawPoints[3];
          */

          face = new int[12];
          face[0] = rawPoints[0];
          face[1] = rawPoints[1];
          face[2] = rawPoints[2];
          face[3] = rawPoints[0];
          face[4] = rawPoints[3];
          face[5] = rawPoints[2];

          face[6] = rawPoints[2]; // reverse faces
          face[7] = rawPoints[3];
          face[8] = rawPoints[0];
          face[9] = rawPoints[2];
          face[10] = rawPoints[1];
          face[11] = rawPoints[0];
        }
        else {
          ErrorLogger.log("[Viewer_POD_BIN] Unsupported Face Size: " + numFacePoints);
        }

        if (face != null) {
          // Add the face
          triangleMesh.getFaces().addAll(face);
        }

      }
      else if (blockType == 5 || blockType == 25) {
        // Face

        // 4 - Number of Points for the Face
        int numFacePoints = fm.readInt();

        // 16 - Unknown
        fm.skip(16);

        // X - Points
        int[] face = null;

        int[] rawPoints = new int[numFacePoints];
        for (int i = 0; i < numFacePoints; i++) {
          // 4 - Point Index
          rawPoints[i] = fm.readInt();
        }

        if (numFacePoints == 3) {
          /*
          face = new int[3];
          face[0] = rawPoints[0];
          face[1] = rawPoints[1];
          face[2] = rawPoints[2];
          */
          face = new int[6];
          face[0] = rawPoints[0];
          face[1] = rawPoints[1];
          face[2] = rawPoints[2];

          face[3] = rawPoints[2]; // reverse faces
          face[4] = rawPoints[1];
          face[5] = rawPoints[0];
        }
        else if (numFacePoints == 4) {
          //System.out.println("4");
          /*
          face = new int[6];
          face[0] = rawPoints[0];
          face[1] = rawPoints[1];
          face[2] = rawPoints[2];
          face[3] = rawPoints[1];
          face[4] = rawPoints[2];
          face[5] = rawPoints[3];
          */

          face = new int[12];
          face[0] = rawPoints[0];
          face[1] = rawPoints[1];
          face[2] = rawPoints[2];
          face[3] = rawPoints[0];
          face[4] = rawPoints[3];
          face[5] = rawPoints[2];

          face[6] = rawPoints[2]; // reverse faces
          face[7] = rawPoints[3];
          face[8] = rawPoints[0];
          face[9] = rawPoints[2];
          face[10] = rawPoints[1];
          face[11] = rawPoints[0];
        }
        else {
          ErrorLogger.log("[Viewer_POD_BIN] Unsupported Face Size: " + numFacePoints);
        }

        if (face != null) {
          // Add the face
          triangleMesh.getFaces().addAll(face);
        }

      }
      else if (blockType == 0) {
        // End of File
        break;
      }
      else {
        ErrorLogger.log("[Viewer_POD_BIN] Unknown Block Type: " + blockType + " at " + fm.getOffset());
        break;
      }

    }

    PreviewPanel_3DModel preview = new PreviewPanel_3DModel(triangleMesh);
    return preview;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void write(PreviewPanel panel, FileManipulator destination) {
  }

}