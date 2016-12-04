### Invite to Server
To invite an already working bot hosted by me by visiting:
https://discordapp.com/oauth2/authorize?client_id=247869756609265664&scope=bot&permissions=0


Write `!trump` (or any other command listed below) in any channel to trigger the bot. It will join the Voicechannel of the author and play trump quotes.

#### Commands
|Command|Action|
|:---|:---|
|!trump, !merkel|Invoke the bot with specified politician|
|-help, -h|Show help-message|
|-c:\<channel>|Send the bot to a specified channel|
|-f:\<pattern-of-file>|Specify sound file to use. Wildcards (*) are supported|
|-f:*, -sounds|List all available sound files|

#### Examples

Play 'let-em-talk.mp3' by using wildcards:  
`!trump -f:*let-em*mp3` or  
`!trump -f:let*talk*`

#####
Send the bot to a voice-channel named 'General' and play 'let-em-talk.mp3':  
`!trump -c:General -f:let-em-talk.mp3`

### Installation
If you want to host your own instance, follow below instructions.

Get a copy of the bot:   
`git clone https://github.com/Blogshot/Trump-Bot.git`

Then, just execute the .jar file in the bin/-directory with a token-parameter. The audio-folder must be in the working directory.
```
#!/bin/bash  

# get token from https://discordapp.com/developers/applications/me
cd /root/discord-bots/Trump-Bot/ && java -jar bin/Trump-Bot.jar --token=<token here> &
```

##### Logging
The log can be found in the working directory of the .jar file.