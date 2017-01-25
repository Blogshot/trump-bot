module.exports = {

  writeMessage: function (channel, text) {
    channel.sendMessage(text).catch(error => {

      // message could not be send, try to communicate using another channel
      var channels = Array.from(channel.guild.channels.values());

      // try first channel (i=0)
      checkChannel(channels,
        text + "\n\n(This message was meant to be sent to channel '"
        + channel.name + "', but I don't have permission to write there)", 0)
    });
  },

  helpText: "Trump-Bot usage:\n```\n"
  + "!trump   [options]\n"
  + "!clinton [options]\n"
  + "!merkel  [options]\n"
  + "\nOptions:\n\n"
  + "  -h, --help  \tShow this message\n"
  + "  -c <channel>\tSpecify voice channel to join\n"
  + "  -f <pattern>\tSpecify sound file to play. Wildcard: *\n"
  + "  --sounds    \tList all available sound files\n"
  + "  --stats     \tPrint a short summary of statistics\n"
  + "  --leave     \tForce-leave the current channel\n\n"
  + "Examples:\n"
  + "!trump -f big-china.mp3 -c General\n"
  + "!trump -f big-*.mp3 -c General\n"
  + "```\n"
  + "Keeping the server running costs money, please consider donating. Visit https://trump.knotti.org for more info.\n\n"
  + "If you need assistance or want to share feedback, contact Bloggi#7559 or join the support-discord: https://discord.gg/MzfyfTm"

}

function checkChannel(channels, text, i) {

  var channel = channels[i];

  // make sure it's a text-channel
  if (channel.type == "text") {

    // send message again
    channel.sendMessage(text).catch(error => {
      // if message could not be send, try next channel
      checkChannel(channels, text, i + 1);
    });
  } else {
    // if its not a text-channel, skip it
    checkChannel(channels, text, i + 1);
  }
}