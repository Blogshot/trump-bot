package main;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import listener.ChatListener;
import listener.TrackFinishedListener;
import org.slf4j.LoggerFactory;
import sx.blah.discord.Discord4J;
import sx.blah.discord.api.ClientBuilder;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.api.events.EventDispatcher;
import sx.blah.discord.handle.impl.events.ReadyEvent;
import sx.blah.discord.handle.impl.events.shard.LoginEvent;
import sx.blah.discord.handle.obj.*;
import sx.blah.discord.util.*;
import sx.blah.discord.util.audio.AudioPlayer;
import util.ErrorReporter;
import util.LavaPlayer;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;

public class Main {
  
  private final int[] milestones = {
      10000, 15000, 20000, 25000, 30000, 40000, 50000, 50000, 75000, 100000
  };
  private static Main instance;
  public final long startedInMillis = System.currentTimeMillis();
  private AtomicLong played = new AtomicLong(0);
  private long guilds = 0;
  private String token = "";
  public IDiscordClient client;
  public LavaPlayer lavaPlayer = new LavaPlayer();
  public boolean useLavaPlayer;
  
  public static Main getInstance() {
    return instance;
  }
  
  public static void main(String[] args) throws FileNotFoundException {
    instance = new Main();
    instance.start(args);
  }
  
  public static void log(String message) {
    Date date = new Date();
    SimpleDateFormat sdf = new SimpleDateFormat("hh:mm:ss.SSS");
    String formattedDate = sdf.format(date);
    
    System.out.println(formattedDate + "   " + message);
  }
  
  public void saveStats() {
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
      
    } catch (FileNotFoundException e) {
      new ErrorReporter(client).report(e);
    }
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
  
  private void start(String[] args) {
    
    boolean debug = false;
    
    for (String arg : args) {
      if (arg.startsWith("--token=")) {
        token = arg.replace("--token=", "");
      }
      //noinspection StatementWithEmptyBody
      if (arg.equals("--debug")) {
        debug = true;
      }
  
      if (arg.equals("--lavaplayer")) {
        this.useLavaPlayer = true;
      }
    }
    
    // disable warning for missing permissions on text-channels
    Discord4J.disableChannelWarnings();
    
    Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
    if (debug) {
      root.setLevel(Level.DEBUG);
    } else {
      root.setLevel(Level.INFO);
    }
    
    try {
      client = getClient(token); // Gets the client object
      
      // no caching of messages to reduce RAM-usage
      MessageList.setEfficiency(client, MessageList.EfficiencyLevel.HIGH);
      
      EventDispatcher dispatcher = client.getDispatcher(); // Gets the EventDispatcher
      
      client.login();
      
      // sleep until login
      dispatcher.waitFor(LoginEvent.class);
      
      // set status to booting
      client.changePresence(true);
      client.changeStatus(Status.game("Restarting..."));
      
      // sleep until ready
      dispatcher.waitFor(ReadyEvent.class);
      
      played = new AtomicLong(Long.parseLong(Main.getInstance().readStat("played")));
      guilds = client.getGuilds().size();
      
      // set listeners
      dispatcher.registerListener(new ChatListener());
      dispatcher.registerListener(new TrackFinishedListener());
      
      // finally, set status to available
      client.changePresence(false);
      client.changeStatus(Status.game("!trump --help"));
      
    } catch (Exception e) {
      new ErrorReporter(client).report(e);
    }
  }
  
  public String readStat(String stat) {
    return Main.getInstance().getStatsAsJson().get(stat).getAsString();
  }
  
  private JsonObject getStatsAsJson() {
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
  public void playAudio(
      IVoiceChannel voiceChannel, IChannel textChannel, URL soundFile, IUser user) {
    // Join channel
    try {
      voiceChannel.join();
      
      AudioPlayer player = AudioPlayer.getAudioPlayerForGuild(voiceChannel.getGuild());
      
      // feed the player with audio
      Main.log("Queuing " + soundFile);
      
      player.queue(soundFile);
      
      checkMilestones(textChannel, user);
      
    } catch (MissingPermissionsException e) {
      writeMessage(textChannel, "I have no permission to join this channel.");
    } catch (Exception e) {
      new ErrorReporter(client).report(e);
    }
  }
  
  public void checkMilestones(IChannel textChannel, IUser user) {
  
    for (int milestone : milestones) {
    
      // if the sound is the x'th sound
      if (played.get() == milestone - 1) {
      
        Main.log("Milestone reached!");
        writeMessage(
            textChannel,
            user.getName()
                + " just broke the "
                + milestone
                + "-milestone! Congratulations, have a friendly handshake! :handshake:");
      }
    }
    
  }
  
  
  public void writeMessage(IChannel channel, String message) {
    
    try {
      new MessageBuilder(client).withChannel(channel).withContent(message).build();
    } catch (MissingPermissionsException | RateLimitException ignored) {
    } catch (DiscordException e) {
      new ErrorReporter(client).report(e);
    }
  }
  
  private IDiscordClient getClient(String token)
      throws Exception { // Returns an instance of the Discord client
    
    if (client == null) {
      /*
       0-2500 guilds = 0 + 1
       2501-5000 guilds = 1 + 1
       5001-7500 guilds = 2 + 1
       ...
      */
      // simulate some guilds to leave a buffer for a restart
      int shards = (int) ((guilds + 500) / 2500) + 1;
      
      ClientBuilder clientBuilder =
          new ClientBuilder() // Creates the ClientBuilder instance
              .withToken(token) // Adds the login info to the builder
              .withShards(shards);
      
      return clientBuilder.build(); // Creates the client instance
      
    } else return client;
  }
  
  public void handleTrackFinished(AudioPlayer player) {
    // Leave current channel after audio finished
    Main.getInstance().leaveVoiceChannel(player.getGuild());
  
    // Clean memory to countermeasure memory leak
    // https://github.com/austinv11/Discord4J/issues/191)
    player.clean();
  
    Main.log("Left voice channel and cleaned guild's player.");
  
    // Update stats
    Main.getInstance().played.incrementAndGet();
    Main.getInstance().saveStats();
  
    Main.log("Increased 'played' and saved stats.");
  }
  
  public void handleTrackFinished(IGuild guild) {
  
    // Leave current channel after audio finished
    Main.getInstance().leaveVoiceChannel(guild);
    
    Main.log("Left voice channel and cleaned guild's player.");
  
    // Update stats
    Main.getInstance().played.incrementAndGet();
    Main.getInstance().saveStats();
  
    Main.log("Increased 'played' and saved stats.");
    
  }
  
  public enum Politician {
    trump,
    clinton,
    merkel
  }
}
