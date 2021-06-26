package net.sourceforge.lhadecompressor;

import java.io.*;

/**
 * 
 * @author Nobuyasu SUEHIRO <nosue@users.sourceforge.net>
 */
public class NocompressDecoder implements LhaDecoder {
	private InputStream in;
	private long originalSize;

	public NocompressDecoder(InputStream in, long originalSize) {
		this.in = in;
		this.originalSize = originalSize;
	}

	public int read(byte[] b, int off, int len) throws LhaException, IOException {
		if (len <= 0) {
			return (0);
		}

		if (originalSize <= 0) {
			return (-1);
		}

		int sl = len;

		while ((originalSize > 0) && (len > 0)) {
			b[off++] = (byte) in.read();

			--originalSize;
			--len;
		}

		return (sl - len);
	}

	public void close() {
		this.in = null;
	}
}
