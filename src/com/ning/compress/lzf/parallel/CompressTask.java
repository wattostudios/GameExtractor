package com.ning.compress.lzf.parallel;

import java.util.concurrent.Callable;

import com.ning.compress.lzf.ChunkEncoder;
import com.ning.compress.lzf.LZFChunk;
import com.ning.compress.lzf.util.ChunkEncoderFactory;

/**
 * @author C&eacute;drik LIME
 */
class CompressTask implements Callable<LZFChunk> {
	private static final ThreadLocal<ChunkEncoder> ENCODER = new ThreadLocal<ChunkEncoder>() {
		@Override
		protected ChunkEncoder initialValue() {
			return ChunkEncoderFactory.optimalInstance();
		}
	};

	protected byte[] data;
	protected int offset, length;
	protected BlockManager blockManager;

	public CompressTask(byte[] input, int offset, int length, BlockManager blockManager) {
		super();
		this.data = input;
		this.offset = offset;
		this.length = length;
		this.blockManager = blockManager;
	}
	public CompressTask(byte[] input, BlockManager blockManager) {
		this(input, 0, input.length, blockManager);
	}

	/** {@inheritDoc} */
	@Override
	public LZFChunk call() {
		if (data != null) {
			LZFChunk lzfChunk = ENCODER.get().encodeChunk(data, offset, length);
			// input data is fully processed, we can now discard it
			blockManager.releaseBlockToPool(data);
			return lzfChunk;
		} else {
			// cleanup time!
			ENCODER.remove();
			return null;
		}
	}

}
