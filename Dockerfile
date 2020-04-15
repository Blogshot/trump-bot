FROM node:12
LABEL maintainer knottsascha@gmail.com

# install npm dependencies
RUN npm install discord.js @discordjs/opus

# prepare environment
RUN mkdir -p /discord-bots/trump-bot
WORKDIR /discord-bots/trump-bot

# copy source code into container
COPY . .

# create stats file
RUN echo '{ "guildCount": 0, "shards": 0 }' > stats.json

# Invoke build with "docker build --build-arg token=<token> -t trump-bot ."
ARG token

# create config file
RUN echo "module.exports = { token: \"$token\" };" > config.js

ENTRYPOINT node sharder.js
