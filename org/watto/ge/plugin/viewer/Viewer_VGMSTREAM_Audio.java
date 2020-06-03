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

import java.io.File;
import org.watto.ErrorLogger;
import org.watto.Language;
import org.watto.Settings;
import org.watto.component.PreviewPanel;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************
Uses VGMSTREAM to convert various audio formats into generic WAV_RIFF, and then play it
**********************************************************************************************
**/
public class Viewer_VGMSTREAM_Audio extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_VGMSTREAM_Audio() {
    super("VGMSTREAM_Audio_WAV_RIFF", "vgmstream Audio Files");
    setExtensions("2dx9",
        "aaap",
        "aax",
        "acm",
        "adp",
        "adpcm",
        "ads",
        "adx",
        "afc",
        "agsc",
        "ahx",
        "aifc",
        "aiff",
        "aix",
        "as4",
        "asd",
        "asf",
        "asr",
        "ass",
        "ast",
        "at3",
        "aud",
        "aus",
        "baf",
        "baka",
        "bao",
        "bar",
        "bg00",
        "bgw",
        "bh2pcm",
        "bmdx",
        "bns",
        "bnsf",
        "bo2",
        "brstm",
        "caf",
        "capdsp",
        "ccc",
        "cfn",
        "cnk",
        "dcs",
        "ddsp",
        "de2",
        "dec",
        "dmsg",
        "dsp",
        "dvi",
        "dxh",
        "eam",
        "emff",
        "enth",
        "fag",
        "filp",
        "fsb",
        "gca",
        "gcm",
        "gcsw",
        "gcw",
        "gms",
        "gsp",
        "hca",
        "hgc1",
        "his",
        "hps",
        "hwas",
        "idsp",
        "idvi",
        "ikm",
        "ild",
        "int",
        "ish",
        "ivaud",
        "ivb",
        "joe",
        "kces",
        "kcey",
        "khv",
        "kraw",
        "leg",
        "lmp4",
        "logg",
        "lps",
        "lsf",
        "lstm",
        "lwav",
        "matx",
        "mc3",
        "mca",
        "mcg",
        "mi4",
        "mib",
        "mic",
        "mihb",
        "mp4",
        "mpdsp",
        "msa",
        "msf",
        "mss",
        "msvp",
        "mta2",
        "mtaf",
        "mus",
        "musc",
        "musx",
        "mwv",
        "myspd",
        "ndp",
        "npsf",
        "nwa",
        "ogg",
        "ogl",
        "p3d",
        "pcm",
        "pdt",
        "pk",
        "pnb",
        "psh",
        "psw",
        "raw",
        "rkv",
        "rnd",
        "rrds",
        "rsd",
        "rsf",
        "rstm",
        "rwar",
        "rwav",
        "rws",
        "rwsd",
        "rwx",
        "rxw",
        "s14",
        "sab",
        "sad",
        "sap",
        "sb0",
        "sb1",
        "sb2",
        "sb3",
        "sb4",
        "sb5",
        "sb6",
        "sb7",
        "sc",
        "scd",
        "sd9",
        "sdt",
        "seg",
        "sfs",
        "sgb",
        "sgd",
        "sgx",
        "sl3",
        "sm0",
        "sm1",
        "sm2",
        "sm3",
        "sm4",
        "sm5",
        "sm6",
        "sm7",
        "smp",
        "smpl",
        "snd",
        "sng",
        "sns",
        "sps",
        "spsd",
        "spt",
        "spw",
        "ss2",
        "ssm",
        "sss",
        "ster",
        "stm",
        "str",
        "strm",
        "sts",
        "stx",
        "svag",
        "svs",
        "swav",
        "swd",
        "tec",
        "thp",
        "tk5",
        "tydsp",
        "ulw",
        "um3",
        "vag",
        "vas",
        "vgs",
        "vig",
        "vjdsp",
        "voi",
        "vpk",
        "vs",
        "vsf",
        "waa",
        "wac",
        "wad",
        "wam",
        "was",
        "wav",
        "wavm",
        "wb",
        "wem",
        "wii",
        "wp2",
        "wsd",
        "wsi",
        "wvs",
        "xa",
        "xa2",
        "xa30",
        "xma",
        "xmu",
        "xss",
        "xvas",
        "xwav",
        "xwb",
        "ydsp",
        "ymf",
        "zsd",
        "zwdsp");
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
  Uses vgmstream to convert the audio file to generic WAV_RIFF
  **********************************************************************************************
  **/
  public File convertAudio(File source) {
    try {

      String commandPath = Settings.getString("vgmstream_Path");

      File commandFile = new File(commandPath);

      if (commandFile.exists() && commandFile.isDirectory()) {
        // Path is a directory, append the filename to it
        commandPath = commandPath + File.separatorChar + "vgmstream.exe";
        commandFile = new File(commandPath);
      }

      if (!commandFile.exists()) {
        // command path is invalid
        ErrorLogger.log("vgmstream can't be found at the path " + commandFile.getAbsolutePath());
        return null;
      }

      commandPath = commandFile.getAbsolutePath();

      String outputFilePath = source.getAbsolutePath() + ".conv.wav";
      if (new File(outputFilePath).exists()) {
        // already converted - previewed this file already
        return new File(outputFilePath);
      }

      String inputPath = source.getAbsolutePath();

      // first, see if we can read the metadata for the file, check that it's valid before trying to decode it
      ProcessBuilder pb = new ProcessBuilder(commandPath, "-m", inputPath);

      Process convertProcess = pb.start();
      int returnCode = convertProcess.waitFor(); // wait for the command to finish

      if (returnCode != 0) {
        // Nope, not a valid audio file (or some other error)
        return null;
      }

      // now try to do the actual conversion
      pb = new ProcessBuilder(commandPath, "-o", outputFilePath, inputPath);

      // Progress dialog
      TaskProgressManager.show(1, 0, Language.get("Progress_ConvertingFiles"));
      TaskProgressManager.setIndeterminate(true);

      // Start the task
      TaskProgressManager.startTask();

      convertProcess = pb.start();
      returnCode = convertProcess.waitFor(); // wait for the command to finish

      // Stop the task
      TaskProgressManager.stopTask();

      if (returnCode == 0) {
        // successful conversion
        File outputFile = new File(outputFilePath);
        if (outputFile.exists()) {
          return outputFile;
        }
      }

      return null;

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
  public int getMatchRating(FileManipulator fm) {
    try {

      int rating = 0;

      if (FieldValidator.checkExtension(fm, extensions)) {
        rating += 25;
      }

      if (fm.readString(4).equals("RIFF")) {
        rating += 24; // as 25 would cause an auto-match - don't want to use this unless the next header matches too!
      }

      fm.skip(16);

      if (fm.readShort() == -1) { // WWise Format
        rating += 50;
      }

      return rating;

    }
    catch (Throwable e) {
      return 0;
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public PreviewPanel read(File source) {

    File convertedFile = convertAudio(source);
    if (convertedFile == null || !convertedFile.exists()) {
      return null;
    }

    return new Viewer_WAV_RIFF().read(convertedFile);

  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public PreviewPanel read(FileManipulator fm) {
    return read(fm.getFile());
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void write(PreviewPanel preview, FileManipulator fm) {
  }

}
