package com.ning.compress.lzf.parallel;

import java.io.OutputStream;
import java.util.concurrent.Future;

import com.ning.compress.lzf.LZFChunk;

/**
 * @author C&eacute;drik LIME
 */
class WriteTask implements Runnable {
    private final OutputStream output;
    private final Future<LZFChunk> lzfFuture;
    private final PLZFOutputStream caller;

    public WriteTask(OutputStream output, Future<LZFChunk> lzfFuture, PLZFOutputStream caller) {
        super();
        this.output = output;
        this.lzfFuture = lzfFuture;
        this.caller = caller;
    }

    /** {@inheritDoc} */
    @Override
    public void run() {
        try {
            LZFChunk lzfChunk = lzfFuture.get();
            while (lzfChunk != null) {
                output.write(lzfChunk.getData());
                lzfChunk = lzfChunk.next();
            }
        } catch (Exception e) {
            caller.writeException = e;
        }
    }
}
