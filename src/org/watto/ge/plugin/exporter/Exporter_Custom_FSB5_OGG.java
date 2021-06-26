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

package org.watto.ge.plugin.exporter;

import java.io.File;
import org.watto.ErrorLogger;
import org.watto.Settings;
import org.watto.datatype.Resource;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.resource.Resource_FSB_Audio;
import org.watto.io.FileManipulator;
import org.watto.io.buffer.ByteBuffer;
import org.watto.io.converter.ByteConverter;
import org.watto.io.converter.IntConverter;
import com.jcraft.jogg.Packet;
import com.jcraft.jogg.Page;
import com.jcraft.jogg.StreamState;
import com.jcraft.jorbis.Comment;
import com.jcraft.jorbis.Info;

/**
**********************************************************************************************
This will rebuild all the OGG pages from a FSB5 source, but doesn't build the OGG ID/Comment/Setup
pages, so still doesn't really work.
**********************************************************************************************
**/
public class Exporter_Custom_FSB5_OGG extends ExporterPlugin {

  File sourceFile = null;

  /** the exporter that will do all the actual work **/
  ExporterPlugin exporter = null;

  /** the offset to each compressed block **/
  long[] blockOffsets;

  /** the length of each compressed block **/
  long[] blockLengths;

  int currentBlock = -1;

  int oggPageNumber = 0;

  byte[] oggPage = new byte[0];

  int oggPagePos = 0;

  int oggPageLength = 0;

  // OGG Vorbis-specific CRC lookup table
  static int[] rogg_crc_lookup = new int[] {
      0x00000000, 0x04c11db7, 0x09823b6e, 0x0d4326d9,
      0x130476dc, 0x17c56b6b, 0x1a864db2, 0x1e475005,
      0x2608edb8, 0x22c9f00f, 0x2f8ad6d6, 0x2b4bcb61,
      0x350c9b64, 0x31cd86d3, 0x3c8ea00a, 0x384fbdbd,
      0x4c11db70, 0x48d0c6c7, 0x4593e01e, 0x4152fda9,
      0x5f15adac, 0x5bd4b01b, 0x569796c2, 0x52568b75,
      0x6a1936c8, 0x6ed82b7f, 0x639b0da6, 0x675a1011,
      0x791d4014, 0x7ddc5da3, 0x709f7b7a, 0x745e66cd,
      0x9823b6e0, 0x9ce2ab57, 0x91a18d8e, 0x95609039,
      0x8b27c03c, 0x8fe6dd8b, 0x82a5fb52, 0x8664e6e5,
      0xbe2b5b58, 0xbaea46ef, 0xb7a96036, 0xb3687d81,
      0xad2f2d84, 0xa9ee3033, 0xa4ad16ea, 0xa06c0b5d,
      0xd4326d90, 0xd0f37027, 0xddb056fe, 0xd9714b49,
      0xc7361b4c, 0xc3f706fb, 0xceb42022, 0xca753d95,
      0xf23a8028, 0xf6fb9d9f, 0xfbb8bb46, 0xff79a6f1,
      0xe13ef6f4, 0xe5ffeb43, 0xe8bccd9a, 0xec7dd02d,
      0x34867077, 0x30476dc0, 0x3d044b19, 0x39c556ae,
      0x278206ab, 0x23431b1c, 0x2e003dc5, 0x2ac12072,
      0x128e9dcf, 0x164f8078, 0x1b0ca6a1, 0x1fcdbb16,
      0x018aeb13, 0x054bf6a4, 0x0808d07d, 0x0cc9cdca,
      0x7897ab07, 0x7c56b6b0, 0x71159069, 0x75d48dde,
      0x6b93dddb, 0x6f52c06c, 0x6211e6b5, 0x66d0fb02,
      0x5e9f46bf, 0x5a5e5b08, 0x571d7dd1, 0x53dc6066,
      0x4d9b3063, 0x495a2dd4, 0x44190b0d, 0x40d816ba,
      0xaca5c697, 0xa864db20, 0xa527fdf9, 0xa1e6e04e,
      0xbfa1b04b, 0xbb60adfc, 0xb6238b25, 0xb2e29692,
      0x8aad2b2f, 0x8e6c3698, 0x832f1041, 0x87ee0df6,
      0x99a95df3, 0x9d684044, 0x902b669d, 0x94ea7b2a,
      0xe0b41de7, 0xe4750050, 0xe9362689, 0xedf73b3e,
      0xf3b06b3b, 0xf771768c, 0xfa325055, 0xfef34de2,
      0xc6bcf05f, 0xc27dede8, 0xcf3ecb31, 0xcbffd686,
      0xd5b88683, 0xd1799b34, 0xdc3abded, 0xd8fba05a,
      0x690ce0ee, 0x6dcdfd59, 0x608edb80, 0x644fc637,
      0x7a089632, 0x7ec98b85, 0x738aad5c, 0x774bb0eb,
      0x4f040d56, 0x4bc510e1, 0x46863638, 0x42472b8f,
      0x5c007b8a, 0x58c1663d, 0x558240e4, 0x51435d53,
      0x251d3b9e, 0x21dc2629, 0x2c9f00f0, 0x285e1d47,
      0x36194d42, 0x32d850f5, 0x3f9b762c, 0x3b5a6b9b,
      0x0315d626, 0x07d4cb91, 0x0a97ed48, 0x0e56f0ff,
      0x1011a0fa, 0x14d0bd4d, 0x19939b94, 0x1d528623,
      0xf12f560e, 0xf5ee4bb9, 0xf8ad6d60, 0xfc6c70d7,
      0xe22b20d2, 0xe6ea3d65, 0xeba91bbc, 0xef68060b,
      0xd727bbb6, 0xd3e6a601, 0xdea580d8, 0xda649d6f,
      0xc423cd6a, 0xc0e2d0dd, 0xcda1f604, 0xc960ebb3,
      0xbd3e8d7e, 0xb9ff90c9, 0xb4bcb610, 0xb07daba7,
      0xae3afba2, 0xaafbe615, 0xa7b8c0cc, 0xa379dd7b,
      0x9b3660c6, 0x9ff77d71, 0x92b45ba8, 0x9675461f,
      0x8832161a, 0x8cf30bad, 0x81b02d74, 0x857130c3,
      0x5d8a9099, 0x594b8d2e, 0x5408abf7, 0x50c9b640,
      0x4e8ee645, 0x4a4ffbf2, 0x470cdd2b, 0x43cdc09c,
      0x7b827d21, 0x7f436096, 0x7200464f, 0x76c15bf8,
      0x68860bfd, 0x6c47164a, 0x61043093, 0x65c52d24,
      0x119b4be9, 0x155a565e, 0x18197087, 0x1cd86d30,
      0x029f3d35, 0x065e2082, 0x0b1d065b, 0x0fdc1bec,
      0x3793a651, 0x3352bbe6, 0x3e119d3f, 0x3ad08088,
      0x2497d08d, 0x2056cd3a, 0x2d15ebe3, 0x29d4f654,
      0xc5a92679, 0xc1683bce, 0xcc2b1d17, 0xc8ea00a0,
      0xd6ad50a5, 0xd26c4d12, 0xdf2f6bcb, 0xdbee767c,
      0xe3a1cbc1, 0xe760d676, 0xea23f0af, 0xeee2ed18,
      0xf0a5bd1d, 0xf464a0aa, 0xf9278673, 0xfde69bc4,
      0x89b8fd09, 0x8d79e0be, 0x803ac667, 0x84fbdbd0,
      0x9abc8bd5, 0x9e7d9662, 0x933eb0bb, 0x97ffad0c,
      0xafb010b1, 0xab710d06, 0xa6322bdf, 0xa2f33668,
      0xbcb4666d, 0xb8757bda, 0xb5365d03, 0xb1f740b4 };

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Exporter_Custom_FSB5_OGG() {
    setName("FSB5 OGG Audio Exporter");
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Exporter_Custom_FSB5_OGG(long[] blockOffsets, long[] blockLengths) {
    this.blockOffsets = blockOffsets;
    this.blockLengths = blockLengths;
    this.exporter = Exporter_Default.getInstance();
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public boolean available() {
    if (oggPagePos < oggPageLength) {
      // still more to read from the current page
      return true;
    }

    // otherwise, we need to build a new oggPage
    buildOggPage();

    if (oggPagePos < oggPageLength) {
      // a new page was created successfully
      return true;
    }
    else {
      // we have reached the end-of-file, so no more pages 
      return false;
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/

  private boolean blockAvailable() {
    if (exporter.available()) {
      // still reading the current block
      return true;
    }

    // the current block is finished, move on to the next block
    currentBlock++;
    if (currentBlock < blockOffsets.length) {
      // open the next block
      exporter.close();
      //System.out.println("Opening block at " + blockOffsets[currentBlock] + " with compressed length " + blockLengths[currentBlock]);
      exporter.open(new Resource(sourceFile, "", blockOffsets[currentBlock], blockLengths[currentBlock]));
      boolean result = exporter.available();
      return result;
    }
    else {
      // finished reading the last block
      exporter.close();
      return false;
    }

  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void close() {
    exporter.close();
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  private void blockClose() {
    exporter.close();
  }

  public long[] getBlockLengths() {
    return blockLengths;
  }

  public long[] getBlockOffsets() {
    return blockOffsets;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public String getDescription() {
    return "This exporter adds the OGG audio header bytes when extracting from an FSB5 FMOD Soundbank archive\n\n" + super.getDescription();
  }

  int channels = 2;

  int frequency = 44100;

  int setupCRC = 0;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void open(Resource source) {
    try {

      // reset the Ogg Properties
      oggPageNumber = 0;

      // prepare the internal exporters
      blockOpen(source);

      channels = 2;
      frequency = 44100;
      setupCRC = 0;

      if (source instanceof Resource_FSB_Audio) {
        Resource_FSB_Audio resource = (Resource_FSB_Audio) source;

        channels = resource.getChannels();
        frequency = resource.getFrequency();
        setupCRC = resource.getSetupCRC();
      }

      // Build the OGG Header as the first 2 pages
      buildOggHeader();

      oggPageNumber = 2; // we have used the first 2 pages for the OGG Header

    }
    catch (Throwable t) {
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  private void blockOpen(Resource source) {
    try {

      // get the source file - we already know everything else
      sourceFile = source.getSource();

      // open the first block, ready to go
      currentBlock = 0;
      exporter.open(new Resource(sourceFile, "", blockOffsets[currentBlock], blockLengths[currentBlock]));

    }
    catch (Throwable t) {
    }
  }

  /**
   **********************************************************************************************
   * NOT DONE
   **********************************************************************************************
   **/
  @Override
  public void pack(Resource source, FileManipulator destination) {
    try {

      ExporterPlugin exporter = source.getExporter();
      exporter.open(source);

      while (exporter.available()) {
        destination.writeByte(exporter.read());
      }

      exporter.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

  /** when we load in a setup header, store it in memory for quick access next time, instead of reading continuously from a File **/
  static long[] setupHeaders = new long[161]; // max number of headers we currently know about

  static byte[][] setupHeaderBytes = new byte[161][];

  static int numSetupHeaders = 0;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void buildOggHeader() {

    /*
    FileManipulator dumpIn = new FileManipulator(new File("C:\\_WATTOz\\____Development_Stuff\\FSB5_Converted_Files\\main_menu_button_click-0000.ogg"), false);
    FileManipulator dumpOut = new FileManipulator(new File("C:\\_WATTOz\\Google Drive\\Development\\Java\\Game Extractor\\external_bins\\setupTable_1560547591.ogg"), true);
    dumpIn.seek(159);
    dumpOut.writeBytes(dumpIn.readBytes(3189));
    dumpOut.close();
    dumpIn.close();
    
    */

    // Straight up, we need to read in the setup table (from a file) so that we know how big it is, and therefore...
    // 1. Know how many header segments there are
    // 2. Know the complete size of the Ogg Header, so can set the array sizes appropriately

    // See if we've loaded this setup header before
    long unsignedCRC = IntConverter.unsign(setupCRC);

    byte[] setupTableBytes = null;
    for (int i = 0; i < numSetupHeaders; i++) {
      if (setupHeaders[i] == unsignedCRC) {
        setupTableBytes = setupHeaderBytes[i];
        break;
      }
    }
    if (setupTableBytes == null) {
      // not previously loaded into memory - load it now, from File

      FileManipulator headerFM;
      try {
        headerFM = new FileManipulator(new File(Settings.getString("OGG_Headers_Path") + File.separator + "setupTable_" + unsignedCRC + ".ogg"), false);
      }
      catch (Throwable t) {
        ErrorLogger.log("[Exporter_Custom_FSB5_OGG] Missing OGG Header for CRC " + unsignedCRC);
        return;
        //headerFM = new FileManipulator(new File("C:\\_WATTOz\\Google Drive\\Development\\Java\\Game Extractor\\external_bins\\setupTable_2104318331.ogg"), false);
      }
      int setupTableLength = (int) headerFM.getLength();
      setupTableBytes = headerFM.readBytes(setupTableLength);
      headerFM.close();

      // store in memory
      setupHeaders[numSetupHeaders] = unsignedCRC;
      setupHeaderBytes[numSetupHeaders] = setupTableBytes;
      numSetupHeaders++;
    }

    int setupTableLength = 0;
    if (setupTableBytes == null) {
      setupTableLength = 0;
    }
    else {
      setupTableLength = setupTableBytes.length;
    }

    // determine the number of segments
    int numSegments = (setupTableLength / 255) + 1 + 1; // +1 for the extra length (or null, if it's a full segment), and +1 for the Header Comment Length
    int lastSegmentLength = setupTableLength % 255;

    int pageSize = 85 + numSegments + 60 + setupTableLength; // header (to numSegments field) + numSegments + comment (60) + setupTable
    //int pageSize = 4303;

    byte[] oggData = new byte[pageSize];

    FileManipulator fm = new FileManipulator(new ByteBuffer(oggData));
    fm.seek(0); // just in case

    //
    // Build the header of the OGG file
    //

    // 4 - Header (OggS)
    fm.writeString("OggS");

    // 1 - Version (0)
    fm.writeByte(0);

    // 1 - Header Type (2)
    fm.writeByte(2);

    // 8 - Granule Position (0)
    fm.writeLong(0);

    // 4 - Bit Stream Number (1)
    fm.writeInt(1);

    // 4 - Page Sequence Number (0)
    fm.writeInt(0);

    // 4 - Checksum (0) // calculated later on
    fm.writeInt(0);

    // 1 - Number of Segments (1)
    fm.writeByte(1);

    // 1 - Segment Table (only 1 entry, 30)
    fm.writeByte(30);

    //
    // Build the ID header
    //

    // 1 - Packet Type (1)
    fm.writeByte(1);

    // 6 - Codec (vorbis)
    fm.writeString("vorbis");

    // 4 - Version (0)
    fm.writeInt(0);

    // 1 - Channels
    fm.writeByte(channels);

    // 4 - Frequency
    fm.writeInt(frequency);

    // 4 - Maximum Bitrate (0)
    fm.writeInt(0);

    // 4 - Nominal Bitrate (0)
    fm.writeInt(0);

    // 4 - Minimum Bitrate (0)
    fm.writeInt(0);

    // 1 - Block Size
    //   4 bits - Short Blocksize [-3]
    //   4 bits - Long Blocksize [-3]
    fm.writeByte(184);

    // 1 - Framing Bit (1)
    fm.writeByte(1);

    // now calculate the CRC32 checksum for this header
    int crc = 0;
    for (int i = 0; i < 58; i++) {
      crc = (crc << 8) ^ rogg_crc_lookup[((crc >> 24) & 0xFF) ^ (ByteConverter.unsign(oggData[i]))];
    }

    // write the crc to the buffer
    fm.seek(22);
    fm.writeInt(crc);

    // go back to the current position, ready to start writing the Comment Header
    fm.seek(58);

    //
    // Build the header of the Comment and Setup Page
    //

    // 4 - Header (OggS)
    fm.writeString("OggS");

    // 1 - Version (0)
    fm.writeByte(0);

    // 1 - Header Type (0)
    fm.writeByte(0);

    // 8 - Granule Position (0)
    fm.writeLong(0);

    // 4 - Bit Stream Number (1)
    fm.writeInt(1);

    // 4 - Page Sequence Number (1)
    fm.writeInt(1);

    // 4 - Checksum (0) // calculated later on
    fm.writeInt(0);

    // 1 - Number of Segments (18)
    //fm.writeByte(18);
    fm.writeByte(numSegments);

    // 1 - Segment Table
    fm.writeByte(60); // comment 
    for (int i = 0; i < numSegments - 2; i++) {
      fm.writeByte(255);
    }
    //fm.writeByte(60);
    fm.writeByte(lastSegmentLength); // last segment

    //
    // Build the Comment header
    //

    // 1 - Packet Type (3)
    fm.writeByte(3);

    // 6 - Codec (vorbis)
    fm.writeString("vorbis");

    // 4 - Comment Length (44)
    fm.writeInt(44);

    // 44 - Comment ("Xiph.Org libVorbis I 20150105 (" + (bytes)226,155,132,226,155,132,155,132,155,132 + ")")
    fm.writeString("Xiph.Org libVorbis I 20150105 (");
    for (int i = 0; i < 4; i++) {
      fm.writeByte(226);
      fm.writeByte(155);
      fm.writeByte(132);
    }
    fm.writeString(")");

    // 4 - (0)
    fm.writeInt(0);

    // 1 - Framing Bit (1)
    fm.writeByte(1);

    //
    // Build the Setup header
    //

    // we already read this in earlier, so now we just output it
    fm.writeBytes(setupTableBytes);

    // now calculate the CRC32 checksum for this header
    crc = 0;
    for (int i = 58; i < pageSize; i++) {
      crc = (crc << 8) ^ rogg_crc_lookup[((crc >> 24) & 0xFF) ^ (ByteConverter.unsign(oggData[i]))];
    }

    // write the crc to the buffer
    fm.seek(80);
    fm.writeInt(crc);

    // should be done now

    // close the manipulator
    fm.close();

    // Increment the OGG page number for next time
    oggPageNumber++;

    // Store the page and the properties in the global variables
    oggPage = oggData;
    oggPagePos = 0;
    oggPageLength = oggPage.length;

    //
    //
    // The rest of this section is preparing the OGG encoder with information to be able to split the stream into appropriately-sized Pages
    //
    //
    Packet idPacket = new Packet();
    idPacket.bytes = 30;

    byte[] idBytes = new byte[30];
    System.arraycopy(oggData, 28, idBytes, 0, 30);
    idPacket.packet_base = idBytes;

    //idPacket.packet = 1;
    idPacket.packet = 0;
    idPacket.b_o_s = 1;
    idPacket.e_o_s = 0;
    idPacket.granulepos = 0;
    idPacket.packetno = 0;

    Packet commentPacket = new Packet();
    Comment comment = new Comment();
    comment.init();
    comment.header_out(commentPacket);

    Packet setupPacket = new Packet();
    setupPacket.bytes = setupTableLength;

    //byte[] setupBytes = new byte[setupTableLength];
    //System.arraycopy(oggData, 163, setupBytes, 0, 4140);
    setupPacket.packet_base = setupTableBytes;

    //setupPacket.packet = 5;
    setupPacket.packet = 0;
    setupPacket.b_o_s = 0;
    setupPacket.e_o_s = 0;
    setupPacket.granulepos = 0;
    setupPacket.packetno = 2;

    vorbisInfo = new Info();
    vorbisInfo.init();
    vorbisInfo.synthesis_headerin(comment, idPacket);
    vorbisInfo.synthesis_headerin(comment, commentPacket);
    vorbisInfo.synthesis_headerin(comment, setupPacket);

    streamState = new StreamState();
    streamState.init(1);

    streamState.packetin(idPacket);
    streamState.pageout(new Page());
    streamState.packetin(commentPacket);
    streamState.pageout(new Page());
    streamState.packetin(setupPacket);
    streamState.pageout(new Page());
    streamState.flush(new Page());

    // prepare for the first real page
    packetNo = 1;
    granulePos = 0;
    prevBlockSize = 0;
  }

  StreamState streamState = null;

  Info vorbisInfo = null;

  int packetNo = 1;

  int granulePos = 0;

  int prevBlockSize = 0;

  /**
  **********************************************************************************************
  NOT CURRENTLY USED
  **********************************************************************************************
  **/
  public void buildOggPageManual() {

    //
    // First up, read the OGG data to the maximum buffer size.
    // This will let us know whether we have a full page, or a small one.
    // This reads the actual raw data from the file (from the exporter), and fills the buffer.
    //
    int maxPageSize = 64770;

    byte[] oggData = new byte[maxPageSize];
    int oggDataLength = 0;
    while (oggDataLength < maxPageSize) {
      if (blockAvailable()) {
        oggData[oggDataLength] = (byte) blockRead();
        oggDataLength++;
      }
      else {
        break;
      }
    }

    if (oggDataLength <= 0) {
      // end of the whole file
      blockClose();

      oggPage = new byte[0];
      oggPagePos = 0;
      oggPageLength = oggPage.length;
      return;
    }

    int segmentCount = 255;
    int lastSegmentSize = 0;

    if (oggDataLength < maxPageSize) {
      // not a full page - shrink it, and add the page header

      // first, work out how many segments were filled
      segmentCount = oggDataLength / 255 + 1; // +1 to hold the remaining data, and also holds a "0" if it fits nicely in 255 blocks, no no further calculation needed here
      lastSegmentSize = oggDataLength % 255;
    }
    else {
      // just add the page header
    }

    int headerSize = 27 + segmentCount;
    int pageSize = headerSize + oggDataLength;

    byte[] rawOggData = oggData;
    oggData = new byte[pageSize];
    System.arraycopy(rawOggData, 0, oggData, headerSize, oggDataLength); // note, copies the raw data into the appropriate place in the buffer

    //
    // Now we go and fill in the OGG header
    //

    FileManipulator fm = new FileManipulator(new ByteBuffer(oggData));
    fm.seek(0); // just in case

    // 4 - Header (OggS)
    fm.writeString("OggS");

    // 1 - Version (0)
    fm.writeByte(0);

    // 1 - Header Type (0)
    fm.writeByte(0);

    // 8 - Granule Position (decoded length or something)
    fm.writeLong(64770 * oggPageNumber / 4); // random

    // 4 - Bit Stream Number (1)
    fm.writeInt(1);

    // 4 - Page Sequence Number (incremental from 2, because 0 and 1 are used for the OGG header)
    fm.writeInt(oggPageNumber);

    // 4 - Checksum (CRC32 with value 0x04C11DB7) (calculated over this entire chunk, including header, where this field is set to 0 for the calculation)
    fm.writeInt(0);

    // 1 - Number of Segments (255)
    fm.writeByte(segmentCount);

    // 255 - Segment Table (where each byte is 255 except the last one which is 0)
    for (int i = 0; i < segmentCount - 1; i++) {
      fm.writeByte(255);
    }
    fm.writeByte(lastSegmentSize);

    // 64770 - Ogg Raw Data
    // already filled at the top of this method

    // now calculate the CRC32 checksum, and write it to the buffer
    /*
    CRC32 checksum = new CRC32();
    checksum.update(oggData);
    long checksumValue = checksum.getValue();
    
    fm.seek(22);
    fm.writeInt(checksumValue);
    */
    int crc = 0;
    for (int i = 0; i < pageSize; i++) {
      crc = (crc << 8) ^ rogg_crc_lookup[((crc >> 24) & 0xFF) ^ (ByteConverter.unsign(oggData[i]))];
    }

    fm.seek(22);
    fm.writeInt(crc);

    // close the manipulator
    fm.close();

    // Increment the OGG page number for next time
    oggPageNumber++;

    // Store the page and the properties in the global variables
    oggPage = oggData;
    oggPagePos = 0;
    oggPageLength = oggPage.length;

  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void buildOggPage() {

    // start reading through the source blocks, adding them to the StreamState until pageOut returns a non-zero value (meaning a page is complete)

    while (blockAvailable()) { // this will open the next block if the current block is empty
      packetNo++;

      // get the length of the current block
      int blockLength = (int) blockLengths[currentBlock];

      Packet packet = new Packet();
      packet.packet = 0; // ???

      packet.bytes = blockLength;

      byte[] packetBytes = new byte[blockLength];
      for (int i = 0; i < blockLength; i++) {
        packetBytes[i] = (byte) blockRead();
      }
      packet.packet_base = packetBytes;

      packet.packetno = packetNo;

      if (blockAvailable()) { // increments to the next block, if there is one
        blockLength = (int) blockLengths[currentBlock];
        packet.e_o_s = 0;
      }
      else {
        // end of file
        blockLength = 0;
        packet.e_o_s = 1;
      }

      int blockSize = vorbisInfo.blocksize(packet);
      if (prevBlockSize != 0) {
        granulePos = (granulePos + (blockSize + prevBlockSize) / 4);
      }
      else {
        granulePos = 0;
      }
      packet.granulepos = granulePos;
      prevBlockSize = blockSize;

      streamState.packetin(packet);

      // now see if a full page is ready
      Page page = new Page();
      int pageReady = streamState.pageout(page);
      oggPage = null;
      while (pageReady != 0) {

        int headerLength = page.header_len;
        int bodyLength = page.body_len;

        byte[] oggData = new byte[headerLength + bodyLength];
        System.arraycopy(page.header_base, 0, oggData, 0, headerLength);
        System.arraycopy(page.body_base, 0, oggData, headerLength, bodyLength);

        // Store the page and the properties in the global variables
        if (oggPage == null) {
          oggPage = oggData;
          oggPagePos = 0;
          oggPageLength = oggPage.length;
        }
        else {
          // multiple pages, for some reason. We don't handle this yet!
          ErrorLogger.log("[Exporter_Custom_FSB5_OGG] Multiple pages generated, but we don't support this, so pages were lost.");
        }

        // in case there's more than 1 page, although there hopefully isn't
        page = new Page();
        pageReady = streamState.pageout(page);
      }

      if (oggPage != null) {
        // a page was generated, so break out of the while(available) loop so we can start writing the data
        break;
      }

    }

  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public int read() {
    try {
      int currentByte = oggPage[oggPagePos];
      oggPagePos++;
      return currentByte;
    }
    catch (Throwable t) {
      return 0;
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  private int blockRead() {
    try {
      return exporter.read(); // available() already handles the transition between blocks
    }
    catch (Throwable t) {
      return 0;
    }
  }

  public void setBlockLengths(long[] blockLengths) {
    this.blockLengths = blockLengths;
  }

  public void setBlockOffsets(long[] blockOffsets) {
    this.blockOffsets = blockOffsets;
  }

}