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

import java.io.*;
import java.net.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.codec.binary.Base64;


/**
 *
 * @author nullzion
 */
public class ClientHandler implements Runnable {

    Socket clientSocket;
    DataInputStream inputStream;
    DataOutputStream outputStream;
    ProxySocket proxySocket;
    
    public ClientHandler(Socket clientSocket) {
        this.clientSocket = clientSocket;
        try {
            inputStream = new DataInputStream(clientSocket.getInputStream());
            outputStream = new DataOutputStream(clientSocket.getOutputStream());
        } catch (IOException ex) {
            Logger.getLogger(ClientHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private void handleHandshake(Map<String, String> requestHandshake) throws IOException {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            String key = requestHandshake.get("Sec-WebSocket-Key");
            String newKey = Base64.encodeBase64String(md.digest((key + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11").getBytes()));
            outputStream.write(("HTTP/1.1 101 Switching Protocols\r\n").getBytes());
            outputStream.write(("Upgrade: websocket\r\n").getBytes());
            outputStream.write(("Connection: Upgrade\r\n").getBytes());
            outputStream.write(("Sec-WebSocket-Accept: " + newKey + "\r\n").getBytes());
            outputStream.write(("Sec-WebSocket-Protocol: chat\r\n\r\n").getBytes());
        } catch (NoSuchAlgorithmException ex) {
            Logger.getLogger(ClientHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    @Override
    public void run() {
        try {
            Map<String, String> requestHandshake = new HashMap<>();
            String lineBuffer;
            while(!(lineBuffer = inputStream.readLine()).isEmpty()) {
                String[] line = lineBuffer.split(": ");
                if(line.length == 2) {
                    requestHandshake.put(line[0], line[1]);
                }
            }
            handleHandshake(requestHandshake);
            Frame clientFrame;
            while((clientFrame = fetchNextFrame()) != null) {
                String data = clientFrame.getUnmaskedData();
                System.out.println(data);
                if(data.indexOf(':') == 0) {
                    if(data.contains("CONNECT: ")) {
                        String destination = data.split("\\s")[1];
                        String host = destination.split(":")[0];
                        int port = Integer.parseInt(destination.split(":")[1], 10);
                        proxySocket = new ProxySocket(this, host, port);
                        new Thread(proxySocket).start();
                    }
                } else {
                    if(proxySocket != null) {
                        System.out.println("C >>> " + data);
                        proxySocket.write(data);
                    }
                }
            }
            clientSocket.close();
        } catch (IOException ex) {
            Logger.getLogger(ClientHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private Frame fetchNextFrame() {
        try {
            int opcode = ((int) inputStream.readUnsignedByte()) - 128;
            int maskLength = inputStream.readUnsignedByte(); 
            Boolean mask = maskLength > 128;
            int length = maskLength - 128;
            if(length == 126) {
                byte[] extendedLength = new byte[2];
                inputStream.readFully(extendedLength);
                length = (extendedLength[0] & 0xff) << 8 | extendedLength[1] & 0xff;   
            } else if(length == 127) {
                /* TODO: extra length */
            }
            byte[] maskingKey = new byte[4];
            if(mask) {
                inputStream.readFully(maskingKey);
            } else {
                Arrays.fill(maskingKey,(byte) 0);
            }
            byte[] payloadData = new byte[length];
            inputStream.readFully(payloadData);
            return new Frame(opcode, payloadData, maskingKey);
        } catch (IOException ex) {
            Logger.getLogger(ClientHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }
    
    public void write(String data) {
        Frame serverFrame = new Frame(data);
        try {
            System.out.println("C <<< " + serverFrame.getUnmaskedData());
            outputStream.write(serverFrame.toBytes());
        } catch (IOException ex) {
            Logger.getLogger(ClientHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
}
