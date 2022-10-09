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
import org.watto.io.converter.IntConverter;
import org.watto.io.converter.ShortConverter;
import javafx.geometry.Point3D;
import javafx.scene.shape.TriangleMesh;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_RESOURCES_BLWO_BRML extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_RESOURCES_BLWO_BRML() {
    super("RESOURCES_BLWO_BRML", "Doom 3 BLWO Model");
    setExtensions("blwo");

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
      // 4 - Unknown (1)
      // 4 - null
      fm.skip(20);

      // 4 - Original Filename Length
      int nameLength = fm.readInt();
      FieldValidator.checkFilenameLength(nameLength);

      // X - Original Filename
      fm.skip(nameLength);

      // 4 - Unknown
      // 4 - Unknown Float
      // 4 - Unknown Float
      // 4 - Unknown Float
      // 4 - Unknown Float
      // 4 - Unknown Float
      // 4 - Unknown Float
      // 4 - Unknown
      // 1 - Unknown
      // 4 - Number of Vertices
      fm.skip(37);

      //
      //
      // VERTICES
      //
      //

      // 4 - Number of Vertices (BIG)
      int numVertices = IntConverter.changeFormat(fm.readInt());
      FieldValidator.checkNumVertices(numVertices);

      int numVertices3 = numVertices * 3;
      points = new float[numVertices3];

      int numPoints2 = numVertices * 2;
      texCoords = new float[numPoints2];

      for (int i = 0, j = 0, k = 0; i < numVertices; i++, j += 3, k += 2) {
        // 4 - Vertex X (LITTLE)
        // 4 - Vertex Y (LITTLE)
        // 4 - Vertex Z (LITTLE)
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

      // 4 - null
      fm.skip(4);

      //
      //
      // FACES
      //
      //

      // 4 - Number of Face Indexes
      int numFaces = IntConverter.changeFormat(fm.readInt()) / 3;
      FieldValidator.checkNumFaces(numFaces);

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

      // Skip the rest of the file - we don't need it

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

      return null;
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