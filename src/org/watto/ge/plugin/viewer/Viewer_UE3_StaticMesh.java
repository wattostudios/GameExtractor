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
import org.watto.ge.plugin.archive.PluginGroup_UE3;
import org.watto.io.FileManipulator;
import org.watto.io.converter.ShortConverter;
import javafx.geometry.Point3D;
import javafx.scene.shape.TriangleMesh;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_UE3_StaticMesh extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_UE3_StaticMesh() {
    super("UE3_StaticMesh", "Unreal Engine 3 StaticMesh Viewer");
    setExtensions("staticmesh");

    setGames("Unreal Engine 3");
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
      if (plugin instanceof PluginGroup_UE3) {
        rating += 50;
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

      // Set up the mesh
      TriangleMesh triangleMesh = new TriangleMesh();

      float[] points = null;
      float[] normals = null;
      float[] texCoords = null;
      int[] faces = null;

      minX = 20000f;
      maxX = -20000f;
      minY = 20000f;
      maxY = -20000f;
      minZ = 20000f;
      maxZ = -20000f;

      // for 14 entries
      // 4 - Unknown Float
      fm.skip(56);

      // UNKNOWN DIRECTORY
      // 4 - Number of Entries (6)
      int numUnknown1 = fm.readInt();
      FieldValidator.checkNumFiles(numUnknown1);

      // 4 - Entry Size (64/128/512)
      int unknown1Size = fm.readInt();
      FieldValidator.checkRange(unknown1Size, 0, 2048); // guess

      // for each entry
      // 64/128/512 - Unknown
      fm.skip(numUnknown1 * unknown1Size);

      // FACES
      // 4 - Face Block Size (8)
      int faceBlockSize = fm.readInt();

      // 4 - Number of Faces
      int numFaces = fm.readInt();
      FieldValidator.checkNumFaces(numFaces);

      int numFaces3 = numFaces * 3;
      int numFaces6 = numFaces3 * 2;

      faces = new int[numFaces6]; // need to store front and back faces

      if (faceBlockSize == 8) {
        for (int i = 0, j = 0; i < numFaces; i++, j += 6) {
          // 2 - Face Index 1
          // 2 - Face Index 2
          // 2 - Face Index 3
          // 2 - null
          int facePoint1 = (ShortConverter.unsign(fm.readShort()));
          int facePoint2 = (ShortConverter.unsign(fm.readShort()));
          int facePoint3 = (ShortConverter.unsign(fm.readShort()));

          fm.skip(2);

          // forward face first (so the light shines properly, for this model specifically)
          faces[j] = facePoint1;
          faces[j + 1] = facePoint2;
          faces[j + 2] = facePoint3;

          // reverse face second
          faces[j + 3] = facePoint3;
          faces[j + 4] = facePoint2;
          faces[j + 5] = facePoint1;

        }
      }
      else {
        ErrorLogger.log("[Viewer_UE3_StaticMesh] Unsupported Face Block Size: " + faceBlockSize);
        return null;
      }

      // 4 - Size? (20) (including this field)
      // 16 - null
      // 4 - Unknown (1)
      // 12 - null
      // 4 - Unknown
      fm.skip(40);

      // 4 - Number of Blocks (1/2)
      int numBlocks = fm.readInt();
      FieldValidator.checkNumFiles(numBlocks);

      // for each block
      // 4 - Unknown
      // 4 - Unknown (1)
      // 4 - Unknown (1)
      // 4 - Unknown (1)
      // 4 - Unknown (null for first entry)
      // 4 - Number of Entries?
      // 4 - Unknown (null for first entry)
      // 4 - Number of Entries?
      // 4 - Unknown (null for first entry)
      // 4 - Unknown (1)
      // 4 - Unknown (null for first entry)
      // 4 - Number of ???
      // 1 - Unknown (null)
      fm.skip(numBlocks * 49);

      // VERTICES
      // 4 - Vertex Size? (12)
      // 4 - Number of Vertices
      // 4 - Vertex Size? (12)
      fm.skip(12);

      // 4 - Number of Vertices
      int numVertices = fm.readInt();
      FieldValidator.checkNumVertices(numVertices);

      int numVertices3 = numVertices * 3;
      points = new float[numVertices3];
      normals = new float[numVertices3];

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

        // Dummy Tex Coords for now
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

      // TEXTURE COORDINATES?
      // 4 - Unknown (0/2)
      int texCheck1 = fm.readInt();

      // 4 - Unknown (0/16)
      int texCheck2 = fm.readInt();

      // 8 - Number of Coords
      fm.skip(8);

      if (texCheck1 != 00 && texCheck2 != 0) {
        // 4 - Unknown (2)
        fm.skip(4);

        // 4 - Number of Coords
        int numCoords = fm.readInt();
        FieldValidator.checkNumVertices(numCoords);

        // for each coord
        // 4 - Coordinate U (Float)
        // 4 - Coordinate V (Float)
        // 4 - Unknown
        // 4 - Unknown
        fm.skip(numCoords * 16);

        // 8 - null
        // 4 - Number of Coords
        fm.skip(12);
      }

      if (faces != null && points != null && texCoords != null) {
        // we have a full mesh for a single object - add it to the model
        triangleMesh.getTexCoords().addAll(texCoords);

        triangleMesh.getPoints().addAll(points);
        triangleMesh.getFaces().addAll(faces);
        //triangleMesh.getNormals().addAll(normals);

        faces = null;
        points = null;
        normals = null;
        texCoords = null;
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

      PreviewPanel_3DModel preview = new PreviewPanel_3DModel(triangleMesh, sizes, center);

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