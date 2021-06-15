# INVITE
https://discordapp.com/oauth2/authorize?client_id=247869756609265664&scope=bot&permissions=0

# USAGE/DEPLOYMENT

## Get a bot token for your installation
https://discordapp.com/developers/applications/me

## Docker
Copy the Dockerfile to your local system with Docker installed and build the image like this:  
`docker build --build-arg token=<token> -t trump-bot .`

Afterwards, run the image via executing:  
`docker run --name trump-bot -d trump-bot`

Your container should come up and start deploying shards. You can view it's progress via:
`docker logs -f trump-bot`

## Custom environment installation

### Download/Clone the bot
`git clone https://github.com/Blogshot/trump-bot.git`  

### Install Nodejs
https://nodejs.org/en/download/

```
curl -sL https://deb.nodesource.com/setup_14.x | sudo -E bash -
sudo apt-get install -y nodejs
```

### Install FFMPEG.

#### Debian/Ubuntu
`apt-get install ffmpeg`

#### Windows
[edit your environment variables](http://adaptivesamples.com/how-to-install-ffmpeg-on-windows/)

#### Other OS
Download the appropriate package from here: https://ffmpeg.org/download.html

### Create a file named `stats.json` to store statistics
```
{
  "guildCount": 0,
  "shards": 0
}
```

### Create a file named `config.js` to store configuration data (the bot-token)
```
module.exports = {
  token: "<bot-token>"
};
```

### Initialize NPM
`npm init`  

### Install basic packages
`npm install --global --production windows-build-tools` (Windows only)  
`npm install 

### Start the bot

#### Without sharding
`node bot.js`

#### With sharding
`node sharder.js`

### Other Requirements ###
To be able to interact with the bot, it needs to have the following permissions on at least one text channel:
* Read messages
* Write messages
* Embed links

Also, the bot should be permitted to use voice.
