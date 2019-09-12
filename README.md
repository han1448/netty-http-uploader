# Netty-http-uploader


## http-receiver-sample
Run receiver server
```
cd ~/netty-receiver-sample
mvn compile
```

```jshelllanguage
mvn exec:java -Dexec.mainClass="com.oppa.woodpecker.HttpFileServer" -Dexec.args="8080 /Users/seunghyunhan/Downloads/netty" -Dexec.classpathScope=runtime
```

## http-sender-sample
Send file to server
```java
HttpSender httpSender = new HttpSender("localhost", "8080", "/usr/local/file.txt");
httpSender.send();
```
