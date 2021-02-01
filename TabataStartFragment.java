import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.dzensport.androidclient.DTO.GetParsableLogicForTimer;
import com.dzensport.androidclient.DTO.TabataDTO;
import com.dzensport.androidclient.DTO.TabataEventLogicDTO;
import com.dzensport.androidclient.MainActivity;
import com.dzensport.androidclient.R;
import com.dzensport.androidclient.db.CardViewImageGenerator;
import com.dzensport.androidclient.sharedprefs.TabataStateSharedPreferencesDTO;
import com.dzensport.androidclient.tabata.AdapterTabataSP;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class TabataStartFragment extends Fragment {

    //View
    ImageView tabataMainTypeImageView;
    Chronometer startTabataChronometer;
    Chronometer tabataStartAdditionalTotalChronometer;
    Animation rotate;
    ImageView tabataStartMainImageView_animation;
    ImageView startTabataSoundStateImageButton;
    FloatingActionButton tabataStartEndTimerButton;
    FloatingActionButton tabataStartNextButton;
    TextView startTabataChronometerStringTV_state;
    TextView tabataStartCardTypeTV;
    MediaPlayer gongTick;
    MediaPlayer gongStart;
    MediaPlayer gongStop;
    LinearLayout addTrainingProgramLLRoot;
    TextView TabataStartAdditionalTitleTV;

    int tabataMainTypeImageViewDrawableID;
    int tabataMainTypeImageViewPauseID;
    int tabataMainTypeImageViewPlayID;

    //Generated Views (nested card View list)
    List<TextView> additionalCardStringNumber = new ArrayList<>(); //number in string
    List<TextView> additionalCardStringName = new ArrayList<>(); //name in string
    List<TextView> additionalCardStringTime = new ArrayList<>(); //time in string
    List<String> tabataStartAdditionalTitleTVList = new ArrayList<>();

    //logic___________________
    TabataEventLogicDTO tabataEventLogicDTO;
    SharedPreferences sharedPrefsTabataState;
    private SharedPreferences.Editor editor = null;
    AdapterTabataSP adapterTabataSP = new AdapterTabataSP();
    TabataDTO tabataDTO = new TabataDTO();
    int firstStart5SecCounter = 0; //delay start timer 5sec after first launch fragment
    boolean firstStartDone = false; //+
    int idTabata = 1; //+
    float volume = 1.0f; //+
    //Cursors of time nav
    long pauseValue = 0;  //+
    int timerCursor = 0; //+
    boolean isTimerStart = false; // Play | Pause timer +
    long startTabataChronometerStringLong = 0;
    long tabataStartAdditionalTotalChronometerStringLong = 0;

    //19.10.2020 - logic (restore timer from service)
    long startTabataChronometerSetBase = 0;

    //Service
    Intent intent;
    boolean isAudioServiceLaunch = true;

    private boolean firstCreateView = true;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sharedPrefsTabataState = getActivity().getSharedPreferences(TabataStateSharedPreferencesDTO.TABATASTATELOCALFILENAME, Context.MODE_PRIVATE);
        //First start or restart fragment logic (GET DTO)
        if (getArguments() != null) {
            idTabata = getArguments().getInt("idtabata");
            tabataDTO = adapterTabataSP.getTabataFromSP(getContext(), idTabata);

            //Save arguments to shared prefs from intent
            editor = sharedPrefsTabataState.edit();
            editor.putBoolean(TabataStateSharedPreferencesDTO.FIRSTSTARTDONE, firstStartDone);
            editor.putInt(TabataStateSharedPreferencesDTO.IDOFSTARTTABATA, idTabata);
            editor.putFloat(TabataStateSharedPreferencesDTO.VOLUME, volume);
            editor.putLong(TabataStateSharedPreferencesDTO.PAUSEVALUE, pauseValue);
            editor.putInt(TabataStateSharedPreferencesDTO.TIMERCURSOR, timerCursor);
            editor.putBoolean(TabataStateSharedPreferencesDTO.ISTIMERSTART, isTimerStart);
            editor.commit();
            firstCreateView = false;

            startTabataChronometerSetBase = SystemClock.elapsedRealtime();
        }
        else {
            tabataDTO = adapterTabataSP.getTabataFromSP(getContext(), sharedPrefsTabataState.getInt(TabataStateSharedPreferencesDTO.IDOFSTARTTABATA, 1));
            //from shared prefs
            firstStartDone = sharedPrefsTabataState.getBoolean(TabataStateSharedPreferencesDTO.FIRSTSTARTDONE, false);
            idTabata = sharedPrefsTabataState.getInt(TabataStateSharedPreferencesDTO.IDOFSTARTTABATA, 1);
            volume = sharedPrefsTabataState.getFloat(TabataStateSharedPreferencesDTO.VOLUME, 1.0f);
            pauseValue = sharedPrefsTabataState.getLong(TabataStateSharedPreferencesDTO.PAUSEVALUE, 0);
            timerCursor = sharedPrefsTabataState.getInt(TabataStateSharedPreferencesDTO.TIMERCURSOR, 0);
            isTimerStart = sharedPrefsTabataState.getBoolean(TabataStateSharedPreferencesDTO.ISTIMERSTART, false);
            startTabataChronometerStringLong = sharedPrefsTabataState.getLong(TabataStateSharedPreferencesDTO.STARTTABATACHRONOMETERSTRINGLONG, 0);
            tabataStartAdditionalTotalChronometerStringLong = sharedPrefsTabataState.getLong(TabataStateSharedPreferencesDTO.TABATASTARTADDITIONALTOTALCHRONOMETERSTRINGLONG, 0);
            firstStart5SecCounter = 6; //skip first start delay

            startTabataChronometerSetBase = sharedPrefsTabataState.getLong(TabataStateSharedPreferencesDTO.STARTTABATACHRONOMETERSTRINGLONG, 0);

            firstCreateView = false;
        }
    }

    public View onCreateView(@NonNull final LayoutInflater inflater,
                             ViewGroup container, final Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_tabata_start, container, false);
        //do'nt lock screen
        getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        //get and create Elements in UI
        startTabataChronometerStringTV_state = root.findViewById(R.id.startTabataChronometerStringTV_state);
        tabataStartCardTypeTV = root.findViewById(R.id.tabataStartCardTypeTV);
        tabataStartEndTimerButton = root.findViewById(R.id.tabataStartEndTimerButton);
        tabataStartNextButton = root.findViewById(R.id.tabataStartNextButton);
        tabataMainTypeImageView = root.findViewById(R.id.tabataMainTypeImageView);
        startTabataSoundStateImageButton = root.findViewById(R.id.startTabataSoundStateImageButton);
        startTabataChronometer = root.findViewById(R.id.startTabataChronometer);
        tabataStartAdditionalTotalChronometer = root.findViewById(R.id.tabataStartAdditionalTotalChronometer);
        tabataStartMainImageView_animation = root.findViewById(R.id.tabataStartMainImageView_animation);
        TabataStartAdditionalTitleTV = root.findViewById(R.id.TabataStartAdditionalTitleTV);

        //Audio
        gongTick = MediaPlayer.create(getActivity(), R.raw.tick);
        gongStart = MediaPlayer.create(getActivity(), R.raw.gong);
        gongStop = MediaPlayer.create(getActivity(), R.raw.gongend);
        gongTick.setVolume(volume, volume);
        gongStart.setVolume(volume, volume);
        gongStop.setVolume(volume, volume);
        rotate = AnimationUtils.loadAnimation(getActivity(),R.anim.rotation);
        addTrainingProgramLLRoot = root.findViewById(R.id.addTrainingProgramLLRoot);

        //parse DTO to logic object of timer cycles
        tabataEventLogicDTO = GetParsableLogicForTimer.getParsableLogicForTimer(tabataDTO);

        //set Type of tabata in UI
        switch (tabataDTO.getTypeOfTabata()) {
            case 1:
                tabataStartCardTypeTV.setText(getString(R.string.buttonChangeType1));
                tabataMainTypeImageViewDrawableID = R.drawable.timer_type1;
                tabataMainTypeImageViewPauseID =  R.drawable.timer_type1_pause;
                tabataMainTypeImageViewPlayID =  R.drawable.timer_type1_play;
                break;
            case 2:
                tabataStartCardTypeTV.setText(getString(R.string.buttonChangeType2));
                tabataMainTypeImageViewDrawableID = R.drawable.timer_type2;
                tabataMainTypeImageViewPauseID =  R.drawable.timer_type2_pause;
                tabataMainTypeImageViewPlayID =  R.drawable.timer_type2_play;
                break;
            case 3:
                tabataStartCardTypeTV.setText(getString(R.string.buttonChangeType3));
                tabataMainTypeImageViewDrawableID = R.drawable.timer_type3;
                tabataMainTypeImageViewPauseID =  R.drawable.timer_type3_pause;
                tabataMainTypeImageViewPlayID =  R.drawable.timer_type3_play;
                break;
            case 4:
                tabataStartCardTypeTV.setText(getString(R.string.buttonChangeType4));
                tabataMainTypeImageViewDrawableID = R.drawable.timer_type4;
                tabataMainTypeImageViewPauseID =  R.drawable.timer_type4_pause;
                tabataMainTypeImageViewPlayID =  R.drawable.timer_type4_play;
                break;
        }


        //Volume ON / OFF logic (mute)
        startTabataSoundStateImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               if (volume == 1.0f) {
                startTabataSoundStateImageButton.setImageResource(R.drawable.ic_baseline_volume_off_30);
                volume = 0.0f;
               }
               else {
                   startTabataSoundStateImageButton.setImageResource(R.drawable.ic_baseline_volume_up_30);
                   volume = 1.0f;
               }
                gongTick.setVolume(volume, volume);
                gongStart.setVolume(volume, volume);
                gongStop.setVolume(volume, volume);
            }
        });


        //Start total chronometer after start fragment
        tabataStartAdditionalTotalChronometer.setBase(SystemClock.elapsedRealtime());
        tabataStartAdditionalTotalChronometer.setFormat("%s");
        tabataStartAdditionalTotalChronometer.start();

        //Cycle of sets chronometer
        startTabataChronometer.setFormat("%s");
        startTabataChronometer.setBase(startTabataChronometerSetBase);
        startTabataChronometer.start();

        tabataStartAdditionalTotalChronometer.setOnChronometerTickListener(new Chronometer.OnChronometerTickListener() {
            @Override
            public void onChronometerTick(Chronometer chronometer) {

                //first start. 5 sec delay and starting audio service
                if (!firstStartDone) {
                    gongTick.start();
                }
                if (getArguments() != null) {
                    firstStart5SecCounter++;
                    if (firstStart5SecCounter == 6) {
                        firstStartDone = true;
                        startTabataChronometer.setBase(SystemClock.elapsedRealtime());

                        //view animation
                        isTimerStart = true;
                        tabataStartMainImageView_animation.startAnimation(rotate);
                        tabataMainTypeImageView.setImageResource(tabataMainTypeImageViewPauseID);
                        startTabataChronometerStringTV_state.setTextColor(getResources().getColor(R.color.btnGo));
                        startTabataChronometerStringTV_state.setText("(" + convertTimeInSecToString(tabataEventLogicDTO.getTabataEventCycleList().get(timerCursor).getTimeInSec()) + ")");
                        gongStart.start();

                        TabataStartAdditionalTitleTV.setText(tabataStartAdditionalTitleTVList.get(0));
                    }
                }

                /////////////////////////////////////////////////////////////////////////
                //MAIN Timer logic after first start done!
                if(firstStartDone) {
                    //verify cycle and start next if done
                    if (SystemClock.elapsedRealtime() - startTabataChronometer.getBase() >= tabataEventLogicDTO.getTabataEventCycleList().get(timerCursor).getTimeInSec() * 1000) {

                        if (isTimerStart) { //if pause not active
                            //verify to and timer (from DTO object counter)

                            if (timerCursor < tabataEventLogicDTO.getTabataEventCycleList().size()-1) {
                                setNestedScrollViewData();
                                timerCursor++;
                                startTabataChronometer.setBase(SystemClock.elapsedRealtime());
                                setTimerDescription();
                                playLogicSound();
                            } else {
                                //end of tabata
                                finishTabataDialog();
                            }
                        }
                    }
                }

                //flashing animation of chronometer if pause
                if (!isTimerStart) {
                    if (startTabataChronometer.getVisibility() == View.VISIBLE) {
                        startTabataChronometer.setVisibility(View.INVISIBLE);
                    } else {  startTabataChronometer.setVisibility(View.VISIBLE); }
                }
                if (isTimerStart) { startTabataChronometer.setVisibility(View.VISIBLE); }


            }
        });

        tabataMainTypeImageView.setImageResource(tabataMainTypeImageViewDrawableID);
        tabataMainTypeImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(firstStartDone) { //block button if first launch fragment (5 sec delay)

                    if (!isTimerStart) {
                        playTime();
                    } else {
                        pauseTime();
                    }
                }
            }
        });


        ////Button = next cycle (DTO event)
        tabataStartNextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(firstStartDone) { //block button if first launch fragment (5 sec delay)
                    isTimerStart = true; //reset, if pause active
                    playTime();
                    if (timerCursor < tabataEventLogicDTO.getTabataEventCycleList().size() - 1) {
                        setNestedScrollViewData();
                        timerCursor++;
                        startTabataChronometer.setBase(SystemClock.elapsedRealtime());
                        setTimerDescription();
                        playLogicSound();
                    } else {
                        //конец табаты - диалог
                        finishTabataDialog();
                    }
                }
            }
        });

        //Button = end tabata. Start main activity
        tabataStartEndTimerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopTabataDialog();
            }
        });

        ///////////////////////////////////////////////
        //Additional Nested scroll view card UI ad logic
        //Add strings in card
        for (int i = 0; i < tabataEventLogicDTO.getTabataEventCycleList().size(); i++) {
            View addStringTimerCycle = LayoutInflater.from(getContext()).inflate(R.layout.addtp_training_string_mockup, null);
            addTrainingProgramLLRoot.addView(addStringTimerCycle);
            LinearLayout linearLayoutForParseView = (LinearLayout) addTrainingProgramLLRoot.getChildAt(i);
            LinearLayout linearLayoutForParseViewString = (LinearLayout) linearLayoutForParseView.getChildAt(0);

            TextView tvForParseNumber = (TextView) linearLayoutForParseViewString.getChildAt(0);
            additionalCardStringNumber.add(tvForParseNumber);
            TextView tvForParseName = (TextView) linearLayoutForParseViewString.getChildAt(1);
            additionalCardStringName.add(tvForParseName);
            TextView tvForParseTime = (TextView) linearLayoutForParseViewString.getChildAt(2);
            additionalCardStringTime.add(tvForParseTime);

            String strName = "";
            switch (tabataEventLogicDTO.getTabataEventCycleList().get(i).getStatusOfWorkType()) {
                case 1: //Work
                    //additional timer
                    if (tabataEventLogicDTO.isAdditionalTabata()) {
                        strName = tabataEventLogicDTO.getTabataEventCycleList().get(i).getName() + ". " +
                                getString(R.string.startTmerFragment_CardViewName_type1_work) + " " +
                                String.valueOf(tabataEventLogicDTO.getTabataEventCycleList().get(i).getNumberOfLap()) + "/" +
                                String.valueOf(tabataEventLogicDTO.getTabataEventCycleList().get(i).getLapsCount());
                    } else {
                        //simple timer
                        strName = getString(R.string.startTmerFragment_CardViewName_type1_work) + " " +
                                String.valueOf(tabataEventLogicDTO.getTabataEventCycleList().get(i).getNumberOfLap()) + "/" +
                                String.valueOf(tabataEventLogicDTO.getTabataEventCycleList().get(i).getLapsCount());
                    }
                    break;
                case 2: //Rest
                    strName = getString(R.string.startTmerFragment_CardViewName_type2_rest);
                    break;
                case 3: //big rest after exercise
                    strName = getString(R.string.startTmerFragment_CardViewName_type3_bigRest);
                    break;
            }
            tabataStartAdditionalTitleTVList.add(strName); //add generated DTO strings for title using
            additionalCardStringNumber.get(i).setText(String.valueOf(tabataEventLogicDTO.getTabataEventCycleList().get(i).getTotalNumberInCycle()));
            additionalCardStringName.get(i).setText(strName);
            additionalCardStringTime.get(i).setText(convertTimeInSecToString(tabataEventLogicDTO.getTabataEventCycleList().get(i).getTimeInSec()));

        }





        return root;
    }

    @Override
    public void onStart() {
        super.onStart();

    }

    @Override
    public void onResume() {
        super.onResume();

        if (!firstCreateView) {
            stopAudioService();
            getInstanceFromPrefs();
            recreateViewsAndDataAfterService(); //refactor fragment Views
        }
    }


    @Override
    public void onPause() {
        super.onPause();

        saveInstanceToPrefs();


        if (isAudioServiceLaunch) {
            startAudioService(); //starting service!
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    @Override
    public void onStop() {
        super.onStop();
    }


    public String convertTimeInSecToString(int timeInSec) {
        String time = "";
        String timeMin = "00";
        String timeSec = "00";

        int mins = timeInSec / 60;
        int secs = timeInSec - mins * 60;

        if (mins < 10) { timeMin = "0" + String.valueOf(mins); }
        else { timeMin = String.valueOf(mins); }

        if (secs < 10) { timeSec = "0" + String.valueOf(secs); }
        else { timeSec = String.valueOf(secs); }

        time = timeMin + ":" + timeSec;
        return time;
    }

    public void pauseTime() {
        pauseValue = startTabataChronometer.getBase() - SystemClock.elapsedRealtime();
        startTabataChronometer.stop();
        rotate.cancel();
        tabataStartMainImageView_animation.setAnimation(rotate);
        tabataMainTypeImageView.setImageResource(tabataMainTypeImageViewPlayID);
        isTimerStart = false;

        //put pause to SP for service
        editor = sharedPrefsTabataState.edit();
        editor.putBoolean(TabataStateSharedPreferencesDTO.ISTIMERSTART, isTimerStart);
        editor.commit();
        //////////////////////////////////

        setTimerDescription();
    }

    public void playTime() {
        tabataStartMainImageView_animation.startAnimation(rotate);
        startTabataChronometer.setBase(SystemClock.elapsedRealtime() + pauseValue);
        startTabataChronometer.start();
        tabataMainTypeImageView.setImageResource(tabataMainTypeImageViewPauseID);

        if (!isTimerStart) {
            playLogicSound();
        }
        isTimerStart = true;




        //put pause to SP for service
        editor = sharedPrefsTabataState.edit();
        editor.putBoolean(TabataStateSharedPreferencesDTO.ISTIMERSTART, isTimerStart);
        editor.commit();
        //////////////////////////////////

        setTimerDescription();

    }



    public void setTimerDescription() {
        int type = tabataEventLogicDTO.getTabataEventCycleList().get(timerCursor).getStatusOfWorkType();
        if (!isTimerStart) { //if click pause
            type = 4;
        }

        switch (type) {
            case 1: //play
                startTabataChronometerStringTV_state.setTextColor(getResources().getColor(R.color.btnGo));
                startTabataChronometerStringTV_state.setText("(" + convertTimeInSecToString(tabataEventLogicDTO.getTabataEventCycleList().get(timerCursor).getTimeInSec()) + ")");
                break;

            case 2: //pause
                startTabataChronometerStringTV_state.setText(getString(R.string.startTabataChronometerStringTV_state_REST));
                startTabataChronometerStringTV_state.setTextColor(getResources().getColor(R.color.yellow));
                break;

            case 3: //pause between exercises
                startTabataChronometerStringTV_state.setText(getString(R.string.startTabataChronometerStringTV_state_REST));
                startTabataChronometerStringTV_state.setTextColor(getResources().getColor(R.color.yellow));
                break;

            case 4: //pause between exercises
                startTabataChronometerStringTV_state.setText(getString(R.string.startTabataChronometerStringTV_state_PAUSE));
                startTabataChronometerStringTV_state.setTextColor(getResources().getColor(R.color.yellow));
                break;

            default:
                break;
        }
    }

    public void playLogicSound() {
        int type = tabataEventLogicDTO.getTabataEventCycleList().get(timerCursor).getStatusOfWorkType();

            switch (type) {
                case 1: //play
                    gongStart.start();
                    break;

                case 2: //pause
                    gongStop.start();
                    break;

                case 3: //pause between exercises
                    gongStop.start();
                    break;

                case 4: //pause between exercises
                    //gongStop.start();
                    break;

                default:
                    break;
            }
    }

    public void setNestedScrollViewData() {
       //Set color (done cycle)
       additionalCardStringNumber.get(timerCursor).setTextColor(getResources().getColor(R.color.tabata_txtStringDone));
       additionalCardStringName.get(timerCursor).setTextColor(getResources().getColor(R.color.tabata_txtStringDone));
       additionalCardStringTime.get(timerCursor).setTextColor(getResources().getColor(R.color.tabata_txtStringDone));

        TabataStartAdditionalTitleTV.setText(tabataStartAdditionalTitleTVList.get(timerCursor+1));
    }


    public void finishTabataDialog() {

        final Dialog dialogFinishTabata = new Dialog(getContext());
        dialogFinishTabata.setContentView(R.layout.dialog_finishtabata);
        dialogFinishTabata.show();

        //Stop all timers
        startTabataChronometer.stop();
        tabataStartAdditionalTotalChronometer.stop();

        //set joke motivator
        TextView dialogFinishTabata_motivationTV = dialogFinishTabata.findViewById(R.id.dialogFinishTabata_motivationTV);
        String[] motivators = getResources().getStringArray(R.array.motivator_strings_array);
        int rand = new Random().nextInt(motivators.length - 1 + 1);
        dialogFinishTabata_motivationTV.setText(motivators[rand]);

        //set Type image
        ImageView dialogFinishTabata_typeTPImage = dialogFinishTabata.findViewById(R.id.dialogFinishTabata_typeTPImage);
        CardViewImageGenerator cardViewImageGenerator = new CardViewImageGenerator();
        dialogFinishTabata_typeTPImage.setImageResource(cardViewImageGenerator.getTypeOfTabataIcon(getContext(), tabataDTO.getTypeOfTabata()));

        //set Total time
        TextView dialogFinishTabata_TotalTimeTV = dialogFinishTabata.findViewById(R.id.dialogFinishTabata_TotalTimeTV);
        String timeString = "";
        long totalTimeInSec = (SystemClock.elapsedRealtime() - tabataStartAdditionalTotalChronometer.getBase()) / 1000;
        timeString = getString(R.string.tabataStartCardTitleTV_TOTALTIME) + " " + convertTimeInSecToString((int) totalTimeInSec);
        dialogFinishTabata_TotalTimeTV.setText(timeString);
        
        Button dialogFinishTabata_button = dialogFinishTabata.findViewById(R.id.dialogFinishTabata_button);
        dialogFinishTabata_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isAudioServiceLaunch = false; //don`t start audio service
                editor = sharedPrefsTabataState.edit();
                editor.putBoolean(TabataStateSharedPreferencesDTO.TABATASTART, false);
                editor.commit();

                dialogFinishTabata.dismiss();
                Intent intent = new Intent(getContext().getApplicationContext(), MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK); //!!test
                getContext().startActivity(intent);


            }
        });

        //Update v 1.09 auto stop timer
        pauseTime();

    }

    public void stopTabataDialog() {
        final Dialog dialogStopTabata = new Dialog(getContext());
        dialogStopTabata.setContentView(R.layout.dialog_endtabata);
        dialogStopTabata.show();

        Button dialogEndTabataBtnStay = dialogStopTabata.findViewById(R.id.dialogEndTabataBtnStay);
        dialogEndTabataBtnStay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialogStopTabata.dismiss();
            }
        });

        Button dialogEndTabataBtnEnd = dialogStopTabata.findViewById(R.id.dialogEndTabataBtnEnd);
        dialogEndTabataBtnEnd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isAudioServiceLaunch = false; //don`t start audio service
                editor = sharedPrefsTabataState.edit();
                editor.putBoolean(TabataStateSharedPreferencesDTO.TABATASTART, false);
                editor.commit();

                dialogStopTabata.dismiss();
                Intent intent = new Intent(getContext().getApplicationContext(), MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK); //!!test
                getContext().startActivity(intent);

            }
        });
    }

    public void saveInstanceToPrefs() {
        sharedPrefsTabataState = getActivity().getSharedPreferences(TabataStateSharedPreferencesDTO.TABATASTATELOCALFILENAME, Context.MODE_PRIVATE);
        editor = sharedPrefsTabataState.edit();
        firstStartDone = true;
        //Save arguments to shared prefs from intent
        editor.putBoolean(TabataStateSharedPreferencesDTO.FIRSTSTARTDONE, firstStartDone);
        editor.putInt(TabataStateSharedPreferencesDTO.IDOFSTARTTABATA, idTabata);
        editor.putFloat(TabataStateSharedPreferencesDTO.VOLUME, volume);
        editor.putLong(TabataStateSharedPreferencesDTO.PAUSEVALUE, pauseValue);
        editor.putInt(TabataStateSharedPreferencesDTO.TIMERCURSOR, timerCursor);
        editor.putBoolean(TabataStateSharedPreferencesDTO.ISTIMERSTART, isTimerStart);
        editor.putLong(TabataStateSharedPreferencesDTO.STARTTABATACHRONOMETERSTRINGLONG, startTabataChronometer.getBase());
        editor.putLong(TabataStateSharedPreferencesDTO.TABATASTARTADDITIONALTOTALCHRONOMETERSTRINGLONG, tabataStartAdditionalTotalChronometer.getBase());
        editor.commit();
    }

    public void getInstanceFromPrefs() {
        sharedPrefsTabataState = getActivity().getSharedPreferences(TabataStateSharedPreferencesDTO.TABATASTATELOCALFILENAME, Context.MODE_PRIVATE);
        firstStartDone = sharedPrefsTabataState.getBoolean(TabataStateSharedPreferencesDTO.FIRSTSTARTDONE, false);
        idTabata = sharedPrefsTabataState.getInt(TabataStateSharedPreferencesDTO.IDOFSTARTTABATA, 1);
        volume = sharedPrefsTabataState.getFloat(TabataStateSharedPreferencesDTO.VOLUME, 1.0f);
        pauseValue = sharedPrefsTabataState.getLong(TabataStateSharedPreferencesDTO.PAUSEVALUE, 0);
        timerCursor = sharedPrefsTabataState.getInt(TabataStateSharedPreferencesDTO.TIMERCURSOR, 0);
        isTimerStart = sharedPrefsTabataState.getBoolean(TabataStateSharedPreferencesDTO.ISTIMERSTART, false);
        startTabataChronometerStringLong = sharedPrefsTabataState.getLong(TabataStateSharedPreferencesDTO.STARTTABATACHRONOMETERSTRINGLONG, 0);
        tabataStartAdditionalTotalChronometerStringLong = sharedPrefsTabataState.getLong(TabataStateSharedPreferencesDTO.TABATASTARTADDITIONALTOTALCHRONOMETERSTRINGLONG, 0);
        startTabataChronometerSetBase = sharedPrefsTabataState.getLong(TabataStateSharedPreferencesDTO.STARTTABATACHRONOMETERSTRINGLONG, 0);

        firstCreateView = false;
    }



    public void startAudioService() {
        if (isTimerStart) {
            intent = new Intent(getActivity(), AudioService.class);
            intent.putExtra("stop", false);
            //start service///////////////////////////////////////////////////////
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                getActivity().startForegroundService(intent);
            } else {
                getActivity().startService(intent);


            }
        }
    }

    public void stopAudioService() {
        System.out.println("!!!!!!!!!stopAudioService()!!!!!!");
        if (isTimerStart) {
            intent = new Intent(getActivity(), AudioService.class);
            intent.putExtra("stop", true);
            //start service///////////////////////////////////////////////////////
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                getActivity().startForegroundService(intent);
            } else {
                getActivity().stopService(intent);
            }
        }
    }


    public void recreateViewsAndDataAfterService() {
        ///////////////////////////////////////////////////////////////////////////////
        //Recreate after on destroy
        if (firstStartDone) {
            for (int i = 0; i < timerCursor; i++) {
                additionalCardStringNumber.get(i).setTextColor(getResources().getColor(R.color.tabata_txtStringDone));
                additionalCardStringName.get(i).setTextColor(getResources().getColor(R.color.tabata_txtStringDone));
                additionalCardStringTime.get(i).setTextColor(getResources().getColor(R.color.tabata_txtStringDone));
            }
            TabataStartAdditionalTitleTV.setText(tabataStartAdditionalTitleTVList.get(timerCursor));
            setTimerDescription();
            if (volume == 0) {
                startTabataSoundStateImageButton.setImageResource(R.drawable.ic_baseline_volume_off_30);
            }


            //time reload
            if (isTimerStart) { //if timer start
                playTime();
                startTabataChronometer.setBase(startTabataChronometerStringLong);
            } else { //if pause active
                pauseTime();
                pauseValue = sharedPrefsTabataState.getLong(TabataStateSharedPreferencesDTO.PAUSEVALUE, 0);
                startTabataChronometer.setBase(SystemClock.elapsedRealtime() + pauseValue );
            }
            tabataStartAdditionalTotalChronometer.setBase(tabataStartAdditionalTotalChronometerStringLong);

        }

    }

}
