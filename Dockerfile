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
RUN cat > /discord-bots/trump-bot/stats.json  <<'EOF' \
{ \
  "guildCount": 0, \
  "shards": 0 \
} \
EOF

# create config file
RUN cat > /discord-bots/trump-bot/config.js <<'EOF' \
module.exports = { \
    // https://discordapp.com/developers/applications/me \
    token: "auth_token_here" \
  }; \
EOF

# create init-script and make it executable
COPY entry.sh /entry.sh
RUN chmod +x /entry.sh

# convert entry script to unix line endings if needed
RUN dos2unix /entry.sh

ENTRYPOINT ["/entry.sh"]
