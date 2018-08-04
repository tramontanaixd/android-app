package com.pierdr.pierluigidallarosa.myactivity;

import org.java_websocket.server.WebSocketServer;
import org.java_websocket.WebSocket;
import org.java_websocket.WebSocketImpl;
import org.java_websocket.drafts.Draft;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.framing.Framedata;
import org.java_websocket.framing.FramedataImpl1;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.DefaultSSLWebSocketServerFactory;
import org.java_websocket.server.WebSocketServer;
import org.json.JSONObject;


import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.File;
import java.io.FileInputStream;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.security.KeyStore;
import java.util.Collections;

public class WebsocketManager extends WebSocketServer {
    public interface websocketManagerListener {

        public void onNewMessage(String message, WebSocket socket);

        public void onNewConnection(String newDevice);

        public void onLostConnection(WebSocket socket);

    }
    private websocketManagerListener aListener;

    private static int counter = 0;
    public WebsocketManager( int port, Draft d ) throws UnknownHostException {
        super( new InetSocketAddress( port ), Collections.singletonList( d ) );
        this.aListener = null;
    }
    public void addAListener(websocketManagerListener aListener) {
        this.aListener = aListener;
    }

    public WebsocketManager( InetSocketAddress address, Draft d ) {
        super( address, Collections.singletonList( d ) );
    }

    @Override
    public void onOpen( WebSocket conn, ClientHandshake handshake ) {
        counter++;
        System.out.println( "///////////Opened connection number" + counter );
        aListener.onNewConnection(conn.getRemoteSocketAddress().getAddress().toString());
    }

    @Override
    public void onClose( WebSocket conn, int code, String reason, boolean remote ) {
        System.out.println( "closed" );
    }

    @Override
    public void onError( WebSocket conn, Exception ex ) {
        System.out.println( "Error:" );
        ex.printStackTrace();
    }

    @Override
    public void onStart() {
        System.out.println( "Server started!" );
    }

    @Override
    public void onMessage( WebSocket conn, String message ) {
//        conn.send( message );
        aListener.onNewMessage(message, conn);
    }
    @Override
    public void onFragment( WebSocket conn, Framedata fragment ) {
        System.out.println( "received fragment: " + fragment );
    }




}
