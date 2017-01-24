module.exports = {

  writeMessage: function (channel, text) {
    channel.sendMessage(text).catch(error => {
      // split logging to get whole response. error.toString just gets the message
      console.log("Error sending message:\n");
      console.log(error);
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