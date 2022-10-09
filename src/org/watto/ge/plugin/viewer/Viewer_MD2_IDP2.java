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
import org.watto.ge.plugin.PluginFinder;
import org.watto.ge.plugin.RatedPlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.io.FileManipulator;
import org.watto.io.buffer.ByteBuffer;
import org.watto.io.converter.ByteConverter;
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
public class Viewer_MD2_IDP2 extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_MD2_IDP2() {
    super("MD2_IDP2", "id Software MD2 Mesh Viewer");
    setExtensions("md2");

    setGames("HROT");
    setPlatforms("PC");
    setStandardFileFormat(true);
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

      if (FieldValidator.checkExtension(fm, extensions)) {
        rating += 25;
      }
      else {
        return 0;
      }

      String header = fm.readString(4);
      if (header.equals("IDP2")) {
        rating += 50;
      }

      // 4 - Version (8)
      if (fm.readInt() == 8) {
        rating += 5;
      }

      return rating;

    }
    catch (Throwable t) {
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
      MeshView[] meshView = new MeshView[1]; // we're using MeshView, as we're setting textures on the mesh

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

      String[] textureFiles = new String[0];

      // 4 - Header (IDP2)
      // 4 - Version (8)
      fm.skip(8);

      // 4 - Texture Image Width
      int width = fm.readInt();
      FieldValidator.checkWidth(width + 1);//+1 to allow 0 images

      // 4 - Texture Image Height
      int height = fm.readInt();
      FieldValidator.checkWidth(height + 1);//+1 to allow 0 images

      // 4 - Length of each Frame Entry
      int frameLength = fm.readInt();
      FieldValidator.checkLength(frameLength, arcSize);

      // 4 - Number of Skins
      int numSkins = fm.readInt();
      FieldValidator.checkNumFiles(numSkins + 1);//+1 to allow 0 images

      // 4 - Number of Vertices per Frame
      int numVertices = fm.readInt();
      FieldValidator.checkNumVertices(numVertices);

      // 4 - Number of Texture Co-ordinates
      int numTexPoints = fm.readInt();
      FieldValidator.checkNumVertices(numTexPoints + 1);//+1 to allow 0 images

      // 4 - Number of Triangles
      int numTriangles = fm.readInt();
      FieldValidator.checkNumFaces(numTriangles);

      // 4 - Number of OpenGL Commands
      // 4 - Number of Frames
      fm.skip(8);

      // 4 - Skin Data Offset
      int skinOffset = fm.readInt();
      FieldValidator.checkOffset(skinOffset, arcSize);

      // 4 - Texture Co-ordinate Data Offset
      int texPointOffset = fm.readInt();
      FieldValidator.checkOffset(texPointOffset, arcSize);

      // 4 - Triangle Data Offset
      int triangleOffset = fm.readInt();
      FieldValidator.checkOffset(triangleOffset, arcSize);

      // 4 - Frame Data Offset
      int frameOffset = fm.readInt();
      FieldValidator.checkOffset(frameOffset, arcSize);

      // 4 - OpenGL Command Data Offset
      // 4 - File Length
      fm.skip(8);

      //
      // TEXTURE (SKIN)
      //
      Image image = null;
      if (numSkins > 0) {
        fm.relativeSeek(skinOffset);
        // 64 - Texture Image Filename (null terminated, filled with nulls)
        String skinFilename = fm.readNullString(64);
        image = loadTextureImage(skinFilename, width, height);
      }

      //
      // TEX CO-ORDS
      //
      int numTexPoints2 = numTexPoints * 2;
      float[] texPoints = new float[numTexPoints2];
      if (image != null) {

        fm.relativeSeek(texPointOffset);

        for (int i = 0; i < numTexPoints2; i += 2) {
          // 2 - U Point [/skinWidth]
          float uPoint = ((float) fm.readShort()) / width;

          // 2 - V Point [/skinHeight]
          float vPoint = ((float) fm.readShort()) / width;

          texPoints[i] = uPoint;
          texPoints[i + 1] = vPoint;
        }
      }

      //
      // FACES
      //

      fm.relativeSeek(triangleOffset);

      int numFaces = numTriangles;
      FieldValidator.checkNumFaces(numFaces);

      int numFaces3 = numFaces * 3;
      int numFaces6 = numFaces3 * 2;

      faces = new int[numFaces6]; // need to store front and back faces

      // for the tex-coords
      int numPoints2 = numVertices * 2;
      texCoords = new float[numPoints2];

      for (int f = 0, j = 0; f < numFaces; f++, j += 6) {
        // 2 - Point Index 1
        // 2 - Point Index 2
        // 2 - Point Index 3
        int facePoint1 = (ShortConverter.unsign(fm.readShort()));
        int facePoint2 = (ShortConverter.unsign(fm.readShort()));
        int facePoint3 = (ShortConverter.unsign(fm.readShort()));

        // forward face first (so the light shines properly, for this model specifically)
        faces[j] = facePoint1;
        faces[j + 1] = facePoint2;
        faces[j + 2] = facePoint3;

        // reverse face second
        faces[j + 3] = facePoint3;
        faces[j + 4] = facePoint2;
        faces[j + 5] = facePoint1;

        // 2 - Texture Co-ordinate Index 1
        // 2 - Texture Co-ordinate Index 2
        // 2 - Texture Co-ordinate Index 3
        if (image == null) {
          // no image loaded
          fm.skip(6);
        }
        else {
          // has an image, load proper points
          int faceTexPoint1 = (ShortConverter.unsign(fm.readShort()));
          int faceTexPoint2 = (ShortConverter.unsign(fm.readShort()));
          int faceTexPoint3 = (ShortConverter.unsign(fm.readShort()));

          int arrayPos = facePoint1 * 2;
          texCoords[arrayPos] = texPoints[faceTexPoint1];
          texCoords[arrayPos + 1] = texPoints[faceTexPoint1 + 1];

          arrayPos = facePoint2 * 2;
          texCoords[arrayPos] = texPoints[faceTexPoint2];
          texCoords[arrayPos + 1] = texPoints[faceTexPoint2 + 1];

          arrayPos = facePoint3 * 2;
          texCoords[arrayPos] = texPoints[faceTexPoint3];
          texCoords[arrayPos + 1] = texPoints[faceTexPoint3 + 1];
        }

      }

      //
      // VERTICES
      //

      fm.relativeSeek(frameOffset);

      // 4 - Scale Factor X (Float)
      // 4 - Scale Factor Y (Float)
      // 4 - Scale Factor Z (Float)
      float scaleX = fm.readFloat();
      float scaleY = fm.readFloat();
      float scaleZ = fm.readFloat();

      // 4 - Translation Vector X (Float)
      // 4 - Translation Vector Y (Float)
      // 4 - Translation Vector Z (Float)
      float translateX = fm.readFloat();
      float translateY = fm.readFloat();
      float translateZ = fm.readFloat();

      // 16 - Frame Name (null terminated, filled with nulls)
      fm.skip(16);

      int numVertices3 = numVertices * 3;
      points = new float[numVertices3];

      for (int v = 0, j = 0, k = 0; v < numVertices; v++, j += 3, k += 2) {

        // 1 - X Position Byte [(*scaleX) + translateX]
        float xPoint = (((float) ByteConverter.unsign(fm.readByte())) * scaleX) + translateX;

        // 1 - Y Position Byte [(*scaleY) + translateY]
        float yPoint = (((float) ByteConverter.unsign(fm.readByte())) * scaleY) + translateY;

        // 1 - Z Position Byte [(*scaleZ) + translateZ]
        float zPoint = (((float) ByteConverter.unsign(fm.readByte())) * scaleZ) + translateZ;

        // 1 - Normal Vector Index
        fm.skip(1);

        points[j] = xPoint;
        points[j + 1] = yPoint;
        points[j + 2] = zPoint;

        if (image == null) {
          // Skip the texture mapping for now (ie it wasn't set earlier)
          float xTexture = 0;
          float yTexture = 0;

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
        meshView[0] = view;

        // set the texture
        if (image != null) {
          Material material = new PhongMaterial(Color.WHITE, image, (Image) null, (Image) null, (Image) null);
          view.setMaterial(material);
        }

        rendered = true;
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
  public Image loadTextureImage(String textureFilename, int width, int height) {
    try {

      textureFilename = textureFilename.toLowerCase();

      // see if we can find an exact match
      Resource[] resources = Archive.getResources();
      int numResources = resources.length;
      for (int i = 0; i < numResources; i++) {
        Resource currentResource = resources[i];
        if (currentResource.getName().toLowerCase().equals(textureFilename)) {
          // found the right resource
          return loadTextureImage(resources[i]);
        }
      }

      // if we're here, we didn't find an exact match, so lets see if we can find any file with a matching name
      // and ANY extension, and that loads into an image with the right dimensions 
      int dotPos = textureFilename.lastIndexOf('.');
      if (dotPos > 0) {
        textureFilename = textureFilename.substring(0, dotPos); // note: lowercase
      }

      // now find the resource
      for (int i = 0; i < numResources; i++) {
        Resource currentResource = resources[i];
        String name = currentResource.getName().toLowerCase();
        if (name.startsWith(textureFilename)) {
          // found a potential match, see if it loads
          Image image = loadTextureImage(resources[i]);
          if (image != null && image.getWidth() == width && image.getHeight() == height) {
            return image;
          }
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

        ViewerPlugin viewerPlugin = ((ViewerPlugin) plugins[i].getPlugin());
        if (viewerPlugin instanceof Viewer_MD2_IDP2) {
          // don't want this plugin to pick it up - loop forever
          continue;
        }
        imagePreviewPanel = viewerPlugin.read(fm);

        if (imagePreviewPanel != null && imagePreviewPanel instanceof PreviewPanel_Image) {
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