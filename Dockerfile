FROM node:8
LABEL maintainer knottsascha@gmail.com

ENV token=auth_token_here

# install apt dependencies
RUN apt update && apt install -y \
  git \
  dos2unix \
  nodejs \
  ffmpeg \
  && rm -rf /var/lib/apt/lists/*

# install npm dependencies
RUN npm install discord.js node-opus@0.2.9

# prepare environment
RUN mkdir -p /discord-bots/trump-bot
RUN cd /discord-bots \
  && git clone https://github.com/Blogshot/trump-bot.git \
  && cd ./trump-bot \
  && git checkout js_conversion

# create stats file
RUN echo '{ "guildCount": 0, "shards": 0 }' > /discord-bots/trump-bot/stats.json  

# create config file
RUN echo 'module.exports = { token: "auth_token_here" };' > /discord-bots/trump-bot/config.js

# create init-script and make it executable
RUN echo 'node /discord-bots/trump-bot/bot.js' > /entry.sh
RUN chmod +x /entry.sh

# convert entry script to unix line endings if needed
RUN dos2unix /entry.sh

ENTRYPOINT ["/entry.sh"]
