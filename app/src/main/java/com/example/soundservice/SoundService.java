package com.example.soundservice;

import android.util.Log;

import java.util.Arrays;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.android.AudioDispatcherFactory;
import be.tarsos.dsp.mfcc.MFCC;
import be.tarsos.dsp.pitch.PitchDetectionHandler;
import be.tarsos.dsp.pitch.PitchDetectionResult;
import be.tarsos.dsp.pitch.PitchProcessor;

public class SoundService {

    private static final String SOUNDSERVICE_TAG = "SOUNDSERVICE_TAG";
    Long currentTime;

    private final static int audioBufferSize = 1024;
    private final static int samplingRate = 11025;
    private final static int bufferOverlap = 1024; //half of audioBufferSize
    private final static int amountOfMelFilters = 20;
    private final static int amountOfCepstrumCoef = 30;
    private final static float lowerFilterFreq = 133.33f;
    private final static float upperFilterFreq = 8000f;

    public void extractSignalEnergy() {

        // Audio signal energy is commonly expressed in decibel sound pressure level (dBSPL)
        // Which is calculated by formula
        // Sound energy = intensity


        //2048 is a usual buffer size
        AudioDispatcher dispatcher = AudioDispatcherFactory.fromDefaultMicrophone(samplingRate, audioBufferSize, 0);

        //some devices do not support 44100Hz sampling rate, and so they throw exceptions
        if (dispatcher != null) {
            dispatcher.addAudioProcessor(new AudioProcessor() {
                //TODO check the threshold
                float threshold = -70;//dB

                @Override
                public boolean process(AudioEvent audioEvent) {
                    float[] buffer = audioEvent.getFloatBuffer();
                    //TODO upload to server (sound energy/intensity(loudness))
                    double energy = soundPressureLevel(buffer);
                    currentTime = System.currentTimeMillis();
                    Log.i(SOUNDSERVICE_TAG, currentTime.toString() + " Signal Energy " + energy);

                    //noise and silence detection
                    //TODO upload to server (noisy state)
                    if (energy > threshold) {
                        currentTime = System.currentTimeMillis();
                        Log.i(SOUNDSERVICE_TAG, currentTime.toString() + "Noisy state detected" + "\nENERGY level: " + energy);

                    } else {
                        //TODO upload to server (silence state)
                        currentTime = System.currentTimeMillis();
                        Log.i(SOUNDSERVICE_TAG, currentTime.toString() + " Silence detected" + "\nENERGY level: " + energy);
                    }

                    return true;
                }

                @Override
                public void processingFinished() {
                }

                //soundPressureLevel returns dBSPL for a buffer (energy)
                private double soundPressureLevel(final float[] buffer) {
                    double power = 0.0D;
                    for (float element : buffer) {
                        power += element * element;
                    }
                    double value = Math.pow(power, 0.5) / buffer.length;
                    return 20.0 * Math.log10(value);
                }

            });
        } else {
            Log.e(SOUNDSERVICE_TAG, "extractSignalEnergy: Dispatcher is null");
        }

    }


    PitchDetectionHandler handler = new PitchDetectionHandler() {

        //pitch is sound frequency (Hz)
        @Override
        public void handlePitch(PitchDetectionResult pitchDetectionResult,
                                AudioEvent audioEvent) {

            //TODO upload to server (pitch)
            currentTime = System.currentTimeMillis();
            Log.i(SOUNDSERVICE_TAG, currentTime.toString() + " Pitch: " + pitchDetectionResult.getPitch());
        }
    };

    public void extractPitch() {

//        In extractPitch() first a handler is created which simply prints the detected pitch
//         The AudioDispatcher is attached to the default microphone and has a buffer size of 2048
//         For pitch detection buffer size of 2048 samples is reasonable
//         An audio processor that detects pitch is added to the AudioDispatcher
//         The handler is used there as well.

        AudioDispatcher dispatcher = AudioDispatcherFactory.fromDefaultMicrophone(samplingRate, audioBufferSize, 512);

        dispatcher.addAudioProcessor(new PitchProcessor(PitchProcessor.PitchEstimationAlgorithm.YIN, samplingRate, audioBufferSize, handler));
        dispatcher.run(); //starts a new thread
    }


    public void extractMFCC() {

        AudioDispatcher dispatcher = AudioDispatcherFactory.fromDefaultMicrophone(samplingRate, audioBufferSize, 0);
        if (dispatcher != null) {
            final MFCC mfccProcessor = new MFCC(audioBufferSize, samplingRate,
                    amountOfCepstrumCoef, amountOfMelFilters, lowerFilterFreq, upperFilterFreq);

            dispatcher.addAudioProcessor(mfccProcessor);
            dispatcher.addAudioProcessor(new AudioProcessor() {

                @Override // gets called on each audio frame
                public boolean process(AudioEvent audioEvent) {
                    float[] mfccs = mfccProcessor.getMFCC();

                    //TODO upload to server (MFCC array)
                    currentTime = System.currentTimeMillis();
                    Log.i(SOUNDSERVICE_TAG, currentTime.toString() + " MFCC: " + Arrays.toString(mfccs));
                    return true;
                }

                @Override
                public void processingFinished() {
                }
            });
            dispatcher.run();// starts a new thread
        } else {
            Log.e(SOUNDSERVICE_TAG, "extractMFCC: Dispatcher is null");
        }
    }

    public void calculateJitter() {

        // Jitter is measured in seconds
        // Jitter is the mean absolute (non-negative) difference in consecutive intervals
        // To calculate Jitter we need to calculate period T = floor(Fs/F0),
        // where Fs is sample rate, F0 is average frequency


    }

}

