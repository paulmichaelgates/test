    package server;
    /**
     * GameReturnCode
     * 
     * Desc:    specifies type of game we are playing
     *          i.e., country or a city
     * 
     * Notes:   this type is not used to provide point
     *          values to the user, user module on top
     *          of this will need to poll the points
     *          which are updated and contained within
     *          this here module
     */
    public enum GameReturnCode
    {
        GM_CODE_UNK,    /* game ret is not known         */
        GM_CODE_INV_MV, /* Invalid move                  */
        GM_CODE_WD_LS,  /* Point loss for word           */
        GM_CODE_CH_LS,  /* Point loss for char           */
        GM_CODE_CH_WN,  /* Point for char                */
        GM_CODE_PL_LS,  /* The player has lost the game  */
        GM_CODE_PL_WN,   /* The player has won the game   */
        GM_CODE_CNT
    }
