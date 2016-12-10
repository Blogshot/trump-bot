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
  
  private static String token = "";
  
  private static IDiscordClient client;
  
  public static long played = 0;
  public static long startedInMillis = System.currentTimeMillis();
  
  public static void saveStats() {
    JsonObject obj = new JsonObject();
    
    obj.addProperty("played", played);
    obj.addProperty("guildCount", client.getGuilds().size());
  
    Gson gson = new GsonBuilder().setPrettyPrinting().create();
    String output = gson.toJson(obj);
    
    PrintWriter out;
    try {
      out = new PrintWriter("stats.json");
      out.print(output);
      out.close();
    
    } catch (FileNotFoundException e1) {
      e1.printStackTrace();
    }
  }
  
  public enum Politician {
    trump, clinton, merkel
  }
  
  public static String getUptime() {
    
    long milliseconds = System.currentTimeMillis() - startedInMillis;
    
    int seconds = (int) (milliseconds / 1000) % 60 ;
    int minutes = (int) ((milliseconds / (1000*60)) % 60);
    int hours   = (int) ((milliseconds / (1000*60*60)) % 24);
    int days   = (int) ((milliseconds / (1000*60*60*24)) % 30);
    
    return days + "." + hours + ":" + minutes + ":" + seconds;
  }
  
  public static IVoiceChannel isBusyInGuild(IGuild guild) {
    
    for (IVoiceChannel voiceChannel : client.getConnectedVoiceChannels()) {
      
      if (voiceChannel.getGuild().getID().equals(guild.getID())) {
        return voiceChannel;
      }
      
    }
    return null;
  }
    
  public static void removeGuildFromList(IGuild guild) {
    
    IVoiceChannel channelToLeave = null;
    
    for (IVoiceChannel voiceChannel : client.getConnectedVoiceChannels()) {
    
      if (voiceChannel.getGuild().getID().equals(guild.getID())) {

        channelToLeave = voiceChannel;
        break;

      }
    
    }
    
    if (channelToLeave != null) {
      channelToLeave.leave();
    }
    
  }
  
  public static void main(String[] args) throws FileNotFoundException {
    
    System.setOut(new PrintStream(new FileOutputStream("trump.log")));
    
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
      e.printStackTrace();
    }
  }
  
  public static JsonObject getStatsAsJson() {
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
  public static void playAudio(IVoiceChannel voiceChannel, IChannel textChannel, File soundFile) {
    // Join channel
    try {
      voiceChannel.join();
  
      AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(soundFile);
      
      // feed the player with audio
      AudioPlayer player = AudioPlayer.getAudioPlayerForGuild(voiceChannel.getGuild());
      
      player.queue(audioInputStream);
      
    } catch (MissingPermissionsException e) {
      e.printStackTrace();
      Main.writeMessage(textChannel,
          "I hate to tell you this, but I have no permission to join this channel.");
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  
  public static void writeMessage(IChannel channel, String message) {
    
    try {
      new MessageBuilder(client)
          .withChannel(channel)
          .withContent(message)
          .build();
    } catch (RateLimitException | MissingPermissionsException | DiscordException e) {
      e.printStackTrace();
    }
    
  }
  
  private static IDiscordClient getClient(String token) throws Exception { // Returns an instance of the Discord client
    
    if (Main.client == null) {
      ClientBuilder clientBuilder = new ClientBuilder(); // Creates the ClientBuilder instance
      clientBuilder.withToken(token); // Adds the login info to the builder
      
      return clientBuilder.login(); // Creates the client instance and logs the client in
      
    } else
      return Main.client;
  }
}