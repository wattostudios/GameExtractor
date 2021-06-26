/*
	LzhEntry.java
	
	package		lha
	class		LzhEntry
	
	Copyright (c) 2001-2008 Nobuyasu SUEHIRO All Rights Reserved.
 */

package net.sourceforge.lhadecompressor;

import java.io.*;
import java.util.*;

/**
 * This class is used to represent a lha file entry.
 * 
 * @author Nobuyasu SUEHIRO <nosue@users.sourceforge.net>
 */
public class LhaEntry {
	// A default entry name encoding type
	final public static String HD_STR_ENCODING = "MS932"; // default entry
	// name encoding
	// type

	// Method signatures
	/** The compression method signature for directory */
	final public static String METHOD_SIG_LHD = "-lhd-";
	/** The compression method signature for lh0 */
	final public static String METHOD_SIG_LH0 = "-lh0-";
	/** The compression method signature for lh1 */
	final public static String METHOD_SIG_LH1 = "-lh1-";
	/** The compression method signature for lh2 */
	final public static String METHOD_SIG_LH2 = "-lh2-";
	/** The compression method signature for lh3 */
	final public static String METHOD_SIG_LH3 = "-lh3-";
	/** The compression method signature for lh4 */
	final public static String METHOD_SIG_LH4 = "-lh4-";
	/** The compression method signature for lh5 */
	final public static String METHOD_SIG_LH5 = "-lh5-";
	/** The compression method signature for lh6 */
	final public static String METHOD_SIG_LH6 = "-lh6-";
	/** The compression method signature for lh7 */
	final public static String METHOD_SIG_LH7 = "-lh7-";
	/** The compression method signature for lzs */
	final public static String METHOD_SIG_LZS = "-lzs-";
	/** The compression method signature for lz4 */
	final public static String METHOD_SIG_LZ4 = "-lz4-";
	/** The compression method signature for lz5 */
	final public static String METHOD_SIG_LZ5 = "-lz5-";

	// OS id signatres
	/** The operation system signature for generic */
	final public static byte OSID_SIG_GENERIC = 0x00;
	/** The operation system signature for MS-DOS */
	final public static byte OSID_SIG_MSDOS = 0x4D;
	/** The operation system signature for OS/2 */
	final public static byte OSID_SIG_OS2 = 0x32;
	/** The operation system signature for OS9 */
	final public static byte OSID_SIG_OS9 = 0x39;
	/** The operation system signature for OS/68K */
	final public static byte OSID_SIG_OS68K = 0x4B;
	/** The operation system signature for OS/386 */
	final public static byte OSID_SIG_OS386 = 0x33;
	/** The operation system signature for HUMAN. */
	final public static byte OSID_SIG_HUMAN = 0x48;
	/** The operation system signature for Unix */
	final public static byte OSID_SIG_UNIX = 0x55;
	/** The operation system signature for CP/M */
	final public static byte OSID_SIG_CPM = 0x43;
	/** The operation system signature for Flex */
	final public static byte OSID_SIG_FLEX = 0x46;
	/** The operation system signature for Macintosh */
	final public static byte OSID_SIG_MAC = 0x6D;
	/** The operation system signature for Runser */
	final public static byte OSID_SIG_RUNSER = 0x52;
	/** The operation system signature for Java */
	final public static byte OSID_SIG_JAVA = 0x4A;
	/** The operation system signature for Windows95 (from UNLHA32.DLL) */
	final public static byte OSID_SIG_WIN32 = 0x77;
	/** The operation system signature for WindowsNT (from UNLHA32.DLL) */
	final public static byte OSID_SIG_WINNT = 0x57;

	// Extend header signatres
	/** The extend header signature: header crc and information */
	final public static byte EXHDR_SIG_COMMON = 0x00;
	/** The extend header signature: file name */
	final public static byte EXHDR_SIG_FILENAME = 0x01;
	/** The extend header signature: directory name */
	final public static byte EXHDR_SIG_DIRNAME = 0x02;
	/** The extend header signature: comment */
	final public static byte EXHDR_SIG_COMMENT = 0x3f;
	/** The extend header signature: ms-dos attributes */
	final public static byte EXHDR_SIG_DOSATTR = 0x40;
	/** The extend header signature: ms-dos time stamps (from UNLHA32.DLL) */
	final public static byte EXHDR_SIG_DOSTIMES = 0x41;
	/** The extend header signature: unix permisson */
	final public static byte EXHDR_SIG_UNIXPERM = 0x50;
	/** The extend header signature: unix group id,user id */
	final public static byte EXHDR_SIG_UNIXID = 0x51;
	/** The extend header signature: unix group name */
	final public static byte EXHDR_SIG_UNIXGROUPNAME = 0x52;
	/** The extend header signature: unix user name */
	final public static byte EXHDR_SIG_UNIXUSERNAME = 0x53;
	/** The extend header signature: unix last modified time */
	final public static byte EXHDR_SIG_UNIXLMTIME = 0x54;

	/** method ID */
	protected String method;
	/** compressed size ID */
	protected long compressedSize;
	/** original size */
	protected long originalSize;
	/** time stamp */
	protected Date timeStamp;
	/** file path and name */
	protected File file;
	/** file crc */
	protected int crc;
	/** file crc flag */
	protected boolean fcrc = false;
	/** os type */
	protected byte os;
	/** offset of compressed data from beginning of lzh file */
	protected long offset = -1;

	/**
	 * Creates a new lha entry.
	 */
	public LhaEntry() {
	}

	/**
	 * Sets the compress method id string.
	 * 
	 * @param method
	 *            the compress method id string
	 * @throws IllegalArgumentException
	 *             if the compress method id is not supported
	 * @see #getMethod()
	 */
	protected void setMethod(String method) {
		if ((method.compareTo(METHOD_SIG_LHD) != 0) && (method.compareTo(METHOD_SIG_LH0) != 0)
				&& (method.compareTo(METHOD_SIG_LH1) != 0)
				&& (method.compareTo(METHOD_SIG_LH2) != 0)
				&& (method.compareTo(METHOD_SIG_LH3) != 0)
				&& (method.compareTo(METHOD_SIG_LH4) != 0)
				&& (method.compareTo(METHOD_SIG_LH5) != 0)
				&& (method.compareTo(METHOD_SIG_LH6) != 0)
				&& (method.compareTo(METHOD_SIG_LH7) != 0)
				&& (method.compareTo(METHOD_SIG_LZS) != 0)
				&& (method.compareTo(METHOD_SIG_LZ4) != 0)
				&& (method.compareTo(METHOD_SIG_LZ5) != 0)) {
			throw (new IllegalArgumentException("Invalid lzh entry method " + method));
		}

		this.method = method;
	}

	/**
	 * Returns the compress method id string.
	 * 
	 * @return the compress method id string
	 * @see #setMethod(String)
	 */
	public String getMethod() {
		return (method);
	}

	/**
	 * Sets the compressed size.
	 * 
	 * @param compressedSize
	 *            the compressed data size
	 * @throws IllegalArgumentException
	 *             if the compressed data size is less than 0 or greater than
	 *             0xFFFFFFFF
	 * @see #getCompressedSize()
	 */
	protected void setCompressedSize(long compressedSize) {
		if (compressedSize < 0 || compressedSize > 0xFFFFFFFFL)
			throw (new IllegalArgumentException("Invalid lzh entry compressed data size"));

		this.compressedSize = compressedSize;
	}

	/**
	 * Returns the compressed data size.
	 * 
	 * @return the compressed data size
	 * @see #setCompressedSize(long)
	 */
	public long getCompressedSize() {
		return (compressedSize);
	}

	/**
	 * Sets the original data size.
	 * 
	 * @param originalSize
	 *            the original data size
	 * @throws IllegalArgumentException
	 *             if the original data size is less than 0 or greater than
	 *             0xFFFFFFFF
	 * @see #getOriginalSize()
	 */
	protected void setOriginalSize(long originalSize) {
		if (originalSize < 0 || originalSize > 0xFFFFFFFFL)
			throw (new IllegalArgumentException("Invalid lha entry original data size"));

		this.originalSize = originalSize;
	}

	/**
	 * Returns the original size.
	 * 
	 * @return the original size
	 * @see #setOriginalSize(long)
	 */
	public long getOriginalSize() {
		return (originalSize);
	}

	/**
	 * Sets the time stamp of data.
	 * 
	 * @param timeStamp
	 *            the time stamp of data
	 * @see #getTimeStamp()
	 */
	protected void setTimeStamp(Date timeStamp) {
		this.timeStamp = timeStamp;
	}

	/**
	 * Returns the time stamp of data.
	 * 
	 * @return the time stamp of data
	 * @see #setTimeStamp(Date)
	 */
	public Date getTimeStamp() {
		return ((Date)timeStamp.clone());
	}

	/**
	 * Sets the MS-DOS time stamp of data.
	 * 
	 * @param timeStamp
	 *            the MS-DOS time stamp of data
	 * @see #getDosTimeStamp()
	 */
	protected void setDosTimeStamp(long tstamp) {
		Date ts = new Date();
		ts.setTime(dosToJavaTime(tstamp));

		timeStamp = ts;
	}

	/**
	 * Returns the MS-DOS time stamp of data.
	 * 
	 * @return the MS-DOS time stamp of data
	 * @see #setDosTimeStamp(long)
	 */
	public long getDosTimeStamp() {
		return (javaToDosTime(timeStamp.getTime()));
	}

	/**
	 * Sets the unix time stamp of header.
	 * 
	 * @param timeStamp
	 *            the unix time stamp of header
	 * @see #getHeaderTimeStamp()
	 */
	protected void setHeaderTimeStamp(long tstamp) {
		Date ts = new Date();
		ts.setTime(tstamp * 1000);

		timeStamp = ts;
	}

	/**
	 * Returns the unix time stamp of header.
	 * 
	 * @return the unix time stamp of header
	 * @see #setHeaderTimeStamp(long)
	 */
	public long getHeaderTimeStamp() {
		if (timeStamp == null)
			return (-1L);

		return (timeStamp.getTime());
	}

	/**
	 * Returns the File/Name by string.
	 * 
	 * @param name
	 *            the string of File/Name
	 * @see #setFile(File)
	 * @see #getFile()
	 */
	protected void setFile(String name) {
		file = new File(name);
	}

	/**
	 * Sets the File/Name by file.
	 * 
	 * @param file
	 *            the File/Name
	 * @see #setFile(String)
	 * @see #getFile()
	 */
	protected void setFile(File file) {
		this.file = file;
	}

	/**
	 * Returns the File/Name.
	 * 
	 * @return the File/Name
	 * @see #setFile(String)
	 * @see #setFile(File)
	 */
	public File getFile() {
		return (file);
	}

	/**
	 * Sets the CRC value.
	 * 
	 * @param crc
	 *            the CRC value
	 * @see #getCRC()
	 * @see #hasCRC()
	 */
	protected void setCRC(int crc) {
		this.fcrc = true;
		this.crc = crc;
	}

	/**
	 * Returns the CRC value. Before use this method, you should check this
	 * entry has CRC or not.
	 * 
	 * @return the CRC value
	 * @see #setCRC(int)
	 * @see #hasCRC()
	 */
	public int getCRC() {
		return (crc);
	}

	/**
	 * Returns this entry has CRC or not.
	 * 
	 * @return true if this entry has CRC.
	 * @see #setCRC(int)
	 * @see #getCRC()
	 */
	public boolean hasCRC() {
		return (fcrc);
	}

	/**
	 * Sets the Operation System signature.
	 * 
	 * @param os
	 *            the Operation System signature
	 * @see #getOS()
	 */
	protected void setOS(byte os) {
		this.os = os;
	}

	/**
	 * Returns the Operation System signature.
	 * 
	 * @return the Operation System signature
	 * @see #setOS(byte)
	 */
	public byte getOS() {
		return (os);
	}

	/**
	 * Returns a file path or a name of the lha entry.
	 */
	public String toString() {
		return (file.toString());
	}

	/**
	 * Sets the offset of compression data in file. Be carefull the offset is
	 * not offset in lha entry.
	 * 
	 * @param offset
	 *            the offset of compression data in file
	 * @throws IllegalArgumentException
	 *             if the length of the offset is less than 0 or greater than
	 *             0xFFFFFFFF
	 * @see #getOffset()
	 */
	protected void setOffset(long offset) {
		if (offset < 0 || offset > 0xFFFFFFFFL)
			throw (new IllegalArgumentException("Invalid lzh entry offset"));

		this.offset = offset;
	}

	/**
	 * Returns the offset of compression data in file. Be carefull the offset is
	 * not offset in lha entry.
	 * 
	 * @return the offset from in file
	 * @see #setOffset(long)
	 */
	public long getOffset() {
		return (offset);
	}

	/**
	 * Converts MS-DOS time to Java time.
	 */
	private static long dosToJavaTime(long dtime) {
		GregorianCalendar c = new GregorianCalendar((int) (((dtime >> 25) & 0x7f) + 1980),
				(int) (((dtime >> 21) & 0x0f) - 1), (int) ((dtime >> 16) & 0x1f),
				(int) ((dtime >> 11) & 0x1f), (int) ((dtime >> 5) & 0x3f),
				(int) ((dtime << 1) & 0x3e));

		return ((c.getTime()).getTime());
	}

	/**
	 * Converts Java time to MS-DOS time.
	 */
	private static long javaToDosTime(long time) {
		GregorianCalendar c = new GregorianCalendar();
		c.setGregorianChange(new Date(time));

		int year = c.get(Calendar.YEAR) + 1900;
		if (year < 1980) {
			return (1 << 21) | (1 << 16);
		}

		return ((year - 1980) << 25 | ((c.get(Calendar.MONTH)) + 1) << 21
				| (c.get(Calendar.DAY_OF_MONTH)) << 16 | (c.get(Calendar.HOUR_OF_DAY)) << 11
				| (c.get(Calendar.MINUTE)) << 5 | (c.get(Calendar.SECOND)) >> 1);
	}
}