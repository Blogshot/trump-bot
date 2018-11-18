## INVITE
https://discordapp.com/oauth2/authorize?client_id=247869756609265664&scope=bot&permissions=0

## USAGE/DEPLOYMENT

### Install environment

#### The bot itself
`git clone https://github.com/Blogshot/trump-bot.git`  
`git checkout js_conversion` 

#### Nodejs
https://nodejs.org/en/download/

```
curl -sL https://deb.nodesource.com/setup_8.x | sudo -E bash -
sudo apt-get install -y nodejs
```

#### Python 2.7 (3.x is not supported by some dependencies!)
https://www.python.org/downloads/release/python-2714/


### Download and Install FFMPEG. 

#### Debian/Ubuntu
`apt-get install ffmpeg`

#### Alternative
Download the appropriate package from here: https://ffmpeg.org/download.html

If you are a Windows user, you will have to [edit your environment variables](http://adaptivesamples.com/how-to-install-ffmpeg-on-windows/).

### Create a file named `stats.json` to store statistics
```
{
  "guildCount": 0,
  "shards": 0
}
```

### Create a file named `config.js` to store configuration data (currently only the token)
```
module.exports = {
  // https://discordapp.com/developers/applications/me
  token: "<bot-token>"
};
```

### Create a file named `package.json` to enable npm installation
`npm init`  

### Install basic packages
`npm install --global --production windows-build-tools` (Windows only)  
`npm install discord.js node-opus@0.2.9` 


#### Optional
For best experience, you may choose to install the following packages:  
`npm install bufferutil@3.0.5 (much faster websocket connection)

### Start the bot

#### Without sharding
`node bot.js`

#### With sharding
`node sharder.js`
