package com.example.beast.chatbot;

import com.google.firebase.database.FirebaseDatabase;

//Classe para configuraçao do firebase setando como persistente

public class FirebaseApp extends android.app.Application {

    @Override
    public void onCreate() {
        super.onCreate();
        FirebaseDatabase.getInstance().setPersistenceEnabled(true);
    }
}
