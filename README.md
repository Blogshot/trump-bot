# INVITE
https://discordapp.com/oauth2/authorize?client_id=247869756609265664&scope=bot&permissions=0

# USAGE

### Create a file named `stats.json` to store statistics:
```
{
  "played": o,
  "guildCount": 0
}
```

### Create a file named "config.js" to store configuration data:
```
module.exports = {
  // the token - https://discordapp.com/developers/applications/me
  token: "<bot-token>"
};
```

### Start the bot

#### Without sharding
`node trump-bot.js`

#### With sharding
`node sharder.js`
