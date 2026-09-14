package spl.util;

public class IdGenerator {

    private int nextId = 0;

    /**
     * Returns the next unique ID, then increments the counter.
     */
    public int next() {
        return nextId++;
    }
}