package com.pierdr.pierluigidallarosa.myactivity;

import org.java_websocket.WebSocket;
import org.java_websocket.drafts.Draft;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.util.Collections;

public class WebsocketManager extends WebSocketServer {
    public interface websocketManagerListener {

        void onNewMessage(String message, WebSocket socket);

        void onNewConnection(String newDevice);
    }
    private websocketManagerListener aListener;

    private static int counter = 0;
    WebsocketManager(int port, Draft d) {
        super( new InetSocketAddress( port ), Collections.singletonList( d ) );
        this.aListener = null;
    }
    public void addAListener(websocketManagerListener aListener) {
        this.aListener = aListener;
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
        aListener.onNewMessage(message, conn);
    }
}
