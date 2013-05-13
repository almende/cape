<!-- Uses markdown syntax for neat display at github -->

# CAPE
CAPE stands for context-aware programming environment.

## What does it do?
CAPE offers an interface both in the cloud and on mobile devices to contact or notify a user via any available media, access real-time status information of a user, and retrieve past and future planning of the user. The information sources can be for example calendar information, sensor information, and information on social media.

![CAPE overview](https://github.com/almende/cape/exp/capescheme.jpg "Schematic overview of the CAPE framework")

## Is it good?
The CAPE framework bridges the gap between cloud and mobile devices. Via a low-level messagebus it synchronizes the users state and available media channels between mobile and cloud representation of the user. 
The CAPE framework makes it possible to contact a user from both mobile or cloud, via any available media such as chat, mail, phone, web app, or social media. In the same way, any of the users contacts can be contacted from everywhere via any media channel. The state of a user or any of its contacts can be retrieved immediately via the API.

The CAPE API contains a number of layers:
* The message-bus is a thin, low-level layer which provides a channel between the various CAPE instances, running on a mobile device, in the cloud, on a desktop application, or in a web app.
* The state-bus is built on top of the message-bus, and contains the state of the user. The state is synchronized over the various CAPE instances that are running for a user in both his mobile devices and in the cloud or on his desktop.
* The information is interpreted from information collected from various sources like calendar, sensors, and social networks.
* The Dialog offers an CAPE to contact a user via any available medium such as chat, mail, phone, notifications, or social media.
* Every user has a CAPE instance running in the cloud, which can be accessed even if the user is not online via a mobile phone or web application.
* Every user has a CAPE instance running on his mobile devices.

## What are the alternatives?
These frameworks are developed only recently as it targets newer types of mobile devices. Other such frameworks 
(in different application areas) are [AWARE](http://www.awareframework.com/home/) and [COMPASS](http://link.springer.com/content/pdf/10.1007/978-3-540-27780-4_27.pdf) 

## An example
Example code of a login and calls to several avaible methods:

cape.login("Jos", "********")

location = cape.getState("location")
location2 = cape.getState("location", "Steven")
cape.onStateChange("location", function (state) {
   // update location on screen. ring alarm bell. whatever.
})

contacts = cape.getContacts()
cape.addContact({"name": "Hans", ...})

available = cape.sendMessage("Can you be available the next hour?",
["yes", "no"])
cape.sendNotification("You just won the jackpot")
cape.setState("mood", "happy")

history = cape.getState("availability",
"from": "2012-10-07", "to": "2012-10-14"}) // enter future dates too...

## Where can I read more?
* [University of Minnesota (on context aware programming)](http://www-users.cs.umn.edu/~dkulk/TSE.pdf)

## Copyrights
The copyrights (2013) belong to:

- Author: Ludo Stellingwerff
- Author: Jos de Jong
- Almende B.V., http://www.almende.com
- Rotterdam, The Netherlands
