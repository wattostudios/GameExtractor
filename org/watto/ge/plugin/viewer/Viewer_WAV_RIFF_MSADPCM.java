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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine.Info;
import javax.sound.sampled.UnsupportedAudioFileException;
import org.watto.ErrorLogger;
import org.watto.component.PreviewPanel;
import org.watto.component.PreviewPanel_Audio;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.io.FileManipulator;
import org.watto.io.converter.ByteArrayConverter;

/**
**********************************************************************************************
DISABLED - doesn't work!!!
**********************************************************************************************
**/
public class Viewer_WAV_RIFF_MSADPCM extends ViewerPlugin {

  /**
   * A bunch of magical numbers that predict the sample data from the
   * MSADPCM wavedata. Do not attempt to understand at all costs!
   */
  private static final int[] adaptionTable = {
      230, 230, 230, 230, 307, 409, 512, 614,
      768, 614, 512, 409, 307, 230, 230, 230
  };

  private static final int[] adaptCoeff_1 = {
      256, 512, 0, 192, 240, 460, 392
  };

  private static final int[] adaptCoeff_2 = {
      0, -256, 0, 64, 0, -208, -232
  };

  /**
   * Splits the MSADPCM samples from each byte block.
   *
   * @param block An MSADPCM sample byte
   * @param nibbleBlock we copy the parsed shorts into here
   */
  private static void getNibbleBlock(int block, int[] nibbleBlock) {
    nibbleBlock[0] = block >>> 4; // Upper half
    nibbleBlock[1] = block & 0xF; // Lower half
  }

  /**
  **********************************************************************************************
  DISABLED - doesn't work!!!
  **********************************************************************************************
  **/
  public Viewer_WAV_RIFF_MSADPCM() {
    super("WAV_RIFF_MSADPCM", "MSADPCM Wave Audio");
    setExtensions("wav");
    setEnabled(false); // DISABLED - doesn't work!!!
  }

  /**
   * Calculates PCM samples based on previous samples and a nibble input.
   *
   * @param nibble A parsed MSADPCM sample we got from getNibbleBlock
   * @param predictor The predictor we get from the MSADPCM block's preamble
   * @param sample_1 The first sample we use to predict the next sample
   * @param sample_2 The second sample we use to predict the next sample
   * @param delta Used to calculate the final sample
   * @return The calculated PCM sample
   */
  private short calculateSample(
      int nibble,
      int predictor,
      short[] sample_1,
      short[] sample_2,
      short[] delta) {
    // Get a signed number out of the nibble. We need to retain the
    // original nibble value for when we access AdaptionTable[].
    byte signedNibble = (byte) nibble;// sbyte
    if ((signedNibble & 0x8) == 0x8) {
      signedNibble -= 0x10;
    }

    // Calculate new sample
    int sampleInt = (((sample_1[0] * adaptCoeff_1[predictor]) +
        (sample_2[0] * adaptCoeff_2[predictor])) / 256);
    sampleInt += signedNibble * delta[0];

    // Clamp result to 16-bit
    short sample;
    if (sampleInt < Short.MIN_VALUE) {
      sample = Short.MIN_VALUE;
    }
    else if (sampleInt > Short.MAX_VALUE) {
      sample = Short.MAX_VALUE;
    }
    else {
      sample = (short) sampleInt;
    }

    // Shuffle samples, get new delta
    sample_2[0] = sample_1[0];
    sample_1[0] = sample;
    delta[0] = (short) (adaptionTable[nibble] * delta[0] / 256);

    // Saturate the delta to a lower bound of 16
    if (delta[0] < 16) {
      delta[0] = 16;
    }

    return sample;
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
   * Decodes MSADPCM data to signed 16-bit PCM data.
   *
   * @param source A ByteBuffer containing the headerless MSADPCM data
   * @param numChannels The number of channels (WAVEFORMATEX nChannels)
   * @param blockAlign The ADPCM block size (WAVEFORMATEX nBlockAlign)
   * @return A byte array containing the raw 16-bit PCM wavedata
   *
   *         NOTE: The original MSADPCMToPCM class returns as a short[] array!
   */
  public byte[] convertToPCM(
      FileManipulator source,
      short numChannels,
      short blockAlign) {

    try {
      // We write to output when reading the PCM data, then we convert
      // it back to a short array at the end.
      ByteArrayOutputStream output = new ByteArrayOutputStream();

      // We'll be using this to get each sample from the blocks.
      int[] nibbleBlock = new int[2];

      // Assuming the whole stream is what we want.
      long fileLength = source.getLength();

      // Mono or Stereo?
      if (numChannels == 1) {
        // Read to the end of the file.
        while (source.getOffset() < fileLength) {

          System.out.println(source.getOffset() + " of " + fileLength);

          // Read block preamble
          int predictor = source.readByte() & 0xff;
          short[] delta = { source.readShort() };
          short[] sample_1 = { source.readShort() };
          short[] sample_2 = { source.readShort() };

          // Send the initial samples straight to PCM out.
          output.write(ByteArrayConverter.convertLittle(sample_2[0]));
          output.write(ByteArrayConverter.convertLittle(sample_1[0]));

          // Go through the bytes in this MSADPCM block.
          for (int bytes = 0; bytes < (blockAlign + 15); bytes++) {
            // Each sample is one half of a nibbleBlock.
            getNibbleBlock(source.readByte() & 0xff, nibbleBlock);
            for (int i = 0; i < 2; i++) {
              output.write(ByteArrayConverter.convertLittle(
                  calculateSample(
                      nibbleBlock[i],
                      predictor,
                      sample_1,
                      sample_2,
                      delta)));
            }
          }
        }
      }
      else if (numChannels == 2) {
        // Read to the end of the file.
        while (source.getOffset() < fileLength) {
          // Read block preamble
          int l_predictor = source.readByte() & 0xff;
          int r_predictor = source.readByte() & 0xff;
          short[] l_delta = { source.readShort() };
          short[] r_delta = { source.readShort() };
          short[] l_sample_1 = { source.readShort() };
          short[] r_sample_1 = { source.readShort() };
          short[] l_sample_2 = { source.readShort() };
          short[] r_sample_2 = { source.readShort() };

          // Send the initial samples straight to PCM out.
          output.write(ByteArrayConverter.convertLittle(l_sample_2[0]));
          output.write(ByteArrayConverter.convertLittle(r_sample_2[0]));
          output.write(ByteArrayConverter.convertLittle(l_sample_1[0]));
          output.write(ByteArrayConverter.convertLittle(r_sample_1[0]));

          // Go through the bytes in this MSADPCM block.
          for (int bytes = 0; bytes < ((blockAlign + 15) * 2); bytes++) {
            // Each block carries one left/right sample.
            getNibbleBlock(source.readByte() & 0xff, nibbleBlock);

            // Left channel...
            output.write(ByteArrayConverter.convertLittle(
                calculateSample(
                    nibbleBlock[0],
                    l_predictor,
                    l_sample_1,
                    l_sample_2,
                    l_delta)));

            // Right channel...
            output.write(ByteArrayConverter.convertLittle(
                calculateSample(
                    nibbleBlock[1],
                    r_predictor,
                    r_sample_1,
                    r_sample_2,
                    r_delta)));
          }
        }
      }
      else {
        throw new AssertionError("MSADPCM WAVEDATA IS NOT MONO OR STEREO!");
      }

      // We're done writing PCM data...
      output.close();

      // Return the array.
      return output.toByteArray();
    }
    catch (IOException ex) {
      throw new AssertionError("This should not happen as no I/O resources are used", ex);
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
        rating += 49; // 49 instead of 50, just so the normal WAV decoder comes in before this one
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
    try {

      FileManipulator fm = new FileManipulator(source, false);

      // 4 - RIFF Header
      // 4 - Chunk Size
      // 4 - Format
      // 4 - FMT Header
      // 4 - Chunk Size
      fm.skip(20);

      // 2 - Audio Format
      if (fm.readShort() != 2) {
        // Not MSADPCM
        fm.close();
        return null;
      }

      // 2 - Num Channels
      short numChannels = fm.readShort();

      // 4 - Sample Rate
      // 4 - Byte Rate
      fm.skip(8);

      // 2 - Block Align
      short blockAlign = fm.readShort();

      // 2 - Bits Per Sample
      fm.skip(2);

      // 2 - Extra Data Size
      // X - Extra Data
      fm.skip(fm.readShort());

      // 4 - Data Header
      // 4 - Data Size
      fm.skip(8);

      byte[] sourceData = convertToPCM(fm, numChannels, blockAlign);
      fm.close();

      AudioInputStream stream = AudioSystem.getAudioInputStream(new ByteArrayInputStream(sourceData));
      AudioFormat format = stream.getFormat();
      Info info = new Info(Clip.class, format, ((int) stream.getFrameLength() * format.getFrameSize()));
      Clip sound = (Clip) AudioSystem.getLine(info);
      sound.open(stream);

      PreviewPanel_Audio preview = new PreviewPanel_Audio(sound);

      return preview;

    }
    catch (UnsupportedAudioFileException e) {
      ErrorLogger.log("Viewer_WAV_RIFF could not open the audio file.");
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
