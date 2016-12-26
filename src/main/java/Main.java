import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import sx.blah.discord.api.ClientBuilder;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.api.events.EventDispatcher;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.handle.obj.IVoiceChannel;
import sx.blah.discord.util.DiscordException;
import sx.blah.discord.util.MessageBuilder;
import sx.blah.discord.util.MissingPermissionsException;
import sx.blah.discord.util.RateLimitException;
import sx.blah.discord.util.audio.AudioPlayer;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

public class Main {
  
  private static Main instance;
  
  private String token = "";
  private IDiscordClient client;
  
  public long played = 0;
  public final long startedInMillis = System.currentTimeMillis();
  public static int[] milestones = {
      10000,
      15000,
      20000,
      25000,
      30000,
      40000,
      50000,
      50000,
      75000,
      100000
  };
  
  
  
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
      new ErrorReporter(client).report(e);
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
      new ErrorReporter(client).report(e);
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
  public void playAudio(IVoiceChannel voiceChannel, IChannel textChannel, ArrayList<URL> soundFiles, IUser user) {
    // Join channel
    try {
      System.out.println("Joining voice channel.");
      
      voiceChannel.join();
      
      System.out.println("Joined voice channel.");
  
      AudioPlayer player = AudioPlayer.getAudioPlayerForGuild(voiceChannel.getGuild());
      
      for (URL soundFile : soundFiles) {
        
        // feed the player with audio
        System.out.println(soundFile);
        
        player.queue(soundFile);
      }
      
      for (int milestone : milestones) {
        
        // if the sound is the x'th sound
        if (played == milestone -1) {
  
          System.out.println("Milestone reached!");
          writeMessage(textChannel,
              user.getName() + " just broke the " + milestone + "-milestone! Congratulations, have a friendly handshake! :handshake:");
          
        }
        
      }
      
    } catch (MissingPermissionsException e) {
      writeMessage(textChannel,
          "I have no permission to join this channel.");
    } catch (Exception e) {
      new ErrorReporter(client).report(e);
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
      new ErrorReporter(client).report(e);
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