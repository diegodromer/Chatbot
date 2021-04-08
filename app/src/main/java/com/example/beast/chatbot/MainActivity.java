package com.example.beast.chatbot;

import android.Manifest;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.provider.Settings.Secure;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Locale;

import ai.api.AIDataService;
import ai.api.AIListener;
import ai.api.AIServiceException;
import ai.api.android.AIConfiguration;
import ai.api.android.AIService;
import ai.api.model.AIRequest;
import ai.api.model.AIResponse;
import ai.api.model.Result;

public class MainActivity extends AppCompatActivity implements AIListener {

    RecyclerView recyclerView; //recyclerView para pegar info
    EditText editText;
    RelativeLayout addBtn;
    DatabaseReference ref;
    FirebaseRecyclerAdapter<ChatMessage, chat_rec> adapter;
    Boolean flagFab = true;
    Boolean respAud = false;

    String addressID;
    String address = "";
    int ativaID = 0; //0 = ID e 1 = wifi, serve para saber qual metodo usar

    private AIService aiService;

    private TextToSpeech tts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, 1);

        recyclerView = findViewById(R.id.recyclerView);
        editText = findViewById(R.id.editText);
        addBtn = findViewById(R.id.addBtn);

        recyclerView.setHasFixedSize(true);
        final LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(linearLayoutManager);

        // referencia do firebase
        ref = FirebaseDatabase.getInstance().getReference();

        ref.keepSynced(true);

        //chave da sua api no site dialogflow
        final AIConfiguration config = new AIConfiguration("da771b4f9dfe441b9bfb64d9456d592a", //IMPORTANTE: CHAVE DA API ONLINE
                AIConfiguration.SupportedLanguages.PortugueseBrazil, //talvez tenha que mudar para portugues para fazer o comando de voz, mas tambem deve alterar na api para portugues
                AIConfiguration.RecognitionEngine.System);

        aiService = AIService.getService(this, config);
        aiService.setListener(this);

        final AIDataService aiDataService = new AIDataService(config);
        final AIRequest aiRequest = new AIRequest();


        //tratando a versao do android para recurso de identificacao do android 8-API level 26
        if (Build.VERSION.SDK_INT >= 26) { //recurso android 8 identificacao ID
            //novo metodo de identificacao Android 8
            addressID = Secure.getString(getContentResolver(), Secure.ANDROID_ID);
            ativaID = 0;
        } else { //recurso android versoes anteriores
            ///metodo anterior, nao muito eficiente
            WifiManager manager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            assert manager != null;
            WifiInfo info = manager.getConnectionInfo();
            address = info.getMacAddress();
            ativaID = 1;
        }

        tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int i) {
                tts.setLanguage(new Locale("pt-BR"));
            }
        });

        //botao que envia a msg
        addBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                String message = editText.getText().toString().trim();

                if (!message.equals("")) {
                    ChatMessage chatMessage = new ChatMessage(message, "user");
                    if (ativaID == 0) {
                        ref.child("chat" + "/" + addressID).push().setValue(chatMessage); //chat padrao
                        //ref.child("chatArt" + "/" + address).push().setValue(chatMessage); //usado para fazer estudo de caso do artigo
                    } else {
                        ref.child("chat" + "/" + address).push().setValue(chatMessage); //chat padrao
                    }
                    //executando a api

                    aiRequest.setQuery(message);
                    new AsyncTask<AIRequest, Void, AIResponse>() {

                        @Override
                        protected AIResponse doInBackground(AIRequest... aiRequests) {
                            final AIRequest request = aiRequests[0];
                            try {
                                return aiDataService.request(aiRequest);
                            } catch (AIServiceException ignored) {
                            }
                            return null;
                        }

                        @Override
                        protected void onPostExecute(AIResponse response) { //mandar a voz por aqui
                            if (response != null) {

                                Result result = response.getResult(); //guarda a string do usuario
                                String reply = result.getFulfillment().getSpeech(); //guarda a string do bot

                                //fazendo o sintetizador de voz
                                if (Build.VERSION.SDK_INT >= 21 && tts != null) {
                                    if (respAud) {
                                        tts.speak(reply, TextToSpeech.QUEUE_FLUSH, null, null);
                                    }
                                }

                                ChatMessage chatMessage = new ChatMessage(reply, "bot");
                                if (ativaID == 0) {
                                    ref.child("chat" + "/" + addressID).push().setValue(chatMessage);
                                } else {
                                    ref.child("chat" + "/" + address).push().setValue(chatMessage);
                                    //ref.child("chatArt" + "/" + address).push().setValue(chatMessage); //teste pro artigo
                                }
                            }
                        }
                    }.execute(aiRequest);
                    //ao executar as perguntas e respostas da api, o código acima as salva
                    //no firebase, para que possa ser recuperado em tempo real na recycleView

                } else {
                    aiService.startListening(); //se o usuario nao digita nada a api pega automatico a voz e faz a devida
                    // analise de cordo com o que foi treinado para responder
                    respAud = true; //indica que deve ser respondido com audio
                }

                editText.setText("");//limpando o campo de envio de msg

            }
        });

        addBtn.setOnTouchListener(new View.OnTouchListener() { //colocar o som de click para segurar o botao, nao ta dando certo

            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                //MediaPlayer mp = MediaPlayer.create(getApplicationContext(), R.raw.clickstart);
                //mp.start();
                //mp.seekTo(0);
                return false;
            }
        });

        //imagem para enviar o texto caso tenha alguma coisa escrita ou enviar audio caso o usuario pressione e segure o botao
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                ImageView fab_img = findViewById(R.id.fab_img);
                Bitmap img = BitmapFactory.decodeResource(getResources(), R.drawable.imagem_enviar);
                Bitmap img1 = BitmapFactory.decodeResource(getResources(), R.drawable.imagem_microfone);


                if (s.toString().trim().length() != 0 && flagFab) { //texto digitado muda a imagem
                    ImageViewAnimatedChange(MainActivity.this, fab_img, img);
                    flagFab = false;

                } else if (s.toString().trim().length() == 0) { //sera enviado audio permanesse a imagem, como true padrao
                    ImageViewAnimatedChange(MainActivity.this, fab_img, img1);
                    flagFab = true;
                }


            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        //recuperando as informaçoes do firebase para carregar na recycleView (ListView)
        //carrega as mensagens do firebase no adpter para chamar na tela
        if (ativaID == 0) {
            adapter = new FirebaseRecyclerAdapter<ChatMessage, chat_rec>(
                    ChatMessage.class, R.layout.msglist, chat_rec.class, ref.child("chat" + "/" + addressID)) {
                //adapter = new FirebaseRecyclerAdapter<ChatMessage, chat_rec>(ChatMessage.class, R.layout.msglist, chat_rec.class, ref.child("chatArt" + "/" + address)) {
                //adapter trocado de chat pra chatArt para fazer testes de mesa do artigo
                @Override
                protected void populateViewHolder(chat_rec viewHolder, ChatMessage model, int position) {

                    if (model.getMsgUser().equals("user")) {


                        viewHolder.rightText.setText(model.getMsgText());

                        viewHolder.rightText.setVisibility(View.VISIBLE);
                        viewHolder.leftText.setVisibility(View.GONE);
                    } else {
                        viewHolder.leftText.setText(model.getMsgText());

                        viewHolder.rightText.setVisibility(View.GONE);
                        viewHolder.leftText.setVisibility(View.VISIBLE);
                    }
                }
            };
        } else {
            adapter = new FirebaseRecyclerAdapter<ChatMessage, chat_rec>(
                    ChatMessage.class, R.layout.msglist, chat_rec.class, ref.child("chat" + "/" + address)) {
                //adapter = new FirebaseRecyclerAdapter<ChatMessage, chat_rec>(ChatMessage.class, R.layout.msglist, chat_rec.class, ref.child("chatArt" + "/" + address)) {
                //adapter trocado de chat pra chatArt para fazer testes de mesa do artigo
                @Override
                protected void populateViewHolder(chat_rec viewHolder, ChatMessage model, int position) {

                    if (model.getMsgUser().equals("user")) {


                        viewHolder.rightText.setText(model.getMsgText());

                        viewHolder.rightText.setVisibility(View.VISIBLE);
                        viewHolder.leftText.setVisibility(View.GONE);
                    } else {
                        viewHolder.leftText.setText(model.getMsgText());

                        viewHolder.rightText.setVisibility(View.GONE);
                        viewHolder.leftText.setVisibility(View.VISIBLE);
                    }
                }
            };
        }

        //organizando a recyclerView
        adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);

                int msgCount = adapter.getItemCount();
                int lastVisiblePosition = linearLayoutManager.findLastCompletelyVisibleItemPosition();

                if (lastVisiblePosition == -1 ||
                        (positionStart >= (msgCount - 1) &&
                                lastVisiblePosition == (positionStart - 1))) {
                    recyclerView.scrollToPosition(positionStart);

                }

            }
        });

        recyclerView.setAdapter(adapter);


    }

    //transicao do icone
    public void ImageViewAnimatedChange(Context c, final ImageView v, final Bitmap new_image) {
        final Animation anim_out = AnimationUtils.loadAnimation(c, R.anim.zoom_out);
        final Animation anim_in = AnimationUtils.loadAnimation(c, R.anim.zoom_in);

        anim_out.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                v.setImageBitmap(new_image);
                anim_in.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                    }
                });
                v.startAnimation(anim_in);
            }
        });
        v.startAnimation(anim_out);
    }

    @Override
    public void onResult(ai.api.model.AIResponse response) {

        //faz o filtro para cada mac anddress dos celulares (precisa fazer para dados moveis tb)
        WifiManager manager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        assert manager != null;
        WifiInfo info = manager.getConnectionInfo();
        final String address = info.getMacAddress();

        //pegando string da pessoa
        Result result = response.getResult();

        //salvando conversa do usuario
        String message = result.getResolvedQuery();
        ChatMessage chatMessage0 = new ChatMessage(message, "user");
        ref.child("chat" + "/" + address).push().setValue(chatMessage0);
        //ref.child("chatArt" + "/" + address).push().setValue(chatMessage0);
        //salvando conversa do bot
        String reply = result.getFulfillment().getSpeech();//pegando string da pessoa
        ChatMessage chatMessage = new ChatMessage(reply, "bot");
        ref.child("chat" + "/" + address).push().setValue(chatMessage);
        //ref.child("chatArt" + "/" + address).push().setValue(chatMessage);

        //sintetizador de voz
        if (Build.VERSION.SDK_INT >= 21 && tts != null) {
            tts.speak(reply, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }

    @Override
    public void onError(ai.api.model.AIError error) {

    }

    @Override
    public void onAudioLevel(float level) {

    }

    @Override
    public void onListeningStarted() {

    }

    @Override
    public void onListeningCanceled() {

    }

    @Override
    public void onListeningFinished() {

    }
}
