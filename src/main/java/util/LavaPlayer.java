package util;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrame;
import main.Main;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sx.blah.discord.handle.audio.IAudioProvider;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.handle.obj.IVoiceChannel;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class LavaPlayer {
  private static final Logger log = LoggerFactory.getLogger(Main.class);
  
  private final AudioPlayerManager playerManager;
  private final Map<Long, GuildMusicManager> musicManagers;
  
  public LavaPlayer() {
    this.musicManagers = new HashMap<>();
    
    this.playerManager = new DefaultAudioPlayerManager();
    AudioSourceManagers.registerRemoteSources(playerManager);
    AudioSourceManagers.registerLocalSource(playerManager);
  }
  
  private synchronized GuildMusicManager getGuildAudioPlayer(IGuild guild) {
    long guildId = Long.parseLong(guild.getID());
    GuildMusicManager musicManager = musicManagers.get(guildId);
    
    if (musicManager == null) {
      musicManager = new GuildMusicManager(playerManager, guild);
      musicManagers.put(guildId, musicManager);
    }
    
    guild.getAudioManager().setAudioProvider(musicManager.getAudioProvider());
    
    return musicManager;
  }
  
  public void playAudio(IVoiceChannel voiceChannel, IChannel textChannel, URL soundFile, IUser user) {
    
    GuildMusicManager musicManager = getGuildAudioPlayer(textChannel.getGuild());
    
    playerManager.loadItemOrdered(musicManager, soundFile.getPath(), new AudioLoadResultHandler() {
      @Override
      public void trackLoaded(AudioTrack track) {
        voiceChannel.join();
        
        musicManager.scheduler.queue(track);
        
        Main.getInstance().checkMilestones(textChannel, user);
      }
      
      @Override
      public void playlistLoaded(AudioPlaylist playlist) {}
      
      @Override
      public void noMatches() {}
      
      @Override
      public void loadFailed(FriendlyException exception) {
        sendMessageToChannel(textChannel, "Could not play: " + exception.getMessage());
      }
    });
  }
  
  
  private void sendMessageToChannel(IChannel channel, String message) {
    try {
      channel.sendMessage(message);
    } catch (Exception e) {
      log.warn("Failed to send message {} to {}", message, channel.getName(), e);
    }
  }
}


/**
 * This class schedules tracks for the audio player. It contains the queue of tracks.
 */
class TrackScheduler extends AudioEventAdapter {
  private final AudioPlayer player;
  private final BlockingQueue<AudioTrack> queue;
  private final IGuild guild;
  
  /**
   * @param player The audio player this scheduler uses
   */
  public TrackScheduler(AudioPlayer player, IGuild guild) {
    this.player = player;
    this.queue = new LinkedBlockingQueue<>();
    this.guild = guild;
  }
  
  /**
   * Add the next track to queue or play right away if nothing is in the queue.
   *
   * @param track The track to play or add to queue.
   */
  public void queue(AudioTrack track) {
    // Calling startTrack with the noInterrupt set to true will start the track only if nothing is currently playing. If
    // something is playing, it returns false and does nothing. In that case the player was already playing so this
    // track goes to the queue instead.
    if (!player.startTrack(track, true)) {
      queue.offer(track);
    }
  }
  
  @Override
  public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
    if (endReason == AudioTrackEndReason.FINISHED) {
      Main.getInstance().handleTrackFinished(guild);
    }
  }
}

/**
 * Holder for both the player and a track scheduler for one guild.
 */
class GuildMusicManager {
  /**
   * Audio player for the guild.
   */
  public final AudioPlayer player;
  /**
   * Track scheduler for the player.
   */
  public final TrackScheduler scheduler;
  
  /**
   * Creates a player and a track scheduler.
   * @param manager Audio player manager to use for creating the player.
   */
  public GuildMusicManager(AudioPlayerManager manager, IGuild guild) {
    player = manager.createPlayer();
    scheduler = new TrackScheduler(player, guild);
    player.addListener(scheduler);
  }
  
  /**
   * @return Wrapper around AudioPlayer to use it as an AudioSendHandler.
   */
  public AudioProvider getAudioProvider() {
    return new AudioProvider(player);
  }
}

/**
 * This is a wrapper around AudioPlayer which makes it behave as an IAudioProvider for D4J. As D4J calls canProvide
 * before every call to provide(), we pull the frame in canProvide() and use the frame we already pulled in
 * provide().
 */
class AudioProvider implements IAudioProvider {
  private final AudioPlayer audioPlayer;
  private AudioFrame lastFrame;
  
  /**
   * @param audioPlayer Audio player to wrap.
   */
  public AudioProvider(AudioPlayer audioPlayer) {
    this.audioPlayer = audioPlayer;
  }
  
  @Override
  public boolean isReady() {
    if (lastFrame == null) {
      lastFrame = audioPlayer.provide();
    }
    
    return lastFrame != null;
  }
  
  @Override
  public byte[] provide() {
    if (lastFrame == null) {
      lastFrame = audioPlayer.provide();
    }
    
    byte[] data = lastFrame != null ? lastFrame.data : null;
    lastFrame = null;
    
    return data;
  }
  
  @Override
  public int getChannels() {
    return 2;
  }
  
  @Override
  public AudioEncodingType getAudioEncodingType() {
    return AudioEncodingType.OPUS;
  }
}