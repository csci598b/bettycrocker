package bettycrocker;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class Song {
	
	private final CountDownLatch startLatch = new CountDownLatch(1);
	
	private List<Track> tracks;
	private boolean isPlaying = false;
	private boolean isLooping = false;
	
	public Song() {
		tracks = new ArrayList<Track>();
	}
	
	public Song(List<Track> tracks) {
		for (Track track : tracks) {
			addTrack(track);
		}
	}
	
	public void startPlaying() {
		startPlaying(false);
	}
	
	public void startPlaying(boolean loop) {
		isLooping = loop;
		isPlaying = true;
		new PlayThread().start();
	}
	
	public void stopPlaying() {
		isPlaying = false;
		isLooping = false;
		
		for(Track track : tracks) {
			track.stopPlaying();
		}
	}
	
	public void addTrack(Track track) {
		insertTrack(tracks.size(), track);
	}
	
	public void insertTrack(int index, Track track) {
		track.setStartLatch(startLatch);
		tracks.add(index, track);
	}
	
	private void playTracks() {
		for(Track track : tracks) {
			track.startPlaying();
		}
		
		//Sleep for a short amount of time to give all of the tracks a chance to start playing
		try {
			Thread.sleep(200);
		} catch (InterruptedException e) {
			System.out.println("Problem sleeping before starting track playback.");
			e.printStackTrace();
		}
		
		//Count down the start latch to have all tracks begin playing at the same time
		startLatch.countDown();
	}
	
	private boolean allTracksFinished() {
		boolean allTracksFinished = true;
		
		for (Track track : tracks) {
			if (track.isPlaying()) {
				allTracksFinished = false;
				break;
			}
		}
		
		return allTracksFinished;
	}
	
	public void setLooping(boolean isLooping) {
		this.isLooping = isLooping;
	}
	
	public List<Track> getTracks() {
		return tracks;
	}
	
	
	private class PlayThread extends Thread {
		
		public void run() {
			playTracks();
			
			while (isPlaying && isLooping) {
				if (allTracksFinished()) {
					playTracks();
				}
			}
		}
		
	}

}
