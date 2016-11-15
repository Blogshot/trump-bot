import sx.blah.discord.api.events.IListener;
import sx.blah.discord.util.audio.events.TrackFinishEvent;

public class TrackFinishedListener implements IListener<TrackFinishEvent> {
  
  @Override
  public void handle(TrackFinishEvent event) { // This is called when the ReadyEvent is dispatched
    
    event.getClient().getConnectedVoiceChannels().get(0).leave();
    
    Main.isBusy = false;
  }
}