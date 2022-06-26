FROM node:16
LABEL maintainer knottsascha@gmail.com

# install apt dependencies
RUN apt update && apt install -y \
  git \
  dos2unix \
  nano \
  htop \
  ffmpeg \
  && rm -rf /var/lib/apt/lists/*

# prepare environment
RUN mkdir -p /bot/
WORKDIR /bot

COPY . .

# install npm dependencies
RUN npm install

# create stats file
RUN echo '{ "guildCount": 0, "shards": 1 }' > stats.json

# Invoke build with "docker build --build-arg token=<token> -t trump-bot ."
ARG token

# create config file
RUN echo "module.exports = { token: \"$token\" };" > config.js

ENTRYPOINT node sharder.js
