/**
 * Author: Paul Gates
 * 
 * Desc:   Generic UDP Network Interface for
 *         sending and receiving packets
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

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import org.json.JSONException;
import org.json.JSONObject;

import common.MessagePacket;

import static common.ConsoleLogger.*;
import javax.imageio.ImageIO;

public class UDPNetworkIntf implements NetworkIntf
{
    public DatagramSocket socket = null;
    public int BUFF_SIZE         =  0xFFF;
    public int MAX_IMG_CHUNK_SZ  =  0x7FF;
    public int TIMEOUT           =  10000; /* 10 seconds */
    public int port              = -1;
    public int MAX_ATTEMPTS      =  2;
    public int endPointPort      = -1;

    public String hostname = null;

    //TODO PMG I don't really like this but it's fine for now
    public boolean isServer = false;

    public UDPNetworkIntf
        (
        String  hostname,
        int     port,
        boolean isServer
        )
    {
        this.hostname = hostname;
        this.port     = port;
        this.isServer = isServer;
        /*
         * Set the socket 
         */
        try {
            /*
             * If the port is not needed then we are the server
             */
            if( isServer )
                {
                /**
                 * connect to the provided port
                 */
                socket  = new DatagramSocket( port );

                /**
                 * log success
                 */
                log_success("Server started on port: " + port);
                }
            /*
             * Handle client set up
             */
            else
                {
                /**
                 * If we are the client then we need to bind to
                 * any port that is available. the server will 
                 * be the end point port (i.e., the one that we
                 * will send out to)
                 */
                socket       = new DatagramSocket( );
                endPointPort = port;

                /**
                 * log success
                 *
                 */
                log_success("Client started on port: " + socket.getLocalPort());
                }
        } catch (SocketException e) {
            /**
             * log error
             *
             */
            log_err("Unable to create socket");
        }

        /**
         * Set the timeout for the socket
         */
        try {
            socket.setSoTimeout(TIMEOUT);
        } catch (SocketException e) {
            /**
             * log error
             *
             */
            log_err("Unable to set timeout");
        }

    }

    /*
     * sends a packet to the server
     * data: args from user of what to send
     * to the server
     */
    public boolean network_intf_tx
        (
        MessagePacket messagePacket
        ) 
    {
        /**
         * Ensure that the end point port is set
         * correctly. We should not be sending data
         * if we have not yet established an end
         * point to send out data to
         */
        if( endPointPort == -1 )
            {
                log_err("End point port is not set");
                log_err_rx();

                return false;
            }

        /**
         * Ensure that this object is initialized
         * correctly
         */
        if( ( hostname == null ) 
        &&  ( port > 0         ) )
            {
                log_err("Hostname is null");
                log_err_rx();

                return false;
            }

        /* Local vars */
        byte[] buffer = new byte[ BUFF_SIZE ];

        int numAttmpts = 0;
        boolean data_sent = false;

        while( numAttmpts < MAX_ATTEMPTS)
            {
            /**
             *  add a time for the other end to catch up
             */
            try {
                Thread.sleep( 3000 );
            } catch (InterruptedException e1) {
                log_err("Unable to sleep thread");
                break;
            }    
            
            /*
            * initialize the socket with the port
            * that the application will send packets
            * from
            */
            try 
                {

                                /* load data into the pkt */
                char[] tmp_data = messagePacket.getJSON().toString().toCharArray();

                /**
                 * log the length of the data sending
                 */
                assert_msg_exit( tmp_data.length < BUFF_SIZE, "Data to send is too large. Exiting...");

                /* copy into buffer used in */
                for ( int i = 0; i < tmp_data.length; i++ )
                    {
                    buffer[ i ] = ( byte ) tmp_data[ i ];
                    }

                /* set up the packet with buffer containing data to send */
                DatagramPacket pkt = new DatagramPacket( buffer, BUFF_SIZE );

                pkt.setAddress( InetAddress.getByName(hostname) );
                pkt.setPort( endPointPort ); //TODO fix this port number
                socket.send(pkt);

                data_sent = true;
                } 
            catch (SocketException e) 
                {
                /**
                 * log socket error
                 *
                 */
                log_err("Unable to create socket");
                }
            catch( UnknownHostException e )
                {
                /**
                 * log host name error
                 *
                 */
                log_err("Unable to resolve hostname");
                }
            catch( IOException e )
                {
                /**
                 * log sending packet error
                 *
                 */
                log_err("Packet Exception");
                }
            finally
                {
                /**
                 * close the socket
                 */
                if( numAttmpts == MAX_ATTEMPTS )
                    {
                    this.assert_msg_exit( data_sent, "Could not send packet on attempt " + numAttmpts );
                    }
                
                if( data_sent )
                    {
                    break;
                    }
                else
                    {
                    /*
                     * increment the number of attempts
                     */
                    numAttmpts++;
                    log_msg( "Attempt " + numAttmpts + " failed");

                    }
                }

            }
    /**
     * Determine if the packet was sent
     *
     */
    if( data_sent )
        {
        log_success("Packet sent with data: " +  messagePacket.getJSON().toString() );
        return true;
        }
    else
        {
        log_err("Packet not sent");
        return false;
        }

    }

    /* handles the reception of packets */
    public boolean network_intf_rx
        (
        MessagePacket messagePacket
        ) 
    {
        /**
         * Ensure that this object is initialized
         * correctly
         */
        if( ( hostname == null ) 
        &&  ( port > 0         ) )
            {
                log_err("Hostname is null");
                log_err_rx();

                return false;
            }

        int numAttmpts = 0;
        boolean data_received = false;

        while( numAttmpts < MAX_ATTEMPTS )
            {
            /* Local vars */
            byte[] buffer = new byte[BUFF_SIZE];

            /*
            * create a packet which will store
            * the received data
            */
            DatagramPacket pkt = new DatagramPacket(buffer, BUFF_SIZE);

            /**
             *  add a time delay for the client to catch up
             */
            try {
                Thread.sleep( 3000 );
            } catch (InterruptedException e1) {
                log_err("Unable to sleep thread");
                break;
            }

            /*
            * recieve the packet
            */
            try {
                socket.receive( pkt );
                
                /*
                 * check if we are the Server or
                 * the client
                 */
                if( isServer )
                    {
                    /**
                     * Check to see if we have established a "connection"
                     * with the client yet
                     */
                    if( endPointPort < 0 )
                        {
                        /**
                         * grab the port number from the packet
                         * and use this as our reference to the
                         * client
                         */
                        endPointPort = pkt.getPort();

                        /**
                         * log the connection
                         *
                         */
                        log_success("Our client is at port " + endPointPort);
                        
                        }
                    else
                        {
                        /**
                         * check to see if the packet is from the
                         * client we are expecting
                         */
                        if( pkt.getPort() != endPointPort )
                            {
                            /**
                             * log the error
                             */
                            log_msg("Packet received from unknown client");

                            /**
                             * send back an error message to the
                             * client that is trying to connect
                             * with us
                             */
                            network_intf_tx( MessagePacket.getErrPacket( "This server is busy." ) );

                            }
                        }
                    }
                /*
                * convert the buffer into a string
                */
                String jsonString = new String( buffer );

                /*
                * parse the json string into a message packet
                */
                JSONObject jsonData = new JSONObject( jsonString );
                messagePacket.parseJSON( jsonData );

                log_success("Packet received" +  messagePacket.getJSON().toString());
                
                return true;

            } catch (IOException e) {
                /*
                * log error
                *
                */
                log_err("Unable to receive packet");
                e.printStackTrace();
            } catch (JSONException e) {
                /*
                * log error
                *
                */
                log_err("JSON Parsing error");
                e.printStackTrace();
            } catch (Exception e)
                {
                /* 
                 * log error
                 */
                log_err("Unknown error");
                }
            finally
                {
                /**
                 * close the socket
                 */
                if( numAttmpts == MAX_ATTEMPTS )
                    {
                    this.assert_msg_exit( data_received, "Could not receive packet on attempt " + numAttmpts );
                    }
                
                if( data_received )
                    {
                    break;
                    }
                else
                    {
                    /*
                     * 
                     */
                    numAttmpts++;
                    log_msg( "Attempt " + numAttmpts + " failed");

                    }
                
                } 
        }

        /**
         * Determine if the packet was received
         */
        if( data_received )
            {
            log_success("Packet received with data: " +  messagePacket.getJSON().toString() );
            return true;
            }
        else
            {
            log_err("Packet not received");
            return false;
            }
    }


    @Override
    public boolean network_intf_tx
        (
        BufferedImage image
        ) 
    {
        /*
         * convert the image into a byte array
         */
        ByteArrayOutputStream byteArrayOutputStream = null;
        try
            {
            byteArrayOutputStream = new ByteArrayOutputStream();
            ImageIO.write(image, "jpg", byteArrayOutputStream);
            }
        catch( IOException e )
            {
            log_err("Unable to convert image to byte array");
            return false;
            }

        /**
         * Determine the image size
         */
        int imageSize = byteArrayOutputStream.size();

        /**
         * TX The image size
         */
        MessagePacket imageSizeMessage = new MessagePacket();
        imageSizeMessage.msg_data      = String.valueOf( imageSize );
        if( !network_intf_tx( imageSizeMessage ) )
            {
            log_err("Unable to send image size");
            return false;
            }
        /**
         * log the image size sent to the client
         *
         */

        log_success( "Image size sent to client: " + imageSize );

         /**
         * Wait for the client response
         */
        MessagePacket clientResponse = new MessagePacket();
        if( !network_intf_rx( clientResponse ) )
            {
            log_err("Unable to receive client response from image size message");
            return false;
            }

        /**
         * get the number of packets the client is expecting
         */
        int numPackets = Integer.parseInt( clientResponse.msg_data );

        /**
         * log the number of packets the client is expecting
         *
         */
        log_msg( "Client is expecting " + numPackets + " packets" );

        /* 
         * send the image one packet at a time using the number of packets
         * the client is expecting
         */
        int packetSize = MAX_IMG_CHUNK_SZ;
        int numBytesSent = 0;

        for( int i = 0; i < numPackets; i++ )
            {
            log_success( "Sending packet " + i + " of " + numPackets );
            /**
             * create the packet
             */
            String tx_str = new String( byteArrayOutputStream.toByteArray(), numBytesSent, packetSize );

            /**
             * send the packet
             */
            if( !network_intf_tx( tx_str ) )
                {
                log_err("Unable to send packet");
                return false;
                }

            /**
             * Wait for the client response
             */
            MessagePacket clientRxIMGResponse = new MessagePacket();
            if( !network_intf_rx( clientRxIMGResponse ) )
                {
                log_err("Unable to receive client response");
                return false;
                }
            

            /**
             * log the packet sent
             */
            log_success( "Packet " + i + " sent" );

            /**
             * increment the number of bytes sent
             */
            numBytesSent += packetSize;
            }
        
        return true;
    }

    private byte[] network_intf_rx
        (
        int flags 
        )
    {
        /*
        * create the buffer
        */
        byte[] buffer = new byte[ BUFF_SIZE ];

        /*
        * create the packet
        */
        DatagramPacket packet = new DatagramPacket( buffer, buffer.length );

        /*
        * receive the packet
        */
        try
            {
            this.socket.receive( packet );

            }
        catch( IOException e )
            {
            /*
            * log error
            *
            */
            log_err("Unable to receive packet");
            e.printStackTrace();
            }
            return buffer;
    }


    private boolean network_intf_tx
        ( String tx_str 
        ) 
    {
        /*
        Send the data over udp
         */
        try
            {
            /*
            * create the packet
            */
            DatagramPacket packet = new DatagramPacket(tx_str.getBytes(), tx_str.length() );
            packet.setAddress( InetAddress.getByName( hostname ) );
            packet.setPort( port );

            packet.setData( tx_str.getBytes() );


            /*
            * send the packet
            */
            this.socket.send( packet );

            /*
            * log success
            */
            log_success("Packet sent: " + tx_str);

            return true;
            }
        catch( IOException e )
            {
            /*
            * log error
            *
            */
            log_err("Unable to send packet");
            e.printStackTrace();
            return false;
            }

    }

    @Override
    public BufferedImage network_intf_rx() {
        
        /**
         * RX the image size
         */
        MessagePacket imageSizeMessage = new MessagePacket();
        if( !network_intf_rx( imageSizeMessage ) )
            {
            log_err("Unable to receive image size");
            return null;
            }
        
        /**
         * get the total image size from the message
         */
        int imageSize = Integer.parseInt( imageSizeMessage.msg_data );

        /*
         * Get the image ceiling of imageSize / ( 64k - 16 )
         */
        int numPackets = (int) Math.ceil( imageSize / MAX_IMG_CHUNK_SZ );
        
        /**
         * TX the client response
         */
        MessagePacket clientResponse = new MessagePacket();
        clientResponse.msg_data = String.valueOf( numPackets );

        if( !network_intf_tx( clientResponse ) )
            {
            log_err("Unable to send client response");
            return null;
            }
        
        /**
         * Recieve each image packet into the image buffer
         */
        byte[] imageBuffer = new byte[ imageSize ];
        int imageBufferIndex = 0;

        for( int i = 0; i < numPackets; i++ )
            {
            /**
             * log packet receiving
             */
            log_success( "Receiving packet " + i + " of " + numPackets );

            
            /**
             * Get the image data
             */
            byte[] imageData = network_intf_rx( 0 );
            
                        /**
             * log the packet received
             *
             */
            log_success( "Packet " + i + " received" );
            
            /**
             * Copy the image data into the image buffer
             */
            System.arraycopy( imageData, 0, imageBuffer, imageBufferIndex, imageData.length );
            imageBufferIndex += imageData.length;


            /**
             * Send the packet received response
             */
            MessagePacket packetReceivedResponse = new MessagePacket();
            packetReceivedResponse.msg_data = "Packet Received";
            if( !network_intf_tx( packetReceivedResponse ) )
                {
                log_err("Unable to send packet received response");
                return null;
                }
            }

        /**
         * Once we are done return the image up to the
         * calling function
         *
         */
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream( imageBuffer );
        BufferedImage image = null;
        try
            {
            image = ImageIO.read( byteArrayInputStream );
            }
        catch( IOException e )
            {
            log_err("Unable to convert byte array to image");
            return null;
            }

        return image;
    }

    /**
     * 
     * safe assert_msg_exit which will close resources
     * for this module
     */
    private void assert_msg_exit
        (
        boolean condition,
        String msg
        )
    {
        if( !condition )
            {
            this.socket.close();
            log_err(msg);
            System.exit(1);
            }
    }

    /**
     * pendingConnection
     */
    public boolean alert_other_clients
        (
        //void
        )
    {
        /**
         *  Generic interface does not
         *  support pending connections
         *
         *  Right now, this is used above
         *  when we recieve a packet that
         *  is not from our client
         */
        return false;
    }

}
