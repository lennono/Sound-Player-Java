import javax.sound.sampled.*;
import java.applet.Applet;
import java.awt.*;
import java.awt.event.*;
import java.io.*;

class Player extends Panel implements Runnable {
    private static final long serialVersionUID = 1L;
    private TextField textfield;
    private TextArea textarea;
	private FloatControl volume; 
	private BooleanControl mute;
    private Font font;
    private String filename;
    public Player(String filename){
		font = new Font(Font.SANS_SERIF, Font.PLAIN, 12);
		textfield = new TextField();
		textarea = new TextArea();
		textarea.setFont(font);
		textfield.setFont(font);
		setLayout(new BorderLayout());
		add(BorderLayout.SOUTH, textfield);
		add(BorderLayout.CENTER, textarea);
		this.volume=volume;
		this.mute=mute;
		this.textarea= textarea;
		this.textfield = textfield;
		this.filename = filename;
		new Thread(this).start();
    }

    public void run() {
	try {
	    AudioInputStream s = AudioSystem.getAudioInputStream(new File(filename));
	    AudioFormat format = s.getFormat();	    
	    System.out.println("Audio format: " + format.toString());
	    DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
	    if (!AudioSystem.isLineSupported(info)) {
		throw new UnsupportedAudioFileException();
	    }
	    SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
	    line.open(format);
		volume = (FloatControl) line.getControl(FloatControl.Type.MASTER_GAIN); 
		mute =  (BooleanControl) line.getControl(BooleanControl.Type.MUTE);
	    line.start();
		BoundedBuffer buffer = new BoundedBuffer(); //initialise buffer
        Thread a = new Thread(new Producer(buffer, s)); /* Threads */
        Thread b = new Thread(new Consumer(buffer, s, line));
		Thread c= new Thread(new Stopping(buffer, textfield, textarea, volume, mute));
        a.start();
        b.start();
		c.start();
        a.join();
        b.join();
		c.join();
 
	} catch (UnsupportedAudioFileException e ) {
	    System.out.println("Player initialisation failed");
	    e.printStackTrace();
	    System.exit(1);
	} catch (LineUnavailableException e) {
	    System.out.println("Player initialisation failed");
	    e.printStackTrace();
	    System.exit(1);
	} catch (IOException e) {
	    System.out.println("Player initialisation failed");
	    e.printStackTrace();
	    System.exit(1);
	}catch(InterruptedException e){ System.out.println("Player  failed");}
	}
}		
    class Producer  implements Runnable {
    private BoundedBuffer buffer;
    AudioInputStream s;
 
    public Producer(BoundedBuffer b, AudioInputStream s1) {
        buffer = b;
        s = s1;
    }
	
	public void run() {
        try {
            AudioFormat format = s.getFormat();
			long frames = s.getFrameLength();
			int duration = (int)((frames+0.0) / format.getFrameRate());
            int oneSecond = (int)(format.getChannels() * format.getSampleRate() * format.getSampleSizeInBits() / 8); //get one second chunk
			int count=0;
			boolean done=false;
            while(count !=duration+2)// Still need to fix  
			{
                done=buffer.insertChunk(oneSecond);
				count++;
				if(done==true){break;}
				Thread.sleep((int)(Math.random() * 100)); //sleep thread
			}
		}catch (InterruptedException e) { } 
			System.out.println("Goodbye from Producer (" + 
                    Thread.currentThread().getName() + ")"); //goodbye message
    }
}
	class Stopping implements Runnable{
		private BoundedBuffer buffer;
		private TextField textfield;
		private TextArea textarea;
		private FloatControl volume; 
		private BooleanControl mute;
		
	public Stopping(BoundedBuffer b, TextField t, TextArea s, FloatControl v, BooleanControl m){
		buffer=b;
		textfield=t;
		textarea=s;
		volume=v;
		mute=m;
	}
	
	public void run() {
			textfield.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
			String vol = String.format("%.0f",volume.getValue());
			if(e.getActionCommand().equals("x")){ 
				buffer.stops("x"); 
				textarea.append("Song ended\n");
			}
			if(e.getActionCommand().equals("p")){ 
				buffer.stops("p");
				textarea.append("Paused\n");				
			}
			if(e.getActionCommand().equals("r")){ 
				buffer.stops("r");
				textarea.append("Resumed\n");					
			}
			if(e.getActionCommand().equals("m")&& mute.getValue()==false){ 
				mute.setValue(true);
				textarea.append("Song Muted\n");
			}
			if(e.getActionCommand().equals("u")&& mute.getValue()==true){
				mute.setValue(false);
				textarea.append("Song UnMuted\n");
			}
			if(e.getActionCommand().equals("q"))
			{
				if((volume.getValue()+1.0f)>0)
				{
					textarea.append("MAX Volume\n");	
				}
				else
				{
					volume.shift(volume.getValue(),volume.getValue()+1.0f,1);
					textarea.append("The current Volume is: "+vol+"\n");
				}
			}
			if(e.getActionCommand().equals("a"))
			{ 
				if((volume.getValue()-1.0f)<-80f)
				{
					textarea.append("MIN Volume\n");
				}
				else
				{
					volume.shift(volume.getValue(),volume.getValue()-1.0f,1);
					textarea.append("The current Volume is: "+vol+"\n");
				}
			}
		}
	    });
	}
}
	
	
	class Consumer  implements Runnable {
    private BoundedBuffer buffer;
    private AudioInputStream s;
    private SourceDataLine line;
 
    public Consumer(BoundedBuffer b, AudioInputStream s1, SourceDataLine l1) {
        buffer = b;
        s = s1;
        line = l1;
    }
 
    public void run() {
        try {
            int byteRead = 0;
			int temp=0;
			AudioFormat format = s.getFormat();
			int oneSecond = (int)(format.getChannels() * format.getSampleRate() * format.getSampleSizeInBits() / 8);
            byte[] audioChunk = new byte[oneSecond];
			audioChunk = new byte[buffer.removeChunk()];
			byteRead = s.read(audioChunk);
            while(byteRead != -1) {
				line.write(audioChunk, 0, byteRead); //play audio chunk
				temp=buffer.removeChunk();
				if(temp==-1){break;}
                audioChunk = new byte[temp];
                byteRead = s.read(audioChunk);
				Thread.sleep((int)(Math.random() * 100)); //sleep thread
            }
		} catch (InterruptedException e) { } 
			catch (IOException e) {}
 
        finally{
			line.drain();
            line.stop();
            line.close();
            System.out.println("Goodbye from Consumer (" + 
                    Thread.currentThread().getName() + ")"); //goodbye message
		}
	}
	
}

public class StudentPlayerApplet extends Applet{
	private static final long serialVersionUID = 1L;
	public void init() {
		setLayout(new BorderLayout());
		add(BorderLayout.CENTER, new Player(getParameter("file")));
	}
}

class BoundedBuffer {
    int[] buffer; //buffer
    int nextIn;
    int nextOut;
    int size;
    int occupied;
	int outs;
    boolean dataAvailable;
    boolean roomAvailable;
	boolean done;
 
    public BoundedBuffer() {
        size = 10;
        buffer = new int[size];
        roomAvailable = true;
        dataAvailable = false;
        nextIn = 0;
        nextOut = 0;
        occupied = 0;
		outs=0;
		done=false;
    }
	
	public synchronized boolean insertChunk(int chunk) {  
	while (roomAvailable == false || occupied == size) 
		{
			try 
			{
				wait();
			} catch (InterruptedException e) 
			{
				System.out.println("Error inserting item.");
			}
		}
		buffer[nextIn] = chunk;
		nextIn = (nextIn + 1) % size;
		occupied++;
		if (occupied == 10){
			roomAvailable = false;
		}
		dataAvailable=true;
		notifyAll();
		return done;
	}
 
    public synchronized int removeChunk() {
       
		while(dataAvailable == false || occupied == 0){
            try {
                wait();
            } catch (InterruptedException e) { }
        }
        outs = buffer[nextOut % size]; //remove chunk to return
        nextOut++; //update where to take from
        occupied--;
 
        if(occupied == 0) { //if none left change variables
            dataAvailable = false;
        }
		roomAvailable=true;
		if(done == true){
			outs = -1;
		}
		notifyAll();
        return outs; //return chunk
    }//removeChunk
	
	public synchronized void stops(String action) {
		if(action.equals("x")){
			done=true;
		}
		else if(action.equals("p")){
			roomAvailable=false;
			dataAvailable=false;
		}
		else if(action.equals("r")&& occupied !=10 ){
			roomAvailable=true;
		}
		else if(action.equals("r")&& occupied !=0 ){
			dataAvailable=true;
		}
		notifyAll();	
	}
}//BoundedBuffer..
