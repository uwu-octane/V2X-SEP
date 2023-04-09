package de.ibr.v2x.data;


import de.ibr.v2x.data.client.V2XClient;
import de.ibr.v2x.data.server.V2XServer;

public class Main {

    public static V2XServer server;
    public static void main(String[] args) throws Exception {
        //new V2XParser();
        V2XClient.getInstance();
        server = new V2XServer();
    }










}


