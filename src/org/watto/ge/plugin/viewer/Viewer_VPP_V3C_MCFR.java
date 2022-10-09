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
import org.watto.ge.plugin.archive.Plugin_VPP;
import org.watto.io.FileManipulator;
import org.watto.io.buffer.ByteBuffer;
import org.watto.io.converter.ShortConverter;
import javafx.geometry.Point3D;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_VPP_V3C_MCFR extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_VPP_V3C_MCFR() {
    super("VPP_V3C_MCFR", "Red Faction V3C Model");
    setExtensions("v3c", "v3m");

    setGames("Red Faction");
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
      if (readPlugin instanceof Plugin_VPP) {
        rating += 50;
      }

      if (FieldValidator.checkExtension(fm, extensions)) {
        rating += 25;
      }
      else {
        return 0;
      }

      String header = fm.readString(4);
      if (header.equals("MCFR") || header.equals("D3FR")) {
        rating += 50;
      }

      if (fm.readInt() == 262144) {
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
      MeshView[] meshView = new MeshView[100]; // guess
      int numMeshes = 0;

      float[] points = null;
      //float[] normals = null;
      float[] texCoords = null;
      int[] faces = null;

      minX = 20000f;
      maxX = -20000f;
      minY = 20000f;
      maxY = -20000f;
      minZ = 20000f;
      maxZ = -20000f;

      // 4 - Header (MCFR)
      // 4 - Version (262144)
      fm.skip(8);

      // 4 - Number of Sub-Meshes
      int numSubMeshes = fm.readInt();
      FieldValidator.checkRange(numSubMeshes, 1, 100); // guess

      // 12 - null
      // 4 - Number of Materials in all Sub-Meshes
      // 8 - null
      // 4 - Number of Col Spheres
      fm.skip(28);

      // 4 - Header (MBUS)
      String header = fm.readString(4);
      if (!header.equals("MBUS")) {
        return null;
      }

      // 4 - null
      fm.skip(4);

      for (int m = 0; m < numSubMeshes; m++) {
        System.out.println("SubMesh " + m + " of " + numSubMeshes);

        // 24 - Sub-Mesh Name (null terminated, filled with nulls)
        // 24 - Unknown Name (null terminated, filled with nulls)
        // 4 - Version (7)
        fm.skip(52);

        // 4 - Number of LODs
        int numLODs = fm.readInt();
        FieldValidator.checkRange(numLODs, 1, 100);//guess

        // for each LOD
        //   4 - LOD Distance
        fm.skip(numLODs * 4);

        // 4 - Offset X
        // 4 - Offset Y
        // 4 - Offset Z
        // 4 - Radius
        fm.skip(16);

        // 4 - Bounding Box 1 X
        // 4 - Bounding Box 1 Y
        // 4 - Bounding Box 1 Z

        // 4 - Bounding Box 2 X
        // 4 - Bounding Box 2 Y
        // 4 - Bounding Box 2 Z
        fm.skip(24);

        for (int lod = 0; lod < numLODs; lod++) {
          System.out.println("  LOD " + lod + " of " + numLODs);

          // 4 - Flags (32=HasTrianglePlanes, 1=HasMorphVerticesMap)
          int flags = fm.readInt();

          // 4 - Number of Vertices
          int numLodVerts = fm.readInt();
          FieldValidator.checkNumVertices(numLodVerts);

          // 2 - Number of Batches
          short numBatches = fm.readShort();
          FieldValidator.checkRange(numBatches, 1, 100);//guess

          // 4 - LOD Data Length
          int lodLength = fm.readInt();
          FieldValidator.checkLength(lodLength, arcSize);

          // X - LOD Data
          long lodOffset = fm.getOffset();
          byte[] lodData = fm.readBytes(lodLength);

          // 4 - Unknown
          fm.skip(4);

          short[] bVerts = new short[numBatches];
          short[] bTris = new short[numBatches];
          short[] bPosSize = new short[numBatches];
          short[] bIndSize = new short[numBatches];
          short[] bSameSize = new short[numBatches];
          short[] bBoneSize = new short[numBatches];
          short[] bTexSize = new short[numBatches];
          for (int b = 0; b < numBatches; b++) {
            // 2 - Number of Vertices
            bVerts[b] = fm.readShort();

            // 2 - Number of Triangles
            bTris[b] = fm.readShort();

            // 2 - Positions Size
            bPosSize[b] = fm.readShort();

            // 2 - Indices Size
            bIndSize[b] = fm.readShort();

            // 2 - Same Pos Vertex Offsets Size
            bSameSize[b] = fm.readShort();

            // 2 - Bone Links Size
            bBoneSize[b] = fm.readShort();

            // 2 - Tex Coords Size
            bTexSize[b] = fm.readShort();

            // 4 - Render Flags
            fm.skip(4);
          }

          // 4 - Number of Prop Points
          fm.skip(4);

          // 4 - Number of Textures
          int numTextures = fm.readInt();
          FieldValidator.checkRange(numTextures, 0, 100);//guess

          for (int t = 0; t < numTextures; t++) {
            // 1 - Texture ID
            fm.skip(1);

            // X - Texture Filename
            // 1 - null Filename Terminator
            fm.readNullString();
          }

          // now that we have the details of the batches, read the LOD data for this LOD
          FileManipulator lodFM = new FileManipulator(new ByteBuffer(lodData));

          // for each batch
          //   32 - Unknown
          //   4 - Texture ID
          //   20 - Unknown
          lodFM.skip(56 * numBatches);

          // X - Padding so *LOD Data* is a multiple of 16 bytes
          lodFM.skip(ArchivePlugin.calculatePadding(lodFM.getOffset(), 16));

          for (int b = 0; b < numBatches; b++) {
            System.out.println("    Batch " + b + " of " + numBatches);

            //
            //
            // VERTICES
            //
            //
            int numVertices = bPosSize[b] / 12;
            FieldValidator.checkNumFaces(numVertices);

            System.out.println("      Verts = " + numVertices);
            System.out.println("        Offset = " + (lodOffset + lodFM.getOffset()));

            int numVertices3 = numVertices * 3;

            points = new float[numVertices3];
            //normals = new float[numVertices3];

            int numPoints2 = numVertices * 2;
            texCoords = new float[numPoints2];

            // for each position (count = PositionsSize/12)
            for (int i = 0, j = 0, k = 0; i < numVertices; i++, j += 3, k += 2) {
              // 4 - Vertex X
              // 4 - Vertex Y
              // 4 - Vertex Z
              float xPoint = lodFM.readFloat();
              float yPoint = lodFM.readFloat();
              float zPoint = lodFM.readFloat();

              points[j] = xPoint;
              points[j + 1] = yPoint;
              points[j + 2] = zPoint;

              // Don't do the texture co-ords yet
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

            // X - Padding so *LOD Data* is a multiple of 16 bytes
            lodFM.skip(ArchivePlugin.calculatePadding(lodFM.getOffset(), 16));

            // for each position (count = PositionsSize/12)
            //   4 - Normal X
            //   4 - Normal Y
            //   4 - Normal Z
            lodFM.skip(bPosSize[b]);

            // X - Padding so *LOD Data* is a multiple of 16 bytes
            lodFM.skip(ArchivePlugin.calculatePadding(lodFM.getOffset(), 16));

            // for each tex coord (count = TexCoordSize/8)
            //   4 - Tex Coord U
            //   4 - Tex Coord V
            lodFM.skip(bTexSize[b]);

            // X - Padding so *LOD Data* is a multiple of 16 bytes
            lodFM.skip(ArchivePlugin.calculatePadding(lodFM.getOffset(), 16));

            //
            //
            // FACES
            //
            //
            int numFaces = bIndSize[b] / 8;
            FieldValidator.checkNumFaces(numFaces);

            System.out.println("      Faces = " + numFaces);
            System.out.println("        Offset = " + (lodOffset + lodFM.getOffset()));

            int numFaces3 = numFaces * 3;
            FieldValidator.checkNumFaces(numFaces3);

            int numFaces6 = numFaces3 * 2;

            faces = new int[numFaces6]; // need to store front and back faces

            // for each triangle (count = IndicesSize/8)
            for (int i = 0, j = 0; i < numFaces; i++, j += 6) {
              // 2 - Face Index 1
              // 2 - Face Index 2
              // 2 - Face Index 3
              int facePoint1 = (ShortConverter.unsign(lodFM.readShort()));
              int facePoint2 = (ShortConverter.unsign(lodFM.readShort()));
              int facePoint3 = (ShortConverter.unsign(lodFM.readShort()));

              // reverse face first (so the light shines properly, for this model specifically)
              faces[j] = facePoint3;
              faces[j + 1] = facePoint2;
              faces[j + 2] = facePoint1;

              // forward face second
              faces[j + 3] = facePoint1;
              faces[j + 4] = facePoint2;
              faces[j + 5] = facePoint3;

              // 2 - Flags
              lodFM.skip(2);
            }

            // X - Padding so *LOD Data* is a multiple of 16 bytes
            lodFM.skip(ArchivePlugin.calculatePadding(lodFM.getOffset(), 16));

            if ((flags & 32) == 32) {
              // for each plane (count = NumTriangles)
              //   4 - Plane X
              //   4 - Plane Y
              //   4 - Plane Z
              //   4 - Distance
              lodFM.skip(bTris[b] * 16);

              // X - Padding so *LOD Data* is a multiple of 16 bytes
              lodFM.skip(ArchivePlugin.calculatePadding(lodFM.getOffset(), 16));
            }

            // for each same_pos_vertex_offsets (count = same_pos_vertex_offsetsSize/2)
            //   2 - Unknown
            lodFM.skip(bSameSize[b]);

            // X - Padding so *LOD Data* is a multiple of 16 bytes
            lodFM.skip(ArchivePlugin.calculatePadding(lodFM.getOffset(), 16));

            // for each bone link (count = BoneLinkSize/8)
            //   1 - Bone Weight 1
            //   1 - Bone Weight 2
            //   1 - Bone Weight 3
            //   1 - Bone Weight 4
            //   1 - Bone 1
            //   1 - Bone 2
            //   1- Bone 3
            //   1 - Bone 4
            lodFM.skip(bBoneSize[b]);

            // X - Padding so *LOD Data* is a multiple of 16 bytes
            lodFM.skip(ArchivePlugin.calculatePadding(lodFM.getOffset(), 16));

            if ((flags & 1) == 1) {
              // for each Morph Vertices Map (count = NumVertices)
              //   2 - Unknown
              lodFM.skip(numLodVerts * 2);

              // X - Padding so *LOD Data* is a multiple of 16 bytes
              lodFM.skip(ArchivePlugin.calculatePadding(lodFM.getOffset(), 16));
            }

            // add the LOD to the model
            if (faces != null && points != null && texCoords != null) {
              System.out.println("      Added the batch to the mesh.");
              // we have a full mesh for a single object - add it to the model
              TriangleMesh triangleMesh = new TriangleMesh();

              triangleMesh.getTexCoords().addAll(texCoords);

              triangleMesh.getPoints().addAll(points);
              triangleMesh.getFaces().addAll(faces);
              //triangleMesh.getNormals().addAll(normals);

              MeshView view = new MeshView(triangleMesh);
              meshView[numMeshes] = view;
              numMeshes++;

              faces = null;
              points = null;
              //normals = null;
              texCoords = null;
            }

          }

          // for each prop point
          //   68 - Name (null terminated, filled with nulls)

          //   4 - Rotation X
          //   4 - Rotation Y
          //   4 - Rotation Z
          //   4 - Rotation W

          //   4 - Position X
          //   4 - Position Y
          //   4 - Position Z

          //   4 - Parent Bone Index
          //lodFM.skip(numPropPoints * 96); // DON'T WORRY ABOUT THIS, IT'S AT THE END, JUST CLOSE THE LOD DATA

          lodFM.close();

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

      if (numMeshes < meshView.length) {
        MeshView[] oldMeshView = meshView;
        meshView = new MeshView[numMeshes];
        System.arraycopy(oldMeshView, 0, meshView, 0, numMeshes);
      }

      PreviewPanel_3DModel preview = new PreviewPanel_3DModel(meshView, sizes, center);

      return preview;
    }
    catch (

    Throwable t) {
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