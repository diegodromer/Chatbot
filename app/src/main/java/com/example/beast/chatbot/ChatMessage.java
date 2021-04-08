package com.example.beast.chatbot;

//Classe ChatMessage que vai ser salva no firebase

public class ChatMessage {

    private String msgText;
    private String msgUser;


    public ChatMessage(String msgText, String msgUser) {
        this.msgText = msgText;
        this.msgUser = msgUser;

    }


    public ChatMessage() {

    }

    public String getMsgText() {
        return msgText;
    }

    public String getMsgUser() {
        return msgUser;
    }

}
