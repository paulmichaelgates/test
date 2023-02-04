package common;

import network.NetworkIntf;
import network.UDPNetworkIntf;
import network.TCPNetworkIntf;
import common.UDPPublic;

import server.network.ServerTCPNetworkIntf;

import static common.ConsoleLogger.*;

import java.awt.image.BufferedImage;

/**
 * NetworkUserAPIs
 * 
 * Desc: This class is used to provide a common user interface
 *       For talking to the low level network interfaces
 *       
 */
public class NetworkUserAPIs 
{

    private int               PROTOCOL_MAX_ATTMPTS = 1; /* default attempts to 1 */
    private int               PROTOCOL_DELAY       = 0;       /* default timeout to 0 seconds */

    private NetworkIntf       networkIntf          = null;

    /**
     * NetworkUserAPIs
     */
    public NetworkUserAPIs
        (
        String  conString,
        String  host,
        int     port,
        boolean isServer
        )
    {
        if( conString.equals("-udp") )
            {
            /**
             * Create the UDP Network Interface
             */
            networkIntf = new UDPNetworkIntf( host, port, isServer );

            /**
             * Set the UDP Configuration
             */
            PROTOCOL_MAX_ATTMPTS = UDPPublic.UDP_ATTEMPTS;
            PROTOCOL_DELAY       = UDPPublic.UDP_DELAY;
            }
        else if( conString.equals( "-tcp") )
            {
            /**
             * Create the TCP Network interface
             * 
             * Note that the it is a given that the TCP 
             * socket will live on local host
             */
            networkIntf = new ServerTCPNetworkIntf( port );
            }
        /**
         * Unkown configuration string
         *
         */
        else
            {
            log_err( "Unknown configuration string" );
            }
    }

    /**
     * userNetworkRX
     */
    public boolean userNetworkRX
        (
        MessagePacket messagePacket
        )
    {
        /**
         * set up local vars
         */
        boolean success = false;

        /**
         * Attempt to receive the message
         * Note: For TCP this should just simply
         * send the message with no delays or 
         * repeated attempts
         */
        for( int i = 0; i < PROTOCOL_MAX_ATTMPTS; i++ )
            {
            /*
             * Attempt to receive the message, if we fail
             * go ahead and try again. That was the
             * whole point of abstracting this module
             * for 20 minutes
             */
            success = networkIntf.network_intf_rx( messagePacket );
            if( success )
                {
                break;
                }
            
            /* 
            * Delay for next attempt
            */
            delayForOnePeriod();
            }

        return success;
    }

    /**
     * userNetworkTX
     */
    public boolean userNetworkTX
        (
        MessagePacket messagePacket
        )
    {
        /**
         * set up local vars
         */
        boolean success = false;

        /**
         * Attempt to send the message         
         * Note: For TCP this should just simply
         * receive the message with no delays or 
         * repeated attempts
         */
        for( int i = 0; i < PROTOCOL_MAX_ATTMPTS; i++ )
            {
            /**
             * Attempt to send the message, if we fail
             * go ahead and try again. That was the 
             * whole point of abstracting this module
             * for 20 minutes
             */
            success = networkIntf.network_intf_tx( messagePacket );
            if ( success )
                {
                break;
                }
            /* 
            * Delay for next attempt
            */
           delayForOnePeriod();

            
            }
        return success;
    }

    /**
     * userNetworkSendCommandWaitForResponse
     * @param image
     */

    public boolean userNetworkSendCommandWaitForResponse
        (
        MessagePacket messagePacket,
        MessagePacket responsePacket
        )
    {

        /**
         * set up local vars
         *
         */
        boolean success = false;

        for( int i = 0; i < PROTOCOL_MAX_ATTMPTS; i++ )
            {
            /**
             * Attempt to send the message
             */
            success = this.userNetworkTX( messagePacket );

            if( success )
                {
                /**
                 * Attempt to receive the message
                 */
                if( this.userNetworkRX( responsePacket ) )
                    {
                    break;
                    }
                }

            /*
             * log the lack of response
             */
            log_err( "No response Recieved....Trying again. ATTEMPT=" + ( i + 1 ) );
             
            /**
             * 
             * Delay for next attempt 
             */
            delayForOnePeriod();

            }

        return success;
    }


    private void delayForOnePeriod
        (
        //void
        )
    {
        /**
         * 
         * Delay for next attempt
         *
         */
        try
            {
            Thread.sleep( PROTOCOL_DELAY );
            }
        catch( InterruptedException e )
            {
            log_err( "Interrupted Exception" );
            }
    }
    /**
     *  userNetworkTX
     *
     */
    public void userNetworkTX
        (
        BufferedImage image
        )
    {
        /**
         * Attempt to send the message         
         * Note: For TCP this should just simply
         * receive the message with no delays or 
         * repeated attempts
         */
        for( int i = 0; i < PROTOCOL_MAX_ATTMPTS; i++ )
            {
            
            networkIntf.network_intf_tx( image );
            /* 
            * Delay for next attempt
            */
           delayForOnePeriod();
            }
    }

    /**
     * userNetworkRX
     */
    public BufferedImage userNetworkRX
        (
        //void
        )
        {
        /**
         * Image to return
         */
        BufferedImage image = null;

        /**
         * Attempt to receive the message
         * Note: For TCP this should just simply
         * send the message with no delays or 
         * repeated attempts
         */
        for( int i = 0; i < PROTOCOL_MAX_ATTMPTS; i++ )
            {
            image = networkIntf.network_intf_rx(  );
            /* 
            * Delay for next attempt
            */
            delayForOnePeriod();
            }

            return image;
        }


        /**
         * connectionIsOpen
         *
         */
        public boolean connectionIsOpen
            (
            //void
            )
        {
        return  ( networkIntf != null );
        }

        /**
         * dealWithOtherClients
         * REQ21: If another client is trying to connect send them
         *      a nice little error message
         */
        public boolean dealWithOtherClients
            (
            //void
            )
        {
            /**
             * check to see if any other clients are
             * trying to connect to the server
             */
            return networkIntf.alert_other_clients();
        }
}
