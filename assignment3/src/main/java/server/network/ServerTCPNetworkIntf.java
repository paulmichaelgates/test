/**
 * Author:  Paul Gates
 *
 * Desc:    This class is used to establish a connection to a client
 *          and then pass the connection to the TCPNetworkIntf class
 *          which will handle the actual communication
 * 
 * 
 * **COPY PASTED WHERE APPLICABLE**
 * 
 * REQ18:     Allow header data to be interpreted by the client
 *         to the server and vice versa
 * 
 * REQ19:    Robust checks are done here in order to prevent client
 *         or server from crashing
 */
 
package server.network;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.TreeMap;

import common.MessagePacket;
import static common.MessagePacket.*;
import static common.ConsoleLogger.*;

import java.net.SocketTimeoutException;

import network.NetworkIntf;

import network.TCPNetworkIntf;

public class ServerTCPNetworkIntf implements NetworkIntf{

    private ServerSocket socket;
    private Socket       clientSocket = null;

    TCPNetworkIntf proxy_socket;

    public ServerTCPNetworkIntf
        (
        int port
        )
    {
        /**
         * Attempt to lock on to a port
         */
        try
            {
            socket = new ServerSocket( port );
            socket.setSoTimeout( 0 );
            }
        catch( IOException ex )
            {
            log_err("Unable to establish a connection to port " + port);
            }

        /**
         *  Wait for clients
         *  note that this is a blocking call
         */
        try 
            {
           clientSocket = socket.accept();
            }
        catch( IOException ex )
            {
            ex.printStackTrace(); // TODO console log this
            }

        proxy_socket = new TCPNetworkIntf( clientSocket );

        /**
         * After connection is established start listening 
         * give control back to the driver so it can decide
         * when the listen for incoming requests and then
         * it will give control back to us at that time
         */
        
    }

    @Override
    public boolean network_intf_rx(MessagePacket msg) {

        /**
         * alert clients that are attempting to connect
         */
        alert_other_clients();

        /**
         * pass to the common TCP rx interface
         */
       return proxy_socket.network_intf_rx(  msg );

    }

    @Override
    public boolean network_intf_tx(MessagePacket msg) {
        /**
         * alert clients that are attempting to connect
         *
         */
        alert_other_clients();

        /**
         * pass to the common TCP tx interface
         */
        return proxy_socket.network_intf_tx(  msg );

    }

    @Override
    public boolean network_intf_tx(BufferedImage image) {
        /**
         * pass to common TCP tx interface
         */
        return proxy_socket.network_intf_tx(  image );
    }

    @Override
    public BufferedImage network_intf_rx() {
        /**
         * Do nothing server cannot receive images
         */
        return null;
    }
    
    @Override
    public boolean alert_other_clients() {

        /**
         * If we have not established a connection
         * then there is no pending connection
         *
         */
        if( clientSocket == null )
            {
            return false;
            }
        /**
         * If we are already connected to a client
         * then we need to handle any pending clients
         * 
         * Temporarily set a timeout value on the
         * blocking accept call
         */

        try
            {
            socket.setSoTimeout( 1000 );
            }
        catch( IOException ex )
            {
            ex.printStackTrace(); // TODO console log this
            }

        Socket unkownClient = null;
        /**
         * check to see if another client is
         * trying to connect
         */
        try
            {
            unkownClient = socket.accept();
            }
        catch( SocketTimeoutException ex )
            {
            /**
             * If we get a timeout then there is no
             * pending connection which is totally ok
             */
            log_msg( "No pending connection on periodic client check" );
            }
            catch( IOException ex )
            {
            ex.printStackTrace(); // TODO console log this
            }

        /**
         * If the there was a waiting client
         * send it the error message
         */
        if( unkownClient != null )
            {
            /**
             * Send the client an error message
             */
            MessagePacket msg = MessagePacket.getErrPacket( "Server is busy." );
            TCPNetworkIntf tempTCPNet = new TCPNetworkIntf( unkownClient );
            tempTCPNet.network_intf_tx( msg );
            }

        /**
         * Put the timeout back to zero
         */
        try
            {
            socket.setSoTimeout( 0 );
            }
        catch( IOException ex )
            {
            ex.printStackTrace(); // TODO console log this
            }

        /**
         * Return true to indicate that there was a pending
         * connection and we dealt with it
         */
        return true;

    }
}
