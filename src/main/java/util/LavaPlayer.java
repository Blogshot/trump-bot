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
    
    playerManager.loadItem(soundFile.getPath(), new AudioLoadResultHandler() {
      @Override
      public void trackLoaded(AudioTrack track) {
        voiceChannel.join();
        musicManager.player.playTrack(track);
        Main.getInstance().checkMilestones(textChannel, user);
      }
      
      @Override
      public void playlistLoaded(AudioPlaylist playlist) {}
      
      @Override
      public void noMatches() {}
      
      @Override
      public void loadFailed(FriendlyException exception) {
        Main.getInstance().writeMessage(textChannel, "Could not play: " + exception.getMessage());
      }
    });
  }
}


class GuildMusicManager {
  public final AudioPlayer player;
  
  public GuildMusicManager(AudioPlayerManager manager, IGuild guild) {
    player = manager.createPlayer();

    player.addListener(new AudioEventAdapter() {
      @Override
      public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
        Main.getInstance().handleTrackFinished(guild);
      }
    });
    
  }
  
  public AudioProvider getAudioProvider() {
    return new AudioProvider(player);
  }
}

class AudioProvider implements IAudioProvider {
  private final AudioPlayer audioPlayer;
  private AudioFrame lastFrame;
  
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