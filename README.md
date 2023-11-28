# Mqtt_broker


## Introduce

##### An integrated MQTT Broker, message subscription server





## Features

##### This interface and function are very simple, just open or close the Mqtt service







## Function

 <img src="http://pic.song0123.com/img/Mqtt_broker.png" alt="Mqtt_broker" style="zoom:50%;" />



##### Host cannot be modified, it is the local IP read. Port can be customized. If the following identity authentication is turned on, identity information needs to be verified when connecting to the service. If not enabled, no authentication is required.





## Special Note 

##### If the server and client operate on the same device, you can use the host 127.0.0.1 to connect, but the device may need to not be connected to the Internet (disconnect WiFi)




## Sources

##### Quoting the code base below

``` implementation 'io.moquette:moquette-netty-parser:0.9'
    implementation 'io.moquette:moquette-broker:0.11'
    implementation 'io.moquette:moquette-parser-commons:0.8.1'
```





