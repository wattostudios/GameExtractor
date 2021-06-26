package com.ning.compress;

import java.io.IOException;

/**
 * Interface used by {@link Uncompressor} implementations: receives
 * uncompressed data and processes it appropriately.
 */
public interface DataHandler
{
    /**
     * Method called with uncompressed data as it becomes available.
     *<p>
     * NOTE: return value was added (from void to boolean) in 0.9.9
     * 
     * @return True, if caller should process and feed more data; false if
     *   caller is not interested in more data and processing should be terminated
     *   (and {@link #allDataHandled} should be called immediately)
     */
    public boolean handleData(byte[] buffer, int offset, int len) throws IOException;

    /**
     * Method called after last call to {@link #handleData}, for successful
     * operation, if and when caller is informed about end of content
     * Note that if an exception thrown by {@link #handleData} has caused processing
     * to be aborted, this method might not get called.
     * Implementation may choose to free resources, flush state, or perform
     * validation at this point.
     */
    public void allDataHandled() throws IOException;
}
