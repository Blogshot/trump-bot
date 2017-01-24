## INVITE
https://discordapp.com/oauth2/authorize?client_id=247869756609265664&scope=bot&permissions=0

## USAGE/DEPLOYMENT

### Install environment
```
curl -sL https://deb.nodesource.com/setup_6.x | sudo -E bash -
sudo apt-get install -y nodejs
```

`npm install discord.js node-opus --save`

### Create a file named `stats.json` to store statistics:
```
{
  "played": 0,
  "guildCount": 0
}
```

### Create a file named `config.js` to store configuration data:
```
module.exports = {
  // https://discordapp.com/developers/applications/me
  token: "<bot-token>"
};
```

### Start the bot

#### Without sharding
`node trump-bot.js`

#### With sharding
`node sharder.js`
