/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2021 wattostudios
 *
 * License Information:
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License
 * published by the Free Software Foundation; either version 2 of the License, or (at your option) any later versions. This
 * program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranties
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License at http://www.gnu.org for more
 * details. For further information on this application, refer to the authors' website.
 */

package org.watto.ge.plugin.archive;

import java.io.File;
import java.util.Arrays;
import org.watto.ErrorLogger;
import org.watto.Settings;
import org.watto.component.WSPluginException;
import org.watto.datatype.FileType;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.helper.UE4Helper;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.archive.datatype.UnrealImportEntry;
import org.watto.ge.plugin.exporter.BlockExporterWrapper;
import org.watto.ge.plugin.exporter.Exporter_AES_ZLib;
import org.watto.ge.plugin.exporter.Exporter_Default;
import org.watto.ge.plugin.exporter.Exporter_Encryption_AES;
import org.watto.ge.plugin.exporter.Exporter_QuickBMS_Decompression;
import org.watto.ge.plugin.exporter.Exporter_ZLib;
import org.watto.ge.plugin.resource.Resource_PAK_38;
import org.watto.io.FileManipulator;
import org.watto.io.buffer.ExporterByteBuffer;
import org.watto.io.converter.ByteConverter;
import org.watto.io.converter.IntConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_PAK_65 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_PAK_65() {

    super("PAK_65", "Unreal Engine 4 PAK Archive [PAK_65]");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Scavengers",
        "Splitgate",
        "Super Squad",
        "System Shock (2021)",
        "Train Sim World 2");
    setExtensions("pak"); // MUST BE LOWER CASE
    setPlatforms("PC");

    setFileTypes(getFileTypes()); // They use many of the same file types

    //setFileTypes(new FileType("txt", "Text Document", FileType.TYPE_DOCUMENT),
    //             new FileType("bmp", "Bitmap Image", FileType.TYPE_IMAGE)
    //             );

  }

  /**
   **********************************************************************************************
   COPIED FROM Unity3DHelper, with a few new ones added at the end!
   **********************************************************************************************
   **/
  public static FileType[] getFileTypes() {
    FileType[] types = new FileType[] { new FileType("object", "Object", FileType.TYPE_OTHER),
        new FileType("gameobject", "Game Object", FileType.TYPE_OTHER),
        new FileType("component", "Component", FileType.TYPE_OTHER),
        new FileType("levelgamemanager", "Level Game Manager", FileType.TYPE_OTHER),
        new FileType("transform", "Transform", FileType.TYPE_OTHER),
        new FileType("timemanager", "Time Manager", FileType.TYPE_OTHER),
        new FileType("globalgamemanager", "Global Game Manager", FileType.TYPE_OTHER),
        new FileType("gamemanager", "Game Manager", FileType.TYPE_OTHER),
        new FileType("behaviour", "Behaviour", FileType.TYPE_OTHER),
        new FileType("gamemanager", "Game Manager", FileType.TYPE_OTHER),
        new FileType("audiomanager", "Audio Manager", FileType.TYPE_OTHER),
        new FileType("particleanimator", "Particle Animator", FileType.TYPE_OTHER),
        new FileType("inputmanager", "Input Manager", FileType.TYPE_OTHER),
        new FileType("ellipsoidparticleemitter", "Ellipsoid Particle Emitter", FileType.TYPE_OTHER),
        new FileType("pipeline", "Pipeline", FileType.TYPE_OTHER),
        new FileType("editorextension", "Editor Extension", FileType.TYPE_OTHER),
        new FileType("physics2dsettings", "Physics 2D Settings", FileType.TYPE_OTHER),
        new FileType("camera", "Camera", FileType.TYPE_OTHER),
        new FileType("material", "Material", FileType.TYPE_OTHER),
        new FileType("meshrenderer", "Mesh Renderer", FileType.TYPE_OTHER),
        new FileType("renderer", "Renderer", FileType.TYPE_OTHER),
        new FileType("particlerenderer", "Particle Renderer", FileType.TYPE_OTHER),
        new FileType("texture", "Texture", FileType.TYPE_OTHER),
        new FileType("texture2d", "Texture 2D", FileType.TYPE_IMAGE),
        new FileType("scenesettings", "Scene Settings", FileType.TYPE_OTHER),
        new FileType("graphicssettings", "Graphics Settings", FileType.TYPE_OTHER),
        new FileType("pipelinemanager", "Pipeline Manager", FileType.TYPE_OTHER),
        new FileType("meshfilter", "Mesh  Filter", FileType.TYPE_OTHER),
        new FileType("gamemanager", "Game Manager", FileType.TYPE_OTHER),
        new FileType("occlusionportal", "Occlusion Portal", FileType.TYPE_OTHER),
        new FileType("mesh", "Mesh", FileType.TYPE_OTHER),
        new FileType("skybox", "Skybox", FileType.TYPE_OTHER),
        new FileType("gamemanager", "Game Manager", FileType.TYPE_OTHER),
        new FileType("qualitysettings", "Quality Settings", FileType.TYPE_OTHER),
        new FileType("shader", "Shader", FileType.TYPE_OTHER),
        new FileType("textasset", "Text Asset", FileType.TYPE_DOCUMENT),
        new FileType("rigidbody2d", "Rigidbody 2D", FileType.TYPE_OTHER),
        new FileType("physics2dmanager", "Physics 2D Manager", FileType.TYPE_OTHER),
        new FileType("notificationmanager", "Notification Manager", FileType.TYPE_OTHER),
        new FileType("collider2d", "Collider 2D", FileType.TYPE_OTHER),
        new FileType("rigidbody", "Rigidbody", FileType.TYPE_OTHER),
        new FileType("physicsmanager", "Physics Manager", FileType.TYPE_OTHER),
        new FileType("collider", "Collider", FileType.TYPE_OTHER),
        new FileType("joint", "Joint", FileType.TYPE_OTHER),
        new FileType("circlecollider2d", "Circle Collider 2D", FileType.TYPE_OTHER),
        new FileType("hingejoint", "Hinge Joint", FileType.TYPE_OTHER),
        new FileType("polygoncollider2d", "Polygon Collider 2D", FileType.TYPE_OTHER),
        new FileType("boxcollider2d", "Box Collider 2D", FileType.TYPE_OTHER),
        new FileType("physicsmaterial2d", "Physics Material 2D", FileType.TYPE_OTHER),
        new FileType("gamemanager", "Game Manager", FileType.TYPE_OTHER),
        new FileType("meshcollider", "Mesh Collider", FileType.TYPE_OTHER),
        new FileType("boxcollider", "Box Collider", FileType.TYPE_OTHER),
        new FileType("spritecollider2d", "Sprite Collider 2D", FileType.TYPE_OTHER),
        new FileType("edgecollider2d", "Edge Collider 2D", FileType.TYPE_OTHER),
        new FileType("capsulecollider2d", "Capsule Collider 2D", FileType.TYPE_OTHER),
        new FileType("animationmanager", "Animation Manager", FileType.TYPE_OTHER),
        new FileType("computeshader", "Compute Shader", FileType.TYPE_OTHER),
        new FileType("animationclip", "Animation Clip", FileType.TYPE_OTHER),
        new FileType("constantforce", "Constant Force", FileType.TYPE_OTHER),
        new FileType("worldparticlecollider", "World Particle Collider", FileType.TYPE_OTHER),
        new FileType("tagmanager", "Tag Manager", FileType.TYPE_OTHER),
        new FileType("audiolistener", "Audio Listener", FileType.TYPE_OTHER),
        new FileType("audiosource", "Audio Source", FileType.TYPE_OTHER),
        new FileType("audioclip", "Audio Clip", FileType.TYPE_AUDIO),
        new FileType("rendertexture", "Render Texture", FileType.TYPE_OTHER),
        new FileType("customrendertexture", "Custom Render Texture", FileType.TYPE_OTHER),
        new FileType("meshparticleemitter", "Mesh Particle Emitter", FileType.TYPE_OTHER),
        new FileType("particleemitter", "Particle Emitter", FileType.TYPE_OTHER),
        new FileType("cubemap", "Cubemap", FileType.TYPE_OTHER),
        new FileType("avatar", "Avatar", FileType.TYPE_OTHER),
        new FileType("animatorcontroller", "Animator Controller", FileType.TYPE_OTHER),
        new FileType("guilayer", "GUI Layer", FileType.TYPE_OTHER),
        new FileType("runtimeanimatorcontroller", "Runtime Animator Controller", FileType.TYPE_OTHER),
        new FileType("scriptmapper", "Script Mapper", FileType.TYPE_OTHER),
        new FileType("animator", "Animator", FileType.TYPE_OTHER),
        new FileType("trailrenderer", "Trail Renderer", FileType.TYPE_OTHER),
        new FileType("delayedcallmanager", "Delayed Call Manager", FileType.TYPE_OTHER),
        new FileType("textmesh", "Text Mesh", FileType.TYPE_OTHER),
        new FileType("rendersettings", "Render Settings", FileType.TYPE_OTHER),
        new FileType("light", "Light", FileType.TYPE_OTHER),
        new FileType("cgprogram", "CG Program", FileType.TYPE_OTHER),
        new FileType("baseanimationtrack", "Base Animation Track", FileType.TYPE_OTHER),
        new FileType("animation", "Animation", FileType.TYPE_OTHER),
        new FileType("monobehaviour", "Mono Behaviour", FileType.TYPE_OTHER),
        new FileType("monoscript", "Mono Script", FileType.TYPE_OTHER),
        new FileType("monomanager", "Mono Manager", FileType.TYPE_OTHER),
        new FileType("texture3d", "Texture 3D", FileType.TYPE_OTHER),
        new FileType("newanimationtrack", "New Animation Track", FileType.TYPE_OTHER),
        new FileType("projector", "Projector", FileType.TYPE_OTHER),
        new FileType("linerenderer", "Line Renderer", FileType.TYPE_OTHER),
        new FileType("flare", "Flare", FileType.TYPE_OTHER),
        new FileType("halo", "Halo", FileType.TYPE_OTHER),
        new FileType("lensflare", "Lens Flare", FileType.TYPE_OTHER),
        new FileType("flarelayer", "Flare Layer", FileType.TYPE_OTHER),
        new FileType("halolayer", "Halo Layer", FileType.TYPE_OTHER),
        new FileType("navmeshareas", "Nav Mesh Areas", FileType.TYPE_OTHER),
        new FileType("halomanager", "Halo Manager", FileType.TYPE_OTHER),
        new FileType("font", "Font", FileType.TYPE_OTHER),
        new FileType("playersettings", "Player Settings", FileType.TYPE_OTHER),
        new FileType("namedobject", "Named Object", FileType.TYPE_OTHER),
        new FileType("guitexture", "GUI Texture", FileType.TYPE_OTHER),
        new FileType("guitext", "GUI Text", FileType.TYPE_OTHER),
        new FileType("guielement", "GUI Element", FileType.TYPE_OTHER),
        new FileType("physicmaterial", "Physic Material", FileType.TYPE_OTHER),
        new FileType("spherecollider", "Sphere Collider", FileType.TYPE_OTHER),
        new FileType("capsulecollider", "Capsule Collider", FileType.TYPE_OTHER),
        new FileType("skinnedmeshrenderer", "Skinned Mesh Renderer", FileType.TYPE_OTHER),
        new FileType("fixedjoint", "Fixed Joint", FileType.TYPE_OTHER),
        new FileType("raycastcollider", "Raycast Collider", FileType.TYPE_OTHER),
        new FileType("buildsettings", "Build Settings", FileType.TYPE_OTHER),
        new FileType("assetbundle", "Asset Bundle", FileType.TYPE_OTHER),
        new FileType("charactercontroller", "Character Controller", FileType.TYPE_OTHER),
        new FileType("characterjoint", "Character Joint", FileType.TYPE_OTHER),
        new FileType("springjoint", "Spring Joint", FileType.TYPE_OTHER),
        new FileType("wheelcollider", "Wheel Collider", FileType.TYPE_OTHER),
        new FileType("resourcemanager", "Resource Manager", FileType.TYPE_OTHER),
        new FileType("networkview", "Network View", FileType.TYPE_OTHER),
        new FileType("networkmanager", "Network Manager", FileType.TYPE_OTHER),
        new FileType("preloaddata", "Preload Data", FileType.TYPE_OTHER),
        new FileType("movietexture", "Movie Texture", FileType.TYPE_OTHER),
        new FileType("configurablejoint", "Configurable Joint", FileType.TYPE_OTHER),
        new FileType("terraincollider", "Terrain Collider", FileType.TYPE_OTHER),
        new FileType("masterserverinterface", "Master Server Interface", FileType.TYPE_OTHER),
        new FileType("terraindata", "Terrain Data", FileType.TYPE_OTHER),
        new FileType("lightmapsettings", "Lightmap Settings", FileType.TYPE_OTHER),
        new FileType("webcamtexture", "Web Cam Texture", FileType.TYPE_OTHER),
        new FileType("editorsettings", "Editor Settings", FileType.TYPE_OTHER),
        new FileType("interactivecloth", "Interactive Cloth", FileType.TYPE_OTHER),
        new FileType("clothrenderer", "Cloth Renderer", FileType.TYPE_OTHER),
        new FileType("editorusersettings", "Editor User Settings", FileType.TYPE_OTHER),
        new FileType("skinnedcloth", "Skinned Cloth", FileType.TYPE_OTHER),
        new FileType("audioreverbfilter", "Audio Reverb Filter", FileType.TYPE_OTHER),
        new FileType("audiohighpassfilter", "Audio High Pass Filter", FileType.TYPE_OTHER),
        new FileType("audiochorusfilter", "Audio Chorus Filter", FileType.TYPE_OTHER),
        new FileType("audioreverbzone", "Audio Reverb Zone", FileType.TYPE_OTHER),
        new FileType("audioechofilter", "Audio Echo Filter", FileType.TYPE_OTHER),
        new FileType("audiolowpassfilter", "Audio Low Pass Filter", FileType.TYPE_OTHER),
        new FileType("audiodistortionfilter", "Audio Distortion Filter", FileType.TYPE_OTHER),
        new FileType("sparsetexture", "Sparse Texture", FileType.TYPE_OTHER),
        new FileType("audiobehaviour", "Audio Behaviour", FileType.TYPE_OTHER),
        new FileType("audiofilter", "Audio Filter", FileType.TYPE_OTHER),
        new FileType("windzone", "Wind Zone", FileType.TYPE_OTHER),
        new FileType("cloth", "Cloth", FileType.TYPE_OTHER),
        new FileType("substancearchive", "Substance Archive", FileType.TYPE_OTHER),
        new FileType("proceduralmaterial", "Procedural Material", FileType.TYPE_OTHER),
        new FileType("proceduraltexture", "Procedural Texture", FileType.TYPE_OTHER),
        new FileType("texture2darray", "Texture 2D Array", FileType.TYPE_OTHER),
        new FileType("cubemaparray", "Cubemap Array", FileType.TYPE_OTHER),
        new FileType("offmeshlink", "Off Mesh Link", FileType.TYPE_OTHER),
        new FileType("occlusionarea", "Occlusion Area", FileType.TYPE_OTHER),
        new FileType("tree", "Tree", FileType.TYPE_OTHER),
        new FileType("navmeshobsolete", "Nav Mesh Obsolete", FileType.TYPE_OTHER),
        new FileType("navmeshagent", "Nav Mesh Agent", FileType.TYPE_OTHER),
        new FileType("navmeshsettings", "Nav Mesh Settings", FileType.TYPE_OTHER),
        new FileType("lightprobeslegacy", "LightProbesLegacy", FileType.TYPE_OTHER),
        new FileType("particlesystem", "Particle System", FileType.TYPE_OTHER),
        new FileType("particlesystemrenderer", "Particle System Renderer", FileType.TYPE_OTHER),
        new FileType("shadervariantcollection", "Shader Variant Collection", FileType.TYPE_OTHER),
        new FileType("lodgroup", "LOD Group", FileType.TYPE_OTHER),
        new FileType("blendtree", "Blend Tree", FileType.TYPE_OTHER),
        new FileType("motion", "Motion", FileType.TYPE_OTHER),
        new FileType("navmeshobstacle", "Nav Mesh Obstacle", FileType.TYPE_OTHER),
        new FileType("terraininstance", "Terrain Instance", FileType.TYPE_OTHER),
        new FileType("spriterenderer", "Sprite Renderer", FileType.TYPE_OTHER),
        new FileType("sprite", "Sprite", FileType.TYPE_OTHER),
        new FileType("cachedspriteatlas", "Cached Sprite Atlas", FileType.TYPE_OTHER),
        new FileType("reflectionprobe", "Reflection Probe", FileType.TYPE_OTHER),
        new FileType("reflectionprobes", "Reflection Probes", FileType.TYPE_OTHER),
        new FileType("terrain", "Terrain", FileType.TYPE_OTHER),
        new FileType("lightprobegroup", "Light Probe Group", FileType.TYPE_OTHER),
        new FileType("animatoroverridecontroller", "Animator Override Controller", FileType.TYPE_OTHER),
        new FileType("canvasrenderer", "Canvas Renderer", FileType.TYPE_OTHER),
        new FileType("canvas", "Canvas", FileType.TYPE_OTHER),
        new FileType("recttransform", "Rect Transform", FileType.TYPE_OTHER),
        new FileType("canvasgroup", "Canvas Group", FileType.TYPE_OTHER),
        new FileType("billboardasset", "Billboard Asset", FileType.TYPE_OTHER),
        new FileType("billboardrenderer", "Billboard Renderer", FileType.TYPE_OTHER),
        new FileType("speedtreewindasset", "Speed Tree Wind Asset", FileType.TYPE_OTHER),
        new FileType("anchoredjoint2d", "Anchored Joint 2D", FileType.TYPE_OTHER),
        new FileType("joint2d", "Joint 2D", FileType.TYPE_OTHER),
        new FileType("springjoint2d", "Spring Joint 2D", FileType.TYPE_OTHER),
        new FileType("distancejoint2d", "Distance Joint 2D", FileType.TYPE_OTHER),
        new FileType("hingejoint2d", "Hinge Joint 2D", FileType.TYPE_OTHER),
        new FileType("sliderjoint2d", "Slider Joint 2D", FileType.TYPE_OTHER),
        new FileType("wheeljoint2d", "Wheel Joint 2D", FileType.TYPE_OTHER),
        new FileType("clusterinputmanager", "Cluster Input Manager", FileType.TYPE_OTHER),
        new FileType("basevideotexture", "Base Video Texture", FileType.TYPE_OTHER),
        new FileType("navmeshdata", "Nav Mesh Data", FileType.TYPE_OTHER),
        new FileType("audiomixer", "Audio Mixer", FileType.TYPE_OTHER),
        new FileType("audiomixercontroller", "Audio Mixer Controller", FileType.TYPE_OTHER),
        new FileType("audiomixergroupcontroller", "Audio Mixer Group Controller", FileType.TYPE_OTHER),
        new FileType("audiomixereffectcontroller", "Audio Mixer Effect Controller", FileType.TYPE_OTHER),
        new FileType("audiomixersnapshotcontroller", "Audio Mixer Snapshot Controller", FileType.TYPE_OTHER),
        new FileType("physicsupdatebehaviour2d", "Physics Update Behaviour 2D", FileType.TYPE_OTHER),
        new FileType("constantforce2d", "Constant Force 2D", FileType.TYPE_OTHER),
        new FileType("effector2d", "Effector 2D", FileType.TYPE_OTHER),
        new FileType("areaeffector2d", "Area Effector 2D", FileType.TYPE_OTHER),
        new FileType("pointeffector2d", "Point Effector 2D", FileType.TYPE_OTHER),
        new FileType("platformeffector2d", "Platform Effector 2D", FileType.TYPE_OTHER),
        new FileType("surfaceeffector2d", "Surface Effector 2D", FileType.TYPE_OTHER),
        new FileType("buoyancyeffector2d", "Buoyancy Effector 2D", FileType.TYPE_OTHER),
        new FileType("relativejoint2d", "Relative Joint 2D", FileType.TYPE_OTHER),
        new FileType("fixedjoint2d", "Fixed Joint 2D", FileType.TYPE_OTHER),
        new FileType("frictionjoint2d", "Friction Joint 2D", FileType.TYPE_OTHER),
        new FileType("targetjoint2d", "Target Joint 2D", FileType.TYPE_OTHER),
        new FileType("lightprobes", "Light Probes", FileType.TYPE_OTHER),
        new FileType("lightprobeproxyvolume", "Light Probe Proxy Volume", FileType.TYPE_OTHER),
        new FileType("sampleclip", "Sample Clip", FileType.TYPE_OTHER),
        new FileType("audiomixersnapshot", "Audio Mixer Snapshot", FileType.TYPE_OTHER),
        new FileType("audiomixergroup", "Audio Mixer Group", FileType.TYPE_OTHER),
        new FileType("assetbundlemanifest", "Asset Bundle Manifest", FileType.TYPE_OTHER),
        new FileType("runtimeinitializeonloadmanager", "Runtime Initialize On Load Manager", FileType.TYPE_OTHER),
        new FileType("unityconnectsettings", "Unity Connect Settings", FileType.TYPE_OTHER),
        new FileType("avatarmask", "Avatar Mask", FileType.TYPE_OTHER),
        new FileType("playabledirector", "Playable Director", FileType.TYPE_OTHER),
        new FileType("videoplayer", "Video Player", FileType.TYPE_OTHER),
        new FileType("videoclip", "Video Clip", FileType.TYPE_OTHER),
        new FileType("particlesystemforcefield", "Particle System Force Field", FileType.TYPE_OTHER),
        new FileType("spritemask", "Sprite Mask", FileType.TYPE_OTHER),
        new FileType("worldanchor", "World Anchor", FileType.TYPE_OTHER),
        new FileType("occlusioncullingdata", "Occlusion Culling Data", FileType.TYPE_OTHER),
        new FileType("prefab", "Prefab", FileType.TYPE_OTHER),
        new FileType("editorextensionimpl", "Editor Extension Impl", FileType.TYPE_OTHER),
        new FileType("assetimporter", "Asset Importer", FileType.TYPE_OTHER),
        new FileType("assetdatabase", "Asset Database", FileType.TYPE_OTHER),
        new FileType("mesh3dsimporter", "Mesh 3DS Importer", FileType.TYPE_OTHER),
        new FileType("textureimporter", "Texture Importer", FileType.TYPE_OTHER),
        new FileType("shaderimporter", "Shader Importer", FileType.TYPE_OTHER),
        new FileType("computeshaderimporter", "Compute Shader Importer", FileType.TYPE_OTHER),
        new FileType("avatarmask", "Avatar Mask", FileType.TYPE_OTHER),
        new FileType("audioimporter", "Audio Importer", FileType.TYPE_OTHER),
        new FileType("hierarchystate", "Hierarchy State", FileType.TYPE_OTHER),
        new FileType("guidserializer", "GUID Serializer", FileType.TYPE_OTHER),
        new FileType("assetmetadata", "Asset Meta Data", FileType.TYPE_OTHER),
        new FileType("defaultasset", "Default Asset", FileType.TYPE_OTHER),
        new FileType("defaultimporter", "Default Importer", FileType.TYPE_OTHER),
        new FileType("textscriptimporter", "Text Script Importer", FileType.TYPE_OTHER),
        new FileType("sceneasset", "Scene Asset", FileType.TYPE_OTHER),
        new FileType("nativeformatimporter", "Native Format Importer", FileType.TYPE_OTHER),
        new FileType("monoimporter", "Mono Importer", FileType.TYPE_OTHER),
        new FileType("assetservercache", "Asset Server Cache", FileType.TYPE_OTHER),
        new FileType("libraryassetimporter", "Library Asset Importer", FileType.TYPE_OTHER),
        new FileType("modelimporter", "Model Importer", FileType.TYPE_OTHER),
        new FileType("fbximporter", "FBX Importer", FileType.TYPE_OTHER),
        new FileType("truetypefontimporter", "True Type Font Importer", FileType.TYPE_OTHER),
        new FileType("movieimporter", "Movie Importer", FileType.TYPE_OTHER),
        new FileType("editorbuildsettings", "Editor Build Settings", FileType.TYPE_OTHER),
        new FileType("ddsimporter", "DDS Importer", FileType.TYPE_OTHER),
        new FileType("inspectorexpandedstate", "Inspector Expanded State", FileType.TYPE_OTHER),
        new FileType("annotationmanager", "Annotation Manager", FileType.TYPE_OTHER),
        new FileType("pluginimporter", "Plugin Importer", FileType.TYPE_OTHER),
        new FileType("editoruserbuildsettings", "Editor User Build Settings", FileType.TYPE_OTHER),
        new FileType("pvrimporter", "PVR Importer", FileType.TYPE_OTHER),
        new FileType("astcimporter", "ASTC Importer", FileType.TYPE_OTHER),
        new FileType("ktximporter", "KTX Importer", FileType.TYPE_OTHER),
        new FileType("ihvimageformatimporter", "IHV Image Format Importer", FileType.TYPE_OTHER),
        new FileType("animatorstatetransition", "Animator State Transition", FileType.TYPE_OTHER),
        new FileType("animatorstate", "Animator State", FileType.TYPE_OTHER),
        new FileType("humantemplate", "Human Template", FileType.TYPE_OTHER),
        new FileType("animatorstatemachine", "Animator State Machine", FileType.TYPE_OTHER),
        new FileType("previewassettype", "Preview Asset Type", FileType.TYPE_OTHER),
        new FileType("animatortransition", "Animator Transition", FileType.TYPE_OTHER),
        new FileType("speedtreeimporter", "Speed Tree Importer", FileType.TYPE_OTHER),
        new FileType("animatortransitionbase", "Animator Transition Base", FileType.TYPE_OTHER),
        new FileType("substanceimporter", "Substance Importer", FileType.TYPE_OTHER),
        new FileType("lightmapparameters", "Lightmap Parameters", FileType.TYPE_OTHER),
        new FileType("lightmapsnapshot", "Lightmap Snapshot", FileType.TYPE_OTHER),
        new FileType("sketchupimporter", "Sketch Up Importer", FileType.TYPE_OTHER),
        new FileType("buildreport", "Build Report", FileType.TYPE_OTHER),
        new FileType("packedassets", "Packed Assets", FileType.TYPE_OTHER),
        new FileType("videoclipimporter", "Video Clip Importer", FileType.TYPE_OTHER),
        new FileType("int", "int", FileType.TYPE_OTHER),
        new FileType("bool", "bool", FileType.TYPE_OTHER),
        new FileType("float", "float", FileType.TYPE_OTHER),
        new FileType("monoobject", "Mono Object", FileType.TYPE_OTHER),
        new FileType("collision", "Collision", FileType.TYPE_OTHER),
        new FileType("vector3f", "Vector 3f", FileType.TYPE_OTHER),
        new FileType("rootmotiondata", "Root Motion Data", FileType.TYPE_OTHER),
        new FileType("collision2d", "Collision 2D", FileType.TYPE_OTHER),
        new FileType("audiomixerliveupdatefloat", "Audio Mixer Live Update Float", FileType.TYPE_OTHER),
        new FileType("audiomixerliveupdatebool", "Audio Mixer Live Update Bool", FileType.TYPE_OTHER),
        new FileType("polygon2d", "Polygon 2D", FileType.TYPE_OTHER),
        new FileType("void", "void", FileType.TYPE_OTHER),
        new FileType("tilemapcollider2d", "Tilemap Collider 2D", FileType.TYPE_OTHER),
        new FileType("assetimporterlog", "Asset Importer Log", FileType.TYPE_OTHER),
        new FileType("vfxrenderer", "VFX Renderer", FileType.TYPE_OTHER),
        new FileType("serializablemanagedreftestclass", "Serializable Managed Ref Test Class", FileType.TYPE_OTHER),
        new FileType("grid", "Grid", FileType.TYPE_OTHER),
        new FileType("preset", "Preset", FileType.TYPE_OTHER),
        new FileType("emptyobject", "Empty Object", FileType.TYPE_OTHER),
        new FileType("iconstraint", "IConstraint", FileType.TYPE_OTHER),
        new FileType("testobjectwithspeciallayoutone", "Test Object With Special Layout One", FileType.TYPE_OTHER),
        new FileType("assemblydefinitionreferenceimporter", "Assembly Definition Reference Importer", FileType.TYPE_OTHER),
        new FileType("siblingderived", "Sibling Derived", FileType.TYPE_OTHER),
        new FileType("testobjectwithserializedmapstringnonalignedstruct", "Test Object With Serialized Map String Non Aligned Struct", FileType.TYPE_OTHER),
        new FileType("subderived", "Sub Derived", FileType.TYPE_OTHER),
        new FileType("assetimportinprogressproxy", "Asset Import In Progress Proxy", FileType.TYPE_OTHER),
        new FileType("pluginbuildinfo", "Plugin Build Info", FileType.TYPE_OTHER),
        new FileType("editorprojectaccess", "Editor Project Access", FileType.TYPE_OTHER),
        new FileType("prefabimporter", "Prefab Importer", FileType.TYPE_OTHER),
        new FileType("testobjectwithserializedarray", "Test Object With Serialized Array", FileType.TYPE_OTHER),
        new FileType("testobjectwithserializedanimationcurve", "Test Object With Serialized Animation Curve", FileType.TYPE_OTHER),
        new FileType("tilemaprenderer", "Tilemap Renderer", FileType.TYPE_OTHER),
        new FileType("spriteatlasdatabase", "Sprite Atlas Database", FileType.TYPE_OTHER),
        new FileType("audiobuildinfo", "Audio Build Info", FileType.TYPE_OTHER),
        new FileType("cachedspriteatlasruntimedata", "Cached Sprite Atlas Runtime Data", FileType.TYPE_OTHER),
        new FileType("rendererfake", "Renderer Fake", FileType.TYPE_OTHER),
        new FileType("assemblydefinitionreferenceasset", "Assembly Definition Reference Asset", FileType.TYPE_OTHER),
        new FileType("builtassetbundleinfoset", "Built Asset Bundle Info Set", FileType.TYPE_OTHER),
        new FileType("spriteatlas", "Sprite Atlas", FileType.TYPE_OTHER),
        new FileType("raytracingshaderimporter", "Ray Tracing Shader Importer", FileType.TYPE_OTHER),
        new FileType("raytracingshader", "Ray Tracing Shader", FileType.TYPE_OTHER),
        new FileType("platformmodulesetup", "Platform Module Setup", FileType.TYPE_OTHER),
        new FileType("aimconstraint", "Aim Constraint", FileType.TYPE_OTHER),
        new FileType("vfxmanager", "VFX Manager", FileType.TYPE_OTHER),
        new FileType("visualeffectsubgraph", "Visual Effect Subgraph", FileType.TYPE_OTHER),
        new FileType("visualeffectsubgraphoperator", "Visual Effect Subgraph Operator", FileType.TYPE_OTHER),
        new FileType("visualeffectsubgraphblock", "Visual Effect Subgraph Block", FileType.TYPE_OTHER),
        new FileType("prefab", "Prefab", FileType.TYPE_OTHER),
        new FileType("localizationimporter", "Localization Importer", FileType.TYPE_OTHER),
        new FileType("derived", "Derived", FileType.TYPE_OTHER),
        new FileType("propertymodificationstargettestobject", "Property Modifications Target Test Object", FileType.TYPE_OTHER),
        new FileType("referencesartifactgenerator", "References Artifact Generator", FileType.TYPE_OTHER),
        new FileType("assemblydefinitionasset", "Assembly Definition Asset", FileType.TYPE_OTHER),
        new FileType("scenevisibilitystate", "Scene Visibility State", FileType.TYPE_OTHER),
        new FileType("lookatconstraint", "Look At Constraint", FileType.TYPE_OTHER),
        new FileType("multiartifacttestimporter", "Multi Artifact Test Importer", FileType.TYPE_OTHER),
        new FileType("gameobjectrecorder", "Game Object Recorder", FileType.TYPE_OTHER),
        new FileType("lightingdataassetparent", "Lighting Data Asset Parent", FileType.TYPE_OTHER),
        new FileType("presetmanager", "Preset Manager", FileType.TYPE_OTHER),
        new FileType("testobjectwithspeciallayouttwo", "Test Object With Special Layout Two", FileType.TYPE_OTHER),
        new FileType("streamingmanager", "Streaming Manager", FileType.TYPE_OTHER),
        new FileType("lowerresblittexture", "Lower Res Blit Texture", FileType.TYPE_OTHER),
        new FileType("streamingcontroller", "Streaming Controller", FileType.TYPE_OTHER),
        new FileType("testobjectvectorpairstringbool", "Test Object Vector Pair String Bool", FileType.TYPE_OTHER),
        new FileType("gridlayout", "Grid Layout", FileType.TYPE_OTHER),
        new FileType("assemblydefinitionimporter", "Assembly Definition Importer", FileType.TYPE_OTHER),
        new FileType("parentconstraint", "Parent Constraint", FileType.TYPE_OTHER),
        new FileType("fakecomponent", "Fake Component", FileType.TYPE_OTHER),
        new FileType("positionconstraint", "Position Constraint", FileType.TYPE_OTHER),
        new FileType("rotationconstraint", "Rotation Constraint", FileType.TYPE_OTHER),
        new FileType("scaleconstraint", "Scale Constraint", FileType.TYPE_OTHER),
        new FileType("tilemap", "Tilemap", FileType.TYPE_OTHER),
        new FileType("packagemanifest", "Package Manifest", FileType.TYPE_OTHER),
        new FileType("packagemanifestimporter", "Package Manifest Importer", FileType.TYPE_OTHER),
        new FileType("terrainlayer", "Terrain Layer", FileType.TYPE_OTHER),
        new FileType("spriteshaperenderer", "Sprite Shape Renderer", FileType.TYPE_OTHER),
        new FileType("nativeobjecttype", "Native Object Type", FileType.TYPE_OTHER),
        new FileType("testobjectwithserializedmapstringbool", "Test Object With Serialized Map String Bool", FileType.TYPE_OTHER),
        new FileType("serializablemanagedhost", "Serializable ManagedHost", FileType.TYPE_OTHER),
        new FileType("visualeffectasset", "Visual Effect Asset", FileType.TYPE_OTHER),
        new FileType("visualeffectimporter", "Visual Effect Importer", FileType.TYPE_OTHER),
        new FileType("visualeffectresource", "Visual Effect Resource", FileType.TYPE_OTHER),
        new FileType("visualeffectobject", "Visual Effect Object", FileType.TYPE_OTHER),
        new FileType("visualeffect", "Visual Effect", FileType.TYPE_OTHER),
        new FileType("localizationasset", "Localization Asset", FileType.TYPE_OTHER),
        new FileType("scriptedimporter", "Scripted Importer", FileType.TYPE_OTHER),
        // NEW ONES AFTER HERE
        new FileType("soundwave", "Sound Wave", FileType.TYPE_AUDIO)
    };

    return types;
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

      long arcSize = fm.getLength();

      // 8 - null
      if (FieldValidator.checkEquals(fm.readLong(), 0)) {
        rating += 5;
      }

      // 8 - Compressed Length (not including the file header fields or padding)
      if (FieldValidator.checkLength(fm.readLong(), arcSize)) {
        rating += 5;
      }

      // 8 - Decompressed Length
      if (FieldValidator.checkLength(fm.readLong())) {
        rating += 5;
      }

      // 4 - Compression Type (0=uncompressed, 1=ZLib, 2=GZip, 4=Snappy)
      int compressionType = fm.readInt();
      if (compressionType == 0 || compressionType == 1 || compressionType == 2 || compressionType == 4) {
        rating += 4; // 4, so that this plugin is only tried *after* the PAK_38 plugin, which reads most UE4 archives
      }

      return rating;

    }
    catch (Throwable t) {
      return 0;
    }
  }

  /**
   **********************************************************************************************
   * Reads an [archive] File into the Resources
   **********************************************************************************************
   **/
  @Override
  public Resource[] read(File path) {
    try {

      // NOTE - Compressed files MUST know their DECOMPRESSED LENGTH
      //      - Uncompressed files MUST know their LENGTH

      addFileTypes();

      ExporterPlugin exporterZLib = Exporter_ZLib.getInstance();
      //ExporterPlugin exporterGZip = Exporter_GZip.getInstance();
      //ExporterPlugin exporterSnappy = Exporter_Snappy.getInstance();
      ExporterPlugin exporterZLibWithEncryption = Exporter_Default.getInstance();
      ExporterPlugin exporterDefaultWithEncryption = Exporter_Default.getInstance();

      // NOTE: Not real OODLE compression, but we want to tell people that it is, in case they look in the file info sidepanel
      //ExporterPlugin exporterOodle = new Exporter_Default();
      //exporterOodle.setName("Oodle Compression");
      ExporterPlugin exporterOodle = new Exporter_QuickBMS_Decompression("oodle");

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // GO TO THE END OF THE ARCHIVE
      fm.seek(arcSize - 36);

      // 8 - Directory Offset
      long dirOffset = fm.readLong();
      FieldValidator.checkOffset(dirOffset, arcSize);

      if (dirOffset == 0) {
        // maybe there's some kind of padding at the end - go back a bit more (eg Game: Rainy Season)
        fm.seek(arcSize - 128 - 36);

        // 8 - Directory Offset
        try {
          dirOffset = fm.readLong();
          FieldValidator.checkOffset(dirOffset, arcSize);

          if (arcSize - dirOffset > 100000000) {
            throw new Exception("Dir isn't near the end of the archive, so try again further down");
          }

        }
        catch (Throwable t) {
          // go back another 32 bytes
          fm.seek(arcSize - 128 - 36 - 32);

          dirOffset = fm.readLong();
          try {
            FieldValidator.checkOffset(dirOffset, arcSize);
          }
          catch (Throwable t2) {
            // final try
            fm.seek(arcSize - 197); // 1 byte earlier than the previous try

            dirOffset = fm.readLong();
            FieldValidator.checkOffset(dirOffset, arcSize);
          }

        }

      }

      // 8 - Directory Length
      // 20 - Unknown
      fm.skip(28);

      // 32 - Compression Type 1
      String compression1 = fm.readNullString(32);
      // 32 - Compression Type 2
      String compression2 = fm.readNullString(32);

      fm.seek(dirOffset);

      long originalDirOffset = dirOffset;
      boolean decrypted = false;
      FileManipulator originalFM = fm;

      int numFiles = 0;
      try {

        // 4 - Relative Directory Name Length (including null terminator) (10)
        fm.skip(4);

        // 9 - Relative Directory Name (../../../)
        // 1 - null Relative Directory Name Terminator
        fm.readNullString();

        // 4 - Number of Files
        numFiles = fm.readInt();
        try {
          FieldValidator.checkNumFiles(numFiles / 4);
        }
        catch (Throwable t) {
          // second try

          fm.seek(arcSize - 197); // 1 byte earlier than the previous try

          dirOffset = fm.readLong();
          FieldValidator.checkOffset(dirOffset, arcSize);

          fm.seek(dirOffset);

          // 4 - Relative Directory Name Length (including null terminator) (10)
          fm.skip(4);

          // 9 - Relative Directory Name (../../../)
          // 1 - null Relative Directory Name Terminator
          fm.readNullString();

          // 4 - Number of Files
          numFiles = fm.readInt();
          FieldValidator.checkNumFiles(numFiles / 4);

        }
      }
      catch (Throwable t) {
        // If we get here, perhaps it's encrypted with AES
        dirOffset = originalDirOffset;

        fm.seek(dirOffset);

        long dirLength = (int) (arcSize - dirOffset);

        // Try all the keys we know about, see if we can find one that works
        byte[][] keys = UE4Helper.getAESKeys();
        int numKeys = keys.length;

        byte[] key = null;

        int testLength = 64;
        for (int k = 0; k < numKeys; k++) {
          try {
            key = keys[k];
            Exporter_Encryption_AES decryptor = new Exporter_Encryption_AES(key);

            Resource dirResource = new Resource(path, "", dirOffset, testLength, testLength, decryptor);
            ExporterByteBuffer exporterBuffer = new ExporterByteBuffer(dirResource);

            FileManipulator testFM = new FileManipulator(exporterBuffer);

            // 4 - Relative Directory Name Length (including null terminator) (10)
            int nameLength = testFM.readInt();
            FieldValidator.checkRange(nameLength, 0, 64);

            // 9 - Relative Directory Name (../../../)
            // 1 - null Relative Directory Name Terminator
            testFM.readNullString();

            // 4 - Number of Files
            numFiles = testFM.readInt();
            FieldValidator.checkNumFiles(numFiles / 4);

            // found one
            break;
          }
          catch (Throwable t2) {
            numFiles = 0;
            key = null;
          }
        }

        //byte[] key = ByteArrayConverter.convertLittle(new Hex("D0BAAAE538F6B96FBE77F4A1EF75DDEB62AAE6A54790B37F46AE055D2E787821"));
        if (key == null) {
          throw new WSPluginException("[PAK_65] No matching AES key found.");
        }

        Exporter_Encryption_AES decryptor = new Exporter_Encryption_AES(key);

        Resource dirResource = new Resource(path, "", dirOffset, dirLength, dirLength, decryptor);
        ExporterByteBuffer exporterBuffer = new ExporterByteBuffer(dirResource);

        //fm.close();
        fm = new FileManipulator(exporterBuffer);

        //FileManipulator temp = new FileManipulator(new File("c:\\test.txt"), true);
        //temp.writeBytes(fm.readBytes((int) dirLength));
        //temp.close();

        // 4 - Relative Directory Name Length (including null terminator) (10)
        fm.skip(4);

        // 9 - Relative Directory Name (../../../)
        // 1 - null Relative Directory Name Terminator
        fm.readNullString();

        // 4 - Number of Files
        numFiles = fm.readInt();
        FieldValidator.checkNumFiles(numFiles / 4);

        decrypted = true;

        // Set up the Exporters for files that are encrypted
        exporterZLibWithEncryption = new Exporter_AES_ZLib(key);
        exporterDefaultWithEncryption = new Exporter_Encryption_AES(key);
      }

      if (numFiles <= 0) {
        return null;
      }

      // Set the exporters based on the Compression types in the Footer
      ExporterPlugin exporter1 = null;
      ExporterPlugin exporter1WithEncryption = null;
      ExporterPlugin exporter2 = null;
      ExporterPlugin exporter2WithEncryption = null;

      if (compression1 != null && !compression1.equals("")) {
        if (compression1.equalsIgnoreCase("Zlib")) {
          exporter1 = exporterZLib;
          exporter1WithEncryption = exporterZLibWithEncryption;
        }
        else if (compression1.equalsIgnoreCase("Oodle")) {
          // Oodle compression not implemented
          exporter1 = exporterOodle;
          exporter1WithEncryption = exporterDefaultWithEncryption;
        }
      }

      if (compression2 != null && !compression2.equals("")) {
        if (compression2.equalsIgnoreCase("Zlib")) {
          exporter2 = exporterZLib;
          exporter2WithEncryption = exporterZLibWithEncryption;
        }
        else if (compression2.equalsIgnoreCase("Oodle")) {
          // Oodle compression not implemented
          exporter2 = exporterOodle;
          exporter2WithEncryption = exporterDefaultWithEncryption;
        }
      }

      // 8 - Unknown
      // 4 - Unknown (1)
      // 8 - Directory 2 Offset
      // 8 - Directory 2 Length
      // 20 - Unknown
      // 4 - Unknown (1)
      fm.skip(52);

      // 8 - Directory 3 Offset
      long dir3Offset = fm.readLong();
      FieldValidator.checkOffset(dir3Offset, arcSize);

      if (decrypted) {
        // we've decrypted the directory, need to change the dir3offset to be relative to the decrypted content
        dir3Offset -= originalDirOffset;
      }

      // 8 - Directory 3 Length
      // 20 - Unknown
      // 4 - Directory 1 Length
      fm.skip(32);

      Resource_PAK_38[] resources = new Resource_PAK_38[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      int[] entryOffsets = new int[numFiles]; // for matching the filenames to the entries
      long dirStart = fm.getOffset();
      for (int i = 0; i < numFiles; i++) {
        TaskProgressManager.setValue(i);

        int entryOffset = (int) (fm.getOffset() - dirStart);
        entryOffsets[i] = entryOffset;

        // 4 - Unknown ((bytes)0,0,0,224)
        byte[] entryType = fm.readBytes(4);

        /*
        System.out.println((fm.getOffset() - 4) + "\t" + entryType[0] + "\t" + entryType[1] + "\t" + entryType[2] + "\t" + entryType[3]);
        if (entryType[3] != -32) {
          System.out.println("HERE");
        }
        */

        if (entryType[0] == 0 && entryType[1] == 0 && entryType[2] == 0 && entryType[3] == 0) {
          // A Table of Paper
          // Absolute Tactics: Daughters of Mercy
          // Faraday Protocol
          ErrorLogger.log("[PAK_65] Early Directory Exit at offset " + fm.getOffset());

          numFiles = i;

          Resource_PAK_38[] oldResources = resources;
          resources = new Resource_PAK_38[numFiles];
          System.arraycopy(oldResources, 0, resources, 0, numFiles);

          break;
        }

        while (entryType[3] == 0) {
          // probably didn't read far enough in the previous entry, skip it
          entryType = fm.readBytes(4); // (Kid A Mnesia Exhibition)
        }

        long offset = 0;
        long length = 0;
        boolean swapped = false;

        if (entryType[3] == 97) {
          if (entryType[0] >= 64 && entryType[0] < 127) {
            // 8 - File Offset
            offset = fm.readLong();
            FieldValidator.checkOffset(offset, arcSize);

            // 4 - File Length (not including the file header or the padding)
            length = IntConverter.unsign(fm.readInt());
            FieldValidator.checkLength(length, arcSize);
          }
          else {
            // 4 - File Length (not including the file header or the padding)
            length = IntConverter.unsign(fm.readInt());
            FieldValidator.checkLength(length, arcSize);

            // 8 - File Offset
            offset = fm.readLong();
            FieldValidator.checkOffset(offset, arcSize);
          }

          swapped = true;
        }
        else if (entryType[3] == 96) {
          // 8 - File Offset
          offset = fm.readLong();
          FieldValidator.checkOffset(offset, arcSize);

          // 4 - File Length (not including the file header or the padding)
          length = IntConverter.unsign(fm.readInt());
          FieldValidator.checkLength(length, arcSize);

          swapped = true;
        }
        else if (entryType[2] == -64 && entryType[0] >= 64) { // Faraday Protocol
          // 4 - File Offset
          offset = IntConverter.unsign(fm.readInt());
          FieldValidator.checkOffset(offset, arcSize);

          // 4 - Decomp Length?
          fm.skip(4);

          // 4 - File Length (not including the file header or the padding)
          length = IntConverter.unsign(fm.readInt());
          FieldValidator.checkLength(length, arcSize);

          swapped = true;
        }
        else {
          // 4 - File Offset
          offset = IntConverter.unsign(fm.readInt());
          FieldValidator.checkOffset(offset, arcSize);

          // 4 - File Length (not including the file header or the padding)
          length = IntConverter.unsign(fm.readInt());
          FieldValidator.checkLength(length, arcSize);
        }

        if (entryType[0] >= 64 && entryType[0] < 127) {
          // 4 - Unknown
          fm.skip(4);
        }

        if (entryType[0] == -1) {
          if (!swapped) {
            long oldOffset = offset;
            offset = length;
            length = oldOffset;
          }

          // 0 = 20 bytes
          // 1 = ? bytes
          // 2 = 52 bytes

          // 20 - Unknown
          fm.skip(20);

          // count*16 - Unknown
          int count = entryType[1];
          fm.skip(count * 16);
        }
        else if (entryType[0] == 63) {
          if (!swapped) {
            long oldOffset = offset;
            offset = length;
            length = oldOffset;
          }

          // 4 - Unknown
          // 4 - Unknown
          fm.skip(8);

          // count*16 - Unknown
          int count = entryType[1];
          fm.skip(count * 16);
        }
        else if (entryType[0] == -65) { // 191
          if (!swapped) {
            long oldOffset = offset;
            offset = length;
            length = oldOffset;
          }

          // count*16 - Unknown
          int count = entryType[1];
          fm.skip((count + 1) * 16);
        }
        else if (entryType[0] == 127) {
          // 0 = 8 bytes
          // 1 = 28 bytes   
          // 9 = 156 bytes

          // 4 - File Offset
          // 4 - File Length (not including the file header or the padding)

          if (!swapped) {
            offset = length;
          }
          length = IntConverter.unsign(fm.readInt());
          FieldValidator.checkLength(length, arcSize);

          fm.skip(4);

          // count*20 - Unknown
          int count = entryType[1];
          fm.skip(count * 16);

          if (count >= 1) {
            // additional 4
            fm.skip(4);
          }
          if (entryType[2] == -64) {
            // additional 4
            fm.skip(4);
          }

        }
        else if (entryType[0] == -96) { // 160 (Train Sim World 2)
          // 12 - Unknown
          fm.skip(12);

          // count*16 - Unknown
          int count = ByteConverter.unsign(entryType[1]);
          if (entryType[2] == -127) {
            count += 256;
          }
          else if (entryType[2] == -126) {
            count += 512;
          }
          else if (entryType[2] == -125) {
            count += 768;
          }
          fm.skip((count) * 16);
        }
        else if (entryType[0] == -32) { // 224 (Train Sim World 2)
          // count*16 - Unknown
          int count = ByteConverter.unsign(entryType[1]);
          if (entryType[2] == -127) {
            count += 256;
          }
          else if (entryType[2] == -126) {
            count += 512;
          }
          else if (entryType[2] == -125) {
            count += 768;
          }
          fm.skip((count + 1) * 16);
        }
        else if (entryType[0] == 32) { // (Train Sim World 2)
          // 4 - Unknown
          fm.skip(4);

          // count*16 - Unknown
          int count = ByteConverter.unsign(entryType[1]);
          if (entryType[2] == -127) {
            count += 256;
          }
          else if (entryType[2] == -126) {
            count += 512;
          }
          else if (entryType[2] == -125) {
            count += 768;
          }
          fm.skip((count) * 16);
        }
        else if (entryType[0] == 96) { // (Train Sim World 2)
          // 4 - Unknown
          fm.skip(4);

          // count*16 - Unknown
          int count = ByteConverter.unsign(entryType[1]);
          if (entryType[2] == -127) {
            count += 256;
          }
          else if (entryType[2] == -126) {
            count += 512;
          }
          else if (entryType[2] == -125) {
            count += 768;
          }
          fm.skip((count) * 16);

          if (count == 0) { // Faraday Protocol
            // 12 - Unknown
            // technically, this should be incorporated into the above if/else somehow.
            // ie. we only do 12 here because we've already skipped 4 of Unknown before the if-else
            fm.skip(12);
          }
        }

        String filename = Resource.generateFilename(i); // need a default name here, so the sort() later on works

        //path,name,offset,length,decompLength,exporter
        resources[i] = new Resource_PAK_38(path, filename, offset, length);

        //System.out.println((entryOffset + dirStart) + "\t" + offset + "\t" + length + "\t" + entryType[0] + "\t" + entryType[3]);
      }

      // Now read the filename directory
      if (decrypted) {
        //int lengthRead = (int) (fm.getOffset() - originalDirOffset);
        dir3Offset -= fm.getOffset(); // noting that fm.getOffset() is reading from the decrypted content
        fm.skip(dir3Offset);
      }
      else {
        fm.seek(dir3Offset);
      }

      // 4 - Number of Directories
      int numDirs = fm.readInt();
      FieldValidator.checkNumFiles(numDirs);

      for (int i = 0; i < numDirs; i++) {

        // 4 - Directory Name Length (including null terminator)
        int dirNameLength = fm.readInt();

        String dirName = "";
        if (dirNameLength > 0) {
          dirNameLength -= 1; // remove the null terminator

          // X - Directory Name (ASCII)
          // 1 - null Directory Name Terminator
          dirName = fm.readString(dirNameLength);
          fm.skip(1);
        }
        else if (dirNameLength < 0) {
          dirNameLength = 0 - dirNameLength; //Negate it
          dirNameLength -= 1; // remove the null terminator

          // X - Directory Name (Unicode)
          // 2 - null Directory Name Terminator
          dirName = fm.readUnicodeString(dirNameLength);
          fm.skip(2);
        }

        // 4 - Number of Files in this Directory (can be null)
        int numSubFiles = fm.readInt();
        FieldValidator.checkRange(numSubFiles, 0, numFiles);

        // read each file in this directory
        for (int j = 0; j < numSubFiles; j++) {
          // 4 - Filename Length (including null terminator)
          int filenameLength = fm.readInt();

          String filename = "";
          if (filenameLength > 0) {
            filenameLength -= 1; // remove the null terminator

            // X - Filename (ASCII)
            // 1 - null Filename Terminator
            filename = fm.readString(filenameLength);
            fm.skip(1);
          }
          else if (filenameLength < 0) {
            filenameLength = 0 - filenameLength; //Negate it
            filenameLength -= 1; // remove the null terminator

            // X - Filename (Unicode)
            // 2 - null Filename Terminator
            filename = fm.readUnicodeString(filenameLength);
            fm.skip(2);
          }

          filename = dirName + filename;

          // 4 - Offset to corresponding entry in Directory 1 (relative to the start of Directory 1)
          //int fileID = (fm.readInt() / 12);
          int entryOffset = fm.readInt();
          int fileID = Arrays.binarySearch(entryOffsets, entryOffset);

          if (fileID < 0) {
            // not found - skip this one
          }
          else {
            FieldValidator.checkRange(fileID, 0, numFiles);

            Resource resource = resources[fileID];
            resource.setName(filename);
            resource.setOriginalName(filename);
          }
        }
      }

      // if we've decrypted the directory, need to go back to the original archive now.
      if (fm != originalFM) {
        fm.close(); // close the decrypted file
        fm = originalFM;// swap back to the original archive
      }

      // Now go in to each file and work out if it's compressed or not
      fm.getBuffer().setBufferSize(28);

      for (int i = 0; i < numFiles; i++) {
        TaskProgressManager.setValue(i);

        try {
          // Try - if we have trouble reading any of this, the offset is probably wrong, but still allow it for now (TrainSimWorld2 has a funny item in 1 file)

          Resource resource = resources[i];

          long offset = resource.getOffset();
          //System.out.println("-->" + offset);
          fm.seek(offset);

          // 8 - null
          fm.skip(8);

          // 8 - Compressed Length (not including the file header fields or padding)
          long length = fm.readLong();
          FieldValidator.checkLength(length, arcSize);

          // 8 - Decompressed Length
          long decompLength = fm.readLong();
          FieldValidator.checkLength(decompLength);

          // 4 - Compression Type (0=uncompressed, 1=ZLib, 2=Oodle)
          int compressionType = fm.readInt();
          FieldValidator.checkRange(compressionType, 0, 4);

          if (compressionType == 0) {
            // uncompressed

            // 20 - Unknown
            fm.skip(20);

            // 1 - Encryption (0=unencrypted, 1=encrypted)
            int encryptionFlag = fm.readByte();

            // 4 - null
            fm.skip(4);

            offset = fm.getOffset();
            resource.setOffset(offset);

            if (encryptionFlag == 1) {
              // encrypted
              resource.setExporter(exporterDefaultWithEncryption);
            }
          }
          else {

            //System.out.println(offset + "\t" + compressionType);

            // 20 - Unknown
            fm.skip(20);

            // 4 - Number of Compressed Blocks
            int numBlocks = fm.readInt();
            FieldValidator.checkNumFiles(numBlocks);

            long[] blockOffsets = new long[numBlocks];
            long[] blockLengths = new long[numBlocks];
            boolean addOffset = true;
            for (int b = 0; b < numBlocks; b++) {
              // 8 - Offset to the start of the compressed data block (relative to the start of the archive)
              long blockStartOffset = fm.readLong();
              FieldValidator.checkOffset(blockStartOffset, arcSize);

              // 8 - Offset to the end of the compressed data block (relative to the start of the archive)
              long blockEndOffset = fm.readLong();
              FieldValidator.checkOffset(blockEndOffset, arcSize);

              long blockLength = blockEndOffset - blockStartOffset;
              FieldValidator.checkLength(blockLength);

              if (compressionType == 1) {
                if (b == 0) {
                  // IN SOME ONLY, ZLib compression uses an offset relative to the start of the Data for *this* File
                  long difference = blockStartOffset - offset - (numBlocks * 16);
                  if (difference >= 0 && difference < 250) {
                    addOffset = false;
                  }
                  else {
                    addOffset = true;
                  }
                }

                if (addOffset) {
                  blockStartOffset += offset;
                }
              }

              blockOffsets[b] = blockStartOffset;
              blockLengths[b] = blockLength;
            }

            // 1 - Encryption (0=unencrypted, 1=encrypted)
            int encryptionFlag = fm.readByte();

            // 4 - Decompressed Block Size (65536 if multiple blocks, otherwise the same as the decompressed length)
            int blockSize = fm.readInt();
            FieldValidator.checkLength(blockSize);

            ExporterPlugin exporter = null;
            if (compressionType == 1) {
              if (encryptionFlag == 1) {
                // encrypted and compressed
                if (exporter1WithEncryption != null) {
                  exporter = exporter1WithEncryption;
                }
                else {
                  exporter = exporterZLibWithEncryption;
                }
              }
              else {
                // not encrypted, just compressed
                if (exporter1 != null) {
                  exporter = exporter1;
                }
                else {
                  exporter = exporterZLib;
                }
              }
            }
            else if (compressionType == 2) {
              if (encryptionFlag == 1) {
                // encrypted and compressed
                if (exporter2WithEncryption != null) {
                  exporter = exporter2WithEncryption;
                }
                else {
                  exporter = exporterDefaultWithEncryption;
                }
              }
              else {
                // not encrypted, just compressed
                if (exporter2 != null) {
                  exporter = exporter2;
                }
                else {
                  exporter = exporterOodle;
                }
              }
            }
            else {
              throw new WSPluginException("Unknown Compression type: " + compressionType);
            }

            // Put the wrapper around the exporter
            if (numBlocks == 1) {
              // don't need a wrapper (as it's only 1 block) - just set the offset/length as appropriate
              offset = blockOffsets[0];
              length = blockLengths[0];
              decompLength = blockSize;
            }
            else {
              // put a wrapper around the exporter, giving the blocks details

              // work out the decompLengths
              long[] decompLengths = new long[numBlocks];
              for (int b = 0; b < numBlocks - 1; b++) {
                decompLengths[b] = blockSize;
              }
              long remainingSize = decompLength - (blockSize * (numBlocks - 1));
              decompLengths[numBlocks - 1] = remainingSize;

              exporter = new BlockExporterWrapper(exporter, blockOffsets, blockLengths, decompLengths);
            }

            resource.setLength(length);
            resource.setDecompressedLength(decompLength);
            resource.setOffset(fm.getOffset());
            resource.setExporter(exporter);
          }

        }
        catch (Throwable t) {
          ErrorLogger.log(t);
        }
      }

      // HERE WE DO...
      // 1. For all uasset files, go through and find all the related files (uexp, ubulk, etc) - files with the same name, different extension.
      //    Each related file is added as a "Related Resource" of the uasset file, and is also removed from the overall Resource[].
      // 2. For all uasset files, read a little bit of the file data, work out what the Class is, and therefore what the file type is. 

      // First, sort the resources
      Arrays.sort(resources);

      Resource_PAK_38[] culledResources = new Resource_PAK_38[numFiles];
      int numCulledFiles = 0;

      // Now find any with the same name as a uasset
      Resource_PAK_38 asset = null;
      String assetName = "";
      for (int i = 0; i < numFiles; i++) {
        Resource_PAK_38 resource = resources[i];

        if (resource.getExtension().equals("uasset")) {
          // found an asset
          asset = resource;
          assetName = resource.getName();

          culledResources[numCulledFiles] = resource; // we want to keep this file - it's a main uasset file
          numCulledFiles++;

          int dotPos = assetName.lastIndexOf(".uasset");
          if (dotPos > 0) {
            assetName = assetName.substring(0, dotPos + 1);
          }

          // now read a bit of the uasset file to determine the Class
          String className = readUAssetClass(resource);
          if (className != null) {
            String name = resource.getName() + "." + className;
            resource.setName(name);
            resource.setOriginalName(name);
          }

        }
        else {
          // see if the name matches the uasset
          boolean addedLink = false;

          if (asset != null) {
            String resName = resource.getName();
            int dotPos = resName.lastIndexOf(".");
            if (dotPos > 0) {
              resName = resName.substring(0, dotPos + 1);
            }
            if (resName.equals(assetName)) {
              // found a "related" resource
              asset.addRelatedResource(resource);
              addedLink = true;
            }

          }

          if (!addedLink) { // only want to keep files that haven't been related to the uasset file
            culledResources[numCulledFiles] = resource; // we want to keep this file - it's not a uasset file, and not a related file
            numCulledFiles++;
          }

        }

        TaskProgressManager.setValue(i);

      }

      // If any resources were culled, want to shrink the array...
      if (Settings.getBoolean("UE4CullRelatedResources")) { // So that we can easily turn it off to show the ubulk/uexp files and extract them for analysis
        if (numCulledFiles != numFiles) {
          resources = new Resource_PAK_38[numCulledFiles];
          System.arraycopy(culledResources, 0, resources, 0, numCulledFiles);
        }
      }

      fm.close();

      return resources;

    }
    catch (

    Throwable t) {
      logError(t);
      return null;
    }
  }

  /**
   **********************************************************************************************
   Reads the properties of a UAsset file, looks for the Class, and returns it
   **********************************************************************************************
   **/
  public String readUAssetClass(Resource resource) {
    // If this file is a uAsset, it really shouldn't use OODLE compression, but just in case we want to disable it for now (as it uses QuickBMS)
    ExporterPlugin originalExporter = resource.getExporter();

    try {
      long arcSize = resource.getDecompressedLength();

      /*
      byte[] headerBytes = null;
      
      int maxHeaderSize = 2000;
      
      // we may need to decompress some of the file first, to read the header
      if (arcSize < maxHeaderSize) {
        headerBytes = new byte[(int) arcSize];
      }
      else {
        headerBytes = new byte[maxHeaderSize];// only want a small amount
      }
      
      // make a dummy resource with a maximum decompLength of 2000
      long fakeLength = resource.getLength();
      if (fakeLength > maxHeaderSize) {
        fakeLength = maxHeaderSize;
      }
      long fakeDecompLength = resource.getDecompressedLength();
      if (fakeDecompLength > maxHeaderSize) {
        fakeDecompLength = maxHeaderSize;
      }
      
      ExporterPlugin exporter = resource.getExporter();
      // path,name,offset,length,decompLength,exporter
      Resource fakeResource = new Resource(resource.getSource(), "", resource.getOffset(), fakeLength, fakeDecompLength, exporter);
      
      exporter.open(fakeResource);
      for (int i = 0; i < maxHeaderSize; i++) {
        if (exporter.available()) {
          headerBytes[i] = (byte) exporter.read();
        }
        else {
          break;
        }
      }
      exporter.close();
      
      FileManipulator fm = new FileManipulator(new ByteBuffer(headerBytes));
      */

      ExporterByteBuffer byteBuffer = new ExporterByteBuffer(resource);
      FileManipulator fm = new FileManipulator(byteBuffer);

      // 4 - Unreal Header (193,131,42,158)
      // 4 - Version (6) (XOR with 255)
      // 16 - null
      // 4 - File Directory Offset?
      // 4 - Unknown (5)
      // 4 - Package Name (None)
      // 4 - null
      // 1 - Unknown (128)
      fm.skip(41);

      // 4 - Number of Names
      int nameCount = fm.readInt();
      FieldValidator.checkNumFiles(nameCount);

      // 4 - Name Directory Offset
      long nameDirOffset = IntConverter.unsign(fm.readInt());
      FieldValidator.checkOffset(nameDirOffset, arcSize);

      // 8 - null
      fm.skip(8);

      // 4 - Number Of Exports
      int exportCount = fm.readInt();
      FieldValidator.checkNumFiles(exportCount);

      // 4 - Exports Directory Offset
      long exportDirOffset = IntConverter.unsign(fm.readInt());
      FieldValidator.checkOffset(exportDirOffset, arcSize);

      // 4 - Number Of Imports
      int importCount = fm.readInt();
      FieldValidator.checkNumFiles(importCount);

      // 4 - Import Directory Offset
      long importDirOffset = IntConverter.unsign(fm.readInt());
      FieldValidator.checkOffset(importDirOffset, arcSize);

      // 16 - null
      // 4 - [optional] null
      // 16 - GUID Hash
      if (importDirOffset == 0) {
        // that skipped 8 "null" bytes probably wasn't in this archive, so correct the import details
        importCount = exportCount;
        importDirOffset = exportDirOffset;
        fm.skip(32 - 8);
      }
      else {
        fm.skip(32);
      }

      // 4 - Unknown (1)
      if (fm.readInt() != 1) { // this is to skip the OPTIONAL 4 bytes in MOST circumstances
        fm.skip(4);
      }

      // 4 - Unknown (1/2)
      // 4 - Unknown (Number of Names - again?)
      // 36 - null
      // 4 - Unknown
      // 4 - null
      // 4 - Padding Offset
      // 4 - File Length [+4] (not always - sometimes an unknown length/offset)
      // 8 - null
      fm.skip(68);

      // 4 - Number of ???
      int numToSkip = fm.readInt();
      if (numToSkip > 0 && numToSkip < 10) {
        // 4 - Unknown
        fm.skip(numToSkip * 4);
      }

      // 4 - Unknown (-1)
      fm.skip(4);

      // 4 - Files Data Offset
      long filesDirOffset = IntConverter.unsign(fm.readInt());
      FieldValidator.checkOffset(filesDirOffset, arcSize + 1);

      // Read the Names Directory
      fm.relativeSeek(nameDirOffset); // VERY IMPORTANT (because seek() doesn't allow going backwards in ExporterByteBuffer)
      UE4Helper.readNamesDirectory(fm, nameCount);

      // Read the Import Directory
      fm.relativeSeek(importDirOffset); // VERY IMPORTANT (because seek() doesn't allow going backwards in ExporterByteBuffer)
      UnrealImportEntry[] imports = UE4Helper.readImportDirectory(fm, importCount);

      int numFiles = importCount;

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        UnrealImportEntry entry = imports[i];

        if (entry.getType().equals("Class")) {
          fm.close();

          // put the original exporter back
          resource.setExporter(originalExporter);

          return entry.getName();
        }

      }

      fm.close();
    }
    catch (Throwable t) {
    }

    // put the original exporter back
    resource.setExporter(originalExporter);
    return null;

  }

}
