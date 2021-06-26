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

package org.watto.task;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import org.watto.ErrorLogger;
import org.watto.Language;
import com.jcraft.jogg.Packet;
import com.jcraft.jogg.Page;
import com.jcraft.jogg.StreamState;
import com.jcraft.jogg.SyncState;
import com.jcraft.jorbis.Block;
import com.jcraft.jorbis.Comment;
import com.jcraft.jorbis.DspState;
import com.jcraft.jorbis.Info;

/**
 **********************************************************************************************
 * Play OGG Vorbis audio
 **********************************************************************************************
 **/
public class Task_PlayAudio_OggVorbis extends AbstractTask {

  static final int BUFSIZE = 4096 * 2;

  static SourceDataLine outputLine = null;

  /** The direction to perform in the thread **/
  int direction = 1;

  InputStream bitStream = null;
  AudioFormat audioFormat;
  int convsize = BUFSIZE * 2;

  byte[] convbuffer = new byte[convsize];
  private int RETRY = 3;

  int retry = RETRY;
  SyncState oy = new SyncState();
  StreamState os = new StreamState();
  Page og = new Page();

  Packet op = new Packet();
  Info vi = new Info();
  Comment vc = new Comment();
  DspState vd = new DspState();

  Block vb = new Block(vd);

  DataLine.Info info;
  byte[] buffer = null;

  int bytes = 0;
  int rate = 0;
  int channels = 0;

  int frameSizeInBytes;
  int bufferLengthInBytes;
  int bufferLengthInFrames;

  File path;

  boolean stopAudio = false;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Task_PlayAudio_OggVorbis(File path) {
    this.path = path;
    this.stopAudio = false;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public SourceDataLine getOutputLine(int channels, int rate) {
    if (outputLine != null || this.rate != rate || this.channels != channels) {
      if (outputLine != null) {
        outputLine.drain();
        outputLine.stop();
        outputLine.close();
      }

      audioFormat = new AudioFormat(rate, 16, channels, true, false);
      info = new DataLine.Info(SourceDataLine.class, audioFormat, AudioSystem.NOT_SPECIFIED);
      if (!AudioSystem.isLineSupported(info)) {
        //System.out.println("Line " + info + " not supported.");
        return null;
      }
      try {
        outputLine = (SourceDataLine) AudioSystem.getLine(info);
        //outputLine.addLineListener(this);
        outputLine.open(audioFormat);
      }
      catch (LineUnavailableException e) {
        System.out.println("Unable to open the sourceDataLine: " + e);
        return null;
      }
      catch (IllegalArgumentException e) {
        System.out.println("Illegal Argument: " + e);
        return null;
      }

      frameSizeInBytes = audioFormat.getFrameSize();
      bufferLengthInFrames = outputLine.getBufferSize() / frameSizeInBytes / 2;
      bufferLengthInBytes = bufferLengthInFrames * frameSizeInBytes;

      this.rate = rate;
      this.channels = channels;

      outputLine.start();

    }
    return outputLine;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @SuppressWarnings("unused")
  public void playAudio() {
    stopAudio = false;

    try {
      //Thread me = Thread.currentThread();

      //URL url = new URL(file.getAbsolutePath());
      //URLConnection urlc = url.openConnection();
      //bitStream = urlc.getInputStream();

      if (bitStream != null) {
        bitStream.close();
      }
      bitStream = new FileInputStream(path);
      //bitStream = new AudioInputStream(new FileInputStream(file),audioFormat,bufferLengthInFrames);
      //bitStream = AudioSystem.getAudioInputStream(file);
    }
    catch (Exception e) {
      ErrorLogger.log(e);
      return;
    }

    oy.init();

    retry = RETRY;

    loop: while (true) {
      int eos = 0;

      int index = oy.buffer(BUFSIZE);
      buffer = oy.data;

      try {
        bytes = bitStream.read(buffer, index, BUFSIZE);
      }
      catch (Exception e) {
        //ErrorLogger.log(e);
        return;
      }
      oy.wrote(bytes);

      if (oy.pageout(og) != 1) {
        if (bytes < BUFSIZE) {
          break;
        }
        //System.out.println("Input does not appear to be an Ogg bitstream.");
        return;
      }

      os.init(og.serialno());
      os.reset();

      vi.init();
      vc.init();

      if (os.pagein(og) < 0) {
        // error; stream version mismatch perhaps
        //System.out.println("Error reading first page of Ogg bitstream data.");
        return;
      }

      retry = RETRY;

      if (os.packetout(op) != 1) {
        // no page? must not be vorbis
        //System.out.println("Error reading initial header packet.");
        break;
        //      return;
      }

      if (vi.synthesis_headerin(vc, op) < 0) {
        // error case; not a vorbis header
        //System.out.println("This Ogg bitstream does not contain Vorbis audio data.");
        return;
      }

      int i = 0;

      while (i < 2) {
        while (i < 2) {
          int result = oy.pageout(og);
          if (result == 0) {
            break; // Need more data
          }
          if (result == 1) {
            os.pagein(og);
            while (i < 2) {
              result = os.packetout(op);
              if (result == 0) {
                break;
              }
              if (result == -1) {
                //System.out.println("Corrupt secondary header.  Exiting.");
                //return;
                break loop;
              }
              vi.synthesis_headerin(vc, op);
              i++;
            }
          }
        }

        index = oy.buffer(BUFSIZE);
        buffer = oy.data;

        try {
          bytes = bitStream.read(buffer, index, BUFSIZE);
        }
        catch (Exception e) {
          ErrorLogger.log(e);
          return;
        }

        if (bytes == 0 && i < 2) {
          //System.out.println("End of file before finding all Vorbis headers!");
          return;
        }

        oy.wrote(bytes);

      }

      {
        byte[][] ptr = vc.user_comments;
        StringBuffer sb = null;

        for (int j = 0; j < ptr.length; j++) {
          if (ptr[j] == null) {
            break;
          }
          //System.out.println("Comment: "+new String(ptr[j], 0, ptr[j].length-1));
          if (sb != null) {
            sb.append(" " + new String(ptr[j], 0, ptr[j].length - 1));
          }
        }
        //System.out.println("Bitstream is "+vi.channels+" channel, "+vi.rate+"Hz");
        //System.out.println("Encoded by: "+new String(vc.vendor, 0, vc.vendor.length-1)+"\n");
      }

      convsize = BUFSIZE / vi.channels;

      vd.synthesis_init(vi);
      vb.init(vd);

      double[][][] _pcm = new double[1][][];
      float[][][] _pcmf = new float[1][][];
      int[] _index = new int[vi.channels];

      getOutputLine(vi.channels, vi.rate);

      while (eos == 0) {
        while (eos == 0) {

          //if(playThread!= me){
          try {
            // THIS WAS CLOSING THE INPUT TOO SOON!
            //bitStream.close();
          }
          catch (Exception ee) {
          }
          //  return;
          //  }

          int result = oy.pageout(og);
          if (result == 0) {
            break; // need more data
          }
          if (result == -1) { // missing or corrupt data at this page position
          }
          else {
            os.pagein(og);
            while (true) {
              result = os.packetout(op);
              if (result == 0) {
                break; // need more data
              }
              if (result == -1) { // missing or corrupt data at this page position
              }
              else {
                // we have a packet.  Decode it
                int samples;
                if (vb.synthesis(op) == 0) { // test for success!
                  vd.synthesis_blockin(vb);
                }
                while ((samples = vd.synthesis_pcmout(_pcmf, _index)) > 0) {
                  double[][] pcm = _pcm[0];
                  float[][] pcmf = _pcmf[0];
                  boolean clipflag = false;
                  int bout = (samples < convsize ? samples : convsize);

                  // convert doubles to 16 bit signed ints (host order) and
                  // interleave
                  for (i = 0; i < vi.channels; i++) {
                    int ptr = i * 2;
                    //int ptr = i;
                    int mono = _index[i];
                    for (int j = 0; j < bout; j++) {
                      int val = (int) (pcmf[i][mono + j] * 32767.);
                      if (val > 32767) {
                        val = 32767;
                        clipflag = true;
                      }
                      if (val < -32768) {
                        val = -32768;
                        clipflag = true;
                      }
                      if (val < 0) {
                        val = val | 0x8000;
                      }
                      convbuffer[ptr] = (byte) (val);
                      convbuffer[ptr + 1] = (byte) (val >>> 8);
                      ptr += 2 * (vi.channels);
                    }
                  }
                  outputLine.write(convbuffer, 0, 2 * vi.channels * bout);
                  vd.synthesis_read(bout);
                }
              }
            }
            if (og.eos() != 0 || stopAudio) {
              eos = 1;
            }
          }
        }

        if (eos == 0) {
          index = oy.buffer(BUFSIZE);
          buffer = oy.data;

          try {
            bytes = bitStream.read(buffer, index, BUFSIZE);
          }
          catch (Exception e) {
            ErrorLogger.log(e);
            return;
          }

          if (bytes == -1) {
            break;
          }

          oy.wrote(bytes);
          if (bytes == 0) {
            eos = 1;
          }
        }
      }

      os.clear();
      vb.clear();
      vd.clear();
      vi.clear();

      if (stopAudio) {
        break;
      }

    }

    oy.clear();

    try {
      if (bitStream != null) {
        bitStream.close();
      }
    }
    catch (Exception e) {
      ErrorLogger.log(e);
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void redo() {
    //while (sound.isRunning()){
    //  // don't want heaps of threads trying to play the same sound
    //  return;
    //  }

    try {
      outputLine.drain();
      outputLine.stop();
      outputLine.close();
      if (bitStream != null) {
        bitStream.close();
      }
    }
    catch (Exception e) {
    }

    playAudio();

  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void stopAudio() {
    stopAudio = true;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @SuppressWarnings("rawtypes")
  @Override
  public String toString() {
    Class cl = getClass();
    String name = cl.getName();
    Package pack = cl.getPackage();

    if (pack != null) {
      name = name.substring(pack.getName().length() + 1);
    }

    return Language.get(name);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void undo() {
  }

}
