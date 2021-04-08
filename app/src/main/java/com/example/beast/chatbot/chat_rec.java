package com.example.beast.chatbot;


import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

//Classe para colocar o lado do texto na caixa de msg
//direito eh o usuario esquerdo o bot

public class chat_rec extends RecyclerView.ViewHolder {

    final TextView leftText;
    final TextView rightText;

    public chat_rec(View itemView) {
        super(itemView);

        leftText = itemView.findViewById(R.id.leftText);
        rightText = itemView.findViewById(R.id.rightText);


    }
}
