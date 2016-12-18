import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import sx.blah.discord.api.ClientBuilder;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.api.events.EventDispatcher;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IVoiceChannel;
import sx.blah.discord.util.DiscordException;
import sx.blah.discord.util.MessageBuilder;
import sx.blah.discord.util.MissingPermissionsException;
import sx.blah.discord.util.RateLimitException;
import sx.blah.discord.util.audio.AudioPlayer;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Main {
  
  private static Main instance;
  
  private String token = "";
  private IDiscordClient client;
  
  public long played = 0;
  public final long startedInMillis = System.currentTimeMillis();
  
  public static Main getInstance() {
    return instance;
  }
  
  public void saveStats() {
    JsonObject obj = new JsonObject();
    
    obj.addProperty("played", played);
    
    if (client != null) {
      obj.addProperty("guildCount", client.getGuilds().size());
    }
    
    Gson gson = new GsonBuilder().setPrettyPrinting().create();
    String output = gson.toJson(obj);
    
    PrintWriter out;
    try {
      out = new PrintWriter("stats.json");
      out.print(output);
      out.close();
      
    } catch (FileNotFoundException e) {
      new ErrorReporter(client, e).report();
    }
  }
  
  public enum Politician {
    trump, clinton, merkel
  }
  
  public String getUptime() {
    
    long milliseconds = System.currentTimeMillis() - startedInMillis;
    
    int seconds = (int) (milliseconds / 1000) % 60;
    int minutes = (int) ((milliseconds / (1000 * 60)) % 60);
    int hours = (int) ((milliseconds / (1000 * 60 * 60)) % 24);
    int days = (int) ((milliseconds / (1000 * 60 * 60 * 24)) % 30);
    
    return days + "." + hours + ":" + minutes + ":" + seconds;
  }
  
  public IVoiceChannel isBusyInGuild(IGuild guild) {
    
    for (IVoiceChannel voiceChannel : client.getConnectedVoiceChannels()) {
      
      if (voiceChannel.getGuild().getID().equals(guild.getID())) {
        return voiceChannel;
      }
      
    }
    return null;
  }
  
  public void leaveVoiceChannel(IGuild guild) {
    
    for (IVoiceChannel voiceChannel : client.getConnectedVoiceChannels()) {
      
      if (voiceChannel.getGuild().getID().equals(guild.getID())) {
        
        voiceChannel.leave();
        break;
        
      }
      
    }
    
  }
  
  public static void main(String[] args) throws FileNotFoundException {
    instance = new Main();
    instance.start(args);
  }
  
  public void start(String[] args) throws FileNotFoundException {
    
    for (String arg : args) {
      if (arg.startsWith("--token=")) {
        token = arg.replace("--token=", "");
      }
    }
    
    played = getStatsAsJson().get("played").getAsLong();
    
    try {
      client = getClient(token); // Gets the client object
      
      EventDispatcher dispatcher = client.getDispatcher(); // Gets the EventDispatcher instance for this client instance
      
      // Register some listeners
      dispatcher.registerListener(new ChatListener()); // Listener which reacts to commands
      dispatcher.registerListener(new TrackFinishedListener()); // Listener which reacts to finished audio
      
      
    } catch (Exception e) {
      new ErrorReporter(client, e).report();
    }
    
  }
  
  public JsonObject getStatsAsJson() {
    try {
      byte[] encoded = Files.readAllBytes(Paths.get("stats.json"));
      String json = new String(encoded, Charset.forName("UTF-8"));
      
      return new Gson().fromJson(json, JsonObject.class);
      
    } catch (Exception e) {
      
      // create new Stats-File
      saveStats();
      
      return getStatsAsJson();
    }
  }
  
  // Join channel and play specified audio
  public void playAudio(IVoiceChannel voiceChannel, IChannel textChannel, File soundFile) {
    // Join channel
    try {
      System.out.println("Joining voice channel.");
  
      voiceChannel.join();
  
      System.out.println("Joined voice channel.");
  
  
      System.out.println("Playing file " + soundFile.getName());
      // load file as inputstream
      AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(soundFile);
      
      // feed the player with audio
      AudioPlayer player = AudioPlayer.getAudioPlayerForGuild(voiceChannel.getGuild());
      
      player.queue(audioInputStream);
      
    } catch (MissingPermissionsException e) {
      writeMessage(textChannel,
          "I have no permission to join this channel.");
    } catch (Exception e) {
      new ErrorReporter(client, e).report();
    }
  }

  
  public void writeMessage(IChannel channel, String message) {
    
    try {
      new MessageBuilder(client)
          .withChannel(channel)
          .withContent(message)
          .build();
    } catch (RateLimitException ignored) {
      
    } catch (MissingPermissionsException | DiscordException e) {
      new ErrorReporter(client, e).report();
    }
    
  }
  
  private IDiscordClient getClient(String token) throws Exception { // Returns an instance of the Discord client
    
    if (client == null) {
      ClientBuilder clientBuilder = new ClientBuilder(); // Creates the ClientBuilder instance
      clientBuilder.withToken(token); // Adds the login info to the builder
      
      return clientBuilder.login(); // Creates the client instance and logs the client in
      
    } else
      return client;
  }
}