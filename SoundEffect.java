package bettycrocker;

import java.io.File;
import java.io.IOException;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;


public class SoundEffect {
	
	private File soundFile;
	private Clip clip;
	
	public SoundEffect(String fileName) {
		soundFile = new File(fileName);
	}
	
	public void play() {
		new PlayThread().start();
	}
	
	public void play(int times) {
		clip.loop(times - 1);
	}
	
	public void loop() {
		clip.loop(Clip.LOOP_CONTINUOUSLY);
	}
	
	public void stop() {
		if (clip.isRunning()) {
			clip.stop();
		}
	}
	
	public File getSoundFile() {
		return soundFile;
	}
	
	public void setVolume(float newVolume) {
		FloatControl volume = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
		volume.setValue(newVolume);
	}
	
	
	
	private class PlayThread extends Thread {
		
		public void run() {
			try {
				AudioInputStream audioIn = AudioSystem.getAudioInputStream(soundFile);
				
				clip = AudioSystem.getClip();
				clip.open(audioIn);
				clip.start();
			} catch (UnsupportedAudioFileException e) {
				System.out.println("Unsupported audio file: " + soundFile.getName());
				e.printStackTrace();
			} catch (IOException e) {
				System.out.println("Error reading the file: " + soundFile.getName());
				e.printStackTrace();
			} catch (LineUnavailableException e) {
				System.out.println("Problem playing sound file: " + soundFile.getName() + ". Line was unavailable for playback.");
				e.printStackTrace();
			}
			
		}

	}
	
}
