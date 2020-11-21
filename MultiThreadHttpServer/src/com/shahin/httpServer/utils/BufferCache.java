package com.shahin.httpServer.utils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayDeque;

public class BufferCache {

    public static int BUFFER_SIZE = 8 * 1024;
    private static int QUEUE_LENGTH = 32;
    private static ArrayDeque<ByteBuffer> buffers = new ArrayDeque<>(QUEUE_LENGTH);

    public static void initCache(int bufferSize, int queueLength){
        BUFFER_SIZE = bufferSize;
        QUEUE_LENGTH = queueLength;
        buffers = new ArrayDeque<>(QUEUE_LENGTH);
    }

    public static void recycleBuffer(ByteBuffer buffer){
        if(buffer != null && buffer.capacity() == BUFFER_SIZE && buffers.size() < QUEUE_LENGTH){
            buffers.offer(buffer);
        }
    }

    public static ByteBuffer generateBuffer(){
        ByteBuffer buffer = buffers.poll();
        if(buffer == null){
            buffer = ByteBuffer.allocate(BUFFER_SIZE).order(ByteOrder.nativeOrder());
        }
        buffer.clear();
        return buffer;
    }
}
