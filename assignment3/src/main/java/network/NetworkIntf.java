package network;

import common.MessagePacket;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.nio.Buffer;

public interface NetworkIntf { 
    /**
     * network_intf_rx
     * 
     */
    public  boolean network_intf_rx
        (
        MessagePacket msg
        );
    
    /**
     * network_intf_tx
     * 
     */
    public boolean network_intf_tx
        (
        MessagePacket msg
        );

    /**
     * network_intf_tx
     * 
     */
    public boolean network_intf_tx
        (
        BufferedImage image
        );

    /**
     * network_intf_rx
     * 
     */
    public BufferedImage network_intf_rx
        (
            //void
        );

    /**
     * pendindConnection
     */
    public boolean alert_other_clients
        (
        //void
        );
}
