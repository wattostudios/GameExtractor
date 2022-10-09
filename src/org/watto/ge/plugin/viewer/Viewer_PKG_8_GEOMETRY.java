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

import java.awt.Image;
import org.watto.ErrorLogger;
import org.watto.component.PreviewPanel;
import org.watto.component.PreviewPanel_3DModel;
import org.watto.datatype.Archive;
import org.watto.datatype.ImageResource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.archive.Plugin_PKG_8;
import org.watto.io.FileManipulator;
import org.watto.io.converter.IntConverter;
import org.watto.io.converter.ShortConverter;
import javafx.geometry.Point3D;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_PKG_8_GEOMETRY extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_PKG_8_GEOMETRY() {
    super("PKG_8_GEOMETRY", "World of Warships GEOMETRY Model");
    setExtensions("geometry");

    setGames("World of Warships");
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
      if (readPlugin instanceof Plugin_PKG_8) {
        rating += 50;
      }

      if (FieldValidator.checkExtension(fm, extensions)) {
        rating += 25;
      }
      else {
        return 0;
      }

      if (fm.readInt() == 1 && fm.readInt() == 1) {
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

      long arcSize = fm.getLength();

      // Read in the model

      // Set up the mesh
      //TriangleMesh triangleMesh = new TriangleMesh();
      MeshView[] meshView = null; // for storing multiple parts

      float[] points = null;
      float[] texCoords = null;
      int[] faces = null;

      minX = 20000f;
      maxX = -20000f;
      minY = 20000f;
      maxY = -20000f;
      minZ = 20000f;
      maxZ = -20000f;

      // 4 - Unknown (1)
      // 4 - Unknown (1)
      fm.skip(8);

      // 4 - Number of Parts 1 Entries
      int numParts = fm.readInt();

      // 4 - Number of Parts 2 Entries
      // 8 - null
      fm.skip(12);

      // 8 - Parts Directory 1 Offset
      long parts1DirOffset = fm.readLong();
      FieldValidator.checkOffset(parts1DirOffset, arcSize);

      // 8 - Parts Directory 2 Offset
      long parts2DirOffset = fm.readLong();
      FieldValidator.checkOffset(parts2DirOffset, arcSize);

      // 8 - Vertices Directory Offset
      long vertDirOffset = fm.readLong();
      FieldValidator.checkOffset(vertDirOffset, arcSize);

      // 8 - Face Index Directory Offset
      long faceDirOffset = fm.readLong();
      FieldValidator.checkOffset(faceDirOffset, arcSize);

      // 16 - null

      //
      //
      // PARTS
      //
      //
      fm.seek(parts1DirOffset);

      int[] partVertexStartIndexes = new int[numParts];
      int[] partVertexCounts = new int[numParts];
      int[] partIDs = new int[numParts];
      int[] partTypes = new int[numParts];
      int realNumParts = 0;
      for (int p = 0; p < numParts; p++) {
        // 4 - Hash?
        fm.skip(4);

        // 2 - Unknown (0=Part, 1=something else)
        int partType = fm.readShort();
        partTypes[p] = partType;
        if (partType == 0) {
          realNumParts++;
        }

        // 2 - Part ID
        int partID = fm.readShort();
        partIDs[p] = partID;

        // 4 - First Vertex for this Part
        int partVertexStartIndex = fm.readInt();
        partVertexStartIndexes[p] = partVertexStartIndex;

        // 4 - Number of Vertices for this Part
        int partVertexCount = fm.readInt();
        partVertexCounts[p] = partVertexCount;
      }

      // this is the number of parts, without the other funny ones
      meshView = new MeshView[realNumParts];

      fm.seek(parts2DirOffset);

      int[] partFaceStartIndexes = new int[numParts];
      int[] partFaceCounts = new int[numParts];
      for (int p = 0; p < numParts; p++) {
        // 4 - Hash?
        // 2 - null
        fm.skip(6);

        // 2 - Part ID
        int partID = fm.readShort();
        int partIndex = p;
        if (partIDs[partIndex] != partID) {
          // go through the part ID's to find the one that matches
          for (int i = 0; i < numParts; i++) {
            if (partIDs[i] == partID) {
              partIndex = i;
              break;
            }
          }
        }

        // 4 - First Face Index for this Part
        int partFaceStartIndex = fm.readInt();
        partFaceStartIndexes[partIndex] = partFaceStartIndex;

        // 4 - Number of Face Indexes for this Part
        int partFaceCount = fm.readInt();
        partFaceCounts[partIndex] = partFaceCount;
      }

      //
      //
      // VERTICES
      //
      //
      fm.seek(vertDirOffset);

      // 8 - Unknown (32)
      // 8 - Unknown (16)
      // 8 - Vertices Data Length (including these header fields)
      fm.skip(24);

      // 4 - Vertices Data Length (not including these header fields)
      long vertexDataLength = IntConverter.unsign(fm.readInt());

      // 2 - Vertex Index Size? (28/36)
      short vertexSize = fm.readShort();
      if (vertexSize < 0) {
        return null;
      }

      // 2 - Unknown (256/257)
      fm.skip(2);

      int numVertices = (int) (vertexDataLength / vertexSize);
      FieldValidator.checkNumVertices(numVertices);

      short vertexSkipSize = (short) (vertexSize - 12);

      int numVertices3 = numVertices * 3;
      points = new float[numVertices3];

      int numPoints2 = numVertices * 2;
      texCoords = new float[numPoints2];

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

        // 4 - Unknown
        // 4 - Unknown
        // 4 - Unknown
        // 4 - Unknown
        fm.skip(vertexSkipSize);

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

      //
      //
      // FACES
      //
      //
      fm.seek(faceDirOffset);

      // 8 - Unknown (16)
      fm.skip(8);

      // 4 - Face Indexes Data Length (not including these header fields)
      long faceDataLenth = IntConverter.unsign(fm.readInt());

      // 2 - null
      fm.skip(2);

      // 2 - Face Index Size (2/4)
      short faceSize = fm.readShort();

      int numFaces = (int) (faceDataLenth / faceSize);
      FieldValidator.checkNumFaces(numFaces);

      int numFaces3 = numFaces;
      FieldValidator.checkNumFaces(numFaces3);

      numFaces = numFaces3 / 3;
      int numFaces6 = numFaces3 * 2;

      faces = new int[numFaces6]; // need to store front and back faces

      if (faceSize == 2) {
        for (int i = 0, j = 0; i < numFaces; i++, j += 6) {
          // 2 - Point Index 1
          // 2 - Point Index 2
          // 2 - Point Index 3
          int facePoint1 = ShortConverter.unsign(fm.readShort());
          int facePoint2 = ShortConverter.unsign(fm.readShort());
          int facePoint3 = ShortConverter.unsign(fm.readShort());

          // reverse face first (so the light shines properly, for this model specifically)
          faces[j] = facePoint3;
          faces[j + 1] = facePoint2;
          faces[j + 2] = facePoint1;

          // forward face second
          faces[j + 3] = facePoint1;
          faces[j + 4] = facePoint2;
          faces[j + 5] = facePoint3;
        }
      }
      else if (faceSize == 4) {
        for (int i = 0, j = 0; i < numFaces; i++, j += 6) {
          // 4 - Point Index 1
          // 4 - Point Index 2
          // 4 - Point Index 3
          int facePoint1 = fm.readInt();
          int facePoint2 = fm.readInt();
          int facePoint3 = fm.readInt();

          // reverse face first (so the light shines properly, for this model specifically)
          faces[j] = facePoint3;
          faces[j + 1] = facePoint2;
          faces[j + 2] = facePoint1;

          // forward face second
          faces[j + 3] = facePoint1;
          faces[j + 4] = facePoint2;
          faces[j + 5] = facePoint3;
        }
      }
      else {
        ErrorLogger.log("[PKG_8] Invalid face index size: " + faceSize);
      }

      //
      //
      // NOW CONVERT INTO PARTS
      //
      //

      int currentPart = 0;
      for (int p = 0; p < numParts; p++) {
        if (partTypes[p] != 0) {
          continue; // only =0 are parts, the others are something else
        }

        int numPartVertices = partVertexCounts[p];
        int numPartVertices2 = numPartVertices * 2; // 2 tex co-ord points per vertex
        int numPartVertices3 = numPartVertices * 3; // 3 points per vertex

        int numPartFaces = partFaceCounts[p];
        int numPartFaces6 = numPartFaces * 2; // front and back

        int partVertexStart = partVertexStartIndexes[p];
        int partVertexStart2 = partVertexStart * 2;
        int partVertexStart3 = partVertexStart * 3;

        int partFaceStart = partFaceStartIndexes[p];
        int partFaceStart6 = partFaceStart * 2;

        float[] partPoints = new float[numPartVertices3];
        float[] partTexCoords = new float[numPartVertices2];
        int[] partFaces = new int[numPartFaces6];

        // copy from the master arrays into the part arrays
        System.arraycopy(points, partVertexStart3, partPoints, 0, numPartVertices3);
        System.arraycopy(texCoords, partVertexStart2, partTexCoords, 0, numPartVertices2);
        System.arraycopy(faces, partFaceStart6, partFaces, 0, numPartFaces6);

        int lowestFace = 999999;
        int highestFace = -999999;
        for (int i = 0; i < numPartFaces6; i++) {
          int faceIndex = partFaces[i];
          if (faceIndex < lowestFace) {
            lowestFace = faceIndex;
          }
          if (faceIndex > highestFace) {
            highestFace = faceIndex;
          }
        }
        //System.out.println("Part " + p + " ... Face indexes " + lowestFace + " to " + highestFace + " with face count " + numPartVertices);

        // we have a full mesh for a single object - add it to the model
        TriangleMesh triangleMesh = new TriangleMesh();

        triangleMesh.getTexCoords().addAll(partTexCoords);
        triangleMesh.getPoints().addAll(partPoints);
        triangleMesh.getFaces().addAll(partFaces);

        MeshView view = new MeshView(triangleMesh);
        meshView[currentPart] = view;
        currentPart++;
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