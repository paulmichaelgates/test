/**
* Message Packet
*
* Desc: High level representation of JSON data
*       passed between sockets
* REQ18 This class provides the interface for the header data.
        Two methods in this class are getJSON() and parseJSON()
        which are used to convert this object to and from JSON.

        The remainder of this requirement is implemented in the
        TCPNetworkIntf and UDPNetworkIntf classes.
*/
package common;

import org.json.JSONObject;

import server.GameLogic;

public class MessagePacket 
{
    /*
     * MESSAGE PACKET HEADER DATA
     */
    public GameState        gameState;
    public String           msg_data;
    public GamePacket       game_pkt;
    public boolean          img_flag    = false; /* for clarity  */

    /**
     * Gets the json representation of a message packet
     * Does not send null data
     * @return
     */
    public JSONObject getJSON()
    {
        JSONObject messageJSON = new JSONObject();

        if( gameState != null ) messageJSON.put("state", gameState.toString());
            else messageJSON.put("state", GameState.GS_UNK.toString());
            
        if( msg_data != null ) messageJSON.put("data",  msg_data);

        /**
         * Game Packet Data Initialization
         */
        if( game_pkt != null )
            {
            /**
             * Dont send null data
             */
            if( game_pkt.points    != null ) messageJSON.put("points",  game_pkt.points);
                else messageJSON.put("points",  "");

            if( game_pkt.updt_msg  != null ) messageJSON.put("updt_msg",  game_pkt.updt_msg);
                else messageJSON.put("updt_msg",  "");

            if( game_pkt.curr_word != null ) messageJSON.put("curr_word",  game_pkt.curr_word);
                else messageJSON.put("curr_word",  "");
            }
        else
            {
            messageJSON.put("points",  "");
            messageJSON.put("updt_msg",  "");
            messageJSON.put("curr_word",  "");
            }

        messageJSON.put( "img_flag", img_flag );

        return messageJSON;
        
    }
    /**
     * parseJSON
     * 
     * Desc:    takes in a json object and parses it into a message packet
     */
    public void parseJSON
        (
        JSONObject jsonData
        ) 
    {
        /**
         * Message Packet Data Initialization
         */
        img_flag   = Boolean.valueOf( jsonData.get( "img_flag").toString() );

        gameState           = GameState.valueOf(jsonData.get("state").toString());
        msg_data            = jsonData.get("data").toString();

        /**
         * Game Packet Data Initialization
         */
        game_pkt = new GamePacket();

        /*
         * parse the json String into a message packet
         */
        game_pkt.points     = jsonData.get("points").toString();
        game_pkt.updt_msg   = jsonData.get("updt_msg").toString();
        game_pkt.curr_word  = jsonData.get("curr_word").toString();      

    }

    public static MessagePacket getErrPacket
        (
        String err_msg
        )
    {
        MessagePacket err_pkt = new MessagePacket();
        err_pkt.msg_data = "ERROR: " + err_msg;



        return err_pkt;
    }
    

}

