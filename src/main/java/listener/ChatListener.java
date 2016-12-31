package listener;

import main.Main;
import sx.blah.discord.api.events.IListener;
import sx.blah.discord.handle.impl.events.MessageReceivedEvent;
import sx.blah.discord.handle.obj.*;
import sx.blah.discord.util.DiscordException;
import sx.blah.discord.util.RateLimitException;
import util.ErrorReporter;
import util.SupportRequest;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

public class ChatListener implements IListener<MessageReceivedEvent> {
  
  private final String helptext =
      "Trump-Bot usage:\n```\n"
          + "!trump  \t[options]\n"
          + "!clinton\t[options]\n"
          + "!merkel \t[options]\n"
          + "\nOptions:\n\n"
          + "  -help, -h      \tShow this message\n"
          + "  -c <channel>   \tSpecify voice channel to join\n"
          + "  -f <pattern>   \tSpecify sound file to play. Wildcard: *\n"
          + "  -sounds        \tList all available sound files\n"
          + "  -stats         \tPrint a short summary of statistics\n"
          + "  -leave         \tForce-leave the current channel\n"
          + "  -contact <text>\tContact the creator (bugs, feedback, etc)```";
  
  private final String helptext_admin =
      "Trump-Bot admin usage:\n```\n"
          + "!trump-admin\t[options]\n"
          + "\nOptions:\n\n"
          + "  -h, --help                         \tShow this message\n"
          + "  --list-tickets                     \tList open tickets\n"
          + "  --close-ticket <ticketID>          \tClose ticket\n"
          + "  --reply-ticket <ticketID> <message>\tReply to ticket```";
  
  @Override
  public void handle(MessageReceivedEvent event) {
    
    // get message content
    String message = event.getMessage().getContent().toLowerCase();
    IUser user = event.getMessage().getAuthor();
    
    // support-console
    if (message.startsWith("!trump-admin") && user.getID().equals(Main.getInstance().adminID)) {
      handleAdmin(event);
      return;
    }
    
    Main.Politician politician = null;
    
    // set politician
    for (Main.Politician value : Main.Politician.values()) {
      if (message.startsWith("!" + value.name())) {
        politician = value;
      }
    }
    
    if (politician != null) {
      handleDefault(event, politician);
    }
  }
  
  private void handleAdmin(MessageReceivedEvent event) {
    
    // get message content
    String message = event.getMessage().getContent().toLowerCase();
    
    message = message.replace("!trump-admin ", "");
    
    boolean hasArguments = !message.equals("");
    
    if (hasArguments) {
      
      for (String argument : getArguments(message)) {
        
        if (argument.equals("--list-tickets")) {
          new ErrorReporter(event.getClient())
              .report(Main.getInstance().supportRequests.createReport());
          return;
        } else if (argument.startsWith("--close-ticket ")) {
          String value = argument.replace("--close-ticket ", "");
          
          String[] ticket_ids = value.split(" ");
          
          for (String ticket_id : ticket_ids) {
            try {
              int id = Integer.parseInt(ticket_id);
              
              Main.getInstance().supportRequests.removeID(id);
            } catch (NumberFormatException e) {
              new ErrorReporter(event.getClient()).report(e);
              return;
            }
          }
          Main.getInstance().saveSupportRequests();
          
        } else if (argument.startsWith("--reply-ticket ")) {
          String value = argument.replace("--reply-ticket ", "");
          
          try {
            String ticket_id = value.substring(0, value.indexOf(" "));
            String reply = value.substring(value.indexOf(" ") + 1);
            
            SupportRequest request =
                Main.getInstance().supportRequests.getFromID(Integer.parseInt(ticket_id));
            
            if (request != null) {
              request.reply(reply, event.getClient(), true);
            } else {
              Main.getInstance()
                  .writeMessage(
                      event.getMessage().getChannel(), "No parameter for content detected.");
            }
          } catch (StringIndexOutOfBoundsException e) {
            Main.getInstance().writeMessage(event.getMessage().getChannel(), "Invalid ticket-id.");
          }
          
        } else {
          printAdminHelp(event.getMessage().getChannel());
        }
      }
    }
  }
  
  private String[] getArguments(String message) {
    return message.replace(" -", " --").split(" -");
  }
  
  private void handleDefault(MessageReceivedEvent event, Main.Politician politician) {
    
    // get message content
    String message = event.getMessage().getContent().toLowerCase();
    IUser user = event.getMessage().getAuthor();
  
    message = message.replace("!" + politician.name() + " ", "");
  
    boolean hasArguments = !message.equals("");

    /*
       INITIALISATION

       we have to initialise
       - text-channel to respond with feedback if needed
       - sound-file to play (requires politician)
       - voice-channel to connect to
    */
    
    // init text channel
    IChannel textChannel = event.getMessage().getChannel();
    
    // init sound with random
    ArrayList<URL> soundFiles = new ArrayList<>();
    
    // init voice channel with author's
    IVoiceChannel voiceChannel = null;
    List<IVoiceChannel> voiceChannels = user.getConnectedVoiceChannels();
    
    if (voiceChannels.size() > 0) {
      voiceChannel = voiceChannels.get(0);
    }

    /*
       HANDLE PARAMETERS
    */
    
    // has parameters
    if (hasArguments) {
      
      for (String argument : getArguments(message)) {
        
        // help-message
        if (argument.equals("--help") || argument.equals("-h")) {

          /*
          print help and exit
           */
          printHelp(textChannel);
          return;

          /*
          custom channel
           */
        } else if (argument.startsWith("--channel ") || argument.startsWith("-c ")) {
          
          String value = argument.substring(argument.indexOf(" ") + 1);
          boolean found = false;
          
          IGuild guild = event.getMessage().getGuild();
          
          // iterate through available channels to find the specified channel
          for (IVoiceChannel candidate : guild.getVoiceChannels()) {
            
            // set voicechannel
            if (candidate.getName().toLowerCase().equals(value)) {
              voiceChannel = candidate;
              found = true;
            }
          }

          /*
          if no channel was found
           */
          if (!found) {
            
            // invalid channel, report and exit
            Main.getInstance()
                .writeMessage(
                    textChannel,
                    "I could not find the voice-channel you specified. Select one of the following:\n"
                        + getVoiceChannelList(guild));
            return;
          }

          /*
          custom sound file
           */
        } else if (argument.startsWith("--file ") || argument.startsWith("-f ")) {
          
          String value = argument.substring(argument.indexOf(" ") + 1);
          
          // make * as wildcard work
          String pattern = ("\\Q" + value + "\\E").replace("*", "\\E.*\\Q");
          
          // get list of matching files
          ArrayList<URL> candidates = getAudio(politician, pattern);
          
          if (candidates.size() == 0) {
            
            // no match found, cant continue. report and exit
            Main.getInstance()
                .writeMessage(
                    textChannel, "I could not find a filename matching the pattern you specified.");
            return;
            
          } else if (candidates.size() > 1) {
            
            // multiple matches
            String matches = fileListToString(candidates);
            
            Main.getInstance()
                .writeMessage(
                    textChannel,
                    "I found multiple audios matching your pattern. Please select one of the following:\n\n"
                        + matches);
            return;
            
          } else {
            
            // set the only match as desired audio
            soundFiles.add(candidates.get(0));
          }

          /*
           list all sounds
          */
        } else if (argument.equals("--sounds")) {
          
          File audio = new File("audio/" + politician.name());
          
          File[] files = audio.listFiles();
          
          String matches = fileListToString(files);
          
          Main.getInstance()
              .writeMessage(textChannel, "Following files are available:\n\n" + matches);
          return;

          /*
           print stats to channel
          */
        } else if (argument.equals("-stats") || argument.equals("-statistics")) {
          printStats(textChannel);
          return;
        } else if (argument.startsWith("--report ") || argument.equals("-r ")) {
          String value = argument.substring(argument.indexOf(" ") + 1);
          
          SupportRequest request = new SupportRequest(user.getID(), value);
          
          Main.getInstance().supportRequests.add(request);
          
          Main.getInstance().saveSupportRequests();
          
          try {
            IPrivateChannel privateChannel = user.getOrCreatePMChannel();
            
            Main.getInstance()
                .writeMessage(
                    privateChannel,
                    "You have successfully opened ticket #"
                        + request.ticket_id
                        + ".\nYou can add comments to it by typing '!trump -ticket "
                        + request.ticket_id
                        + " <message>'");
          } catch (DiscordException | RateLimitException e) {
            e.printStackTrace();
          }
          
          new ErrorReporter(event.getClient())
              .report(
                  "A new support-request was added:\n"
                      + "```\n"
                      + request.ticket_id
                      + ":\n"
                      + request.message
                      + "\n```");
          
          return;
        } else if (argument.startsWith("--ticket ") || argument.startsWith("-t ")) {
          
          String value = argument.substring(argument.indexOf(" ") + 1);
          
          // two arguments given (ID and MESSAGE)
          if (!value.contains(" ")) {
            Main.getInstance()
                .writeMessage(event.getMessage().getChannel(), "You must specify <ticket-id> and <message>, in that order.");
            return;
          }
          
          String ticket_id = value.substring(0, value.indexOf(" "));
          String reply = value.substring(value.indexOf(" ") + 1);
          
          SupportRequest request =
              Main.getInstance().supportRequests.getFromID(Integer.parseInt(ticket_id));
          
          if (request == null) {
            Main.getInstance()
                .writeMessage(event.getMessage().getChannel(), "Invalid ticket-id.");
            return;
          }
          
          if (!request.user_id.equals(user.getID())) {
            Main.getInstance()
                .writeMessage(event.getMessage().getChannel(), "You have no permission to reply to this ticket.");
            return;
          }
          
          request.reply(reply, event.getClient(), false);
          
          return;
        } else if (argument.startsWith("-support")) {
          new ErrorReporter(event.getClient())
              .report(Main.getInstance().supportRequests.createReport());
        } else if (argument.equals("-leave")) {
          Main.getInstance().leaveVoiceChannel(event.getMessage().getGuild());
          return;
        } else {
          // unknown argument, print help and exit
          printHelp("You entered an unknown argument.", textChannel);
          return;
        }
      }
    }
    
    // Abort if bot is busy
    // located here to allow parameters like '-stats' or '-help' to be displayed while bot is active
    
    IVoiceChannel usedChannel = Main.getInstance().isBusyInGuild(event.getMessage().getGuild());
    if (usedChannel != null) {
      Main.getInstance()
          .writeMessage(
              textChannel, "I am currently needed in Channel '" + usedChannel.getName() + "'.");
      return;
    }
    
    if (voiceChannel == null) {
      Main.getInstance()
          .writeMessage(
              event.getMessage().getChannel(),
              "Look, you have to be in a voicechannel (or specify one by adding '-c:<name of channel>' to do this.");
      return;
    }
    
    if (soundFiles.isEmpty()) {
      soundFiles.add(getRandomAudio(politician));
    }
    
    Main.getInstance().playAudio(voiceChannel, textChannel, soundFiles, user);
  }
  
  private void printStats(IChannel textChannel) {
    
    SimpleDateFormat sdf = new SimpleDateFormat("MMM dd,yyyy HH:mm");
    Date startDate = new Date(Main.getInstance().startedInMillis);
    
    String output =
        "Current stats:\n"
            + "```"
            + "Activated:                \t"
            + readStat("played")
            + " times\n"
            + "Online since:             \t"
            + sdf.format(startDate)
            + "\n"
            + "Uptime of current session:\t"
            + Main.getInstance().getUptime()
            + "\n"
            + "Currently active guilds:  \t"
            + readStat("guildCount")
            + "```";
    
    Main.getInstance().writeMessage(textChannel, output);
  }
  
  private String readStat(String stat) {
    
    return Main.getInstance().getStatsAsJson().get(stat).getAsString();
  }
  
  private String fileListToString(File[] files) {
    // multiple matches
    String matches = "";
    for (File file : files) {
      matches += file.getName() + "\n";
    }
    
    return matches.trim();
  }
  
  private String fileListToString(ArrayList<URL> files) {
    // multiple matches
    String matches = "";
    for (URL file : files) {
      matches += file + "\n";
    }
    
    return matches.trim();
  }
  
  private void printAdminHelp(IChannel textChannel) {
    Main.getInstance().writeMessage(textChannel, helptext_admin);
  }
  
  private void printHelp(String intro, IChannel textChannel) {
    Main.getInstance().writeMessage(textChannel, intro + "\n\n" + helptext);
  }
  
  private void printHelp(IChannel textChannel) {
    Main.getInstance().writeMessage(textChannel, helptext);
  }
  
  private String getVoiceChannelList(IGuild guild) {
    
    String list = "";
    
    for (IVoiceChannel channel : guild.getVoiceChannels()) {
      list += channel.getName() + "\n";
    }
    
    return list;
  }
  
  private ArrayList<URL> getAudio(Main.Politician politician, String pattern) {
    
    File audio = new File("audio/" + politician.name());
    
    File[] files = audio.listFiles();
    ArrayList<URL> candidates = new ArrayList<>();
    
    // iterate through available files to find matching ones
    if (files != null) {
      for (File candidate : files) {
        
        // get matches
        if (candidate.getName().matches(pattern)) {
          try {
            candidates.add(candidate.toURI().toURL());
          } catch (MalformedURLException e) {
            e.printStackTrace();
          }
        }
      }
    }
    
    return candidates;
  }
  
  private URL getRandomAudio(Main.Politician politician) {
    
    // set path for selected politician
    File audio = new File("audio/" + politician.name());
    
    URL soundFile = null;
    
    // pick a random audio
    File[] files = audio.listFiles();
    
    if (files != null && files.length > 0) {
      int random = new Random().nextInt(files.length);
      
      try {
        soundFile = files[random].toURI().toURL();
      } catch (MalformedURLException e) {
        e.printStackTrace();
      }
    }
    
    return soundFile;
  }
}
