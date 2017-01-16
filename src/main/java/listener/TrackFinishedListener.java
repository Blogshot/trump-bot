package listener;

import main.Main;
import sx.blah.discord.api.events.IListener;
import sx.blah.discord.util.audio.events.TrackFinishEvent;

public class TrackFinishedListener implements IListener<TrackFinishEvent> {

  @Override
  public void handle(TrackFinishEvent event) {

    Main.getInstance().handleTrackFinished(event.getPlayer());
  }
}
