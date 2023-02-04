/**
 * Author: Paul Gates
 * 
 * TCPNetworkInterface
 * 
 * Desc:    Self contained TCP/IP Netowrking module
 *          deals with communication between the server
 * 
 * Note:    This module communicates with the server TCP
 *          socket and send high level data objects up
 *          the chain to the client driver.
 * 
 * **COPY PASTED WHERE APPLICABLE**
 * 
 * REQ18:     Allow header data to be interpreted by the client
 *         to the server and vice versa
 * 
 * REQ19:    Robust checks are done here in order to prevent client
 *         or server from crashing
 * 
 */
package network;

import static common.ConsoleLogger.*;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

import javax.imageio.ImageIO;

import org.json.JSONException;
import org.json.JSONObject;

import common.GamePacket;
import common.GameState;
import common.MessagePacket;

import java.awt.image.BufferedImage;


public class TCPNetworkIntf implements NetworkIntf {
    
    final static int BUFF_MAX = 4086;

    /**
     * Socket to the server
     */
    Socket mod_socket;

    /**
     * TCPNetworkIntf
     * 
     * Desc:    Establish Connection with the server
     */
    public TCPNetworkIntf(String host, int port)
    {
        /**
         * Attempt to establish a connection with the
         * server
         */
        try
            {
            mod_socket = new Socket( host, port );
            }
        catch( UnknownHostException ex )
            {
            log_err( "Unable to establish connection with server\n"
                     + "Unkown Host" );
            }
        catch( IOException ex)
            {
            log_err( "Unable to establish connection with server\n"
                     + "Ensure that the server is running on the same\n"
                     + "configration" );
            }
    
    }

    /**
     * TCPNetworkIntf
     * 
     * Desc:    Allows for an already created socket to utilize
     *          this module. This will help to avoid dupplication
     *          for TCP socket tx-ing and rx-ing
     */
    public TCPNetworkIntf
        (
        Socket clientSocket
        )
    {
        overrideSocket( clientSocket );
    }

    /**
     * overrideSocket
     * 
     * Following the DRY principle here. The server can override
     * this module using the supplied tx and rx procedures and
     * instead use its own socket it already has with the client
     * 
     * TLDR avoiding code duplication
     */
    public void overrideSocket
        (
        Socket socket
        )
    {
        mod_socket = socket;
    }

    /**
     * network_intf_rx
     * 
     * Desc:    Read in data from the server
     */
    @Override
    public boolean network_intf_rx
        (
        MessagePacket messagePacket
        )
    {
        /**
         * Input protection
         */
        if( messagePacket == null )
            {
            log_err("Invalid argument passed to RX proc");
            }

        String outData = null;
        /**
         * Send the JSON data to the server
         */
        try
            {
            InputStream inputStream = mod_socket.getInputStream();
    
            byte[] byteData = new byte[ BUFF_MAX ];;

            int numBytesReceived = inputStream.read(byteData, 0, BUFF_MAX );
            outData = new String( byteData, 0, numBytesReceived );

            }
        catch( IOException ex )
            {
            log_err("[TX_ERR] Issue receiving data");
            return false;
            }
        catch( Exception ex )
            {
            log_err( "Unknown error occured" );
            }
        

        if( outData == null )
            {
            log_err( "Data loading error occured" );
            log_err_rx();
            return false;
            }

        JSONObject jsonData = null;
        /**
         * Complete JSON parsing
         */
        try
            {
            // TODO PMG put this in a common place and use for UDP
            // TODO PMG something like "loadJSON( MessagePacket msg )"
            jsonData = new JSONObject( outData );

            /*
             * parse the json String into a message packet
             *
             */
            messagePacket.parseJSON( jsonData );

            }
        catch( JSONException ex )
            {
            /**
             * Helps to avoid the client going down because if received some
             * bad data
             */
            log_err("Problem recieving JSON Data:" + ex.getLocalizedMessage());
            log_err_rx();
            }

        /**
         * dont allow null message data to be sent back to the caller
         */
        if( messagePacket.msg_data == null )
            {
            log_err("Message data was null. insert a default message");

            messagePacket.msg_data = "[null]";
            return false;
            }

        /**
         *  print out the raw data we send to the client
         */
        if( jsonData != null )
            {
            log_success( "RX'd=" + jsonData.toString());
            }

        return true;
    }

    /**
     * network_intf_tx
     * 
     * Desc:    Read in data from the server
     */
    @Override
    public boolean network_intf_tx
        (
        MessagePacket messagePacket
        )
    {
        /**
         * Gaurd for an established connection
         */
        assert_msg_exit( ( mod_socket != null ), "Cannot transmit data because " 
                                               + "connection was never established");
        JSONObject            jsonData = null;
        /**
         * Send the JSON data to the server
         */
        try
            {
            /**
             * Get the JSON Representation of the message
             * data
             */
             jsonData              = messagePacket.getJSON();

            OutputStream          outputStream          = mod_socket.getOutputStream();
            BufferedOutputStream  bufferedOutputStream  = new BufferedOutputStream( outputStream );
    
            bufferedOutputStream.write( jsonData.toString().getBytes() );
            bufferedOutputStream.flush();

            }
        catch( IOException ex )
            {
            log_err("[TX_ERR] Issue sending data");
            return false;
            }
        catch( JSONException ex )
            {
            log_err("[TX_ERR] Data parsing issue occured during transmission");
            return false;       
            }

        /**
         *  print out the raw data we send to the client
         */
        if( jsonData != null )
            {
            log_success( "TX'd=" + jsonData.toString());
            }

        return true;
    }

    /*
     * REQ17: The client shall receive images from the server
     */
    @Override
    public BufferedImage network_intf_rx
        (
            //void
        )
    {
        BufferedImage image = null;
        
        /**
         * Read in the size of the image
         */
        byte[] sizeAr = new byte[ 4 ];  /* size of int */
        try
            {
            mod_socket.getInputStream().read(sizeAr);
            }
        catch( IOException ex )
            {
            log_err( "Error: unable to read image size" );
            }

        int size = ByteBuffer.wrap(sizeAr).asIntBuffer().get();
        byte[] imageAr = new byte[size];
        try
            {
            mod_socket.getInputStream().read(imageAr);
            }
        catch( IOException ex )
            {
            log_err( "Error: unable to read image" );
            }

        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(imageAr);
            
        try
            {
            image = ImageIO.read(byteArrayInputStream);
            }
        catch( IOException ex )
            {
            log_err( "Error: unable to read image" );
            }

        if( image != null )
            {
            log_success("Received image from server");

            }
        else
            {
            log_err( "Error: unable to read image" );
            }

        return image;
    }

    /*
     * REQ17: The server shall send images to the client
     */
    @Override
    public boolean network_intf_tx
        (
        BufferedImage image
        )
    {
        /**
         * Input validation
         */
        if( image == null )
            {
            log_err( "Error: attempting to send invalid image" );
            return false;
            }
        
        try
            {
            /*
             * Convert the image to a byte array
             */
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ImageIO.write(image, "jpg", byteArrayOutputStream);

            /*
             * Send the size of the image first
             */
            byte[] size = ByteBuffer.allocate(4).putInt(byteArrayOutputStream.size()).array();
            mod_socket.getOutputStream().write(size);

            /*
             * Send the image
             */
            mod_socket.getOutputStream().write(byteArrayOutputStream.toByteArray());
            mod_socket.getOutputStream().flush();

            /**
             * sleep for a bit to let the other end point catch up
             */
            try
                {
                Thread.sleep( 1000 );
                }
            catch( InterruptedException ex )
                {
                    ex.printStackTrace();
                }
                
                
            }
        catch( IOException ex )
            {
            log_err( "Error: unable to convert image to byte array" );
            }
        
        /**
         * Otherwise if there was no errors return 
         * true to the caller to let them know
         * that the output data is good
         */
        return true;
    }


    /**
     * alert_other_clients
     * REQ21: If another client is trying to connect send them
     * a nice little error message
     */
    @Override
    public boolean alert_other_clients
        (
        //void
        )
    {
        /**
         * generic TCP interface does not support
         * pending connections
         */
        return false;
    }
}
