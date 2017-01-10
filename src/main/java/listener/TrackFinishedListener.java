package listener;

import main.Main;
import sx.blah.discord.api.events.IListener;
import sx.blah.discord.util.audio.events.TrackFinishEvent;

public class TrackFinishedListener implements IListener<TrackFinishEvent> {

  @Override
  public void handle(TrackFinishEvent event) {

    /*
     Wait a few ms to fix 'java.net.SocketException: Socket is closed'.
     (Audio-packet length is 20ms, use 21ms)
      */
    try {
      Thread.sleep(21);
    } catch (InterruptedException ignored) {
    }

    // Leave current channel after audio finished
    Main.getInstance().leaveVoiceChannel(event.getPlayer().getGuild());

    // Clean memory to countermeasure memory leak
    // https://github.com/austinv11/Discord4J/issues/191)
    event.getPlayer().clean();
  
    Main.log("Left voice channel and cleaned guild's player.");

    // Update stats
    Main.getInstance().played++;
    Main.getInstance().saveStats();
  
    Main.log("Increased 'played' and saved stats.");
  }
}
