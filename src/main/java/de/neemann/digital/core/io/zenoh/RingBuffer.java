package de.neemann.digital.core.io.zenoh;

/**
 * A simple ring buffer that stores long values, evicting the oldest value when
 * the buffer is full
 */
public class RingBuffer {
    private final long[] data;
    private final int size;
    private int inBuffer;
    private int newest;
    private int oldest;

    /**
     * Creates a new instance
     *
     * @param size the size of the buffer
     */
    public RingBuffer(int size) {
        data = new long[size];
        this.size = size;
    }

    /**
     * Adds a value at the top of the buffer
     *
     * @param value the value
     */
    synchronized public void put(long value) {
        if (inBuffer < size) {
            data[newest] = value;
            newest = inc(newest);
            inBuffer++;
        } else {
            oldest = inc(oldest);
            data[newest] = value;
            newest = inc(newest);
        }
    }

    /**
     * @return the value at the tail of the buffer
     */
    synchronized public long peek() {
        if (inBuffer > 0) {
            return data[oldest];
        } else
            return -1;
    }

    /**
     * deletes a value from the tail of the buffer
     */
    synchronized public void delete() {
        if (inBuffer > 0) {
            oldest = inc(oldest);
            inBuffer--;
        }
    }

    /**
     * deletes all buffered data
     */
    synchronized public void deleteAll() {
        inBuffer = 0;
        newest = 0;
        oldest = 0;
    }

    private int inc(int index) {
        return (index + 1) % size;
    }

    /**
     * @return true if the buffer has data
     */
    synchronized public boolean hasData() {
        return inBuffer > 0;
    }
}
