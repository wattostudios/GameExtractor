/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2023 wattostudios
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
import org.watto.ge.plugin.archive.Plugin_RESOURCES_2;
import org.watto.ge.plugin.archive.Plugin_TANGORESOURCE;
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
public class Viewer_RESOURCES_2_BMD6MODEL extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_RESOURCES_2_BMD6MODEL() {
    super("RESOURCES_2_BMD6MODEL", "RAGE BMD6MODEL Mesh Viewer");
    setExtensions("bmd6model");

    setGames("RAGE");
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
      if (plugin instanceof Plugin_RESOURCES_2 || plugin instanceof Plugin_TANGORESOURCE) {
        rating += 50;
      }

      if (FieldValidator.checkExtension(fm, extensions)) {
        rating += 25;
      }
      else {
        return 0;
      }

      String header = fm.readString(4);
      if (header.equals((char) 25 + "6MM")) {
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
      long arcSize = fm.getLength();

      // Set up the mesh
      //TriangleMesh triangleMesh = new TriangleMesh();
      MeshView[] meshView = null; // we're using MeshView, as we're setting textures on the mesh

      float[] points = null;

      float[] texCoords = null;
      int[] faces = null;

      minX = 20000f;
      maxX = -20000f;
      minY = 20000f;
      maxY = -20000f;
      minZ = 20000f;
      maxZ = -20000f;

      boolean rendered = false;

      // 4 - Header ((byte)25 + "6MM")
      // 4 - Unknown

      // 4 - Unknown Float (-16.0)
      // 4 - Unknown Float (-16.0)
      // 4 - Unknown Float (-16.0)
      // 4 - Unknown Float (16.0)
      // 4 - Unknown Float (16.0)
      // 4 - Unknown Float (16.0)

      // 4 - Unknown (1) (LITTLE ENDIAN)
      // 4 - null
      // 1 - Unknown (5)
      fm.skip(43);

      // 4 - Joints Directory Offset [+45]
      int jointsDirOffset = ShortConverter.changeFormat(fm.readShort()) + 45;
      FieldValidator.checkOffset(jointsDirOffset, arcSize);

      // 2 - Joints Directory Offset [+45]
      fm.skip(2);

      // 2 - Number of Joints (round to a multiple of 8 for reading the JOINTS directory)
      int numJoints = ShortConverter.changeFormat(fm.readShort());
      FieldValidator.checkRange(numJoints, 1, 500); // guess

      // 4 - Unknown
      // 2 - Pose Data Offset [+?]
      // 2 - Joint Hierarchy Offset [+?]

      // X - Unknown
      fm.relativeSeek(jointsDirOffset);

      numJoints += ArchivePlugin.calculatePadding(numJoints, 8);

      // for each joint
      for (int j = 0; j < numJoints; j++) {
        // 4 - Joint Name Length (LITTLE ENDIAN)
        int jointNameLength = fm.readInt();
        FieldValidator.checkFilenameLength(jointNameLength + 1); // can be null

        // X - Joint Name
        fm.skip(jointNameLength);
      }

      // for each joint
      //   1 - Unknown

      // for each joint
      //   48 - Unknown

      // 4 - Unknown Float
      // 4 - Unknown Float
      // 4 - Unknown Float
      // 4 - Unknown Float
      // 4 - Unknown Float
      // 4 - Unknown Float
      fm.skip((numJoints * 49) + 24);

      // 4 - Number of Meshes
      int numMeshes = IntConverter.changeFormat(fm.readInt());
      FieldValidator.checkRange(numMeshes, 1, 50); // guess

      //System.out.println(numMeshes);

      meshView = new MeshView[numMeshes];

      String[] meshNames = new String[numMeshes];

      for (int m = 0; m < numMeshes; m++) {
        // 4 - Mesh Name Length (LITTLE ENDIAN)
        int nameLength = fm.readInt();
        FieldValidator.checkFilenameLength(nameLength);

        // X - Mesh Name
        meshNames[m] = fm.readString(nameLength);

        // 4 - Material Name Length (LITTLE ENDIAN)
        nameLength = fm.readInt();
        FieldValidator.checkFilenameLength(nameLength);

        // X - Material Name
        fm.skip(nameLength);

        // 1 - Unknown (1)
        fm.skip(1);

        // 4 - Number of Vertices
        int numVertices = IntConverter.changeFormat(fm.readInt());
        FieldValidator.checkNumVertices(numVertices);

        // 4 - Number of Faces (Faces, not Face Indices)
        int numFaces = IntConverter.changeFormat(fm.readInt());
        FieldValidator.checkNumVertices(numFaces);

        // 4 - Unknown Float
        // 4 - Unknown Float
        // 4 - Unknown Float
        // 4 - Unknown Float
        // 4 - Unknown Float
        // 4 - Unknown Float
        fm.skip(24);

        //
        // VERTICES
        //

        int numVertices3 = numVertices * 3;
        points = new float[numVertices3];

        int numPoints2 = numVertices * 2;
        texCoords = new float[numPoints2];

        for (int v = 0, j = 0, k = 0; v < numVertices; v++, j += 3, k += 2) {

          // 4 - Vertex X
          // 4 - Vertex Y
          // 4 - Vertex Z
          float xPoint = FloatConverter.changeFormat(fm.readFloat());
          float yPoint = FloatConverter.changeFormat(fm.readFloat());
          float zPoint = FloatConverter.changeFormat(fm.readFloat());

          points[j] = xPoint;
          points[j + 1] = yPoint;
          points[j + 2] = zPoint;

          texCoords[k] = 0;
          texCoords[k + 1] = 0;

          // 4 - Unknown Float
          // 4 - Unknown Float
          // 4 - Unknown Float
          // 4 - Unknown Float
          // 4 - Unknown Float
          fm.skip(20);

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
        // FACES
        //

        int numFaces3 = numFaces * 3;
        int numFaces6 = numFaces3 * 2;

        faces = new int[numFaces6]; // need to store front and back faces

        // read as 3-index triangles
        for (int f = 0, j = 0; f < numFaces; f++, j += 6) {
          // 2 - Point Index 1
          // 2 - Point Index 2
          // 2 - Point Index 3
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

        // 4 - null
        // 4 - Unknown
        fm.skip(8);

        // 1 - Unknown (0/1)
        int unknownFlag = fm.readByte();

        if (unknownFlag == 1) {
          // for each vertex
          //  4 - Unknown
          fm.skip(numVertices * 4);
        }

        // 4 - null
        fm.skip(4);

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
          meshView[m] = view;

          rendered = true;
        }

      }

      if (!rendered) {
        // didn't find any meshes
        return null;
      }

      // don't want to render the Collision mesh - only the proper mesh
      int realNumMeshes = 0;
      MeshView[] realMeshes = new MeshView[numMeshes];
      for (int m = 0; m < numMeshes; m++) {
        if (meshNames[m].startsWith("textures")) {
          // ignore it
        }
        else {
          realMeshes[realNumMeshes] = meshView[m];
          realNumMeshes++;
        }
      }

      if (realNumMeshes < numMeshes) {
        meshView = new MeshView[realNumMeshes];
        System.arraycopy(realMeshes, 0, meshView, 0, realNumMeshes);
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