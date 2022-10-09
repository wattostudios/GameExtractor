/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2022 wattostudios
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
import org.watto.ge.plugin.archive.Plugin_RESOURCES;
import org.watto.io.FileManipulator;
import org.watto.io.converter.FloatConverter;
import org.watto.io.converter.IntConverter;
import org.watto.io.converter.ShortConverter;
import javafx.geometry.Point3D;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_RESOURCES_BMD5MESH_BRML extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_RESOURCES_BMD5MESH_BRML() {
    super("RESOURCES_BMD5MESH_BRML", "Doom 3 BMD5MESH Model");
    setExtensions("bmd5mesh");

    setGames("Doom 3");
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
      if (readPlugin instanceof Plugin_RESOURCES) {
        rating += 50;
      }

      if (FieldValidator.checkExtension(fm, extensions)) {
        rating += 25;
      }
      else {
        return 0;
      }

      if (fm.readString(4).equals("BRMl")) {
        rating += 25;
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

      // 4 - Header (BRMl) // Note: an L
      // 4 - null
      // 4 - Unknown
      // 4 - null
      // 24 - Unknown
      // 12 - null
      fm.skip(52);

      // 4 - Original Filename Length
      int nameLength = fm.readInt();
      FieldValidator.checkFilenameLength(nameLength);

      // X - Original Filename
      fm.skip(nameLength);

      // 4 - null
      // 8 - Unknown
      // 4 - Unknown (106)
      fm.skip(16);

      // 1 - Number of Parts
      int numParts = fm.readByte();
      FieldValidator.checkPositive(numParts);

      // for each part
      for (int p = 0; p < numParts; p++) {
        // 4 - Part Name Length
        nameLength = fm.readInt();
        FieldValidator.checkFilenameLength(nameLength);

        // X - Part Name
        // 4 - Unknown
        fm.skip(nameLength + 4);
      }

      // 4 - Number of Parts (BIG)
      numParts = IntConverter.changeFormat(fm.readInt());
      FieldValidator.checkRange(numParts, 1, 200); // guess

      // for each part
      //   28 - Unknown
      fm.skip(numParts * 28);

      // 4 - Number of Parts (BIG)
      numParts = IntConverter.changeFormat(fm.readInt());
      FieldValidator.checkRange(numParts, 1, 200); // guess

      // for each part
      //    16 - Unknown
      fm.skip(numParts * 16);

      // for each part
      //    8 - Unknown
      fm.skip(numParts * 8);

      // for each part
      //    16 - Unknown
      fm.skip(numParts * 16);

      // for each part
      //   8 - Unknown
      fm.skip(numParts * 8);

      // 4 - Number of Meshes (BIG)
      numParts = IntConverter.changeFormat(fm.readInt());
      FieldValidator.checkRange(numParts, 1, 200); // guess

      MeshView[] meshViews = new MeshView[numParts];
      int realNumParts = 0;

      int[] faceCounts = new int[numParts];
      for (int p = 0; p < numParts; p++) {
        //System.out.println(fm.getOffset());
        // 4 - Mesh Name Length
        nameLength = fm.readInt();
        FieldValidator.checkFilenameLength(nameLength);

        // X - Mesh Name
        fm.skip(nameLength);

        // 4 - Unknown (BIG) (73)
        fm.skip(4);

        // 4 - Number of Faces (BIG)
        int numFaces = IntConverter.changeFormat(fm.readInt());
        FieldValidator.checkNumFaces(numFaces);

        faceCounts[p] = numFaces;

        // 4 - Unknown (BIG) (1/2)
        int unknownCount = IntConverter.changeFormat(fm.readInt());
        // 1/2 - Unknown
        fm.skip(unknownCount);

        // 4 - Unknown Float
        // 4 - Unknown (BIG) (73)
        fm.skip(8);

        // 4 - Number of Vertices (BIG)
        int numVertices = IntConverter.changeFormat(fm.readInt());
        FieldValidator.checkNumVertices(numVertices);

        // 4 - Block 1 Count (BIG)
        int block1Count = IntConverter.changeFormat(fm.readInt());
        FieldValidator.checkNumFaces(block1Count + 1); // +1 to allow nulls

        // 4 - Block 2 Count (BIG)
        int block2Count = IntConverter.changeFormat(fm.readInt());
        FieldValidator.checkNumFaces(block2Count + 1); // +1 to allow nulls

        // 4 - Block 3 Count (BIG)
        int block3Count = IntConverter.changeFormat(fm.readInt());
        FieldValidator.checkNumFaces(block3Count + 1); // +1 to allow nulls

        // 4 - Block 4 Count (BIG)
        int block4Count = IntConverter.changeFormat(fm.readInt());
        FieldValidator.checkNumFaces(block4Count + 1); // +1 to allow nulls

        //
        //
        // VERTICES
        //
        //
        int numVertices3 = numVertices * 3;
        points = new float[numVertices3];

        int numPoints2 = numVertices * 2;
        texCoords = new float[numPoints2];

        for (int i = 0, j = 0, k = 0; i < numVertices; i++, j += 3, k += 2) {
          // 4 - Vertex X (BIG)
          // 4 - Vertex Y (BIG)
          // 4 - Vertex Z (BIG)
          float xPoint = FloatConverter.changeFormat(fm.readFloat());
          float yPoint = FloatConverter.changeFormat(fm.readFloat());
          float zPoint = FloatConverter.changeFormat(fm.readFloat());

          points[j] = xPoint;
          points[j + 1] = yPoint;
          points[j + 2] = zPoint;

          // 4 - Unknown
          // 4 - Unknown
          // 4 - Unknown
          // 4 - Unknown
          // 4 - Unknown
          fm.skip(20);

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

        int numFaces3 = numFaces * 3;
        FieldValidator.checkNumFaces(numFaces3);

        int numFaces6 = numFaces3 * 2;

        faces = new int[numFaces6]; // need to store front and back faces

        for (int i = 0, j = 0; i < numFaces; i++, j += 6) {
          // 2 - Point Index 1 (BIG)
          // 2 - Point Index 2 (BIG)
          // 2 - Point Index 3 (BIG)
          int facePoint1 = (ShortConverter.unsign(ShortConverter.changeFormat(fm.readShort())));
          int facePoint2 = (ShortConverter.unsign(ShortConverter.changeFormat(fm.readShort())));
          int facePoint3 = (ShortConverter.unsign(ShortConverter.changeFormat(fm.readShort())));

          // forward face first (so the light shines properly, for this model specifically)
          faces[j] = facePoint1;
          faces[j + 1] = facePoint2;
          faces[j + 2] = facePoint3;

          // reverse face second
          faces[j + 3] = facePoint3;
          faces[j + 4] = facePoint2;
          faces[j + 5] = facePoint1;
        }

        //System.out.println(fm.getOffset());

        // for each Block 1 Entry
        //   2 - Unknown Index (BIG)
        fm.skip(block1Count * 2);

        // for each Block 2 Entry
        //      4 - Unknown ID (BIG)
        fm.skip(block2Count * 4);

        // for each Block 3 Entry
        //      4 - Unknown ID 1 (BIG)
        //      4 - Unknown ID 2 (BIG)
        fm.skip(block3Count * 8);

        // for each Block 4 Entry
        //      2 - Unknown Index (BIG)
        //      2 - Unknown Index (BIG)
        //      2 - Unknown Index (BIG)
        //      2 - Unknown Index (BIG)
        fm.skip(block4Count * 8);

        // 4 - null
        fm.skip(4);

        // add the part to the model
        if (faces != null && points != null && texCoords != null) {
          // we have a full mesh for a single object - add it to the model
          TriangleMesh triangleMesh = new TriangleMesh();

          // only keep the real objects (have block2 and block3) - the others are views or something
          //if (block3Count > 0) {
          triangleMesh.getTexCoords().addAll(texCoords);

          triangleMesh.getPoints().addAll(points);
          triangleMesh.getFaces().addAll(faces);

          MeshView view = new MeshView(triangleMesh);
          meshViews[realNumParts] = view;
          realNumParts++;
          //}

          faces = null;
          points = null;
          texCoords = null;
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

      if (realNumParts < numParts) {
        MeshView[] oldMeshViews = meshViews;
        meshViews = new MeshView[realNumParts];
        System.arraycopy(oldMeshViews, 0, meshViews, 0, realNumParts);
      }

      PreviewPanel_3DModel preview = new PreviewPanel_3DModel(meshViews, sizes, center);

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