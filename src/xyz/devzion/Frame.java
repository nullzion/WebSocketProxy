/*
 * The MIT License
 *
 * Copyright 2015 nullzion.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package xyz.devzion;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author nullzion
 */
public class Frame {
    
    public final static int CONTINUATION_FRAME = 0;
    public final static int TEXT_FRAME = 1;
    public final static int BINARY_FRAME = 2;
    public final static int CONNECTION_CLOSE = 8;
    public final static int PING = 9;
    public final static int PONG = 10;
    
    private Boolean finalFrame;
    private int opcode;
    private int length;
    private byte[] mask = null;
    private byte[] payload;
    
    public Frame(String payload) throws UnsupportedEncodingException {
        this.payload = payload.getBytes("UTF-8");
        this.opcode = Frame.TEXT_FRAME;
        this.length = this.payload.length;
        this.finalFrame = true;
    }
    
    public Frame(int opcode, byte[] payload) {
        this.opcode = opcode;
        this.payload = payload;
        length = payload.length;
        this.finalFrame = true;
    }
    
    public Frame(int opcode, byte[] payload, byte[] mask) {
        this.opcode = opcode;
        this.payload = payload;
        this.mask = mask;
        this.length = payload.length;
        this.finalFrame = true;
    }
    
    public Boolean isFinalFrame() {
        return finalFrame;
    }
    
    public int getOptcode() {
        return opcode;
    }
    
    public Boolean isMasked() {
        return mask != null;
    }
    
    public byte[] getMask() {
        return mask;
    }
    
    public byte[] getRawPayload() {
        return payload;
    }
    
    public String getUnmaskedData() {
        if(isMasked()) {
            byte[] unmaskedData = new byte[length];
            for(int i = 0; i < length; i++) {
                unmaskedData[i] = (byte)(payload[i] ^ mask[i % 4]);
            }
            return new String(unmaskedData);
        } else {
            return new String(payload);
        }
    }
    
    public byte[] toBytes() {
        ByteArrayOutputStream data = new ByteArrayOutputStream();
        if(finalFrame) {
            data.write(128 + opcode);
        } else {
            data.write(opcode);
        }
        if(length > 126) {
            data.write(126);
            if(length < 256) {
                data.write(0);
                data.write(length);
            } else {
                byte[] extendedLength = new byte[2];
                extendedLength[0] = (byte) ((length >>> 8) & 0xff);
                extendedLength[1] = (byte) (length & 0xff);
                try {
                    data.write(extendedLength);
                } catch (IOException ex) {
                    Logger.getLogger(Frame.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        } else {
            data.write(length);
        }
        try {
            data.write(payload);
        } catch (IOException ex) {
            Logger.getLogger(Frame.class.getName()).log(Level.SEVERE, null, ex);
        }
        return data.toByteArray();
    }
}
