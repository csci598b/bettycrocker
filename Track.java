package bettycrocker;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.BooleanControl;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;

public class Track {
	
	private File soundFile;
	private AudioInputStream audioInputStream;
	private AudioInputStream decodedAudioInputStream;
	private AudioFormat audioFormat;
	private AudioFormat decodedAudioFormat;
	private SourceDataLine sourceDataLine;
	
	private CountDownLatch startLatch;
	
	private FloatControl gainControl;
	private BooleanControl muteControl;
	//private FloatControl panControl; //PAN ISN'T WORKING FOR SOME REASON
	//private FloatControl balanceControl; //BALANCE ISN'T WORKING FOR SOME REASON
	
	private float gain = 0.0f;
	private boolean mute = false;
	//private float balance = 0.0f; //BALANCE ISN'T WORKING FOR SOME REASON
	//private float pan = 0.0f; //PAN ISN'T WORKING FOR SOME REASON
	private float[] equalizer = new float[32];
	
	private boolean isPlaying = false;
	
	public Track(String fileName) {
		soundFile = new File(fileName);
	}
	
	public void startPlaying() {
		if (!isPlaying) {
			isPlaying = true;
			new PlayThread().start();
		}
	}
	
	public void stopPlaying() {
		isPlaying = false;
	}

	public boolean isPlaying() {
		return isPlaying;
	}
	
	public File getSoundFile() {
		return soundFile;
	}
	
	public void setStartLatch(CountDownLatch startLatch) {
		this.startLatch = startLatch;
	}
	
	public void setGain(float gain) {
		this.gain = gain;
	}
	
	//PAN ISN'T WORKING FOR SOME REASON
	//public void setPan(float pan) {
	//	this.pan = pan;
	//}
	
	//BALANCE ISN'T WORKING FOR SOME REASON
	//public void setBalance(float balance) {
	//	this.balance = balance;
	//}
	
	public void setMute(boolean mute) {
		this.mute = mute;
	}
	
	public void setEqualizer(float[] equalizer) {
		System.arraycopy(equalizer, 0, this.equalizer , 0, 32);
	}
	
	private void adjustBooleanControl(BooleanControl control, boolean value) {
		control.setValue(value);
	}
	
	private void adjustFloatControl(FloatControl control, float value) {
		if (value < control.getMaximum() && value > control.getMinimum())
			control.setValue(value);
		else if (value > control.getMaximum()) {
			control.setValue(control.getMaximum());
		} else if (value < control.getMinimum()) {
			control.setValue(control.getMinimum());
		}
	}
	
	private AudioFormat getDecodedAudioFormat(AudioFormat baseFormat) {
		AudioFormat.Encoding encoding = AudioFormat.Encoding.PCM_SIGNED;
		float sampleRate = baseFormat.getSampleRate();
		int sampleSizeInBits = 16;
		int channels = baseFormat.getChannels();
		int frameSize = baseFormat.getChannels() * 2;
		float frameRate = baseFormat.getSampleRate();
		boolean bigEndian = false;
		
		return new AudioFormat(encoding, sampleRate, sampleSizeInBits, channels, frameSize, frameRate, bigEndian);
	}
	
	
	private class PlayThread extends Thread {
		private final int BUFFER_SIZE = 2000;
		private byte tempBuffer[] = new byte[BUFFER_SIZE];
		
		public void run() {
			try {
				startLatch.await();
				
				try {
					audioInputStream = AudioSystem.getAudioInputStream(soundFile);
					audioFormat = audioInputStream.getFormat();
					
					decodedAudioFormat = getDecodedAudioFormat(audioFormat);
					decodedAudioInputStream = AudioSystem.getAudioInputStream(decodedAudioFormat, audioInputStream);
					
					DataLine.Info dataLineInfo = new DataLine.Info(SourceDataLine.class, decodedAudioFormat);
					sourceDataLine = (SourceDataLine) AudioSystem.getLine(dataLineInfo);
	
					sourceDataLine.open(decodedAudioFormat);
					
					if (sourceDataLine.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
						gainControl = (FloatControl) sourceDataLine.getControl(FloatControl.Type.MASTER_GAIN);
					}
					if (sourceDataLine.isControlSupported(BooleanControl.Type.MUTE)) {
						muteControl = (BooleanControl) sourceDataLine.getControl(BooleanControl.Type.MUTE);
					}
					//PAN ISN'T WORKING FOR SOME REASON
					//if (sourceDataLine.isControlSupported(FloatControl.Type.PAN)) {
					//	panControl = (FloatControl) sourceDataLine.getControl(FloatControl.Type.PAN);
					//}
					//BALANCE ISN'T WORKING FOR SOME REASON
					//if (sourceDataLine.isControlSupported(FloatControl.Type.BALANCE)) {
					//	balanceControl = (FloatControl) sourceDataLine.getControl(FloatControl.Type.BALANCE);
					//}
					
					float[] equalizerControl = new float[32];
					if (decodedAudioInputStream instanceof javazoom.spi.PropertiesContainer) {
						@SuppressWarnings("rawtypes")
						Map properties = ((javazoom.spi.PropertiesContainer) decodedAudioInputStream).properties();
						equalizerControl = (float[]) properties.get("mp3.equalizer");
					}
					
					sourceDataLine.start();
	
					int count;
					while ((count = decodedAudioInputStream.read(tempBuffer, 0, tempBuffer.length)) != -1 && isPlaying) {
						if (count > 0) {
							if (sourceDataLine.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
								adjustFloatControl(gainControl, gain);
							}
							if (sourceDataLine.isControlSupported(BooleanControl.Type.MUTE)) {
								adjustBooleanControl(muteControl, mute);
							}
							//PAN ISN'T WORKING FOR SOME REASON
							//if (sourceDataLine.isControlSupported(FloatControl.Type.PAN)) {
							//	adjustFloatControl(panControl, pan);
							//}
							//BALANCE ISN'T WORKING FOR SOME REASON
							//if (sourceDataLine.isControlSupported(FloatControl.Type.BALANCE)) {
							//	adjustFloatControl(balanceControl, balance);
							//}
							System.arraycopy(equalizer, 0, equalizerControl, 0, 32);
							
							sourceDataLine.write(tempBuffer, 0, count);
						}
					}
					
					sourceDataLine.drain();
					sourceDataLine.close();
					
					audioInputStream.close();
					decodedAudioInputStream.close();
					
					isPlaying = false;
					
				} catch (IOException | UnsupportedAudioFileException | LineUnavailableException e) {
					e.printStackTrace();
					System.exit(0);
				}
				
			} catch (InterruptedException e) {
				System.out.println("Track play was interrupted.");
				e.printStackTrace();
			}
		}

	}
	
}
