package main;

import com.google.gson.*;
import listener.ChatListener;
import listener.TrackFinishedListener;
import org.slf4j.LoggerFactory;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import sx.blah.discord.Discord4J;
import sx.blah.discord.api.ClientBuilder;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.api.events.EventDispatcher;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.handle.obj.IVoiceChannel;
import sx.blah.discord.util.*;
import sx.blah.discord.util.audio.AudioPlayer;
import util.ErrorReporter;
import util.SupportList;
import util.SupportRequest;

import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

public class Main {

  private static final int[] milestones = {
    10000, 15000, 20000, 25000, 30000, 40000, 50000, 50000, 75000, 100000
  };
  private static Main instance;
  public final long startedInMillis = System.currentTimeMillis();
  public long played = 0;
  private long guilds = 0;
  private String token = "";
  public String adminID = "";
  private IDiscordClient client;
  public SupportList supportRequests = new SupportList();

  public static Main getInstance() {
    return instance;
  }

  public static void main(String[] args) throws FileNotFoundException {
    instance = new Main();
    instance.start(args);
  }

  public void saveSupportRequests() {
    try {
      FileWriter writer = new FileWriter("support.json");
      writer.write(supportRequests.serialize().toString());
      writer.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
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
      if (arg.startsWith("--adminID=")) {
        adminID = arg.replace("--adminID=", "");
        System.out.println("Admin-ID set to " + adminID);
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
    
    played = getStatsAsJson().get("played").getAsLong();
    guilds = getStatsAsJson().get("guildCount").getAsLong();

    for (JsonElement element : getSupportRequestsAsJson()) {
      JsonObject obj = element.getAsJsonObject();

      supportRequests.add(new SupportRequest(obj));
    }

    try {
      supportRequests = new Gson().fromJson(getSupportRequestsAsJson(), supportRequests.getClass());
    } catch (Exception ignored) {
      supportRequests = new SupportList();
    }

    try {
      client = getClient(token); // Gets the client object

      // no caching of messages to reduce RAM-usage
      MessageList.setEfficiency(client, MessageList.EfficiencyLevel.HIGH);

      client.login();

      EventDispatcher dispatcher =
          client.getDispatcher(); // Gets the EventDispatcher instance for this client instance

      // Register some listeners
      dispatcher.registerListener(new ChatListener()); // Listener which reacts to commands
      dispatcher.registerListener(
          new TrackFinishedListener()); // Listener which reacts to finished audio

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

  private JsonArray getSupportRequestsAsJson() {
    try {
      byte[] encoded = Files.readAllBytes(Paths.get("support.json"));
      String json = new String(encoded, Charset.forName("UTF-8"));

      return new Gson().fromJson(json, JsonArray.class);

    } catch (IOException e) {
      new ErrorReporter(client).report(e);
      return new JsonArray();
    }
  }

  // Join channel and play specified audio
  public void playAudio(
      IVoiceChannel voiceChannel, IChannel textChannel, ArrayList<URL> soundFiles, IUser user) {
    // Join channel
    try {
      voiceChannel.join();
      
      AudioPlayer player = AudioPlayer.getAudioPlayerForGuild(voiceChannel.getGuild());

      for (URL soundFile : soundFiles) {
        // feed the player with audio
        System.out.println("Queuing " + soundFile);

        player.queue(soundFile);
      }

      for (int milestone : milestones) {

        // if the sound is the x'th sound
        if (played == milestone - 1) {

          System.out.println("Milestone reached!");
          writeMessage(
              textChannel,
              user.getName()
                  + " just broke the "
                  + milestone
                  + "-milestone! Congratulations, have a friendly handshake! :handshake:");
        }
      }

    } catch (MissingPermissionsException e) {
      writeMessage(textChannel, "I have no permission to join this channel.");
    } catch (Exception e) {
      new ErrorReporter(client).report(e);
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
      // subtract some guilds to leave a buffer for a restart
      int shards = (int) ((guilds - 500) / 2500) + 1;

      ClientBuilder clientBuilder = new ClientBuilder(); // Creates the ClientBuilder instance
      clientBuilder.withToken(token); // Adds the login info to the builder
      clientBuilder.withShards(shards);

      return clientBuilder.build(); // Creates the client instance

    } else return client;
  }

  public enum Politician {
    trump,
    clinton,
    merkel
  }
}
