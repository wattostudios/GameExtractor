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

import org.watto.ErrorLogger;
import org.watto.component.PreviewPanel;
import org.watto.component.PreviewPanel_3DModel;
import org.watto.datatype.Archive;
import org.watto.datatype.ImageResource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.archive.Plugin_DAT_GRA2;
import org.watto.io.FileManipulator;
import javafx.geometry.Point3D;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_DAT_GRA2_MODEL_MDL1 extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_DAT_GRA2_MODEL_MDL1() {
    super("DAT_GRA2_MODEL_MDL1", "Legend of Grimrock MODEL Mesh Viewer");
    setExtensions("model");

    setGames("Legend of Grimrock",
        "Legend of Grimrock 2");
    setPlatforms("PC");
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
      if (plugin instanceof Plugin_DAT_GRA2) {
        rating += 50;
      }

      if (FieldValidator.checkExtension(fm, extensions)) {
        rating += 25;
      }
      else {
        return 0;
      }

      String header = fm.readString(4);
      if (header.equals("MDL1")) {
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
  @SuppressWarnings("unused")
  @Override
  public PreviewPanel read(FileManipulator fm) {

    try {

      long arcSize = fm.getLength();

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

      // 4 - Header (MDL1)
      // 4 - Version? (2)
      fm.skip(8);

      // 4 - Number of Parts
      int numParts = fm.readInt();
      FieldValidator.checkNumFiles(numParts);

      // Set up the mesh
      MeshView[] meshView = new MeshView[numParts];
      int realNumParts = 0;

      int vertexStartIndex = 0; // so that each group references the right points in the model
      for (int g = 0; g < numParts; g++) {

        // 4 - Part Name Length
        int nameLength = fm.readInt();
        FieldValidator.checkFilenameLength(nameLength);

        // X - Part Name
        // 52 - Unknown
        fm.skip(nameLength + 52);

        // 4 - Mesh Indicator? (-1=not a mesh, 0=mesh)
        int meshFlag = fm.readInt();
        if (meshFlag != 0) {
          continue; // not a mesh, just some other node
        }

        // 4 - Mesh Header (MESH)
        // 4 - Unknown (2)
        fm.skip(8);

        // 4 - Number of Vertices
        int numVertices = fm.readInt();
        FieldValidator.checkNumVertices(numVertices);

        // 4 - Unknown (3)
        // 4 - Unknown (3)
        // 4 - Vertex Entry Size (12)
        fm.skip(12);

        //
        // VERTICES
        //

        int numVertices3 = numVertices * 3;
        points = new float[numVertices3];

        int numPoints2 = numVertices * 2;
        texCoords = new float[numPoints2];

        // for each vertex
        for (int v = 0, j = 0, k = 0; v < numVertices; v++, j += 3, k += 2) {
          // 4 - Point X (float)
          // 4 - Point Y (float)
          // 4 - Point Z (float)
          float xPoint = fm.readFloat();
          float yPoint = fm.readFloat();
          float zPoint = fm.readFloat();

          points[j] = xPoint;
          points[j + 1] = yPoint;
          points[j + 2] = zPoint;

          // Skip the texture mapping for now
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

        // for (3 more)
        // 4 - Unknown (3)
        // 4 - Unknown (3)
        // 4 - Vertex Entry Size (12)

        // for each vertex
        //   4 - Unknown Float
        //   4 - Unknown Float
        //   4 - Unknown Float

        int skipSize = (((numVertices * 12) + 12) * 3);
        fm.skip(skipSize);

        // UNKNOWN BLOCK 3
        // 4 - Unknown (0)
        // 4 - Unknown (0/4)
        // 4 - Unknown (0/4)
        fm.skip(8);
        int blockCheck = fm.readInt();
        if (blockCheck != 0) {
          // 4 - Unknown
          fm.skip(numVertices * 4);
        }

        // 4 - Unknown (3)
        // 4 - Unknown (2)
        // 4 - Entry Size (8)

        // for each vertex
        //   4 - Unknown Float
        //   4 - Unknown Float

        // 84 - null

        skipSize = 12 + (numVertices * 8) + 84;
        fm.skip(skipSize);
        //long currentOffset = fm.getOffset();

        // UNKNOWN BLOCK 1
        // 4 - Unknown (0/2)
        // 4 - Unknown (0/4)
        // 4 - Unknown (0/16)
        fm.skip(8);
        blockCheck = fm.readInt();
        if (blockCheck != 0) {
          // 16 - Unknown
          fm.skip(numVertices * 16);
        }

        // UNKNOWN BLOCK 2
        // 4 - Unknown (0/2)
        // 4 - Unknown (0/4)
        // 4 - Unknown (0/16)
        fm.skip(8);
        blockCheck = fm.readInt();
        if (blockCheck != 0) {
          // 16 - Unknown
          fm.skip(numVertices * 16);
        }

        // 4 - Number of Face Indexes
        int numFaces = fm.readInt();
        FieldValidator.checkNumFaces(numFaces);

        //
        // FACES
        //

        int numFaces3 = numFaces;
        FieldValidator.checkNumFaces(numFaces3);

        numFaces = numFaces3 / 3;
        int numFaces6 = numFaces3 * 2;

        faces = new int[numFaces6]; // need to store front and back faces

        for (int f = 0, j = 0; f < numFaces; f++, j += 6) {
          // 4 - Point Index 1
          // 4 - Point Index 2
          // 4 - Point Index 3
          int facePoint1 = fm.readInt() + vertexStartIndex; // + vertexStartIndex so that we can render multiple groups in the same model
          int facePoint2 = fm.readInt() + vertexStartIndex;
          int facePoint3 = fm.readInt() + vertexStartIndex;

          // reverse face first (so the light shines properly, for this model specifically)
          faces[j] = facePoint3;
          faces[j + 1] = facePoint2;
          faces[j + 2] = facePoint1;

          // forward face second
          faces[j + 3] = facePoint1;
          faces[j + 4] = facePoint2;
          faces[j + 5] = facePoint3;
        }

        // 4 - Number of Groups (1/3)
        int numGroups = fm.readInt();
        FieldValidator.checkNumFiles(numGroups + 1); // to allow for 0 groups, maybe?

        // for each group
        for (int i = 0; i < numGroups; i++) {
          // 4 - Group Name Length
          int groupNameLength = fm.readInt();
          FieldValidator.checkFilenameLength(groupNameLength);
          // X - Group Name
          // 4 - Unknown (2)
          // 4 - First ID Number of something
          // 4 - Number of Entries of something
          fm.skip(groupNameLength + 12);
        }

        // 40 - Unknown
        fm.skip(40);

        // 4 - Number of Extra Blocks
        int numExtra = fm.readInt();
        FieldValidator.checkNumFiles(numExtra + 1); // allow 0 extra

        // for each extra block
        // 52 - Unknown
        fm.skip(numExtra * 52);

        // 12 - Unknown
        // 1 - End of Part Marker? (1)
        fm.skip(13);

        //
        // RENDER THE MESH
        //

        // we have a full mesh for a single object (including all parts adjusted) - add it to the model
        if (faces != null && points != null && texCoords != null) {
          // Create the Mesh
          TriangleMesh triangleMesh = new TriangleMesh();
          triangleMesh.getTexCoords().addAll(texCoords);

          triangleMesh.getPoints().addAll(points);
          triangleMesh.getFaces().addAll(faces);

          faces = null;
          points = null;
          texCoords = null;

          // Create the MeshView
          MeshView view = new MeshView(triangleMesh);
          meshView[realNumParts] = view;
          realNumParts++;

        }

        // Prepare for the next Group, so that the subsequent groups point to the right vertices
        // NO LONGER NEEDED, AS WE'RE USING SEPARATE MeshView FOR EACH GROUP
        //vertexStartIndex += numVertices;

      }

      if (realNumParts < numParts) {
        MeshView[] oldParts = meshView;
        meshView = new MeshView[realNumParts];
        System.arraycopy(oldParts, 0, meshView, 0, realNumParts);
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

      // return the preview based on a MeshView
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

      java.awt.Image image = preview3D.getImage();
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