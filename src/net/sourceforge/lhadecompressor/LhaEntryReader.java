/*
	LzhEntryReader.java
	
	package		lha
	class		LzhEntryReader
	
	Copyright (c) 2001-2008 Nobuyasu SUEHIRO All Rights Reserved.
 */

package net.sourceforge.lhadecompressor;

import java.io.*;
import java.util.zip.*;

/**
 * This is a abstract class for used to unpack a lha header.<br>
 * <br>
 * Supports level 0,1,2 header format.<br>
 * Does'nt support header search.<br>
 * 
 * @author Nobuyasu SUEHIRO <nosue@users.sourceforge.net>
 */
public abstract class LhaEntryReader {
	/** File path separator : ms-dos,windows */
	final protected static char HD_CHR_DELIM_MSDOS = '\\';
	/** File path separator : unix */
	final protected static char HD_CHR_DELIM_UNIX = '/';
	/** File path separator : macintosh */
	final protected static char HD_CHR_DELIM_MAC = ':';
	/** File path separator : generic */
	final protected static byte HD_CHR_DELIM_EXTRA = (byte) 0xFF;

	/** Union header size and level field offset */
	final protected static int HDRU_SIZE = 21;

	/** Union header field offset : header size */
	final protected static int HDRU_OFF_HEADERSIZE = 0;
	/** Union header field offset : header level */
	final protected static int HDRU_OFF_LVL = 20;

	/** Level 0 header field offset : header check sum */
	final protected static int HDR0_OFF_SUM = 1;
	/** Level 0 header field offset : method signature */
	final protected static int HDR0_OFF_METHOD = 2;
	/** Level 0 header field offset : compressed file size */
	final protected static int HDR0_OFF_COMPSIZE = 7;
	/** Level 0 header field offset : original file size */
	final protected static int HDR0_OFF_ORIGSIZE = 11;
	/** Level 0 header field offset : time stamp (ms-dos format) */
	final protected static int HDR0_OFF_TIMESTAMP = 15;
	/** Level 0 header field offset : file attribute (ms-dos format) */
	final protected static int HDR0_OFF_FILEATTR = 19;

	/** Level 1 header field offset : header check sum */
	final protected static int HDR1_OFF_SUM = 1;
	/** Level 1 header field offset : method signature */
	final protected static int HDR1_OFF_METHOD = 2;
	/** Level 1 header field offset : skip size */
	final protected static int HDR1_OFF_SKIPSIZE = 7;
	/** Level 1 header field offset : original file size */
	final protected static int HDR1_OFF_ORIGSIZE = 11;
	/** Level 1 header field offset : time stamp (ms-dos format) */
	final protected static int HDR1_OFF_TIMESTAMP = 15;
	/** Level 1 header field offset : constant value (0x20) */
	final protected static int HDR1_OFF_19 = 19;

	/** Level 2 header field offset : method signature */
	final protected static int HDR2_OFF_METHOD = 2;
	/** Level 2 header field offset : compressed file size */
	final protected static int HDR2_OFF_COMPSIZE = 7;
	/** Level 2 header field offset : original file size */
	final protected static int HDR2_OFF_ORIGSIZE = 11;
	/** Level 2 header field offset : time stamp unix format) */
	final protected static int HDR2_OFF_TIMESTAMP = 15;
	/** Level 2 header field offset : reserved */
	final protected static int HDR2_OFF_RESERVED = 19;

	/** Header level signature : level 0 */
	final protected static byte HDR_SIG_LVL0 = 0x00;
	/** Header level signature : level 1 */
	final protected static byte HDR_SIG_LVL1 = 0x01;
	/** Header level signature : level 2 */
	final protected static byte HDR_SIG_LVL2 = 0x02;

	final protected static String HD_STR_METHOD_ENCODING = "US-ASCII";

	protected String encoding;
	protected int srcSum;
	protected int srcCRC;
	protected Checksum calcSum;
	protected Checksum calcCRC;
	protected boolean flagSum;
	protected boolean flagCRC;
	protected String fileName = "";
	protected String dirName = "";

	/**
	 * Creates a lha header reader.
	 * 
	 * @param encoding file/directory name encoding
	 */
	public LhaEntryReader(String encoding) {
		this.encoding = encoding;

		calcSum = new Sum();
		calcCRC = new CRC16();
	}

	/**
	 * Reads a lha header infomation.
	 * 
	 * @return a lha entry
	 * @throws LhaException
	 *             if a lha format error has occurred
	 * @throws IOException
	 *             if an I/O error has occurred
	 */
	protected LhaEntry readHeader() throws LhaException, IOException {
		byte base[] = new byte[HDRU_SIZE];

		calcSum.reset();
		calcCRC.reset();
		flagSum = false;
		flagCRC = false;
		fileName = "";
		dirName = "";

		int n = _read(base);
		if ((n <= 0) || ((n == 1) && (base[HDRU_OFF_HEADERSIZE] == 0))) {
			// if((n==1)&&(base[HDRU_OFF_HEADERSIZE]==0))
			return (null);
		} else if (n != HDRU_SIZE) {
			throw (new LhaException("header is broken (header size does'nt match)"));
		}

		LhaEntry e;
		switch (base[HDRU_OFF_LVL]) {
		case HDR_SIG_LVL0:
			e = readHeader_Lv0(base);
			break;

		case HDR_SIG_LVL1:
			e = readHeader_Lv1(base);
			break;

		case HDR_SIG_LVL2:
			e = readHeader_Lv2(base);
			break;

		default:
			throw (new LhaException("Unsupported Lha header level: " + base[HDRU_OFF_LVL]));
		}

		if ((e.getMethod().equals(LhaEntry.METHOD_SIG_LHD))
				&& (e.getFile().getPath().length() == 0)) {
			throw (new LhaException("Lha header is broken (file name length is zero)"));
		}

		if (flagSum && (srcSum != calcSum.getValue())) {
			throw (new LhaException("Lha header is broken (header check sum doesn't match)"));
		}

		if (flagCRC && (srcCRC != calcCRC.getValue())) {
			throw (new LhaException("Lha header is broken (header crc doesn't match"));
		}

		return (e);
	}

	/**
	 * Reads a level 0 lha header infomation.
	 * 
	 * @param base
	 *            readed datas for judge header type
	 * @return a lha entry
	 * @throws LhaException
	 *             if a lha format error has occurred
	 * @throws IOException
	 *             if an I/O error has occurred
	 */
	protected LhaEntry readHeader_Lv0(byte[] base) throws LhaException, IOException {
		LhaEntry e = new LhaEntry();

		flagSum = true;

		int headerSize = base[HDRU_OFF_HEADERSIZE];
		srcSum = base[HDR0_OFF_SUM];
		if (srcSum < 0) {
			srcSum += 256;
		}
		e.setMethod(new String(base, HDR0_OFF_METHOD, 5, HD_STR_METHOD_ENCODING));
		e.setCompressedSize(get32(base, HDR0_OFF_COMPSIZE));
		e.setOriginalSize(get32(base, HDR0_OFF_ORIGSIZE));
		e.setDosTimeStamp(get32(base, HDR0_OFF_TIMESTAMP));

		calcSum.update(base, 2, base.length - 2);

		byte[] buf = new byte[1];
		if (_read(buf) != buf.length) {
			throw (new LhaException("Lha header is broken (header size does'nt match)"));
		}
		int nameSize = buf[0];
		calcSum.update(buf, 0, buf.length);

		buf = new byte[nameSize];
		if (_read(buf) != buf.length) {
			throw (new LhaException("Lha header is broken (cannot read name)"));
		}
		String name = new String(buf, encoding);
		calcSum.update(buf, 0, buf.length);

		int diff = headerSize - nameSize;
		if ((diff != 20) && (diff != 22) && (diff < 23)) {
			throw (new LhaException("Lha header is broken (header size does'nt match)"));
		}

		e.setOS(LhaEntry.OSID_SIG_GENERIC);

		if (diff >= 22) {
			buf = new byte[2];
			if (_read(buf) != buf.length) {
				throw (new LhaException("Lha header is broken (cannot read crc value)"));
			}
			e.setCRC(get16(buf, 0));
			calcSum.update(buf, 0, buf.length);
		}

		if (diff >= 23) {
			buf = new byte[1];
			if (_read(buf) != buf.length) {
				throw (new LhaException("Lha header is broken (cannot read os signature)"));
			}
			e.setOS(buf[0]);
			calcSum.update(buf, 0, buf.length);
		}

		if (diff > 23) {
			buf = new byte[diff - 24];
			if (_read(buf) != buf.length) {
				throw (new LhaException("Lha header is broken (cannot read ext)"));
			}
			calcSum.update(buf, 0, buf.length);
		}

		e.setFile(convertFilePath(name, e.getOS()));

		return (e);
	}

	/**
	 * Reads a level 1 lha header infomation.
	 * 
	 * @param base
	 *            readed datas for judge header type
	 * @return a lha entry
	 * @throws LhaException
	 *             if a lha format error has occurred
	 * @throws IOException
	 *             if an I/O error has occurred
	 */
	protected LhaEntry readHeader_Lv1(byte[] base) throws LhaException, IOException {
		LhaEntry e = new LhaEntry();

		flagSum = true;

		srcSum = base[HDR1_OFF_SUM];
		if (srcSum < 0) {
			srcSum += 256;
		}
		e.setMethod(new String(base, HDR1_OFF_METHOD, 5, HD_STR_METHOD_ENCODING));
		e.setOriginalSize(get32(base, HDR1_OFF_ORIGSIZE));
		e.setDosTimeStamp(get32(base, HDR1_OFF_TIMESTAMP));

		if (base[HDR1_OFF_19] != 0x20) {
			throw (new LhaException("Lha header is broken (offset 19 is not 0x20)"));
		}

		calcSum.update(base, 2, base.length - 2);
		calcCRC.update(base, 0, base.length);

		byte[] buf = new byte[1];
		if (_read(buf) != buf.length) {
			throw (new LhaException("Lha header is broken (cannot read name size)"));
		}
		int nameSize = buf[0];
		calcSum.update(buf, 0, buf.length);
		calcCRC.update(buf, 0, buf.length);

		String name = "";
		if (nameSize > 0) {
			buf = new byte[nameSize];
			if (_read(buf) != buf.length) {
				throw (new LhaException("Lha header is broken (cannot read name)"));
			}
			name = new String(buf, encoding);
			calcSum.update(buf, 0, buf.length);
			calcCRC.update(buf, 0, buf.length);
		}

		buf = new byte[2];
		if (_read(buf) != buf.length) {
			throw (new LhaException("Lha header is broken (cannot read crc value)"));
		}
		e.setCRC(get16(buf, 0));
		calcSum.update(buf, 0, buf.length);
		calcCRC.update(buf, 0, buf.length);

		buf = new byte[1];
		if (_read(buf) != buf.length) {
			throw (new LhaException("Lha header is broken (cannot read os signature)"));
		}
		e.setOS(buf[0]);
		calcSum.update(buf, 0, buf.length);
		calcCRC.update(buf, 0, buf.length);

		long extSize = 0;
		buf = new byte[2];
		if (_read(buf) != buf.length) {
			throw (new LhaException("Lha header is broken (cannot read ext)"));
		}
		calcSum.update(buf, 0, buf.length);
		calcCRC.update(buf, 0, buf.length);
		for (int next = get16(buf, 0); next > 0; next = readExHeader(e, next)) {
			extSize += next;
		}

		e.setCompressedSize(get32(base, HDR0_OFF_COMPSIZE) - extSize);
		if (fileName.length() > 0) {
			name = dirName + fileName;
		} else {
			name = convertFilePath(name, e.getOS());
		}

		e.setFile(name);

		return (e);
	}

	/**
	 * Reads a level 2 lha header infomation.
	 * 
	 * @param base
	 *            readed datas for judge header type
	 * @return a lha entry
	 * @throws LhaException
	 *             if a lha format error has occurred
	 * @throws IOException
	 *             if an I/O error has occurred
	 */
	protected LhaEntry readHeader_Lv2(byte[] base) throws LhaException, IOException {
		LhaEntry e = new LhaEntry();

		e.setMethod(new String(base, HDR2_OFF_METHOD, 5, HD_STR_METHOD_ENCODING));
		e.setCompressedSize(get32(base, HDR2_OFF_COMPSIZE));
		e.setOriginalSize(get32(base, HDR2_OFF_ORIGSIZE));
		e.setHeaderTimeStamp(get32(base, HDR2_OFF_TIMESTAMP));

		calcCRC.update(base, 0, base.length);

		byte[] buf = new byte[2];
		if (_read(buf) != buf.length) {
			throw (new LhaException("Lha header is broken (cannot read crc value)"));
		}
		e.setCRC(get16(buf, 0));
		calcCRC.update(buf, 0, buf.length);

		buf = new byte[1];
		if (_read(buf) != buf.length) {
			throw (new LhaException("Lha header is broken (cannot read os signature)"));
		}
		e.setOS(buf[0]);
		calcCRC.update(buf, 0, buf.length);

		buf = new byte[2];
		if (_read(buf) != buf.length) {
			throw (new LhaException("Lha header is broken (cannot read ext)"));
		}
		calcCRC.update(buf, 0, buf.length);
		for (int next = get16(buf, 0); next > 0; next = readExHeader(e, next))
			;

		e.setFile(dirName + fileName);

		return (e);
	}

	/**
	 * Reads a extra header information in level 2 lha header.
	 * 
	 * @param e
	 *            a current lha entry
	 * @param size
	 *            header size
	 * @return next header size
	 * @throws LhaException
	 *             if a lha format error has occurred
	 * @throws IOException
	 *             if an I/O error has occurred
	 */
	protected int readExHeader(LhaEntry e, int size) throws LhaException, IOException {
		byte[] buf = new byte[size];
		if (_read(buf) != buf.length)
			throw (new LhaException("header is broken"));

		switch (buf[0]) {
		case LhaEntry.EXHDR_SIG_COMMON:
			flagCRC = true;
			srcCRC = get16(buf, 1);
			buf[1] = 0x00;
			buf[2] = 0x00;
			break;

		case LhaEntry.EXHDR_SIG_FILENAME:
			fileName = new String(buf, 1, size - 3, encoding);
			break;

		case LhaEntry.EXHDR_SIG_DIRNAME:
			StringBuffer dname = new StringBuffer();
			int pi = 0;
			for (int i = 1; i <= (size - 3); ++i) {
				if (buf[i] == HD_CHR_DELIM_EXTRA) {
					dname.append(new String(buf, pi + 1, i - pi - 1, encoding));
					dname.append(File.separator);

					pi = i;
				}
			}
			dname.append(new String(buf, pi + 1, size - 3 - pi, encoding));
			dirName = dname.toString();
			break;

		case LhaEntry.EXHDR_SIG_COMMENT:
			break;

		case LhaEntry.EXHDR_SIG_DOSATTR:
			break;

		case LhaEntry.EXHDR_SIG_DOSTIMES:
			break;

		case LhaEntry.EXHDR_SIG_UNIXPERM:
			break;

		case LhaEntry.EXHDR_SIG_UNIXID:
			break;

		case LhaEntry.EXHDR_SIG_UNIXGROUPNAME:
			break;

		case LhaEntry.EXHDR_SIG_UNIXUSERNAME:
			break;

		case LhaEntry.EXHDR_SIG_UNIXLMTIME:
			break;

		default:
			break;
		}

		calcCRC.update(buf, 0, buf.length);

		return (get16(buf, size - 2));
	}

	/**
	 * Converts a OS depend file path to a current system file path.
	 * 
	 * @param s
	 *            the file path string in header
	 * @param os
	 *            the OS signature
	 * @return the file path string on current system
	 */
	protected String convertFilePath(String s, byte os) {
		char delim;

		switch (os) {
		case LhaEntry.OSID_SIG_GENERIC:
		case LhaEntry.OSID_SIG_MSDOS:
		case LhaEntry.OSID_SIG_WIN32:
		case LhaEntry.OSID_SIG_WINNT:
			delim = HD_CHR_DELIM_MSDOS;
			break;

		case LhaEntry.OSID_SIG_MAC:
			delim = HD_CHR_DELIM_MAC;
			break;

		default:
			delim = HD_CHR_DELIM_UNIX;
			break;
		}

		char[] c = s.toCharArray();
		for (int i = 0; i < c.length; ++i) {
			if (c[i] == delim)
				c[i] = File.separatorChar;
			else if (c[i] == File.separatorChar)
				c[i] = delim;
		}

		return (new String(c));
	}

	/**
	 * abstract method for read datas.
	 * 
	 * @param b
	 *            the buffer into which the data is read
	 * @return the total number of bytes read into the buffer, or
	 *         <code>-1</code> if there is no more data because the end of the
	 *         stream has been reached
	 * @throws IOException
	 *             if an I/O error has occurred
	 */
	abstract protected int _read(byte[] b) throws IOException;

	/**
	 * Fetches unsigned 16-bit value from byte array at specified offset. The
	 * bytes are assumed to be in Intel (little-endian) byte order.
	 */
	private static final int get16(byte b[], int off) {
		return ((b[off] & 0xFF) | ((b[off + 1] & 0xFF) << 8));
	}

	/**
	 * Fetches unsigned 32-bit value from byte array at specified offset. The
	 * bytes are assumed to be in Intel (little-endian) byte order.
	 */
	private static final long get32(byte b[], int off) {
		return (get16(b, off) | ((long) get16(b, off + 2) << 16));
	}
}