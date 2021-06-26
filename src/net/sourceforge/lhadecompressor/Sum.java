package net.sourceforge.lhadecompressor;

import java.util.zip.Checksum;

/**
 * A class that can be used to compute the check sum of a data stream.
 * 
 * @author Nobuyasu SUEHIRO <nosue@users.sourceforge.net>
 */
public class Sum implements Checksum {
	/** check sum value */
	private int sum;

	/**
	 * Creates a new check this.sum class.
	 * 
	 */
	public Sum() {
		this.sum = 0;
	}

	/**
	 * Updates check this.sum with specified byte.
	 * 
	 * @param b
	 *            data element
	 */
	public void update(int b) {
		this.sum += b;
	}

	/**
	 * Updates check this.sum with specified array of bytes.
	 * 
	 * @param b
	 *            data element array
	 * @param off
	 *            data element array offset
	 * @param len
	 *            data element array length from offset
	 */
	public void update(byte[] b, int off, int len) {
		while (len-- > 0) {
			this.sum += b[off++];
		}
	}

	/**
	 * Returns check this.sum to initial value.
	 * 
	 * @return sum value
	 */
	public long getValue() {
		return (((long) this.sum) & 0xFFL);
	}

	/**
	 * Resets check this.sum to initial value.
	 * 
	 */
	public void reset() {
		this.sum = 0;
	}
}