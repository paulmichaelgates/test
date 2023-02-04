/**
 * Author: Paul Gates
 * 
 * An actionlistener type that allows for project
 * message data to be passed back and forth
 */
package common;

import java.awt.event.*;

public abstract class MessageActionListener implements ActionListener
{
    /**
     * Modifiable message packet
     */
    public MessagePacket messagePacket;

    public MessageActionListener
        (
        //void
        )
    {
        this.messagePacket = new MessagePacket();
    }
}
