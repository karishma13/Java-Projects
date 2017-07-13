
import java.awt.*;
import javax.swing.*;
import javax.sound.midi.*;
import java.util.*;
import java.awt.event.*;

public class BeatBox {
 

 JPanel mainPanel;
 ArrayList<JCheckBox> checkboxList;//We store the checkboxes in an ArrayList
 Sequencer sequencer;
 Sequence sequence;
 Track track;
 JFrame theFrame;
 
 /*These are the names of the instruments, as a String array, for buildig
 the GUI labels (on each row)*/
 String[] instrumentNames = {"Bass Drum", "Closed Hi-Hat", "Open Hi-Hat",
  "Acoustic Snare", "Crash Cymbal", "Hand Clap", "High Tom", "Hi Bongo",
  "Maracas", "Whistle", "Low Conga", "Cowbell", "Vibraslap", "Low-mid Tom",
  "High Agogo", "Open Hi Conga"};
  
 /*These represent the actual drum 'keys'. The drum channel is like a piano,
 except each 'key' on the piano is a different drum. So the number '35' is
 the key for the Bass Drum, 42 is Closed Hi-Hat, etc.*/
 int[] instruments = {35,42,46,38,49,39,50,60,70,72,64,56,58,47,67,63};
 
 public static void main (String[] args) {
  new BeatBox().buildGUI();
 }
 
 public void buildGUI() {
  theFrame = new JFrame("BeatBox");
  theFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
  BorderLayout layout = new BorderLayout();
  JPanel background = new JPanel(layout);
  background.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
  /*An 'empty border' gives us a margin between the edges of the panel
  and where the components are placed.*/
  
  checkboxList = new ArrayList<JCheckBox>();
  Box buttonBox = new Box(BoxLayout.Y_AXIS);
  
  JButton start = new JButton("Start");
  start.addActionListener(new MyStartListener());
  buttonBox.add(start);
  
  JButton stop = new JButton("Stop");
  stop.addActionListener(new MyStopListener());
  buttonBox.add(stop);
  
  JButton upTempo = new JButton("Tempo Up");
  upTempo.addActionListener(new MyUpTempoListener());
  buttonBox.add(upTempo);
  
  JButton downTempo = new JButton("Tempo Down");
  downTempo.addActionListener(new MyDownTempoListener());
  buttonBox.add(downTempo);
  
  Box nameBox = new Box(BoxLayout.Y_AXIS);
  for (int i = 0; i < 16; i++) {
   nameBox.add(new Label(instrumentNames[i]));
  }
  
  background.add(BorderLayout.EAST, buttonBox);
  background.add(BorderLayout.WEST, nameBox);
  
  theFrame.getContentPane().add(background);
  
  GridLayout grid = new GridLayout(16,16);
  grid.setVgap(1);
  grid.setHgap(2);
  mainPanel = new JPanel(grid);
  background.add(BorderLayout.CENTER, mainPanel);
  
  /*Make the checkboxes, set them to 'false'  and
  add them to the ArrayList AND to the GUI panel*/
  
  for (int i = 0; i < 256; i++) {
   JCheckBox c = new JCheckBox();
   c.setSelected(false);
   checkboxList.add(c);
   mainPanel.add(c);
  }
  
  setUpMidi();
  
  theFrame.setBounds(50,50,300,300);
  theFrame.pack();
  theFrame.setVisible(true);
 }
 
 //The usual MIDI set-up stuff for getting the Sequencer, the Sequence, the Track.
 public void setUpMidi() {
  try {
   sequencer = MidiSystem.getSequencer();
   sequencer.open();
   sequence = new Sequence(Sequence.PPQ,4);
   track = sequence.createTrack();
   sequencer.setTempoInBPM(120);
  } catch (Exception e) {e.printStackTrace();}
 }
 
 /*This is where it all happens! Where we turn checkbox state into MIDI events,
 and add them to the Track*/
 public void buildTrackAndStart() {
  /*We'll make a 16-element array to hold the values for one instrument,
  across all 16 beats. If the instrument is supposed to play on that beat,
  the value at that element will be the key. If that instrument is NOT supposed
  to play on that beat, put in a zero*/
  int[] trackList = null;
  
  //get rid of th old track, make a fresh one.
  sequence.deleteTrack(track);
  track = sequence.createTrack();
  
  for (int i = 0; i < 16; i++) {
   trackList = new int[16];
   
   /*Set the 'key' that represents which instrument this is (Bass,
   Hi-Hat, etc. The instruments array holds the actual MIDI numbers
   for each instrument*/
   int key = instruments[i];
   
   for (int j = 0; j < 16; j++) {
    JCheckBox jc = checkboxList.get(j + 16*i);
    
    /*Is the checkbox at this beat selected? If yes, put the key
    value in this slot in the array (the slot that represents this
    beat). Otherwise, the instrument is NOT supposed to play at this
    beat, so set it to zero*/
    if(jc.isSelected()) {
     trackList[j] = key;
    } else {
     trackList[j] = 0;
    }
   }
   
   /*For this instrument, and for all 16 beats, make events and add
   them to the track*/
   makeTracks(trackList);
   track.add(makeEvent(176,1,127,0,16));
  }
  
  /*We always want to make sure that there IS an event at beat 16 (it
  goes 0 to 15). Otherwise, the BeatBox might not go the full 16 beats
  before it starts over*/
  track.add(makeEvent(192,9,1,0,15));
  try {
   sequencer.setSequence(sequence);
   sequencer.setLoopCount(sequencer.LOOP_CONTINUOUSLY);
   sequencer.start();
   sequencer.setTempoInBPM(120);
  } catch (Exception e) {e.printStackTrace();}
 }
 
 //First of the inner classes, listeners for the buttons.
 public class MyStartListener implements ActionListener {
  public void actionPerformed(ActionEvent a) {
   buildTrackAndStart();
  }
 }
 
 //Second inner class for the buttons
 public class MyStopListener implements ActionListener {
  public void actionPerformed(ActionEvent a) {
   sequencer.stop();
  }
 }
 
 //Third inner class for the buttons
 public class MyUpTempoListener implements ActionListener {
  public void actionPerformed(ActionEvent a) {
   float tempoFactor = sequencer.getTempoFactor();
   sequencer.setTempoFactor((float) (tempoFactor * 1.03));;
  }
 }
 
 /*The Tempo Factor scales the sequnecer's tempo by the factor provided. The
 default is 1.0, so we're adjusting +/- 3% per click*/
 
 //Fourth inner class for the buttons.
 public class MyDownTempoListener implements ActionListener {
  public void actionPerformed(ActionEvent a) {
   float tempoFactor = sequencer.getTempoFactor();
   sequencer.setTempoFactor((float) (tempoFactor * .97));;
  }
 }
 
 /*This makes events for one instrument at a time, for all 16 beats. So it might
 get an int[] for the Bass Drum, and each index in the array will hold either the
 key of that instrument, or a zero. If it's a zero, the instrument isn't supposed
 to play at that beat. Otherwise, make an event and add it to the track*/
 public void makeTracks(int[] list) {
  for (int i = 0; i < 16; i++) {
   int key = list[i];
   
   if (key != 0) {
    track.add(makeEvent(144,9,key,100,i));
    track.add(makeEvent(128,9,key,100,i+1));
   }
  }
 }
 
 //This is a utility method
 public MidiEvent makeEvent(int comd, int chan, int one, int two, int tick) {
  MidiEvent event = null;
  try {
   ShortMessage a = new ShortMessage();
   a.setMessage(comd,chan,one,two);
   event = new MidiEvent(a, tick);
  } catch (Exception e) {e.printStackTrace();}
  return event;
 }
} 