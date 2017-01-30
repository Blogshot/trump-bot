module.exports = {

    log: function (message) {
        var date = new Date();

        // "[30.1.2017 10:44:19] - Here is your message."
        console.log(
            "[" + date.toLocaleDateString() + " " + date.toLocaleTimeString() + "] " +
            message
        );
    }
}