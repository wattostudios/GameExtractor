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

package org.watto.ge.helper;

import org.watto.datatype.FileType;

/**
**********************************************************************************************
Helper methods for reading Unity3D Files
**********************************************************************************************
**/
public class Unity3DHelper {

  /**
   **********************************************************************************************
   Gets the File Extension for the File Type Code
   **********************************************************************************************
   **/
  public static String getFileExtension(int fileTypeCode) {
    if (fileTypeCode == 0) {
      return ".Object";
    }
    else if (fileTypeCode == 1) {
      return ".GameObject";
    }
    else if (fileTypeCode == 2) {
      return ".Component";
    }
    else if (fileTypeCode == 3) {
      return ".LevelGameManager";
    }
    else if (fileTypeCode == 4) {
      return ".Transform";
    }
    else if (fileTypeCode == 5) {
      return ".TimeManager";
    }
    else if (fileTypeCode == 6) {
      return ".GlobalGameManager";
    }
    else if (fileTypeCode == 7) {
      return ".GameManager";
    }
    else if (fileTypeCode == 8) {
      return ".Behaviour";
    }
    else if (fileTypeCode == 9) {
      return ".GameManager";
    }
    else if (fileTypeCode == 11) {
      return ".AudioManager";
    }
    else if (fileTypeCode == 12) {
      return ".ParticleAnimator";
    }
    else if (fileTypeCode == 13) {
      return ".InputManager";
    }
    else if (fileTypeCode == 15) {
      return ".EllipsoidParticleEmitter";
    }
    else if (fileTypeCode == 17) {
      return ".Pipeline";
    }
    else if (fileTypeCode == 18) {
      return ".EditorExtension";
    }
    else if (fileTypeCode == 19) {
      return ".Physics2DSettings";
    }
    else if (fileTypeCode == 20) {
      return ".Camera";
    }
    else if (fileTypeCode == 21) {
      return ".Material";
    }
    else if (fileTypeCode == 23) {
      return ".MeshRenderer";
    }
    else if (fileTypeCode == 25) {
      return ".Renderer";
    }
    else if (fileTypeCode == 26) {
      return ".ParticleRenderer";
    }
    else if (fileTypeCode == 27) {
      return ".Texture";
    }
    else if (fileTypeCode == 28) {
      return ".Texture2D";
    }
    else if (fileTypeCode == 29) {
      return ".SceneSettings";
    }
    else if (fileTypeCode == 30) {
      return ".GraphicsSettings";
    }
    else if (fileTypeCode == 31) {
      return ".PipelineManager";
    }
    else if (fileTypeCode == 33) {
      return ".MeshFilter";
    }
    else if (fileTypeCode == 35) {
      return ".GameManager";
    }
    else if (fileTypeCode == 41) {
      return ".OcclusionPortal";
    }
    else if (fileTypeCode == 43) {
      return ".Mesh";
    }
    else if (fileTypeCode == 45) {
      return ".Skybox";
    }
    else if (fileTypeCode == 46) {
      return ".GameManager";
    }
    else if (fileTypeCode == 47) {
      return ".QualitySettings";
    }
    else if (fileTypeCode == 48) {
      return ".Shader";
    }
    else if (fileTypeCode == 49) {
      return ".TextAsset";
    }
    else if (fileTypeCode == 50) {
      return ".Rigidbody2D";
    }
    else if (fileTypeCode == 51) {
      return ".Physics2DManager";
    }
    else if (fileTypeCode == 52) {
      return ".NotificationManager";
    }
    else if (fileTypeCode == 53) {
      return ".Collider2D";
    }
    else if (fileTypeCode == 54) {
      return ".Rigidbody";
    }
    else if (fileTypeCode == 55) {
      return ".PhysicsManager";
    }
    else if (fileTypeCode == 56) {
      return ".Collider";
    }
    else if (fileTypeCode == 57) {
      return ".Joint";
    }
    else if (fileTypeCode == 58) {
      return ".CircleCollider2D";
    }
    else if (fileTypeCode == 59) {
      return ".HingeJoint";
    }
    else if (fileTypeCode == 60) {
      return ".PolygonCollider2D";
    }
    else if (fileTypeCode == 61) {
      return ".BoxCollider2D";
    }
    else if (fileTypeCode == 62) {
      return ".PhysicsMaterial2D";
    }
    else if (fileTypeCode == 63) {
      return ".GameManager";
    }
    else if (fileTypeCode == 64) {
      return ".MeshCollider";
    }
    else if (fileTypeCode == 65) {
      return ".BoxCollider";
    }
    else if (fileTypeCode == 66) {
      return ".SpriteCollider2D";
    }
    else if (fileTypeCode == 68) {
      return ".EdgeCollider2D";
    }
    else if (fileTypeCode == 70) {
      return ".CapsuleCollider2D";
    }
    else if (fileTypeCode == 71) {
      return ".AnimationManager";
    }
    else if (fileTypeCode == 72) {
      return ".ComputeShader";
    }
    else if (fileTypeCode == 74) {
      return ".AnimationClip";
    }
    else if (fileTypeCode == 75) {
      return ".ConstantForce";
    }
    else if (fileTypeCode == 76) {
      return ".WorldParticleCollider";
    }
    else if (fileTypeCode == 78) {
      return ".TagManager";
    }
    else if (fileTypeCode == 81) {
      return ".AudioListener";
    }
    else if (fileTypeCode == 82) {
      return ".AudioSource";
    }
    else if (fileTypeCode == 83) {
      return ".AudioClip";
    }
    else if (fileTypeCode == 84) {
      return ".RenderTexture";
    }
    else if (fileTypeCode == 86) {
      return ".CustomRenderTexture";
    }
    else if (fileTypeCode == 87) {
      return ".MeshParticleEmitter";
    }
    else if (fileTypeCode == 88) {
      return ".ParticleEmitter";
    }
    else if (fileTypeCode == 89) {
      return ".Cubemap";
    }
    else if (fileTypeCode == 90) {
      return ".Avatar";
    }
    else if (fileTypeCode == 91) {
      return ".AnimatorController";
    }
    else if (fileTypeCode == 92) {
      return ".GUILayer";
    }
    else if (fileTypeCode == 93) {
      return ".RuntimeAnimatorController";
    }
    else if (fileTypeCode == 94) {
      return ".ScriptMapper";
    }
    else if (fileTypeCode == 95) {
      return ".Animator";
    }
    else if (fileTypeCode == 96) {
      return ".TrailRenderer";
    }
    else if (fileTypeCode == 98) {
      return ".DelayedCallManager";
    }
    else if (fileTypeCode == 102) {
      return ".TextMesh";
    }
    else if (fileTypeCode == 104) {
      return ".RenderSettings";
    }
    else if (fileTypeCode == 108) {
      return ".Light";
    }
    else if (fileTypeCode == 109) {
      return ".CGProgram";
    }
    else if (fileTypeCode == 110) {
      return ".BaseAnimationTrack";
    }
    else if (fileTypeCode == 111) {
      return ".Animation";
    }
    else if (fileTypeCode == 114) {
      return ".MonoBehaviour";
    }
    else if (fileTypeCode == 115) {
      return ".MonoScript";
    }
    else if (fileTypeCode == 116) {
      return ".MonoManager";
    }
    else if (fileTypeCode == 117) {
      return ".Texture3D";
    }
    else if (fileTypeCode == 118) {
      return ".NewAnimationTrack";
    }
    else if (fileTypeCode == 119) {
      return ".Projector";
    }
    else if (fileTypeCode == 120) {
      return ".LineRenderer";
    }
    else if (fileTypeCode == 121) {
      return ".Flare";
    }
    else if (fileTypeCode == 122) {
      return ".Halo";
    }
    else if (fileTypeCode == 123) {
      return ".LensFlare";
    }
    else if (fileTypeCode == 124) {
      return ".FlareLayer";
    }
    else if (fileTypeCode == 125) {
      return ".HaloLayer";
    }
    else if (fileTypeCode == 126) {
      return ".NavMeshAreas";
    }
    else if (fileTypeCode == 127) {
      return ".HaloManager";
    }
    else if (fileTypeCode == 128) {
      return ".Font";
    }
    else if (fileTypeCode == 129) {
      return ".PlayerSettings";
    }
    else if (fileTypeCode == 130) {
      return ".NamedObject";
    }
    else if (fileTypeCode == 131) {
      return ".GUITexture";
    }
    else if (fileTypeCode == 132) {
      return ".GUIText";
    }
    else if (fileTypeCode == 133) {
      return ".GUIElement";
    }
    else if (fileTypeCode == 134) {
      return ".PhysicMaterial";
    }
    else if (fileTypeCode == 135) {
      return ".SphereCollider";
    }
    else if (fileTypeCode == 136) {
      return ".CapsuleCollider";
    }
    else if (fileTypeCode == 137) {
      return ".SkinnedMeshRenderer";
    }
    else if (fileTypeCode == 138) {
      return ".FixedJoint";
    }
    else if (fileTypeCode == 140) {
      return ".RaycastCollider";
    }
    else if (fileTypeCode == 141) {
      return ".BuildSettings";
    }
    else if (fileTypeCode == 142) {
      return ".AssetBundle";
    }
    else if (fileTypeCode == 143) {
      return ".CharacterController";
    }
    else if (fileTypeCode == 144) {
      return ".CharacterJoint";
    }
    else if (fileTypeCode == 145) {
      return ".SpringJoint";
    }
    else if (fileTypeCode == 146) {
      return ".WheelCollider";
    }
    else if (fileTypeCode == 147) {
      return ".ResourceManager";
    }
    else if (fileTypeCode == 148) {
      return ".NetworkView";
    }
    else if (fileTypeCode == 149) {
      return ".NetworkManager";
    }
    else if (fileTypeCode == 150) {
      return ".PreloadData";
    }
    else if (fileTypeCode == 152) {
      return ".MovieTexture";
    }
    else if (fileTypeCode == 153) {
      return ".ConfigurableJoint";
    }
    else if (fileTypeCode == 154) {
      return ".TerrainCollider";
    }
    else if (fileTypeCode == 155) {
      return ".MasterServerInterface";
    }
    else if (fileTypeCode == 156) {
      return ".TerrainData";
    }
    else if (fileTypeCode == 157) {
      return ".LightmapSettings";
    }
    else if (fileTypeCode == 158) {
      return ".WebCamTexture";
    }
    else if (fileTypeCode == 159) {
      return ".EditorSettings";
    }
    else if (fileTypeCode == 160) {
      return ".InteractiveCloth";
    }
    else if (fileTypeCode == 161) {
      return ".ClothRenderer";
    }
    else if (fileTypeCode == 162) {
      return ".EditorUserSettings";
    }
    else if (fileTypeCode == 163) {
      return ".SkinnedCloth";
    }
    else if (fileTypeCode == 164) {
      return ".AudioReverbFilter";
    }
    else if (fileTypeCode == 165) {
      return ".AudioHighPassFilter";
    }
    else if (fileTypeCode == 166) {
      return ".AudioChorusFilter";
    }
    else if (fileTypeCode == 167) {
      return ".AudioReverbZone";
    }
    else if (fileTypeCode == 168) {
      return ".AudioEchoFilter";
    }
    else if (fileTypeCode == 169) {
      return ".AudioLowPassFilter";
    }
    else if (fileTypeCode == 170) {
      return ".AudioDistortionFilter";
    }
    else if (fileTypeCode == 171) {
      return ".SparseTexture";
    }
    else if (fileTypeCode == 180) {
      return ".AudioBehaviour";
    }
    else if (fileTypeCode == 181) {
      return ".AudioFilter";
    }
    else if (fileTypeCode == 182) {
      return ".WindZone";
    }
    else if (fileTypeCode == 183) {
      return ".Cloth";
    }
    else if (fileTypeCode == 184) {
      return ".SubstanceArchive";
    }
    else if (fileTypeCode == 185) {
      return ".ProceduralMaterial";
    }
    else if (fileTypeCode == 186) {
      return ".ProceduralTexture";
    }
    else if (fileTypeCode == 187) {
      return ".Texture2DArray";
    }
    else if (fileTypeCode == 188) {
      return ".CubemapArray";
    }
    else if (fileTypeCode == 191) {
      return ".OffMeshLink";
    }
    else if (fileTypeCode == 192) {
      return ".OcclusionArea";
    }
    else if (fileTypeCode == 193) {
      return ".Tree";
    }
    else if (fileTypeCode == 194) {
      return ".NavMeshObsolete";
    }
    else if (fileTypeCode == 195) {
      return ".NavMeshAgent";
    }
    else if (fileTypeCode == 196) {
      return ".NavMeshSettings";
    }
    else if (fileTypeCode == 197) {
      return ".LightProbesLegacy";
    }
    else if (fileTypeCode == 198) {
      return ".ParticleSystem";
    }
    else if (fileTypeCode == 199) {
      return ".ParticleSystemRenderer";
    }
    else if (fileTypeCode == 200) {
      return ".ShaderVariantCollection";
    }
    else if (fileTypeCode == 205) {
      return ".LODGroup";
    }
    else if (fileTypeCode == 206) {
      return ".BlendTree";
    }
    else if (fileTypeCode == 207) {
      return ".Motion";
    }
    else if (fileTypeCode == 208) {
      return ".NavMeshObstacle";
    }
    else if (fileTypeCode == 210) {
      return ".TerrainInstance";
    }
    else if (fileTypeCode == 212) {
      return ".SpriteRenderer";
    }
    else if (fileTypeCode == 213) {
      return ".Sprite";
    }
    else if (fileTypeCode == 214) {
      return ".CachedSpriteAtlas";
    }
    else if (fileTypeCode == 215) {
      return ".ReflectionProbe";
    }
    else if (fileTypeCode == 216) {
      return ".ReflectionProbes";
    }
    else if (fileTypeCode == 218) {
      return ".Terrain";
    }
    else if (fileTypeCode == 220) {
      return ".LightProbeGroup";
    }
    else if (fileTypeCode == 221) {
      return ".AnimatorOverrideController";
    }
    else if (fileTypeCode == 222) {
      return ".CanvasRenderer";
    }
    else if (fileTypeCode == 223) {
      return ".Canvas";
    }
    else if (fileTypeCode == 224) {
      return ".RectTransform";
    }
    else if (fileTypeCode == 225) {
      return ".CanvasGroup";
    }
    else if (fileTypeCode == 226) {
      return ".BillboardAsset";
    }
    else if (fileTypeCode == 227) {
      return ".BillboardRenderer";
    }
    else if (fileTypeCode == 228) {
      return ".SpeedTreeWindAsset";
    }
    else if (fileTypeCode == 229) {
      return ".AnchoredJoint2D";
    }
    else if (fileTypeCode == 230) {
      return ".Joint2D";
    }
    else if (fileTypeCode == 231) {
      return ".SpringJoint2D";
    }
    else if (fileTypeCode == 232) {
      return ".DistanceJoint2D";
    }
    else if (fileTypeCode == 233) {
      return ".HingeJoint2D";
    }
    else if (fileTypeCode == 234) {
      return ".SliderJoint2D";
    }
    else if (fileTypeCode == 235) {
      return ".WheelJoint2D";
    }
    else if (fileTypeCode == 236) {
      return ".ClusterInputManager";
    }
    else if (fileTypeCode == 237) {
      return ".BaseVideoTexture";
    }
    else if (fileTypeCode == 238) {
      return ".NavMeshData";
    }
    else if (fileTypeCode == 240) {
      return ".AudioMixer";
    }
    else if (fileTypeCode == 241) {
      return ".AudioMixerController";
    }
    else if (fileTypeCode == 243) {
      return ".AudioMixerGroupController";
    }
    else if (fileTypeCode == 244) {
      return ".AudioMixerEffectController";
    }
    else if (fileTypeCode == 245) {
      return ".AudioMixerSnapshotController";
    }
    else if (fileTypeCode == 246) {
      return ".PhysicsUpdateBehaviour2D";
    }
    else if (fileTypeCode == 247) {
      return ".ConstantForce2D";
    }
    else if (fileTypeCode == 248) {
      return ".Effector2D";
    }
    else if (fileTypeCode == 249) {
      return ".AreaEffector2D";
    }
    else if (fileTypeCode == 250) {
      return ".PointEffector2D";
    }
    else if (fileTypeCode == 251) {
      return ".PlatformEffector2D";
    }
    else if (fileTypeCode == 252) {
      return ".SurfaceEffector2D";
    }
    else if (fileTypeCode == 253) {
      return ".BuoyancyEffector2D";
    }
    else if (fileTypeCode == 254) {
      return ".RelativeJoint2D";
    }
    else if (fileTypeCode == 255) {
      return ".FixedJoint2D";
    }
    else if (fileTypeCode == 256) {
      return ".FrictionJoint2D";
    }
    else if (fileTypeCode == 257) {
      return ".TargetJoint2D";
    }
    else if (fileTypeCode == 258) {
      return ".LightProbes";
    }
    else if (fileTypeCode == 259) {
      return ".LightProbeProxyVolume";
    }
    else if (fileTypeCode == 271) {
      return ".SampleClip";
    }
    else if (fileTypeCode == 272) {
      return ".AudioMixerSnapshot";
    }
    else if (fileTypeCode == 273) {
      return ".AudioMixerGroup";
    }
    else if (fileTypeCode == 290) {
      return ".AssetBundleManifest";
    }
    else if (fileTypeCode == 300) {
      return ".RuntimeInitializeOnLoadManager";
    }
    else if (fileTypeCode == 310) {
      return ".UnityConnectSettings";
    }
    else if (fileTypeCode == 319) {
      return ".AvatarMask";
    }
    else if (fileTypeCode == 320) {
      return ".PlayableDirector";
    }
    else if (fileTypeCode == 328) {
      return ".VideoPlayer";
    }
    else if (fileTypeCode == 329) {
      return ".VideoClip";
    }
    else if (fileTypeCode == 330) {
      return ".ParticleSystemForceField";
    }
    else if (fileTypeCode == 331) {
      return ".SpriteMask";
    }
    else if (fileTypeCode == 362) {
      return ".WorldAnchor";
    }
    else if (fileTypeCode == 363) {
      return ".OcclusionCullingData";
    }
    else if (fileTypeCode == 1001) {
      return ".Prefab";
    }
    else if (fileTypeCode == 1002) {
      return ".EditorExtensionImpl";
    }
    else if (fileTypeCode == 1003) {
      return ".AssetImporter";
    }
    else if (fileTypeCode == 1004) {
      return ".AssetDatabase";
    }
    else if (fileTypeCode == 1005) {
      return ".Mesh3DSImporter";
    }
    else if (fileTypeCode == 1006) {
      return ".TextureImporter";
    }
    else if (fileTypeCode == 1007) {
      return ".ShaderImporter";
    }
    else if (fileTypeCode == 1008) {
      return ".ComputeShaderImporter";
    }
    else if (fileTypeCode == 1011) {
      return ".AvatarMask";
    }
    else if (fileTypeCode == 1020) {
      return ".AudioImporter";
    }
    else if (fileTypeCode == 1026) {
      return ".HierarchyState";
    }
    else if (fileTypeCode == 1027) {
      return ".GUIDSerializer";
    }
    else if (fileTypeCode == 1028) {
      return ".AssetMetaData";
    }
    else if (fileTypeCode == 1029) {
      return ".DefaultAsset";
    }
    else if (fileTypeCode == 1030) {
      return ".DefaultImporter";
    }
    else if (fileTypeCode == 1031) {
      return ".TextScriptImporter";
    }
    else if (fileTypeCode == 1032) {
      return ".SceneAsset";
    }
    else if (fileTypeCode == 1034) {
      return ".NativeFormatImporter";
    }
    else if (fileTypeCode == 1035) {
      return ".MonoImporter";
    }
    else if (fileTypeCode == 1037) {
      return ".AssetServerCache";
    }
    else if (fileTypeCode == 1038) {
      return ".LibraryAssetImporter";
    }
    else if (fileTypeCode == 1040) {
      return ".ModelImporter";
    }
    else if (fileTypeCode == 1041) {
      return ".FBXImporter";
    }
    else if (fileTypeCode == 1042) {
      return ".TrueTypeFontImporter";
    }
    else if (fileTypeCode == 1044) {
      return ".MovieImporter";
    }
    else if (fileTypeCode == 1045) {
      return ".EditorBuildSettings";
    }
    else if (fileTypeCode == 1046) {
      return ".DDSImporter";
    }
    else if (fileTypeCode == 1048) {
      return ".InspectorExpandedState";
    }
    else if (fileTypeCode == 1049) {
      return ".AnnotationManager";
    }
    else if (fileTypeCode == 1050) {
      return ".PluginImporter";
    }
    else if (fileTypeCode == 1051) {
      return ".EditorUserBuildSettings";
    }
    else if (fileTypeCode == 1052) {
      return ".PVRImporter";
    }
    else if (fileTypeCode == 1053) {
      return ".ASTCImporter";
    }
    else if (fileTypeCode == 1054) {
      return ".KTXImporter";
    }
    else if (fileTypeCode == 1055) {
      return ".IHVImageFormatImporter";
    }
    else if (fileTypeCode == 1101) {
      return ".AnimatorStateTransition";
    }
    else if (fileTypeCode == 1102) {
      return ".AnimatorState";
    }
    else if (fileTypeCode == 1105) {
      return ".HumanTemplate";
    }
    else if (fileTypeCode == 1107) {
      return ".AnimatorStateMachine";
    }
    else if (fileTypeCode == 1108) {
      return ".PreviewAssetType";
    }
    else if (fileTypeCode == 1109) {
      return ".AnimatorTransition";
    }
    else if (fileTypeCode == 1110) {
      return ".SpeedTreeImporter";
    }
    else if (fileTypeCode == 1111) {
      return ".AnimatorTransitionBase";
    }
    else if (fileTypeCode == 1112) {
      return ".SubstanceImporter";
    }
    else if (fileTypeCode == 1113) {
      return ".LightmapParameters";
    }
    else if (fileTypeCode == 1120) {
      return ".LightmapSnapshot";
    }
    else if (fileTypeCode == 1124) {
      return ".SketchUpImporter";
    }
    else if (fileTypeCode == 1125) {
      return ".BuildReport";
    }
    else if (fileTypeCode == 1126) {
      return ".PackedAssets";
    }
    else if (fileTypeCode == 1127) {
      return ".VideoClipImporter";
    }
    else if (fileTypeCode == 100000) {
      return ".int";
    }
    else if (fileTypeCode == 100001) {
      return ".bool";
    }
    else if (fileTypeCode == 100002) {
      return ".float";
    }
    else if (fileTypeCode == 100003) {
      return ".MonoObject";
    }
    else if (fileTypeCode == 100004) {
      return ".Collision";
    }
    else if (fileTypeCode == 100005) {
      return ".Vector3f";
    }
    else if (fileTypeCode == 100006) {
      return ".RootMotionData";
    }
    else if (fileTypeCode == 100007) {
      return ".Collision2D";
    }
    else if (fileTypeCode == 100008) {
      return ".AudioMixerLiveUpdateFloat";
    }
    else if (fileTypeCode == 100009) {
      return ".AudioMixerLiveUpdateBool";
    }
    else if (fileTypeCode == 100010) {
      return ".Polygon2D";
    }
    else if (fileTypeCode == 100011) {
      return ".void";
    }
    else if (fileTypeCode == 19719996) {
      return ".TilemapCollider2D";
    }
    else if (fileTypeCode == 41386430) {
      return ".AssetImporterLog";
    }
    else if (fileTypeCode == 73398921) {
      return ".VFXRenderer";
    }
    else if (fileTypeCode == 76251197) {
      return ".SerializableManagedRefTestClass";
    }
    else if (fileTypeCode == 156049354) {
      return ".Grid";
    }
    else if (fileTypeCode == 181963792) {
      return ".Preset";
    }
    else if (fileTypeCode == 277625683) {
      return ".EmptyObject";
    }
    else if (fileTypeCode == 285090594) {
      return ".IConstraint";
    }
    else if (fileTypeCode == 293259124) {
      return ".TestObjectWithSpecialLayoutOne";
    }
    else if (fileTypeCode == 294290339) {
      return ".AssemblyDefinitionReferenceImporter";
    }
    else if (fileTypeCode == 334799969) {
      return ".SiblingDerived";
    }
    else if (fileTypeCode == 342846651) {
      return ".TestObjectWithSerializedMapStringNonAlignedStruct";
    }
    else if (fileTypeCode == 367388927) {
      return ".SubDerived";
    }
    else if (fileTypeCode == 369655926) {
      return ".AssetImportInProgressProxy";
    }
    else if (fileTypeCode == 382020655) {
      return ".PluginBuildInfo";
    }
    else if (fileTypeCode == 426301858) {
      return ".EditorProjectAccess";
    }
    else if (fileTypeCode == 468431735) {
      return ".PrefabImporter";
    }
    else if (fileTypeCode == 478637458) {
      return ".TestObjectWithSerializedArray";
    }
    else if (fileTypeCode == 478637459) {
      return ".TestObjectWithSerializedAnimationCurve";
    }
    else if (fileTypeCode == 483693784) {
      return ".TilemapRenderer";
    }
    else if (fileTypeCode == 638013454) {
      return ".SpriteAtlasDatabase";
    }
    else if (fileTypeCode == 641289076) {
      return ".AudioBuildInfo";
    }
    else if (fileTypeCode == 644342135) {
      return ".CachedSpriteAtlasRuntimeData";
    }
    else if (fileTypeCode == 646504946) {
      return ".RendererFake";
    }
    else if (fileTypeCode == 662584278) {
      return ".AssemblyDefinitionReferenceAsset";
    }
    else if (fileTypeCode == 668709126) {
      return ".BuiltAssetBundleInfoSet";
    }
    else if (fileTypeCode == 687078895) {
      return ".SpriteAtlas";
    }
    else if (fileTypeCode == 747330370) {
      return ".RayTracingShaderImporter";
    }
    else if (fileTypeCode == 825902497) {
      return ".RayTracingShader";
    }
    else if (fileTypeCode == 877146078) {
      return ".PlatformModuleSetup";
    }
    else if (fileTypeCode == 895512359) {
      return ".AimConstraint";
    }
    else if (fileTypeCode == 937362698) {
      return ".VFXManager";
    }
    else if (fileTypeCode == 994735392) {
      return ".VisualEffectSubgraph";
    }
    else if (fileTypeCode == 994735403) {
      return ".VisualEffectSubgraphOperator";
    }
    else if (fileTypeCode == 994735404) {
      return ".VisualEffectSubgraphBlock";
    }
    else if (fileTypeCode == 1001480554) {
      return ".Prefab";
    }
    else if (fileTypeCode == 1027052791) {
      return ".LocalizationImporter";
    }
    else if (fileTypeCode == 1091556383) {
      return ".Derived";
    }
    else if (fileTypeCode == 1111377672) {
      return ".PropertyModificationsTargetTestObject";
    }
    else if (fileTypeCode == 1114811875) {
      return ".ReferencesArtifactGenerator";
    }
    else if (fileTypeCode == 1152215463) {
      return ".AssemblyDefinitionAsset";
    }
    else if (fileTypeCode == 1154873562) {
      return ".SceneVisibilityState";
    }
    else if (fileTypeCode == 1183024399) {
      return ".LookAtConstraint";
    }
    else if (fileTypeCode == 1223240404) {
      return ".MultiArtifactTestImporter";
    }
    else if (fileTypeCode == 1268269756) {
      return ".GameObjectRecorder";
    }
    else if (fileTypeCode == 1325145578) {
      return ".LightingDataAssetParent";
    }
    else if (fileTypeCode == 1386491679) {
      return ".PresetManager";
    }
    else if (fileTypeCode == 1392443030) {
      return ".TestObjectWithSpecialLayoutTwo";
    }
    else if (fileTypeCode == 1403656975) {
      return ".StreamingManager";
    }
    else if (fileTypeCode == 1480428607) {
      return ".LowerResBlitTexture";
    }
    else if (fileTypeCode == 1542919678) {
      return ".StreamingController";
    }
    else if (fileTypeCode == 1628831178) {
      return ".TestObjectVectorPairStringBool";
    }
    else if (fileTypeCode == 1742807556) {
      return ".GridLayout";
    }
    else if (fileTypeCode == 1766753193) {
      return ".AssemblyDefinitionImporter";
    }
    else if (fileTypeCode == 1773428102) {
      return ".ParentConstraint";
    }
    else if (fileTypeCode == 1803986026) {
      return ".FakeComponent";
    }
    else if (fileTypeCode == 1818360608) {
      return ".PositionConstraint";
    }
    else if (fileTypeCode == 1818360609) {
      return ".RotationConstraint";
    }
    else if (fileTypeCode == 1818360610) {
      return ".ScaleConstraint";
    }
    else if (fileTypeCode == 1839735485) {
      return ".Tilemap";
    }
    else if (fileTypeCode == 1896753125) {
      return ".PackageManifest";
    }
    else if (fileTypeCode == 1896753126) {
      return ".PackageManifestImporter";
    }
    else if (fileTypeCode == 1953259897) {
      return ".TerrainLayer";
    }
    else if (fileTypeCode == 1971053207) {
      return ".SpriteShapeRenderer";
    }
    else if (fileTypeCode == 1977754360) {
      return ".NativeObjectType";
    }
    else if (fileTypeCode == 1981279845) {
      return ".TestObjectWithSerializedMapStringBool";
    }
    else if (fileTypeCode == 1995898324) {
      return ".SerializableManagedHost";
    }
    else if (fileTypeCode == 2058629509) {
      return ".VisualEffectAsset";
    }
    else if (fileTypeCode == 2058629510) {
      return ".VisualEffectImporter";
    }
    else if (fileTypeCode == 2058629511) {
      return ".VisualEffectResource";
    }
    else if (fileTypeCode == 2059678085) {
      return ".VisualEffectObject";
    }
    else if (fileTypeCode == 2083052967) {
      return ".VisualEffect";
    }
    else if (fileTypeCode == 2083778819) {
      return ".LocalizationAsset";
    }
    else if (fileTypeCode == 2089858483) {
      return ".ScriptedImporter";
    }

    return "." + fileTypeCode;
  }

  /**
   **********************************************************************************************
   Gets the list of FileTypes that are loaded into the plugin
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
        new FileType("scriptedimporter", "Scripted Importer", FileType.TYPE_OTHER)
    };

    return types;
  }

}