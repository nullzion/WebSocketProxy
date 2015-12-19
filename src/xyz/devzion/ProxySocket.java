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

import java.net.*;
import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author nullzion
 */
class ProxySocket implements Runnable {
    
    private ClientHandler handler;
    private String host;
    private int port;
    private Socket socket;
    private BufferedReader socketReader;
    private BufferedWriter socketWriter;

    public ProxySocket(ClientHandler handler, String host, int port) {
        this.handler = handler;
        this.host = host;
        this.port = port;
        System.out.println("Connecting to " + host + ":" + port);
    }

    @Override
    public void run() {
        try {
            socket = new Socket(host, port);
            socketReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            socketWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            
            String buffer = "";
            
            while((buffer = socketReader.readLine()) != null) {
                handler.write(buffer);
            }
            
        } catch (IOException ex) {
            Logger.getLogger(ProxySocket.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void write(String data) {
        try {
            socketWriter.write(data);
            socketWriter.flush();
        } catch (IOException ex) {
            Logger.getLogger(ProxySocket.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
}
