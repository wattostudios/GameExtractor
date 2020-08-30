package com.ning.compress.lzf.parallel;

import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * @author C&eacute;drik LIME
 */
class BlockManager {
    /* used as a blocking Stack (FIFO) */
    private final BlockingDeque<byte[]> blockPool;

    public BlockManager(int blockPoolSize, int blockSize) {
//        log.debug("Using block pool size of " + blockPoolSize);
        blockPool = new LinkedBlockingDeque<byte[]>(blockPoolSize);
        for (int i = 0; i < blockPoolSize; ++i) {
            blockPool.addFirst(new byte[blockSize]);
        }
    }

    public byte[] getBlockFromPool() {
        byte[] block = null;
        try {
            block = blockPool.takeFirst();
        } catch (InterruptedException e) {
        	throw new RuntimeException(e);
        }
        return block;
    }

    public void releaseBlockToPool(byte[] block) {
        assert ! blockPool.contains(block);
//        Arrays.fill(block, (byte)0);
        try {
            blockPool.putLast(block);
        } catch (InterruptedException e) {
        	throw new RuntimeException(e);
        }
    }

}
