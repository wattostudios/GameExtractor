/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2025 wattostudios
 *
 * License Information:
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License
 * published by the Free Software Foundation; either version 2 of the License, or (at your option) any later versions. This
 * program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranties
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License at http://www.gnu.org for more
 * details. For further information on this application, refer to the authors' website.
 */

package org.watto.ge.plugin.viewer;

import java.awt.Image;

import org.watto.ErrorLogger;
import org.watto.component.PreviewPanel;
import org.watto.component.PreviewPanel_3DModel;
import org.watto.datatype.Archive;
import org.watto.datatype.ImageResource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.archive.Plugin_CSA;
import org.watto.io.FileManipulator;
import org.watto.io.converter.ShortConverter;

import javafx.geometry.Point3D;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_CSA_PME extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_CSA_PME() {
    super("CSA_PME", "PME Model");
    setExtensions("pme");

    setGames("Star Stable Online");
    setPlatforms("PC");
    setStandardFileFormat(false);
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

      ArchivePlugin readPlugin = Archive.getReadPlugin();
      if (readPlugin instanceof Plugin_CSA) {
        rating += 50;
      }

      if (FieldValidator.checkExtension(fm, extensions)) {
        rating += 25;
      }
      else {
        return 0;
      }

      if (fm.readInt() == 19) {
        rating += 5;
      }

      if (fm.readInt() == 14) {
        rating += 5;
      }

      if (FieldValidator.checkFilenameLength(fm.readInt())) {
        rating += 5;
      }

      return rating;

    }
    catch (

    Throwable t) {
      return 0;
    }
  }

  float minX = 20000f;

  float maxX = -20000f;

  float minY = 20000f;

  float maxY = -20000f;

  float minZ = 20000f;

  float maxZ = -20000f;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public PreviewPanel read(FileManipulator fm) {

    try {

      // Read in the model

      float[] points = null;
      float[] texCoords = null;
      int[] faces = null;

      minX = 20000f;
      maxX = -20000f;
      minY = 20000f;
      maxY = -20000f;
      minZ = 20000f;
      maxZ = -20000f;

      // 4 - Unknown (19)
      // 4 - Unknown (14)
      fm.skip(8);

      // 4 - Model Name Length
      int nameLength = fm.readInt();
      FieldValidator.checkFilenameLength(nameLength);

      // X - Model Name
      fm.skip(nameLength);

      // 2 - Unknown (4)
      // 2 - Unknown (256/0)
      fm.skip(4);

      // 4 - Source Filename Length
      nameLength = fm.readInt();
      FieldValidator.checkFilenameLength(nameLength);

      // X - Source Filename
      fm.skip(nameLength);

      // 8 - Hash?
      // 4 - null
      fm.skip(12);

      // 4 - Unknown Float
      // 4 - Unknown Float
      // 4 - Unknown Float
      // 4 - Unknown Float
      // 4 - Unknown Float
      // 4 - Unknown Float
      // 4 - Unknown Float
      // 4 - Unknown Float
      // 4 - Unknown Float
      // 4 - Unknown Float
      fm.skip(40);

      // 4 - Number of Parts
      int numParts = fm.readInt();
      try {
        FieldValidator.checkNumFiles(numParts);
      }
      catch (Throwable t) {
        // sometimes has another 4 bytes prior to here, so *now* we're at the right place, try again
        numParts = fm.readInt();
        FieldValidator.checkNumFiles(numParts);
      }

      MeshView[] meshView = new MeshView[numParts];

      for (int p = 0; p < numParts; p++) {

        // 4 - Unknown (15)
        fm.skip(4);

        // 4 - Object Name Length
        nameLength = fm.readInt();
        FieldValidator.checkFilenameLength(nameLength);

        // X - Object Name
        fm.skip(nameLength);

        // 4 - null
        // 4 - Unknown (25)
        fm.skip(8);

        // 4 - Part Name Length
        nameLength = fm.readInt();
        FieldValidator.checkFilenameLength(nameLength);

        // X - Part Name
        fm.skip(nameLength);

        // 4 - Lighting Map Name Length
        nameLength = fm.readInt();
        FieldValidator.checkFilenameLength(nameLength + 1); // allows blank names

        // X - Lighting Map Name
        fm.skip(nameLength);

        // 4 - Unknown Float
        // 4 - Unknown Float
        // 4 - Unknown Float
        // 4 - Unknown Float
        // 4 - Unknown Float
        // 4 - Unknown Float
        // 4 - Unknown Float
        // 4 - Unknown Float
        // 4 - Unknown Float
        // 4 - Unknown Float
        fm.skip(40);

        // 4 - null
        // 4 - Unknown (4)
        fm.skip(8);

        // 4 - Vertex Flags (274=32-bytes per Vertex, 338=36-bytes per Vertex)
        int vertexFlags = fm.readInt();

        // 4 - Number of Vertices
        int numVertices = fm.readInt();
        FieldValidator.checkNumFaces(numVertices);

        // 4 - Number of Face Indices
        int numFaces = fm.readInt();
        FieldValidator.checkNumFaces(numFaces);

        //
        //
        // FACES
        //
        //
        int numFaces3 = numFaces;
        FieldValidator.checkNumFaces(numFaces3);

        numFaces = numFaces3 / 3;
        int numFaces6 = numFaces3 * 2;

        faces = new int[numFaces6]; // need to store front and back faces

        for (int i = 0, j = 0; i < numFaces; i++, j += 6) {
          // 2 - Point Index 1
          // 2 - Point Index 2
          // 2 - Point Index 3
          int facePoint1 = (ShortConverter.unsign(fm.readShort()));
          int facePoint2 = (ShortConverter.unsign(fm.readShort()));
          int facePoint3 = (ShortConverter.unsign(fm.readShort()));

          // reverse face first (so the light shines properly, for this model specifically)
          faces[j] = facePoint3;
          faces[j + 1] = facePoint2;
          faces[j + 2] = facePoint1;

          // forward face second
          faces[j + 3] = facePoint1;
          faces[j + 4] = facePoint2;
          faces[j + 5] = facePoint3;

        }

        //
        //
        // VERTICES
        //
        //

        int numVertices3 = numVertices * 3;
        points = new float[numVertices3];

        int numPoints2 = numVertices * 2;
        texCoords = new float[numPoints2];

        int skipSize = 20; // 32 bytes
        if (vertexFlags == 338) {
          skipSize = 24; // 36 bytes
        }
        else if (vertexFlags == 258) {
          skipSize = 8; // 20 bytes
        }
        else if (vertexFlags == 274) {
          skipSize = 20; // 32 bytes
        }
        else if (vertexFlags == 322) {
          skipSize = 12; // 24 bytes
        }
        else if (vertexFlags == 530) {
          skipSize = 28; // 40 bytes
        }
        else {
          ErrorLogger.log("Unknown Vertex Size for flags: " + vertexFlags + " for file " + fm.getFile().getName());
        }

        for (int i = 0, j = 0, k = 0; i < numVertices; i++, j += 3, k += 2) {
          // 4 - Vertex X
          // 4 - Vertex Y
          // 4 - Vertex Z
          float xPoint = fm.readFloat();
          float yPoint = fm.readFloat();
          float zPoint = fm.readFloat();

          points[j] = xPoint;
          points[j + 1] = yPoint;
          points[j + 2] = zPoint;

          // 4 - Unknown Float
          // 4 - Unknown Float
          // 4 - Unknown Float
          // 4 - Unknown Float
          // 4 - Unknown Float
          // (optional) 4 - Unknown Float
          fm.skip(skipSize);

          // Don't know where the texture co-ords are yet
          float xTexture = 0;
          float yTexture = 0;

          texCoords[k] = xTexture;
          texCoords[k + 1] = yTexture;

          // Calculate the size of the object
          if (xPoint < minX) {
            minX = xPoint;
          }
          if (xPoint > maxX) {
            maxX = xPoint;
          }

          if (yPoint < minY) {
            minY = yPoint;
          }
          if (yPoint > maxY) {
            maxY = yPoint;
          }

          if (zPoint < minZ) {
            minZ = zPoint;
          }
          if (zPoint > maxZ) {
            maxZ = zPoint;
          }
        }

        // add the part to the model
        if (faces != null && points != null && texCoords != null) {
          // we have a full mesh for a single object - add it to the model
          TriangleMesh triangleMesh = new TriangleMesh();
          triangleMesh.getTexCoords().addAll(texCoords);

          triangleMesh.getPoints().addAll(points);
          triangleMesh.getFaces().addAll(faces);

          faces = null;
          points = null;

          texCoords = null;

          // Create the MeshView
          MeshView view = new MeshView(triangleMesh);
          meshView[p] = view;
        }
      }

      // calculate the sizes and centers
      float diffX = (maxX - minX);
      float diffY = (maxY - minY);
      float diffZ = (maxZ - minZ);

      float centerX = minX + (diffX / 2);
      float centerY = minY + (diffY / 2);
      float centerZ = minZ + (diffZ / 2);

      Point3D sizes = new Point3D(diffX, diffY, diffZ);
      Point3D center = new Point3D(centerX, centerY, centerZ);

      PreviewPanel_3DModel preview = new PreviewPanel_3DModel(meshView, sizes, center);

      return preview;
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
  public void write(PreviewPanel panel, FileManipulator destination) {
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public ImageResource readThumbnail(FileManipulator source) {
    try {
      PreviewPanel preview = read(source);
      if (preview == null || !(preview instanceof PreviewPanel_3DModel)) {
        return null;
      }

      PreviewPanel_3DModel preview3D = (PreviewPanel_3DModel) preview;

      // generate a thumbnail-sized snapshot
      int thumbnailSize = 150; // bigger than ImageResource, so it is shrunk (and smoothed as a result)
      preview3D.generateSnapshot(thumbnailSize, thumbnailSize);

      Image image = preview3D.getImage();
      if (image != null) {
        ImageResource resource = new ImageResource(image, preview3D.getImageWidth(), preview3D.getImageHeight());
        preview3D.onCloseRequest(); // cleanup memory
        return resource;
      }

      preview3D.onCloseRequest(); // cleanup memory

      return null;
    }
    catch (Throwable t) {
      ErrorLogger.log(t);
      return null;
    }
  }

}