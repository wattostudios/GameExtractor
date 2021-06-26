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

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Arrays;
import org.watto.ErrorLogger;
import org.watto.component.PreviewPanel;
import org.watto.component.PreviewPanel_3DModel;
import org.watto.component.PreviewPanel_Image;
import org.watto.datatype.Archive;
import org.watto.datatype.ImageResource;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.helper.ImageFormatReader;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.PluginFinder;
import org.watto.ge.plugin.RatedPlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.archive.Plugin_GTC;
import org.watto.io.FileManipulator;
import org.watto.io.buffer.ByteBuffer;
import org.watto.io.converter.ShortConverter;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Point3D;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.paint.Material;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_GTC_MD2_MDL3 extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_GTC_MD2_MDL3() {
    super("GTC_MD2_MDL3", "Drome Racers MD2 Mesh Viewer");
    setExtensions("md2");

    setGames("Drome Racers");
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
      if (plugin instanceof Plugin_GTC) {
        rating += 50;
      }

      if (FieldValidator.checkExtension(fm, extensions)) {
        rating += 25;
      }
      else {
        return 0;
      }

      String header = fm.readString(4);
      if (header.equals("MDL3")) {
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

      // Set up the mesh
      //TriangleMesh triangleMesh = new TriangleMesh();
      MeshView[] meshView = null; // we're using MeshView, as we're setting textures on the mesh

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

      boolean rendered = false;

      String[] textureFiles = new String[0];
      String texture = null;

      while (fm.getOffset() < arcSize) {
        // 4 - Block Header
        String header = fm.readString(4);

        // 4 - Block Size
        int blockLength = fm.readInt();
        FieldValidator.checkLength(blockLength);

        // X - Block Data
        if (header.equals("MDL3")) {
          // Need to get the Texture info from here

          // 4 - InertiaTensor X (float)
          // 4 - InertiaTensor Y (float)
          // 4 - InertiaTensor Z (float)
          // 4 - Bounding Radius (float)
          // 4 - AllowDistanceFade
          fm.skip(20);

          // 4 - IncludesBoundingBox
          int boundingBox = fm.readInt();
          if (boundingBox != 0) {
            // 4 - AABBMin X (float)
            // 4 - AABBMin Y (float)
            // 4 - AABBMin Z (float)
            // 4 - AABBMax X (float)
            // 4 - AABBMax Y (float)
            // 4 - AABBMax Z (float)
            // 4 - AABBCenter X (float)
            // 4 - AABBCenter Y (float)
            // 4 - AABBCenter Z (float)
            // 4 - AABBYaw (float)
            fm.skip(40);
          }

          // 4 - UseUniqueMaterials
          // 4 - UseUniqueTextures
          // 4 - UseGenericGeometry
          // 4 - VertexBufferAccessFlags
          fm.skip(16);

          // 12*4 - Unknown
          fm.skip(12 * 4);

          // 4 - Texture Reference Count
          int numTextures = fm.readInt();
          FieldValidator.checkNumFiles(numTextures + 1); // +1 to allow 0 textures

          textureFiles = new String[numTextures];
          for (int t = 0; t < numTextures; t++) {
            // 256 - Filename (null terminated, filled with nulls and junk)
            String textureFilename = fm.readNullString(256);
            FieldValidator.checkFilename(textureFilename);

            textureFiles[t] = textureFilename;

            // 4 - UsageType
            // 4 - BaseIndex
            fm.skip(8);
          }

          // 4 - Material Count
          int numMaterials = fm.readInt();
          FieldValidator.checkNumFiles(numMaterials + 1); // +1 to allow 0 materials

          for (int m = 0; m < numMaterials; m++) {

            // 4 - Ambient X (float)
            // 4 - Ambient Y (float)
            // 4 - Ambient Z (float)
            // 4 - Ambient W (float)
            // 4 - Diffuse X (float)
            // 4 - Diffuse Y (float)
            // 4 - Diffuse Z (float)
            // 4 - Diffuse W (float)
            // 4 - Specular X (float)
            // 4 - Specular Y (float)
            // 4 - Specular Z (float)
            // 4 - Specular W (float)
            // 4 - Emissive X (float)
            // 4 - Emissive Y (float)
            // 4 - Emissive Z (float)
            // 4 - Emissive W (float)
            // 4 - Shininess (float)
            // 4 - Transparency (float)
            // 4 - Transparency Type
            // 4 - Property Bits
            // 12 - Animation Name (null terminated, filled with nulls)
            fm.skip(92);
          }

          //fm.skip(blockLength);
        }
        else if (header.equals("GEO2")) {
          // 4 - Number of LODs
          int numLODs = fm.readInt();
          FieldValidator.checkNumFiles(numLODs);

          // Force to only render the most complex LOD
          numLODs = 1;

          for (int i = 0; i < numLODs; i++) {
            // 4 - Type
            // 4 - Max Edge Length (float)
            fm.skip(8);

            // 4 - Group Count
            int groupCount = fm.readInt();

            // 4 - Positions Pointer
            int posPointer = fm.readInt();

            // 4 - Render Groups Pointer
            fm.skip(4);

            if (posPointer > 0) {
              // 4 - X Point (float)
              // 4 - Y Point (float)
              // 4 - Z Point (float)
              fm.skip(groupCount * 12);
            }

            // prepare the meshViews for storing the textures and meshes
            meshView = new MeshView[groupCount];

            int vertexStartIndex = 0; // so that each group references the right points in the model
            for (int g = 0; g < groupCount; g++) {
              // 4 - ID
              // 2 - Primitive Count
              // 2 - Vertex Count
              // 2 - Materials
              // 2 - Effects
              // 4 - pBumpData
              // 4 - pVertexBuffer
              // 4 - pIndexBuffer

              // 2 - EffectsMask
              // 2 - RendererReference
              // 2 - EffectCount
              // 1 - Custom
              // 1 - CoordsCount
              fm.skip(32);

              // for each BlendCount (4)
              // 4 - Effect
              fm.skip(4);

              // 2 - TextureIndex
              int textureIndex = fm.readShort();

              texture = null;
              if (textureIndex < textureFiles.length) {
                texture = textureFiles[textureIndex];
              }

              // 1 - CoordIndex
              // 1 - TilingInfo
              fm.skip(2);
              fm.skip(3 * 8); // we're only reading the first one, skip the other 3

              // 4 - PositionOffset
              int positionOffset = fm.readInt() / 4; // /4 because we want the float index number, to read the array below

              // 4 - NormalOffset
              int normalOffset = fm.readInt() / 4; // /4 because we want the float index number, to read the array below

              // 4 - ColorOffset
              fm.skip(4);

              // 4 - UVOffset
              int uvOffset = fm.readInt() / 4; // /4 because we want the float index number, to read the array below

              // 4 - VertexSize
              int vertexSize = fm.readInt();

              // 4 - UVCount
              // 2 - VertexComponents
              fm.skip(6);

              // 2 - VertexCount2
              int numVertices = ShortConverter.unsign(fm.readShort());
              FieldValidator.checkNumVertices(numVertices);

              // 2 - ManagedBuffer
              // 2 - CurrentVertex
              // 4 - pSourceBuffer
              // 4 - pVertexOffset
              fm.skip(12);

              //
              // VERTICES
              //

              int numVertices3 = numVertices * 3;
              points = new float[numVertices3];
              normals = new float[numVertices3];

              int numPoints2 = numVertices * 2;
              texCoords = new float[numPoints2];

              int numFloats = vertexSize / 4;
              for (int v = 0, j = 0, k = 0; v < numVertices; v++, j += 3, k += 2) {

                // read in all the floats used below, then work out what each value is
                float[] floats = new float[numFloats];
                for (int f = 0; f < numFloats; f++) {
                  floats[f] = fm.readFloat();
                }

                // 4 - Vertex X
                // 4 - Vertex Y
                // 4 - Vertex Z
                float xPoint = floats[positionOffset];
                float yPoint = floats[positionOffset + 1];
                float zPoint = floats[positionOffset + 2];

                points[j] = xPoint;
                points[j + 1] = yPoint;
                points[j + 2] = zPoint;

                // 4 - Normal X
                // 4 - Normal Y
                // 4 - Normal Z
                float xNormal = floats[normalOffset];
                float yNormal = floats[normalOffset + 1];
                float zNormal = floats[normalOffset + 2];

                normals[j] = xNormal;
                normals[j + 1] = yNormal;
                normals[j + 2] = zNormal;

                // 4 - U Float
                // 4 - V Float
                if (texture == null) {
                  // Skip the texture mapping for now
                  float xTexture = 0;
                  float yTexture = 0;

                  texCoords[k] = xTexture;
                  texCoords[k + 1] = yTexture;
                }
                else {
                  float xTexture = floats[uvOffset];
                  float yTexture = floats[uvOffset + 1];

                  texCoords[k] = xTexture;
                  texCoords[k + 1] = yTexture;
                }

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

              // 4 - PrimitiveBufferCount
              // 4 - PrimitiveType
              fm.skip(8);

              // 4 - IndexCount
              int numFaces = fm.readInt();
              FieldValidator.checkNumFaces(numFaces);

              //
              // FACES
              //

              int faceDataSize = 2;

              int numFaces3 = numFaces;
              FieldValidator.checkNumFaces(numFaces3);

              numFaces = numFaces3 / 3;
              int numFaces6 = numFaces3 * 2;

              //if (texture == null) {
              //// no texture - load front and back faces
              faces = new int[numFaces6]; // need to store front and back faces

              for (int f = 0, j = 0; f < numFaces; f++, j += 6) {
                // 2 - Point Index 1
                // 2 - Point Index 2
                // 2 - Point Index 3
                int facePoint1 = (ShortConverter.unsign(fm.readShort())) + vertexStartIndex; // + vertexStartIndex so that we can render multiple groups in the same model
                int facePoint2 = (ShortConverter.unsign(fm.readShort())) + vertexStartIndex;
                int facePoint3 = (ShortConverter.unsign(fm.readShort())) + vertexStartIndex;

                // reverse face first (so the light shines properly, for this model specifically)
                faces[j] = facePoint3;
                faces[j + 1] = facePoint2;
                faces[j + 2] = facePoint1;

                // forward face second
                faces[j + 3] = facePoint1;
                faces[j + 4] = facePoint2;
                faces[j + 5] = facePoint3;
              }
              /*
              }
              else {
                // has a texture - only load the front faces
              
                faces = new int[numFaces3];
              
                for (int f = 0, j = 0; f < numFaces; f++, j += 3) {
                  // 2 - Point Index 1
                  // 2 - Point Index 2
                  // 2 - Point Index 3
                  int facePoint1 = (ShortConverter.unsign(fm.readShort())) + vertexStartIndex; // + vertexStartIndex so that we can render multiple groups in the same model
                  int facePoint2 = (ShortConverter.unsign(fm.readShort())) + vertexStartIndex;
                  int facePoint3 = (ShortConverter.unsign(fm.readShort())) + vertexStartIndex;
              
                  // forward face 
                  faces[j] = facePoint1;
                  faces[j + 1] = facePoint2;
                  faces[j + 2] = facePoint3;
                }
              }
              */

              //
              // RENDER THE MESH
              //

              // we have a full mesh for a single object (including all parts adjusted) - add it to the model
              if (faces != null && points != null && normals != null && texCoords != null) {
                // Create the Mesh
                TriangleMesh triangleMesh = new TriangleMesh();
                triangleMesh.getTexCoords().addAll(texCoords);

                triangleMesh.getPoints().addAll(points);
                triangleMesh.getFaces().addAll(faces);
                triangleMesh.getNormals().addAll(normals);

                faces = null;
                points = null;
                normals = null;
                texCoords = null;

                // Create the MeshView
                MeshView view = new MeshView(triangleMesh);
                meshView[g] = view;

                if (texture != null) {
                  // set the texture
                  Image image = loadTextureImage(texture);
                  if (image != null) {
                    Material material = new PhongMaterial(Color.WHITE, image, (Image) null, (Image) null, (Image) null);
                    view.setMaterial(material);
                  }
                }

                rendered = true;
              }

              // Prepare for the next Group, so that the subsequent groups point to the right vertices
              // NO LONGER NEEDED, AS WE'RE USING SEPARATE MeshView FOR EACH GROUP
              //vertexStartIndex += numVertices;

            }
          }
        }
        else {
          // skip all other blocks
          fm.skip(blockLength);
        }
      }

      if (!rendered) {
        // didn't find any meshes
        return null;
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
  public Image loadTextureImage(String textureFilename) {
    try {

      textureFilename = textureFilename.toLowerCase();

      // the filename here is .TGA, the filename in the archive is .PC TEXTURE, so we need to change it
      int dotPos = textureFilename.lastIndexOf('.');
      if (dotPos > 0) {
        textureFilename = textureFilename.substring(0, dotPos) + ".pc texture"; // note: lowercase
      }

      // now find the resource
      Resource[] resources = Archive.getResources();
      int numResources = resources.length;
      for (int i = 0; i < numResources; i++) {
        Resource currentResource = resources[i];
        if (currentResource.getName().toLowerCase().equals(textureFilename)) {
          // found the right resource
          return loadTextureImage(resources[i]);
        }
      }

      // not found
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
  public Image loadTextureImage(Resource imageResource) {
    try {

      // 1. Open the file
      ByteBuffer buffer = new ByteBuffer((int) imageResource.getLength());
      FileManipulator fm = new FileManipulator(buffer);
      imageResource.extract(fm);
      fm.setFakeFile(new File(imageResource.getName())); // set a fake file here, so that the ViewerPlugins can check the file extension

      // 2. Get all the ViewerPlugins that can read this file type
      RatedPlugin[] plugins = PluginFinder.findPlugins(fm, ViewerPlugin.class); // NOTE: This closes the fm pointer!!!
      if (plugins == null || plugins.length == 0) {
        // no viewer plugins found that will accept this file
        return null;
      }

      Arrays.sort(plugins);

      // re-open the file - it was closed at the end of findPlugins();
      fm = new FileManipulator(buffer);

      // 3. Try each plugin until we find one that can render the file as an ImageResource
      PreviewPanel imagePreviewPanel = null;
      for (int i = 0; i < plugins.length; i++) {
        fm.seek(0); // go back to the start of the file
        imagePreviewPanel = ((ViewerPlugin) plugins[i].getPlugin()).read(fm);

        if (imagePreviewPanel != null) {
          // 4. We have found a plugin that was able to render the image
          break;
        }
      }

      fm.close();

      if (imagePreviewPanel == null || !(imagePreviewPanel instanceof PreviewPanel_Image)) {
        // no plugins were able to open this file successfully
        return null;
      }

      //
      //
      // If we're here, we have a rendered image
      //
      //

      //java.awt.Image image = ((PreviewPanel_Image) imagePreviewPanel).getImage();
      ImageResource imageResourceObj = ((PreviewPanel_Image) imagePreviewPanel).getImageResource();
      imageResourceObj = ImageFormatReader.flipVertically(imageResourceObj); // the previewer flips the image for this format (so the preview displays properly), we need to flip it back
      java.awt.Image image = imageResourceObj.getImage();
      BufferedImage bufImage = null;
      if (image instanceof BufferedImage) {
        bufImage = (BufferedImage) image;
      }
      else {
        bufImage = new BufferedImage(image.getWidth(null), image.getHeight(null), BufferedImage.TYPE_INT_ARGB);

        // Draw the image on to the buffered image
        Graphics2D bGr = bufImage.createGraphics();
        bGr.drawImage(image, 0, 0, null);
        bGr.dispose();
      }

      return SwingFXUtils.toFXImage(bufImage, null);
    }
    catch (Throwable t) {
      logError(t);
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